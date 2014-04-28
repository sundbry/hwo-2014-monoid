(ns hwo2014bot.characterizer
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [chan go go-loop >! <! close! pipe]]
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

(defn passive-recalibrate [profile]
  (dosync
    (let [cfg (:config profile)
          calib @(:calib-state profile)
          throttle-state (read-state (:throttle profile))
          dash-state (read-state (:dashboard profile))
          accel-est (calculate-accel (:throttle throttle-state) (:velocity dash-state) (:throttle calib) (:drag calib) (:k-friction calib))
          accel-error (- accel-est (:acceleration dash-state))
          #_throttle-cf 
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
                    (:drag calib))]
      (alter (:calib-state profile) merge
             {;:throttle (max (min throttle-cf cal-limit) cal-low-limit)
              :drag (max (min drag-cf cal-limit) cal-low-limit)
              ;:k-friction (max (min k-friction-cf cal-limit) cal-low-limit)
              :acceleration-estimate (max (min accel-est cal-limit) cal-low-limit)
              :acceleration-error (max (min accel-error cal-limit) cal-low-limit)}))))
      
(defrecord PerformanceCharacterizer [config tracer dashboard throttle calib-state output-chan]
  component/Lifecycle
  
  (start [this]
    ;(pipe (output-channel dashboard) output-chan)
    (go (try (loop []
      (when-let [dash-state (<! (output-channel throttle))]
        ; Run passive (continuous) characterization
        (when (and (:passive config) (:auto-cal @calib-state))
          (passive-recalibrate this))
        (trace tracer :calib @calib-state)
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
    {:config conf
     :calib-state (ref
                    {:auto-cal false
                     :throttle 1.0
                     :drag 0.0
                     :k-friction 0.0
                     :acceleration-estimate 0
                     :acceleration-error 0})
     :output-chan (chan)}))
 