(ns hwo2014bot.driver
  (:require [clojure.tools.logging :as log]
            [hwo2014bot.protocol :refer :all]
            [hwo2014bot.driver.mario :as mario]))

(defn new-driver [driver-conf]
  (case (:driver driver-conf)
    "Mario" (mario/map->MarioDriver {:config driver-conf})
    (throw (ex-info (str "Unknown driver AI: " (:driver driver-conf)) driver-conf))))