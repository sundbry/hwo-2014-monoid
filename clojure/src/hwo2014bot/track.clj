(ns hwo2014bot.track
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [chan go go-loop >! <! close!]]
            [com.stuartsierra.component :as component]
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
    :else (throw (ex-info "Invalid lane piece" piece))))

(defn build-lane [pieces-json lane-json]
  (let [lane-offset (:distanceFromCenter lane-json)]
    (loop [sections (vector)
           section-offset 0.0
           pieces (seq pieces-json)]
      (if-let [piece (first pieces)]
        (let [next-section (build-lane-section lane-offset section-offset piece)]
          (recur (conj sections next-section)
                 (+ (:offset next-section) (:length next-section))
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
        (vec sections)))) ; finally. convert to a vector for efficient random access

(defn analyze-lanes [lanes]
  (map analyze-lane lanes))

(defn setup-cars [car-data]
  (into {} (map
             (fn [car]
               [(:name (:id car)) {:color (:color (:id car))}])
             car-data)))

(defn start-displacement [lane-sections piece-pos]
  (if (<= (count lane-sections) (:section-index piece-pos))
    0.0 ; they finished the race
    (let [lane-section (nth lane-sections (:section-index piece-pos))]
      (+ (:offset lane-section) (:section-displacement piece-pos)))))

(defn convert-piece-position [pos-json lap-size]
  "Convert a :piecePosition to a lane section position"
  {:section-index (+ (* (:lap pos-json) lap-size)
                     (:pieceIndex pos-json))
   :section-displacement (:inPieceDistance pos-json)})

(defn update-cars [prev-positions track-state new-positions]
  (into {} (map
             (fn [car]
               (let [pos (:piecePosition car)
                     lane (:lane pos)]
                 [(:name (:id car))
                  {:angle (:angle car)
                   :lane (/ (+ (:startLaneIndex lane) (:endLaneIndex lane)) 2)
                   :start-displacement (start-displacement (nth (:lanes track-state) (:startLaneIndex lane))
                                                           (convert-piece-position pos (:lap-size track-state)))
                   ;:finish-displacement (finish-displacement (nth lanes (:endLaneIndex lane)) pos)
                   }]))
               new-positions)))

(defrecord RaceTrack [tracer input-chan output-chan state]
  component/Lifecycle
  
  (start [this] 
    (go (try (loop []
      (when-let [position-data (<! input-chan)]
        (let [track-state
              (dosync
                (alter state update-in [:tick] inc)
                (alter state
                       (fn [cur-state]
                         (update-in cur-state [:cars] update-cars cur-state position-data))))]
          (>! output-chan track-state))
        (recur)))
      (catch Exception e
        (log/error e "Race track error"))))
    this)
           
  (stop [this]     
    (close! output-chan)
    this)
  
  PRaceTrack
  
  (load-race [this data]
    (let [lanes (vec (analyze-lanes (build-lanes (:track data) (:laps (:raceSession data)))))]
      (dosync
        (ref-set state
                 {:tick -1
                  :lanes lanes
                  :cars (setup-cars (:cars data))
                  :lap-size (count (:pieces (:track data)))}))
      (trace tracer :track @state))
    this)
  
  (finish-race [this data]
    this)
  
  (update-positions [this position-data]
    (go (>! input-chan position-data))
    this)
    

) ; end record

(defn new-track []
  (map->RaceTrack 
    {:input-chan (chan)
     :output-chan (chan)
     :state (ref {})}))
 