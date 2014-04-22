(ns hwo2014bot.racer
  (:require [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [clojure.core.async :refer [<!!]]
            [aleph.tcp :refer [tcp-client]]
            [lamina.core :refer [enqueue wait-for-result wait-for-message]]
            [gloss.core :refer [string]]
            [com.stuartsierra.component :as component]
            [hwo2014bot.protocol :refer :all]))

(defn- json->clj [string]
  (json/read-str string :key-fn keyword))

(defn- send-message [channel message tracer]
  (let [json-str (json/write-str message)]
    (trace tracer :out json-str)
    (enqueue channel json-str)))

(defn- read-message [channel tracer]
  (let [json-str (wait-for-message channel)]
    (trace tracer :in json-str)
    (json->clj json-str)))

(defn- connect-client-channel [host port]
  (log/info "Connecting to game server at" (str host ":" port))
  (let [chan (wait-for-result
               (tcp-client {:host host,
                            :port port,
                            :frame (string :utf-8 :delimiters ["\n"])}))]
    chan))
  

(defn- join-msg [config]
  {:msgType "join" :data (select-keys config [:name :key])})

(defmulti handle-msg :msgType)

(defmethod handle-msg "join" [msg racer]
  (log/debug "Joined race.")
  racer)

(defmethod handle-msg "gameInit" [msg racer]
  (log/debug "Race initialized.")
  (load-race (:track racer) (:race (:data msg)))
  racer)

(defmethod handle-msg "yourCar" [msg racer]
  (log/debug "Your car:" (:name (:data msg)) (str "(" (:color (:data msg)) ")"))
  racer)

(defmethod handle-msg "gameStart" [msg racer]
  (log/info "Race started.")
  racer)

(defmethod handle-msg "gameEnd" [msg racer]
  (log/info "Race results:" (:results (:data msg)))
  racer)

(defmethod handle-msg "tournamentEnd" [msg racer]
  (log/debug "Tournament ended.")
  racer)

(defmethod handle-msg "error" [msg racer]
  (log/error "Server error:" (:data msg))
  racer)

(defmethod handle-msg "carPositions" [msg racer]
  (update-positions (:track racer) (:data msg))
  racer)

(defmethod handle-msg "crash" [msg racer]
  (log/debug (:name (:data msg)) "crashed.")
  racer)

(defmethod handle-msg "spawn" [msg racer]
  (log/debug (:name (:data msg)) "respawned.")
  racer)

(defmethod handle-msg "lapFinished" [msg racer]
  (log/info "Lap" (:lap (:lapTime (:data msg))) "finished.")
  racer)

(defmethod handle-msg "dnf" [msg racer]
  (log/debug (:name (:data msg)) "has been disqualified.")
  racer)

(defmethod handle-msg "finish" [msg racer]
  (log/debug (:name (:data msg)) "finished the race.")
  (finish-race (:track racer) (:data msg))
  racer)

(defmethod handle-msg :default [msg racer]
  (log/warn "Unhandled message:" msg)
  racer)

(defn- game-loop [racer]
  (try
    (let [config (:config racer)]
      (log/debug "Joining race as" (:name config))
      (send-message (:channel racer) (join-msg config) (:tracer racer)))                    
    (loop [racer racer]
      (let [msg (read-message (:channel racer) (:tracer racer))
            racer (handle-msg msg racer)]
        (case (:msgType msg)
          "tournamentEnd" racer ; terminating case
          "carPositions" (recur (tick racer))
          (recur racer))))
    (catch Exception e
      (log/error e "Game loop failure"))
    (finally
      (future (apply (:finish-callback racer) [])) ; invoke finish-callback in a fork so it doesn't block on itself
      nil)))

(defrecord Racer [config tracer finish-callback track dashboard channel game-thread tick-count] ; tracer, finish-callback, track, dashboard get injected before start
  component/Lifecycle
  
  (start [this]
    (log/info "Starting racer.")
    (let [channel (connect-client-channel (:host config) (:port config))
          self (assoc this :channel channel)]
      (assoc self :game-thread (future (game-loop self))))) ; Start the game loop in another thread
  
  ;; Stop the racer by waiting for the race to finish. Does not interrupt a race that is still running.
  (stop [this]
    (log/info "Stopping racer.")
    @game-thread ; wait for game thread to stop. 
    (assoc this :game-thread nil)) 
  
  PTick
  ;; Execute a game tick decision based on current information
  (tick [this]
    ; TODO: insert A* or other logic here
    (let [dash (<!! (:output-chan dashboard))]
      ;(log/debug "Dash received:" dash)
      nil)
    (send-message channel
                  {:msgType "throttle" :data 0.5 :gameTick tick-count}
                  tracer)
    ; or {:msgType "switchLane :data "Left|Right"}
    (update-in this [:tick-count] inc))
  
) ; end record

(defn new-racer [conf]
  (map->Racer ; constructs a Racer record
    {:config conf
     :tick-count 0}))
