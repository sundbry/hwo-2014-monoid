(ns hwo2014bot.track
  (:require [clojure.tools.logging :as log]
            [[com.stuartsierra.component :as component]
            [hwo2014bot.protocol :refer :all]))

(defn determine-position [for-track car-position]
  "Determine the middle-lane track position from a car piece position."
  (throw (UnsupportedOperationException. "Not implemented")))

(defn- new-track-thread [track]
  (thread
    (let [trac-desc (<!! (:input-chan track))]
      
          
    (loop [positions (<!! (:input-chan track))]
      (when positions
  )

(defrecord Track [input-chan proc]
  component/Lifecycle
  
  (start [this] 
    (assoc this :proc (new-track-thread this)))
           
  (stop [this]     
    (close! input-chan)
    (<!! proc) ; block until main loop closes
    (assoc this :proc nil))

) ; end record

(defn new-track []
  (map->Track 
    {:input-chan (chan)}))
 