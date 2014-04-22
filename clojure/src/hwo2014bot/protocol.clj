(ns hwo2014bot.protocol)

(defprotocol PTrace ; Protocols are essentially Java interfaces
  (trace [_ type-kw json-data]))

(defprotocol PTick
  (tick [_]))

(defprotocol PRaceTrack
  (load-race [_ race-data])
  (finish-race [_ finish-data])
  (update-positions [_ position-data]))