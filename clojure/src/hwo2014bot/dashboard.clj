(ns hwo2014bot.dashboard
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [chan go go-loop >! <! close!]]
            [com.stuartsierra.component :as component]
            [hamakar.circular-buffer :refer [cbuf]]
            [hwo2014bot.protocol :refer :all]
            [hwo2014bot.constant :as const]))

(defn take-linear-measurements [dash prev-dash cur-pos delta-t]
  (let [delta-disp (- (:start-displacement cur-pos)
                      (:displacement prev-dash))
        cur-velocity (/ delta-disp delta-t)
        delta-v (- cur-velocity (:velocity prev-dash))
        cur-accel (/ delta-v delta-t)]        
    (merge dash
           {:displacement (:start-displacement cur-pos)
            :velocity cur-velocity
            :acceleration cur-accel})))

(defn take-angular-measurements [dash prev-dash cur-pos delta-t]
  (let [piece-pos (:piecePosition cur-pos)
        turn-radius (:radius (:piece (:section cur-pos)))
        delta-s-disp (- (:angle cur-pos)
                        (:slip-angle prev-dash))
        s-velocity (/ delta-s-disp delta-t)
        delta-s-v (- s-velocity
                     (:slip-velocity prev-dash))
        s-accel (/ delta-s-v delta-t)]
    (merge dash
           {:slip-magnitude (Math/abs (:angle cur-pos))
            :slip-angle (:angle cur-pos)
            :slip-velocity s-velocity ; slip angle velocity
            :slip-acceleration s-accel ; slip angle acceleration
            :turn-angle-displacement 0.0 ; angular displacement around a turn
            :turn-angle-velocity 0.0 ; angular velocity around a turn
            :cent-acceleration 0.0} ; centripital acceleration
           (when turn-radius
             (let [turn-angle (:angle (:piece (:section cur-pos)))
                   a-disp (* (/ (:inPieceDistance piece-pos)
                                (:length (:section cur-pos)))
                             (Math/abs turn-angle))
                   delta-a-disp (- a-disp
                                   (:turn-angle-displacement prev-dash))
                   a-velocity (/ delta-a-disp delta-t)
                   c-accel (/ (* (:velocity dash) (:velocity dash)) turn-radius)] ; Ac = V^2 / r
               {:turn-angle-displacement a-disp
                :turn-angle-velocity a-velocity 
                :cent-acceleration c-accel})))))

(defn- update-dash [dash track-state]  
  (let [state-buf (:state-buf dash)
        cur-pos (get (:cars track-state) (:name (:config dash)))]
    (dosync
      (let [prev-dash (peek @state-buf)
            delta-t (- (:tick track-state)
                       (:tick prev-dash))
            dash-result (-> 
                          {:tick (:tick track-state)}
                          (take-linear-measurements prev-dash cur-pos delta-t)
                          (take-angular-measurements prev-dash cur-pos delta-t))]                  
        (alter state-buf conj dash-result)
        dash-result))))

(defn- initialize-dash [dash]
  (dosync
      (alter (:state-buf dash) conj 
             {:tick -1
              ; linear
              :displacement 0.0
              :velocity 0.0
              :acceleration 0.0
              ; angular
              :slip-magnitude 0.0 ; absolute val of slip angle
              :slip-angle 0.0 ; slip angle
              :slip-velocity 0.0 ; angular velocity
              :slip-acceleration 0.0 ; angular acceleration
              :cent-velocity 0.0 ; centripital velocity 
              :cent-acceleration 0.0 ; centripital acceleration
              })))
    
(defrecord Dashboard [track tracer config output-chan state-buf] ; tracer, track injected 
  component/Lifecycle
  
  (start [this]
    (log/debug "Starting dashboard")
    (initialize-dash this)
    (go (try (loop []
      (when-let [cur-track (<! (output-channel track))]
        (let [dash (update-dash this cur-track)]
          (trace tracer :dashboard dash)
          (>! output-chan dash))
        (recur)))
      (catch Exception e
        (log/error e "Dashboard error"))))
    this)
  
  (stop [this]
    (log/debug "Stopping dashboard")
    (close! output-chan)
    this)
  
  PPassiveComponent
  
  (read-state [this]
    (last @state-buf)) ; The most recent reading is at the end of the circular buf
  
  (output-channel [this] output-chan)
  
) ; end record

(defn new-dashboard [dash-conf]
  (map->Dashboard
    {:config dash-conf
     :output-chan (chan)
     :state-buf (ref (cbuf (:instant dash-conf) :direction :left)) ; FIFO
     }))