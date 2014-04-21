(ns hwo2014bot.track
  (:require [clojure.tools.logging :as log]
            [[com.stuartsierra.component :as component]
            [hwo2014bot.protocol :refer :all]))

(defn arc-length [radius angle-deg]
  {:pre [(< 0 angle-deg) (< 0 radius)]}
  (* (/ angle-deg 360.0) 2.0 Math/PI radius))

(defn straight-section [section-offset piece]
  {:offset section-offset
   :length (:length piece)
   :switch (:switch piece)
   :limit Double/POSITIVE_INFINITY})

(defn turn-section [lane-offset section-offset piece]
  {:offset section-offset
   :length (if (< 0 (:angle piece))
             ; right turn
             (arc-length (- (:radius piece) lane-offset) (:angle piece))
             ; left turn
             (arc-length (+ (:radius piece) lane-offset) (- 0 (:angle piece))))
   :switch (:switch piece)
   :limit 0.6}) ; TODO speed limit formula + analysis

(defn build-lane-section [lane-offset section-offset piece]
  (cond
    (:length piece) (straight-section section-offset piece)
    (:radius piece) (turn-section lane-offset section-offset piece)
    (throw (ex-info "Invalid lane piece" piece))))

(defn build-lane [pieces-json lane-json]
  (let [lane-offset (:distanceFromCenter lane-json)]
    (loop [sections (list)
           section-offset 0.0
           pieces (seq pieces-json)]
      (if-let [piece (first pieces)]
        (let [next-section (build-lane-section lane-offset section-offset piece)]
          (recur (conj sections next-section)
                 (:offset next-section)
                 (rest pieces)))
        sections))))

(defn make-laps [piece-list num-laps]
  (if (>= 1 num-laps)
    piece-list
    (concat piece-list (make-laps piece-list (dec num-laps)))))

(defn build-lanes [track-json num-laps]
  "Lanes are a series of sections, each with an offset from the starting line, a length, a possible switch, and a speed limit"
  (let [track-pieces (make-laps (:pieces track-json) num-laps)]
    (map (partial build-lane track-pieces) (:lanes track-json))))

(defn analyze-lane-section [finish-offset section]
  (assoc section :finish-offset (+ finish-offset (:length section))))

(defn analyze-lane [lane]
  "Determine forecasted speed limits for each lane, and offset from the finish line."
    (loop [sections (list)
           finish-offset 0.0
           rev-sections (reverse lane)]
      (if-let [section (first rev-sections)]
        (let [tail-section (analyze-lane-section finish-offset section)]
          (recur (cons tail-section sections)
               (:finish-offset tail-section)
               (rest rev-sections)))
        sections)))

(defn analyze-lanes [lanes]
  (map analyze-lane lanes))

(defn setup-cars [car-data lanes]
  (into {} (map
             (fn [car]
               [(:name (:id car)) nil]))))

(defrecord Track [input-chan output-chan lanes cars]
  component/Lifecycle
  
  (start [this] 
    (go-loop []
      (if-let [positions (<! input-chan)]
        (do
          (dosync
            (alter cars update-cars @lanes position-data)))
          (>! output-chan (update-track @track-state positions)
          (recur))
        (close! output-chan)))        
    this)
           
  (stop [this]     
    (close! input-chan)
    this)
  
  PTrack
  
  (load-race [this race-data]
    (dosync
      (ref-set lanes (analyze-lanes (build-lanes (:track data) (:laps (:raceSession data)))))
      (ref-set cars (setup-cars (:cars data) @lanes)))
    this)
  
  (update-positions [this position-data]
    (go (>! input-chan position-data))
    this)
    

) ; end record

(defn new-track []
  (map->Track 
    {:input-chan (chan)
     :output-chan (chan)
     :lanes (ref)
     :cars (ref)}))
 