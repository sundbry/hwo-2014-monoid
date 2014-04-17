(ns hwo2014bot.dashboard
  (:require [clojure.tools.logging :as log]
            [[com.stuartsierra.component :as component]
            [hwo2014bot.protocol :refer :all]
            [hwo2014bot.constant :as const]))

(defn- new-dash-thread [dash]
  (thread
    (loop [positions (<!! (:input-chan dash))]
      (when positions
        (dosync
          (let [track-pos (track-position 
                instant
              {:velocity (inst-velocity (
                                                   
        (dosync 
          (alter (:state-buf dash) #(conj % 
        (recur (<!! (:input-chan dash)))))))

(defrecord Dashboard [config state-buf input-chan tracer proc] ; tracer gets injected 
  component/Lifecycle
  
  (start [this]
    (log/debug "Starting dashboard")
    (assoc this :proc (new-dash-thread this)))
  
  (stop [this]
    (log/debug "Stopping dashboard")
    (close! input-chan)
    (<!! proc) ; block until main loop closes
    (assoc this :proc nil))
  
  PReader
  (read [this]
    (peek @state-buf))
  
) ; end record

(defn new-dashboard [dash-conf]
  (map->Dashboard
    {:config dash-conf
     :input-chan (chan)
     :state-buf (ref (cbuf (:buffer dash-conf) :direction :right))}))
  
  #_{:tick -1
                  :velocity 0.0 ; Speedometer
                  :acceleration 0.0 ; Accelerometer
                  ;:displacement 0.0 ; Odometer
                  :track-position 0.0 ; middle-lane track position from start
                  :car-position 1 ; car position (1st, 2nd, ... nth)
                  }