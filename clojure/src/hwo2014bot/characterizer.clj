(ns hwo2014bot.characterizer
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [chan go go-loop >! <! close! pipe]]
            [clojure.inspector :refer [inspect]]
            [com.stuartsierra.component :as component]
            [hamakar.circular-buffer :refer [cbuf]]
            [hwo2014bot.protocol :refer :all]
            [hwo2014bot.constant :as const]))

(def ^:private cal-limit 999999999)
(def ^:private cal-low-limit (- cal-limit))

;; Calculate acceleration based on known coefficients
(defn calculate-accel 
  ([throttle-out velocity drag-coeff]
    (calculate-accel throttle-out velocity drag-coeff 0))
  
  ([throttle-out velocity throttle-coeff drag-coeff k-friction]
    ; Sum of forces Fa = Ft - Ff - Fd
    ; Quadratic function: A = -CD*Velocity^2 + CT*Throttle  - CF
    (let [A-out (- (* throttle-coeff throttle-out)
                   (* drag-coeff velocity velocity)
                   k-friction)]
      A-out)))

(defn calculate-terminal-velocity [throttle-coeff drag-coeff k-friction]
  ; A = 0
  ; V = sqrt((CT - CF) / CD))
  (if (= 0 drag-coeff)
    nil
    (Math/sqrt (/ (- throttle-coeff k-friction)
                  drag-coeff))))

;; Returns throttle calibration factor measurement
(defn calibrate-throttle
  ([throttle-out A-measured V-measured]
    (calibrate-throttle A-measured V-measured 0 0))
  
  ([throttle-out A-measured V-measured drag-coeff k-friction]
    ; Solve for CT
    (let [throttle-cf (/ (+ A-measured
                            (* drag-coeff V-measured V-measured)
                            k-friction)
                         throttle-out)]
      throttle-cf)))

;; Measure drag constant
(defn calibrate-drag
  [throttle-out A-measured V-measured throttle-coeff k-friction]
  ; CD = (CT*Throttle - A - CF) / Velocity^2
  (let [drag-cf (/ (- (* throttle-coeff throttle-out)
                      A-measured
                      k-friction)
                   (* V-measured V-measured))]
    drag-cf))

;; Measure kinetic friction 
(defn calibrate-kinetic-friction
  [throttle-out A-measured V-measured throttle-coeff drag-coeff]
  (let [k-friction (- (* throttle-coeff throttle-out)
                      (* drag-coeff V-measured V-measured)
                      A-measured)]
    k-friction))

(defn matrix-recalibrate [profile]
  (let [cf @(:calib-state profile)
        
        terminal-velocity (:terminal-velocity cf)
        X0 0.0
        V0 (if (nil? terminal-velocity) ; when drag coeff is 0 
             (:guess-terminal-velocity (:config profile))
             terminal-velocity)
        A0 (estimate-accel profile 0.0 V0)
        tick-matrix (transient
                      [[0 X0 V0 A0]])]      
      ;(log/debug "Calibrating stop matrix for terminal velocity:" terminal-velocity)
    (loop [T 1
           Ap A0
           Vp V0
           Xp X0]
      (let [X (+ Xp Vp)
            V (+ Vp Ap)
            A (estimate-accel profile 0.0 V)]
        (conj! tick-matrix
               [T X V A])          
        (when (> V 0)
            ; loop until a stop is reached
          (recur (+ T 1) A V X))))
      (dosync
        (alter (:calib-state profile) merge
               {:stop-matrix (persistent! tick-matrix)}))
      ;(log/debug "Stop matrix calibrated, V0:" terminal-velocity "rows:" (count (:stop-matrix @(:calib-state profile))))
      )
  profile)

