(ns hwo2014bot.racer
  (:require [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [aleph.tcp :refer [tcp-client]]
            [lamina.core :refer [enqueue wait-for-result wait-for-message]]
            [gloss.core :refer [string]]
            [com.stuartsierra.component :as component]
            [hwo2014bot.protocol :refer :all]))

(def ^:dynamic channel-tracer nil) ; this gets bound in thread-local scope with (binding ...)

(defn- json->clj [string]
  (json/read-str string :key-fn keyword))

(defn- send-message [channel message]
  (let [json-str (json/write-str message)]
    (out channel-tracer json-str)
    (enqueue channel json-str)))

(defn- read-message [channel]
  (try
    (let [json-str (wait-for-message channel)]            
      (in channel-tracer json-str)
      (json->clj json-str))
    (catch Exception e
      (log/error e e))))

(defn- connect-client-channel [host port]
  (log/info "Connecting to game server at" (str host ":" port))
  (wait-for-result
    (tcp-client {:host host,
                 :port port,
                 :frame (string :utf-8 :delimiters ["\n"])})))

(defmulti handle-msg :msgType)

(defmethod handle-msg "carPositions" [msg]
  {:msgType "throttle" :data 0.5})

(defmethod handle-msg :default [msg]
  {:msgType "ping" :data "ping"})

(defn- log-msg-received [msg]
  (when-let [info-str
             (case (:msgType msg)
               "join" (println "Joined")
               "gameStart" (println "Race started")
               "crash" (println "Someone crashed")
               "gameEnd" (println "Race ended")
               "error" (println (str "ERROR: " (:data msg)))
               nil)]
    (log/info info-str)))

(defn- game-loop [racer]
  (try
    (binding [channel-tracer (:tracer racer)]
      (let [config (:config racer)]
        (log/debug "Joining race as" (:name config))
        (send-message (:channel racer) 
                      {:msgType "join" :data (select-keys config [:name :key])}))
      (loop []
        (let [msg (read-message (:channel racer))]
          (log-msg-received msg)
          (when (not (= "gameEnd" (:msgType msg)))
            (send-message (:channel racer) (handle-msg msg))
            (recur)))))
    (catch Exception e
      (log/error e "Game loop failure"))
    (finally
      (future (apply (:finish-callback racer) [])) ; invoke finish-callback in a fork so it doesn't block on itself
      nil))) 

(defrecord Racer [config channel tracer finish-callback game-thread] ; tracer, finish-callback get injected before start
  component/Lifecycle
  
  (start [this]
    (log/info "Starting racer.")
    (let [channel (connect-client-channel (:host config) (:port config))
          self (assoc this :channel channel)]
      (assoc self :game-thread (future (game-loop self))))) ; Start the game loop in another thread
  
  (stop [this]
    (log/info "Stopping racer.")
    @game-thread
    (assoc this :game-thread nil)) ; wait for game thread to stop. Does not interrupt a race that is still running.
  
  ) ; end record

(defn new-racer [conf]
  (map->Racer ; constructs a Racer record
    {:config conf}))
