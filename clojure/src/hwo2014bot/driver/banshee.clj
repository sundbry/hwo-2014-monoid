(ns hwo2014bot.driver.banshee
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [chan go go-loop >!! <! >! <!! close!]]
            [com.stuartsierra.component :as component]
            [hwo2014bot.protocol :refer :all]
            [hwo2014bot.message :as message]
            [hwo2014bot.characterizer :as char]))

;;; Banshee AI features:
;;; - Quick throttle calibration process

(defn calib-process [driver]
  ; 1. Calibrate accel (CD = 0, CF = 0) for period [0, 1]
  ; 2. Calibrate friction (CD = 0) (y-intercept intercept for period [1, 2]
  ; 3. Calibrate drag [2, 3] with known accel, friction
  (let [config (:config driver)
        profile (:characterizer driver)
        dash (:dashboard driver)
        throttle (:throttle driver)]
    (concat (list
      (fn [tick-n]
        (set-mode throttle :manual)
        (new-setpoint throttle (:set-throttle config))
        (message/throttle (:throttle (tick throttle tick-n)) tick-n))
      (fn [tick-n]
        (let [throttle-state (read-state throttle)
              dash-state (read-state dash)
              throttle-cf (char/calibrate-throttle ; throttle-out A-measured V-measured drag-coeff k-friction
                            (:throttle throttle-state)
                            (:acceleration dash-state)
                            (:velocity dash-state)
                            0.0
                            0.0)]
          (teach-calib profile :throttle throttle-cf))
        (message/throttle (:throttle (tick throttle tick-n)) tick-n))
      (fn [tick-n]
        (let [calib-state (read-state profile)
              throttle-state (read-state throttle)
              dash-state (read-state dash)
              friction-cf (char/calibrate-kinetic-friction ; throttle-out A-measured V-measured throttle-coeff drag-coeff
                            (:throttle throttle-state)
                            (:acceleration dash-state)
                            (:velocity dash-state)
                            (:throttle calib-state)
                            0.0)]
          (teach-calib profile :k-friction friction-cf))
        (message/ping tick-n))
      (fn [tick-n]
        (let [calib-state (read-state profile)
              throttle-state (read-state throttle)
              dash-state (read-state dash)
              drag-cf (char/calibrate-drag ; throttle-out A-measured V-measured throttle-coeff k-friction
                        (:throttle throttle-state)
                        (:acceleration dash-state)
                        (:velocity dash-state)
                        (:throttle calib-state)
                        (:k-friction calib-state))]
          (teach-calib profile :drag drag-cf)        
          (auto-cal profile true)) ; enable auto cal after
        (message/ping tick-n)))
      #_(repeat 100 (fn [tick-n] (message/ping tick-n)))
      #_(list ; calibrate deceleration
        (fn [tick-n]        
          (new-setpoint throttle (/ (:set-throttle config) 2))
          (message/throttle (:throttle (tick throttle tick-n)) tick-n)))
      #_(repeat 100 (fn [tick-n] (message/ping tick-n)))
      #_(list
        (fn [tick-n]        
          (new-setpoint throttle (:set-throttle config))
          (message/throttle (:throttle (tick throttle tick-n)) tick-n)))
      )))

(defn cruise [driver tick-n]
  (let [throttle (:throttle driver)
        track (:track driver)
        pos (my-position track)
        section (:section pos)
        fwd-turn (next-turn track pos)
        fwd-safe-vel
        (if fwd-turn
          (let [turn-distance (- (:offset fwd-turn)
                                 (:start-displacement pos))
                turn-velocity (:limit fwd-turn)]
            (if (>= turn-distance 0)
              (let [profile (:characterizer driver)
                    safe-vel (safe-velocity profile turn-distance turn-velocity)]
                (trace (:tracer driver) :banshee {:turn-distance turn-distance
                                                  :turn-velocity turn-velocity
                                                  :safe-velocity safe-vel})
                safe-vel)
              (do
                ;(log/debug "negative turn distance:" turn-distance)
                nil)))
          nil)
        safe-vel
        (if (:straight? section)
          fwd-safe-vel
          (if (nil? fwd-safe-vel)
            (:limit section)
            (min (:limit section) fwd-safe-vel)))]
    (if (nil? safe-vel)
      (do        
        (set-mode throttle :manual)
        (new-setpoint throttle 1.0))
      (do     
        (set-mode throttle :velocity)
        (new-setpoint throttle safe-vel)))
    (message/throttle (:throttle (tick throttle tick-n)) tick-n)))

(defrecord Driver [config track dashboard throttle characterizer driver-state tick-chan]
  component/Lifecycle
  
  (start [this]
    (go (try (loop
               [routine (calib-process this)]
               (when-let [tick-num (<! tick-chan)]
                 (if-let [step (first routine)]
                   (>! tick-chan (step tick-num))
                   (>! tick-chan (cruise this tick-num)))
                 (recur (next routine))))
          (catch Exception e
            (log/error e "Banshee driver error"))))
    this)
  
  (stop [this]
    (close! tick-chan)
    this)
  
  PActiveComponent
  
  ;; Responds with an appropriate game protocol message, or nil (ping)
  (tick [this tick-num]
    (>!! tick-chan tick-num)
    (<!! tick-chan))
    
) ; end record

(defn new-driver [opts]
  (map->Driver 
    (merge {:driver-state (ref {})
            :tick-chan (chan)}
           opts)))