(defn passive-recalibrate [profile]
  (dosync    
    (let [cfg (:config profile)
          calib @(:calib-state profile)
          throttle-state (read-state (:throttle profile))
          dash-state (read-state (:dashboard profile))
          accel-est (calculate-accel (:throttle throttle-state) (:velocity dash-state) (:throttle calib) (:drag calib) (:k-friction calib))
          accel-error (- accel-est (:acceleration dash-state))]
      (when (> (:velocity dash-state) 0)
        (let [#_throttle-cf 
              #_(calibrate-throttle ; throttle-out A-measured V-measured drag-coeff k-friction
                        (:throttle throttle-state)
                        (:acceleration dash-state)
                        (:velocity dash-state)
                        (:drag calib)
                        (:k-friction calib))
          drag-cf (calibrate-drag
                    (:throttle throttle-state)
                    (:acceleration dash-state)
                    (:velocity dash-state)
                    (:throttle calib)
                    (:k-friction calib))
          #_k-friction-cf
          #_(calibrate-kinetic-friction
                    (:throttle throttle-state)
                    (:acceleration dash-state)
                    (:velocity dash-state)
                    (:throttle calib)
                    (:drag calib))
          terminal-v (calculate-terminal-velocity (:throttle calib) (:drag calib) (:k-friction calib))]          
          (alter (:calib-state profile) merge
                 {;:throttle (max (min throttle-cf cal-limit) cal-low-limit)
                  :drag (max (min drag-cf cal-limit) cal-low-limit)
                  ;:k-friction (max (min k-friction-cf cal-limit) cal-low-limit)
                  :terminal-velocity terminal-v
                  :acceleration-estimate (max (min accel-est cal-limit) cal-low-limit)
                  :acceleration-error (max (min accel-error cal-limit) cal-low-limit)})))))
  profile)

(defn find-first-row [coll pred]
  (first (filter pred coll)))

(defn find-last-row [coll pred]
  (last (filter pred coll)))
      
(defrecord PerformanceCharacterizer [config tracer dashboard throttle calib-state output-chan]
  component/Lifecycle
  
  (start [this]
    ;(pipe (output-channel dashboard) output-chan)
    (go (try (loop []
      (when-let [dash-state (<! (output-channel throttle))]
        ; Run passive (continuous) characterization
        (when (and (:passive config) (:auto-cal @calib-state))
          (passive-recalibrate this)
          (when (< (rand) (:matrix-freq config))
            (matrix-recalibrate this)))
        (trace tracer :calib (dissoc @calib-state :stop-matrix))
        (>! output-chan @calib-state)
        (recur)))
      (catch Exception e
        (log/error e "Passive characterization error"))))
    this)
  
  (stop [this]
    this)
  
  PPassiveComponent
  
  (read-state [this]
    @calib-state)
  
  (output-channel [this] output-chan)
  
  PCharacterization
  
  (estimate-accel [this throttle-out velocity]
    (let [calib @calib-state]
      (calculate-accel throttle-out velocity (:throttle calib) (:drag calib) (:k-friction calib))))
  
  (safe-velocity [this target-distance target-velocity]
    (dosync (let [target-row (find-first-row (:stop-matrix @calib-state) 
                                     #(<= (nth % 2) target-velocity)) ; target such that V < Vt
          start-row (find-last-row (:stop-matrix @calib-state)
                                   #(>= (- (nth target-row 1)
                                           (nth % 1))
                                        target-distance)) ; start such that D > Dt
          result (if start-row 
                   (nth start-row 2)
                   (:terminal-velocity @calib-state))]
      ;(log/debug "target-row:" target-row "start-row:" start-row)
      ;(log/debug "safe-velocity" target-distance "@" target-velocity ":" result)
      ;(inspect (:stop-matrix @calib-state))
      result)))
  
  ;; Estimate the lower-bound acceleration output, given throttle output and initial velocity.
  ;; Useful for projecting when to speed-up
  #_(estimate-lower-accel [this throttle-out V0]
    nil
    )
  
  ;; Estimate the upper-bound acceleration output, given throttle output and initial velocity.
  ;; Useful for projecting safe brake distance
  #_(estimate-upper-accel [this throttle-out V0]
    nil
    )
  
  (teach-calib [this property value]
    (log/debug (str "Teach calibration " property ":") value)
    (dosync
      (alter calib-state assoc property value))
    this)
  
  ;; Enable continuous passive calibration
  ;; Requires an original estimated calibration
  (auto-cal [this set-enable]
    (dosync
      (alter calib-state assoc :auto-cal set-enable)))
  
)

(defn new-characterizer [conf]
  (map->PerformanceCharacterizer
    {:config (merge
               {:passive false
                :matrix-freq 0.1
                :guess-terminal-velocity 50.0}
               conf)
     :calib-state (ref
                    {:auto-cal false
                     :throttle 0.0
                     :drag 0.0
                     :k-friction 0.0
                     :terminal-velocity 0.0
                     :acceleration-estimate 0
                     :acceleration-error 0
                     :stop-matrix nil})
     :output-chan (chan)}))
 