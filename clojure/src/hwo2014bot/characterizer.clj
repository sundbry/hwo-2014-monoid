(ns hwo2014bot.characterizer
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [chan go go-loop >! <! close! pipe]]
            [com.stuartsierra.component :as component]
            [hamakar.circular-buffer :refer [cbuf]]
            [hwo2014bot.protocol :refer :all]
            [hwo2014bot.constant :as const]))

(defrecord PerformanceCharacterizer [config dashboard throttle output-chan]
  component/Lifecycle
  
  (start [this]
    (pipe (output-channel dashboard) output-chan)
    this)
  
  (stop [this]
    this)
  
  PPassiveComponent
  
  (read-state [this]
    {})
  
  (output-channel [this] output-chan)
  
  PCharacterization
  
  ;; Estimate the lower-bound acceleration output, given throttle output and initial velocity.
  ;; Useful for projecting when to speed-up
  (estimate-lower-accel [this throttle-out V0]
    nil
    )
  
  ;; Estimate the upper-bound acceleration output, given throttle output and initial velocity.
  ;; Useful for projecting safe brake distance
  (estimate-upper-accel [this throttle-out V0]
    nil
    )
  
  )

(defn new-characterizer [conf]
  (map->PerformanceCharacterizer
    {:config conf
     :output-chan (chan)}))
 