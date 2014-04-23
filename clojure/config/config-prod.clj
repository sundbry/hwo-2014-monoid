(ns hwo2014bot.config)

(def default-conf
  {:host "testserver.helloworldopen.com" ; default host, overriden by cli
   :port 8091 ; default port, overriden by cli
   :key "NBANBPb2JZyDGw"
   :name "Monoid"
   ;:trace {:dir "data"} ; optional
   :dashboard
     {:instant 6} ; # of ticks for instantaneous measurements
   :throttle
     {:velocity ; mode parameters
      {:kP 1.0
       :kI 1.0
       :kD 1.0}
      :angle
      {:kP 1.0
       :kI 1.0
       :kD 1.0}}
   :ai
     {:driver "Mario"
      :speed 6.5}
     ; different AI drivers:
     ; :mario drives at fixed speed
     ; :danica (TODO) drives at fixed speed on straights, and fixed angle on turns
     
  })
