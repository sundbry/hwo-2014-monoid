(ns hwo2014bot.core
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [hwo2014bot.config :refer [default-conf]] ; :refer imports individual names into current namespace
            [hwo2014bot.trace :refer [new-tracer]]
            [hwo2014bot.track :refer [new-track]]
            [hwo2014bot.dashboard :refer [new-dashboard]]
            [hwo2014bot.throttle :refer [new-throttle]]
            [hwo2014bot.driver :refer [new-driver]]             
            [hwo2014bot.racer :refer [new-racer]])
  (:gen-class)) ; :gen-class instructs the compiler to build a class file for this namespace


(defn new-race-bot [conf]
  (component/system-map
    ;:conf conf
    ;:db (new-database host port)
    ;:sched (new-scheduler)
    :tracer (new-tracer (:trace conf))
    :track (component/using (new-track)
                            [:tracer])
    :dashboard (component/using (new-dashboard (:name conf) (:dashboard conf))
                                [:tracer :track])
    :throttle (component/using (new-throttle (:throttle conf))
                               [:dashboard])
    :driver (component/using (new-driver (:ai conf))
                             [:tracer :track :dashboard :throttle])                                         
    :racer (component/using (new-racer conf)
                            [:tracer :track :dashboard :driver])))

(defn- finished-race [stopped-cb bot-atom]
  (swap! bot-atom 
         (fn [sys]
           (try
             (component/stop-system sys)
             (log/debug "System stopped.")
             nil
             (catch Exception e
               (log/error e "Failed to stop race bot")))))
  (stopped-cb))

(defn start [& [stopped-callback host port botname botkey]]
  (let [stopped-callback (if (nil? stopped-callback) (fn [] nil) stopped-callback)
        conf
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
        finished-cb #(finished-race stopped-callback speed-racer)]
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

(defn quit-program []
  (shutdown-agents)
  (System/exit 0))

(defn -main [& args]
  (apply start (cons quit-program args)))
