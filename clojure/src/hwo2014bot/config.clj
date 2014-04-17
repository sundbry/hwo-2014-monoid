(ns hwo2014bot.config)

(def default-conf
  {:host "testserver.helloworldopen.com" ; default host, overriden by cli
   :port 8091 ; default port, overriden by cli
   :key "NBANBPb2JZyDGw"
   :name "Monoid"
   :trace {:dir "data"} ; optional
   :dashboard
     {:instant 15; # of ticks for instantaneous measurements
      :buffer 15}; # of ticks to keep buffered (must be at least a full instant)
   })