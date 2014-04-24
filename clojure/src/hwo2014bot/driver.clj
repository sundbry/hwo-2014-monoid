(ns hwo2014bot.driver
  (:require [clojure.tools.logging :as log]
            [hwo2014bot.protocol :refer :all]
            [hwo2014bot.driver.mario :as mario]
            [hwo2014bot.driver.luigi :as luigi]            
            [hwo2014bot.driver.peach :as peach]))

(defn new-driver [driver-conf]
  (log/info "Driving as" (:driver driver-conf))
  (case (:driver driver-conf)
    "Mario" (mario/map->Driver {:config driver-conf})
    "Peach" (peach/map->Driver {:config driver-conf})
    "Luigi" (luigi/map->Driver {:config driver-conf})
    (throw (ex-info (str "Unknown driver AI: " (:driver driver-conf)) driver-conf))))