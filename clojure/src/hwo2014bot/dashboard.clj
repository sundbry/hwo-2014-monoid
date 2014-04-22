(ns hwo2014bot.dashboard
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [chan go go-loop >! <! close!]]
            [com.stuartsierra.component :as component]
            [hamakar.circular-buffer :refer [cbuf]]
            [hwo2014bot.protocol :refer :all]
            [hwo2014bot.constant :as const]))

(defrecord Dashboard [track tracer config output-chan state-buf] ; tracer, track injected 
  component/Lifecycle
  
  (start [this]
    (log/debug "Starting dashboard")
    (dosync
      (alter state-buf conj {:tick -1
                             :start-displacement 0.0
                             :velocity 0.0
                             :acceleration 0.0}))
    (go (try (loop []
      (when-let [cur-track (<! (:output-chan track))]
        (let [cur-tick (:tick cur-track)
              cur-pos (get (:cars cur-track) (:name this))
              cur-dash
              (dosync
                (let [ref-pos (peek @state-buf)
                      delta-disp (- (:start-displacement cur-pos)
                                    (:start-displacement ref-pos))
                      delta-t (- cur-tick
                                 (:tick ref-pos))
                      cur-velocity (/ delta-disp delta-t)
                      delta-v (- cur-velocity (:velocity ref-pos))
                      cur-accel (/ delta-v delta-t)
                      cur-state (merge cur-pos
                                       {:tick cur-tick
                                        :velocity cur-velocity
                                        :acceleration cur-accel})]
                  (alter state-buf conj cur-state)
                  cur-state))]
          (trace tracer :dashboard cur-dash)
          (>! output-chan cur-dash))
        (recur)))
      (catch Exception e
        (log/error e "Dashboard error"))))
    this)
  
  (stop [this]
    (log/debug "Stopping dashboard")
    (close! output-chan)
    this)
  
  ;PReader
  #_(read [this]
    (peek @state-buf))
  
) ; end record

(defn new-dashboard [car-name dash-conf]
  (map->Dashboard
    {:name car-name
     :config dash-conf
     :output-chan (chan)
     :state-buf (ref (cbuf (:buffer dash-conf) :direction :left)) ; FIFO
     }))