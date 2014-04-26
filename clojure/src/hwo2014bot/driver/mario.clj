(ns hwo2014bot.driver.mario
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [chan go go-loop >! <! close!]]
            [com.stuartsierra.component :as component]
            [hwo2014bot.protocol :refer :all]
            [hwo2014bot.message :as message]))

;;; Mario AI drives at fixed speed. Useful for profiling tracks.

(defrecord Driver [config track dashboard throttle characterizer]
  component/Lifecycle
  (start [this]
    (new-setpoint throttle (:speed config))
    this)
  
  (stop [this]
    this)
  
  PActiveComponent
  
  ;; Responds with an appropriate game protocol message, or nil (ping)
  (tick [this tick-num]
    (let [throttle-ctrl (tick throttle tick-num)]
      (message/throttle (:throttle throttle-ctrl) tick-num)))
    
) ; end record
