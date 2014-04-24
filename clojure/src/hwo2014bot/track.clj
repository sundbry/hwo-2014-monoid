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

(defn copy-laps [single-lap num-laps]
  (if (>= 1 num-laps)
    single-lap
    (concat single-lap (copy-laps single-lap (dec num-laps)))))

;;;; TODO build look-ahead limit (1 lap for lazy sequencing)

#_(defn- second-lap [two-laps]
  (drop two-laps (quot (count two-laps) 2)))

(defn build-lap-loop [track-json]
  (map #(build-lane (:pieces track-json) %)
       (:lanes track-json))) ; todo loop the analysis for the tail-end of the track

(defn build-fixed-laps [track-json num-laps]
  (let [laps (copy-laps (:pieces track-json) num-laps)]
    (map #(build-lane laps %)
         (:lanes track-json))))

(defn build-lanes 
  "Lanes are a series of sections, each with an offset from the starting line, a length, a possible switch, and a speed limit"
  ([track] (build-lanes track nil))
  ([track-json num-laps] 
    (if (nil? num-laps)
      (build-lap-loop track-json)
      (build-fixed-laps track-json num-laps))))

(defn analyze-lane-section [finish-offset section]
  (assoc section :finish-offset (+ finish-offset (:length section))))

(defn static-analyze-lane [lane]
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

;;; Car positions

(defn setup-cars [car-data]
  (into {} (map
             (fn [car]
               [(:name (:id car)) {:color (:color (:id car))}])
             car-data)))

(defn lap-displacement [lane-cycle]
  (let [tail (last lane-cycle)]
    (+ (:offset tail)
       (:length tail))))

(defn start-displacement [lane-sections piece-pos]
  (if (< (:section-index piece-pos) (count lane-sections))   
    (let [sect (nth lane-sections (:section-index piece-pos))]
      (+ (:offset sect)
         (:inPieceDistance piece-pos)))
    (let [sect (nth lane-sections (:pieceIndex piece-pos))] ; loop lap 1, TODO: cycle lap 2 instead (provides safe entry into next cycle)
      (+ (* (lap-displacement lane-sections)
            (:lap piece-pos))
         (:offset sect)
         (:inPieceDistance piece-pos)))))

(defn indexed-piece-position [pos-json lap-size]
  "Determine index of :piecePosition as a lane section position"
  (merge pos-json
         {:section-index (if (:lap pos-json)      
                           (+ (* (:lap pos-json) lap-size)
                              (:pieceIndex pos-json))
                           (:pieceIndex pos-json))}))

(defn update-cars [prev-positions track-state new-positions]
  (into {} (map
             (fn [car]
               (let [pos (:piecePosition car)
                     lane (:lane pos)]
                 [(:name (:id car))
                  {:angle (:angle car)
                   :lane (/ (+ (:startLaneIndex lane) (:endLaneIndex lane)) 2)
                   :start-displacement (start-displacement (nth (:lanes track-state) (:startLaneIndex lane))
                                                           (indexed-piece-position pos (:lap-size track-state)))
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
  
  PPassiveComponent
  
  (read-state [this]
    @state)        
  
  PRaceTrack
  
  (load-race [this data]
    (let [session (:raceSession data)
          lanes (vec (map static-analyze-lane (build-lanes (:track data) (:laps session))))]
      (dosync
        (ref-set state
                 {:tick -1
                  :lanes lanes
                  :lap-size (count (:pieces (:track data)))
                  :laps (:laps session) ; nil: qualification loop
                  :cars (setup-cars (:cars data))
                  }))
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
 