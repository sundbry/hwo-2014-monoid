(ns hwo2014bot.trace
  (:require ;[clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.data.csv :refer [write-csv]]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [hwo2014bot.protocol :refer :all])
  (:import (java.util Date)))



(defn- new-trace-file [trace-conf]
  {:pre [(not (nil? (:dir trace-conf)))]} ; Preconditions for the function are enforced
  (let [file-path (str (:dir trace-conf) "/trace-" (.getTime (Date.)) "-" (rand-int 1e9) ".log")]
    (log/info "Opening trace file:" file-path)
    (try
      (java.io.FileWriter. file-path)
      (catch java.io.IOException e
        (log/error e "Failed to open trace file:" file-path)
        nil))))

(defn- trace-msg [fh io-mode msg]
  (let [data (if (string? msg) msg (json/write-str msg))
        entry (str "[" (.getTime (Date.)) ", \"" (name io-mode) "\", " data  "]\n")] ; build a json tuple with the timestamp, i/o, msg
    (print-simple entry fh))
  fh)

(defrecord FileTracer [config trace-agent]
  component/Lifecycle
  
  (start [this]
    (send-off trace-agent (fn [_]
                            (new-trace-file config)))
    this)
  
  (stop [this]
    (send-off trace-agent 
              (fn [fh]
                (when fh
                  (.close fh)
                  nil))))
  
  PTrace
  
  (trace [this type-kw msg]
    (send-off trace-agent (fn [fh]
                            (trace-msg fh type-kw msg)))
    this)
    
) ; end record


(defn- new-trace-csv-file [trace-conf]
  {:pre [(not (nil? (:dir trace-conf)))]} ; Preconditions for the function are enforced
  (let [file-path (str (:dir trace-conf) "/trace-" (.getTime (Date.)) "-" (rand-int 1e9) ".csv")]
    (log/info "Opening trace file:" file-path)
    (try
      {:fh (java.io.FileWriter. file-path)
       :head nil
       :buf {}}
      (catch java.io.IOException e
        (log/error e "Failed to open trace file:" file-path)
        nil))))

(defn- flush-csv-vals [handle]
  (when (not (:head handle))
    (write-csv (:fh handle) [(keys (:buf handle))]))
  (write-csv (:fh handle) [(vals (:buf handle))])
  (assoc handle :head true))

(defn- trace-msg-csv [handle src msg]
  (if (= :out src)
    (if (empty? (:buf handle))
      handle
      (flush-csv-vals handle))
    (let [data (into {} (map
                          (fn [[key val]] (vector (str (name src) "." (name key)) val))
                          msg))
          handle (update-in handle [:buf] merge data)]
      handle)))

(defrecord CSVTracer [config trace-agent]
  component/Lifecycle
  
  (start [this]
    (send-off trace-agent (fn [_]
                            (new-trace-csv-file config)))
    this)
  
  (stop [this]
    (send-off trace-agent 
              (fn [handle]
                (when (:fh handle)
                  (.close (:fh handle))
                  nil))))
  
  PTrace
  
  (trace [this type-kw msg]
    (when (or (= :dashboard type-kw)
              (= :throttle type-kw)
              (= :out type-kw))
      (send-off trace-agent (fn [handle]
                              (trace-msg-csv handle type-kw msg))))
    
    this)
    
) ; end record

(defrecord NullTracer []
  component/Lifecycle  
  (start [this] this)
  (stop [this] this)
  
  PTrace
  (trace [this type-kw msg] this)
) ; end record

(defn new-tracer [trace-conf]
  (cond 
    (:csv trace-conf)
      (map->CSVTracer
        {:config trace-conf
         ; An agent is an abstraction to queue functions on a thread pool,
         ; with only one function per agent executing at a time.
         :trace-agent (agent nil)})
    (map? trace-conf)
      (map->FileTracer
        {:config trace-conf
         ; An agent is an abstraction to queue functions on a thread pool,
         ; with only one function per agent executing at a time.
         :trace-agent (agent nil)})
    :else (->NullTracer)))
  

