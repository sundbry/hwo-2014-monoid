(ns hwo2014bot.driver.peach
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [chan go go-loop >! <! close!]]
            [com.stuartsierra.component :as component]
            [hwo2014bot.protocol :refer :all]
            [hwo2014bot.message :as message]))

;;; Peach AI toggles throttle mode depending on track section

(defrecord Driver [config track dashboard throttle]
  component/Lifecycle
  
  (start [this]
    (set-mode throttle :velocity)
    (new-setpoint throttle (:speed config))
    this)
  
  (stop [this]
    this)
  
  PActiveComponent
  
  ;; Responds with an appropriate game protocol message, or nil (ping)
  (tick [this tick-num]
    (let [cur-pos (my-position track)]
      (if (:straight? (:section cur-pos))
        (dosync
          (set-mode throttle :velocity)
          (new-setpoint throttle (:speed config)))
        (dosync
          (set-mode throttle :slip-magnitude)
          (new-setpoint throttle (:safe-angle config))))
      (let [throttle-ctrl (tick throttle tick-num)]
        (message/throttle (:throttle throttle-ctrl) tick-num))))
    
) ; end record
