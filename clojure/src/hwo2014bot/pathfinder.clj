(ns hwo2014bot.pathfinder
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [chan go go-loop >! <! close!]]
            [com.stuartsierra.component :as component]
            [hwo2014bot.protocol :refer :all]
            [hwo2014bot.constant :as const]))

(defn max-velocity [fwd-lane-sections displacement accel-fn decel-fn]
  10.0)


             