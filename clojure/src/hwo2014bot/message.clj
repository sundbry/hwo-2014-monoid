(ns hwo2014bot.message)

(defn throttle [throttle-val tick-num]
  {:msgType "throttle"
   :data throttle-val
   :gameTick tick-num})