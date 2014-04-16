(ns hwo2014bot.core
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [hwo2014bot.config :refer [default-conf]] ; :refer imports individual names into current namespace
            [hwo2014bot.trace :refer [new-tracer]]
            [hwo2014bot.racer :refer [new-racer]]) 
  (:gen-class)) ; :gen-class instructs the compiler to build a class file for this namespace


(defn new-race-bot [conf]
  (component/system-map
    ;:conf conf
    ;:db (new-database host port)
    ;:sched (new-scheduler)
    :tracer (new-tracer (:trace conf))
    :racer (component/using (new-racer conf) [:tracer])))

(defn- finished-race [bot-atom]
  (swap! bot-atom 
         (fn [sys]
           (try
             (component/stop-system sys)
             (log/debug "System stopped.")
             nil
             (catch Exception e
               (log/error e "Failed to stop race bot"))))))

(defn -main [& [host port botname botkey]]
  (let [conf
        (merge
          default-conf
          (when host
            {:host host})
          (when port
            {:port (Integer/parseInt port)})
          (when botname
            {:name botname})
          (when botkey
            {:key botkey}))
        speed-racer (atom (new-race-bot conf)) ; atom proides boxed mutable state needed for callbacks
        finished-cb #(finished-race speed-racer)]
    (swap! speed-racer
           (fn [sys]            
             (try
               (let [sys1 (update-in sys [:racer] #(assoc % :finish-callback finished-cb))
                     sys2 (component/start-system sys1)]
                 (log/debug "System started.")
                 sys2)
             (catch Exception e
               (log/error e "Failure to start race bot"))))))
    nil)
