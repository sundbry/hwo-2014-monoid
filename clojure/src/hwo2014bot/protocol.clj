(ns hwo2014bot.protocol)

(defprotocol PTrace ; Protocols are essentially Java interfaces
  (in [_ json-data])
  (out [_ json-data]))