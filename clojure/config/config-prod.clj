(ns hwo2014bot.config)

(def default-conf
  {:host "testserver.helloworldopen.com" ; default host, overriden by cli
   :port 8091 ; default port, overriden by cli
   :key "NBANBPb2JZyDGw"
   :name "Monoid"
   ;:trace {:dir "data"} ; optional
   :passive-timeout 30 ; ms to process passive data (leaves us 40ms for decision making)
   :dashboard
     {:instant 1} ; # of ticks for instantaneous measurements
   :throttle
     {:velocity ; mode parameters
      {:kP 1.0
       :kI 1.0
       :kD 1.0}
      :slip-magnitude
      {:kP 1.0
       :kI 1.0
       :kD 1.0}}
   :ai
     {:driver "Peach"        
      :speed 7.0
      :safe-angle 15.0}
     
  })
