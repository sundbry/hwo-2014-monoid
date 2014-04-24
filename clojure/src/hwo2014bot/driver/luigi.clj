(ns hwo2014bot.driver.luigi
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [chan go go-loop >! <! close!]]
            [com.stuartsierra.component :as component]
            [hwo2014bot.protocol :refer :all]
            [hwo2014bot.message :as message]))

;;; Luigi AI controls throttle based on slip angle.

(defrecord Driver [config track dashboard throttle]
  component/Lifecycle
  
  (start [this]
    (set-mode throttle :slip)
    (new-setpoint throttle (:safe-angle config))
    this)
  
  (stop [this]
    this)
  
  PActiveComponent
  
  ;; Responds with an appropriate game protocol message, or nil (ping)
  (tick [this tick-num]
    (let [throttle-ctrl (tick throttle tick-num)]
      (message/throttle (:throttle throttle-ctrl) tick-num)))
    
) ; end record
