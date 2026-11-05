# Elevator controller, for a Linear Servo Drive on the RS485 bus.
# Edit the settings for your shaft (Files tab: open a:/main.janet), save, and press Reset.
#
# How it works: every call button lights its lamp when pressed; the car keeps going the way it is going while there
# are calls that way, stops at each called floor, waits, and turns round when there are no more calls ahead (an
# ordinary "collective" elevator). A fault or a stall is shown on the terminal and the car tries again.

(def settings
  {:drive 40                 # the drive's address (set in its screen)
   :floors [0 4 8]           # the floors' heights, in blocks above the drive's foot, lowest first
   :hall [nil nil nil]       # each floor's call button address on the landing, or nil
   :car [50 51 52]           # each floor's button on the car panel ("Car panel from" in the drive's screen), or nil
   :dwell 3})                # seconds the car waits at a floor

(def drive (settings :drive))
(def floors (settings :floors))
(def n (length floors))
(def calls (array/new-filled n false))

(defn lamps [f on]
  (each a [(get (settings :hall) f) (get (settings :car) f)]
    (when a (light/set a (if on 1 0)))))

(defn call [f]
  (unless (calls f)
    (put calls f true)
    (lamps f true)))

(loop [f :range [0 n]]
  (each a [(get (settings :hall) f) (get (settings :car) f)]
    (when a (bus/on-press a (fn [_] (call f))))))

(defn nearest-floor [h]
  (var best 0)
  (loop [f :range [0 n]]
    (when (< (math/abs (- (floors f) h)) (math/abs (- (floors best) h))) (set best f)))
  best)

(var here (try (nearest-floor (drive/position drive)) ([_] 0)))
(var going :up)

(defn ahead [dir]
  (find |(calls $) (if (= dir :up) (range (+ here 1) n) (reverse (range 0 here)))))

(defn next-stop []
  (if (calls here) here
    (or (ahead going)
        (do (set going (if (= going :up) :down :up)) (ahead going)))))

(loop [f :range [0 n]] (lamps f false))
(print "Elevator: drive " drive ", " n " floors; at floor " (+ here 1))

(forever
  (if-let [stop (next-stop)]
    (try
      (do
        (drive/move drive (floors stop))
        (set here stop)
        (put calls stop false)
        (lamps stop false)
        (sleep (settings :dwell)))
      ([err] (alarm "elevator: " err) (sleep 5)))
    (bus/wait (fn [p] (= :press (p :type))) 30)))
