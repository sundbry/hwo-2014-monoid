(defproject hwo2014bot "0.1.0-SNAPSHOT"
  :description "HWO2014 Clojure Bot by Team Monoid"
  :url "http://example.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [aleph "0.3.2"]
                 [org.clojure/data.json "0.2.3"]
                 [org.clojure/tools.logging "0.2.6"]  
                 [log4j "1.2.17"]
                 ; Non-standard project dependencies below
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"]
                 [org.clojure/data.csv "0.1.2"]
                 [com.stuartsierra/component "0.2.1"]
                 [circular-buffer "0.1.0-SNAPSHOT"]]
  :main ^:skip-aot hwo2014bot.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
