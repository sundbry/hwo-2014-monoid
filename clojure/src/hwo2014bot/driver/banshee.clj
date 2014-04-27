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
  (let [profile (:characterizer driver)
        dash (:dashboard driver)
        throttle (:throttle driver)]
    (list
      (fn [tick-0]
        (let [throttle-state (read-state throttle)
              dash-state (read-state dash)
              throttle-cf (char/calibrate-throttle ; throttle-out A-measured V-measured drag-coeff k-friction
                            (:throttle throttle-state)
                            (:acceleration dash-state)
                            (:velocity dash-state)
                            0.0
                            0.0)]
          (teach-calib profile :throttle throttle-cf))
        (message/ping tick-0))
      (fn [tick-1]
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
        (message/ping tick-1))
      (fn [tick-2]
        (let [calib-state (read-state profile)
              throttle-state (read-state throttle)
              dash-state (read-state dash)
              drag-cf (char/calibrate-drag ; throttle-out A-measured V-measured throttle-coeff k-friction
                        (:throttle throttle-state)
                        (:acceleration dash-state)
                        (:velocity dash-state)
                        (:throttle calib-state)
                        (:k-friction calib-state))]
          (teach-calib profile :drag drag-cf))
        (message/ping tick-2)))))

(defrecord Driver [config track dashboard throttle characterizer driver-state tick-chan]
  component/Lifecycle
  
  (start [this]
    (set-mode throttle :manual)
    (new-setpoint throttle 1.0)
    #_(go (try (loop
               [routine (calib-process this)]
               (when-let [tick-num (<! tick-chan)]
                 (if-let [step (first routine)]
                   (>! tick-chan (apply step tick-num))
                   (>! tick-chan (message/ping tick-num)))
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
