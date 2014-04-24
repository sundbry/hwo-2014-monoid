(ns hwo2014bot.throttle
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [chan go >! <! close!]]
            [com.stuartsierra.component :as component]
            [hwo2014bot.protocol :refer :all]))
  
;; Throttle controller

(def ^:private reset-pid 
  {:previous-error 0
   :integral 0})

(defn limit-throttle [throttle-out]
  (max 0.0 (min 1.0 throttle-out)))

(defn perform-throttle-pid [ctrl feedback-val tick-num]
  ; http://en.wikipedia.org/wiki/PID_controller#Pseudocode
  (let [coeff (get ctrl (:mode ctrl))
        dt (- tick-num (:tick ctrl))
        error (- (:setpoint ctrl) feedback-val)
        integral (limit-throttle (+ (:integral ctrl) (* error dt))) ; prevents "windup"
        derivative (/ (- error (:previous-error ctrl)) dt)
        output (+ (* (:kP coeff) error)
                  (* (:kI coeff) integral)
                  (* (:kD coeff) derivative))]
    (merge ctrl
           {:throttle (limit-throttle output)
            :previous-error error
            :integral integral
            :tick tick-num})))

(defrecord ThrottleController [tracer dashboard config throttle-state]
  
  component/Lifecycle
  
  (start [this]
    this)
  
  (stop [this]
    this)
  
  PActiveComponent
  
  (tick [this tick-num]
    (dosync
      (alter throttle-state
             (fn [ctrl]
               (perform-throttle-pid 
                 ctrl
                 (get (read-state dashboard) (:mode ctrl))
                 tick-num)))))
  
  PController
  
  (new-setpoint [this set-val]
    (dosync
      (alter throttle-state assoc :setpoint set-val))
    this)
  
  (set-mode [this mode-kw]
    (dosync
      (when (not= mode-kw (:mode @throttle-state))
        (alter throttle-state merge {:mode mode-kw} reset-pid)))
    this)
  
)

(defn new-throttle [throttle-conf]
  (map->ThrottleController
    {:config throttle-conf
     :throttle-state (ref (merge 
                            {:tick -1
                             :setpoint 0
                             :throttle 0
                             :mode :velocity
                             :velocity {:kP 1.0 :kI 1.0 :kD 1.0}
                             :slip {:kP 1.0 :kI 1.0 :kD 1.0}}                            
                            reset-pid
                            throttle-conf))}))
