(ns hwo2014bot.driver
  (:require [clojure.tools.logging :as log]
            [hwo2014bot.protocol :refer :all]
            [hwo2014bot.driver.mario :as mario]
            [hwo2014bot.driver.luigi :as luigi]            
            [hwo2014bot.driver.peach :as peach]
            [hwo2014bot.driver.bowser :as bowser]
            [hwo2014bot.driver.banshee :as banshee]))

(defn new-driver [driver-conf]
  (log/info "Driving as" (:driver driver-conf))
  (let [spawn-driver 
        (case (:driver driver-conf)
          "Mario" mario/map->Driver
          "Luigi" luigi/map->Driver 
          "Peach" peach/map->Driver
          "Bowser" bowser/map->Driver
          "Banshee" banshee/new-driver
        (throw (ex-info (str "Unknown driver AI: " (:driver driver-conf)) driver-conf)))]
    (spawn-driver {:config driver-conf})))