(ns hwo2014bot.protocol)

(defprotocol PTrace ; Protocols are essentially Java interfaces
  (trace [_ type-kw json-data]))

;; Passive components are things that do data collection and analysis, without changing the game state.
;; Things like the track and dashboard are passive.
;; They perform analysis each tick before control is handed off to active components.
(defprotocol PPassiveComponent
  (output-channel [_])
  (read-state [_])) ; Responds with the current state 

;; Active components are things that perform some action in response, such as the throttle or the AI driver
;; They must be (tick)ed for every game tick to keep up.
(defprotocol PActiveComponent
  (tick [_ tick-num])) ; Responds with an appropriate reaction

(defprotocol PRaceTrack
  (load-race [_ race-data])
  (finish-race [_ finish-data])
  (update-positions [_ position-data])
  (car-position [_ car-name])
  (my-position [_]))

(defprotocol PController
  (new-setpoint [_ sp-val])
  (set-mode [_ mode]))

(defprotocol PCharacterization
  (estimate-lower-accel [_ throttle-out V0])
  (estimate-upper-accel [_ throttle-out V0]))
