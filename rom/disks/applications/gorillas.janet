# Gorillas, for two computers on one RS485 bus (after QBasic's GORILLAS.BAS). Each player runs it on their own
# computer; the two find each other on the bus and take turns throwing a banana at the other's gorilla: type an angle
# (degrees above the ground, towards the other gorilla) and a speed, each followed by Enter. Mind the wind.
# Click the field first, so it gets the keys. Start it from the Games menu, or open it in the Files tab and press Run.
#
# Both computers work out every throw the same way from the same city, so only the throws go over the bus. They talk
# in :reply packets, which devices ignore (a reply is for whoever asked, and here nobody did). The index byte is the
# message (high 4 bits) and the turn (low 4 bits); the value is the message's number:
#   HELLO  broadcast every 2 s while looking for a partner
#   START  to a computer that said HELLO (the higher address sends it); value: the city's seed
#   THROW  value: angle * 1000 + speed; sent again every second until the other side sends ACK
#   ACK    the throw arrived
#   ALIVE  every 3 s during a game; 12 s of silence ends it

(def W 52)            # the field in cells; row 0 is the top. A cell is about twice as tall as it is wide, so a row
(def H 15)            # is 2 units of height and a column 1 unit of width.
(def HELLO 0xA0)
(def START 0xB0)
(def THROW 0xC0)
(def ACK 0xD0)
(def ALIVE 0xE0)
(def GRAVITY 9.8)     # units a second, each second
(def SPEED 0.3)       # units a second per point of speed
(def WIND 0.4)        # units a second, each second, per point of wind
(def colours [:cyan :red :light-gray :gray :brown :purple])

(defn op [p] (band (p :index) 0xF0))
(defn turn-of [p] (band (p :index) 0x0F))

# ---- the city: buildings, two gorillas, the wind; the same seed makes the same city on both computers

(defn city [seed]
  (def rng (math/rng seed))
  (def grid (seq [_ :range [0 H]] (buffer/new-filled W 0)))   # 0 sky, else a building's colour + 1
  (def buildings @[])
  (var x 0)
  (while (< x W)
    (def w (if (< (- W x) 8) (- W x) (+ 4 (math/rng-int rng 4))))
    (def h (+ 2 (math/rng-int rng 7)))
    (def c (math/rng-int rng (length colours)))
    (def lit (seq [_ :range [0 (* w h)]] (< (math/rng-int rng 3) 2)))
    (array/push buildings [x w h c lit])
    (for r (- H h) H (for i x (+ x w) (put (grid r) i (+ c 1))))
    (+= x w))
  (defn on-top [[bx w h]] @[(+ bx (math/floor (/ (- w 3) 2))) (- H h 2)])   # [left column, top row], 3 by 2 cells
  @{:grid grid :buildings buildings
    :gorillas @[(on-top (buildings 1)) (on-top (buildings (- (length buildings) 2)))]
    :wind (- (math/rng-int rng 11) 5)})

(defn in-gorilla? [g x r] (and (<= (g 0) x (+ (g 0) 2)) (<= (g 1) r (+ (g 1) 1))))

(def blast-cells [[-1 -1] [0 -1] [1 -1] [-2 0] [-1 0] [0 0] [1 0] [2 0] [-1 1] [0 1] [1 1]])

(defn fly
  "The banana's flight, thrown by gorilla `who` (0 left, 1 right): its cell at every tick, where it burst (or nil:
  it left the field), and which gorillas the burst killed. Carves the burst out of the city."
  [c who angle speed]
  (def g ((c :gorillas) who))
  (def dir (if (= who 0) 1 -1))
  (def a (* angle (/ math/pi 180)))
  (var vx (* dir (math/cos a) speed SPEED))
  (var vy (* (math/sin a) speed SPEED))
  (var x (+ (g 0) 1.5))
  (var y (+ 0.3 (* 2 (- H (g 1)))))
  (def ax (* (c :wind) WIND))
  (def dt 0.0125)
  (def path @[])
  (var hit nil)
  (var direct nil)
  (var done false)
  (for tick 0 400
    (for _ 0 4
      (unless done
        (+= vx (* ax dt))
        (-= vy (* GRAVITY dt))
        (+= x (* vx dt))
        (+= y (* vy dt))
        (def cx (math/floor x))
        (def r (- H 1 (math/floor (/ y 2))))
        (cond
          (or (< x 0) (>= x W)) (set done true)
          (< y 0) (do (set hit [cx (- H 1)]) (set done true))
          (>= r 0)
          (do
            (each i [0 1] (when (in-gorilla? ((c :gorillas) i) cx r) (set direct i) (set hit [cx r]) (set done true)))
            (when (and (not done) (pos? (get-in c [:grid r cx]))) (set hit [cx r]) (set done true))))))
    (array/push path [(math/floor x) (- H 1 (math/floor (/ y 2)))])
    (when done (break)))
  (def dead @[])
  (when direct (array/push dead direct))
  (when hit
    (each [dx dr] blast-cells
      (def bx (+ (hit 0) dx))
      (def br (+ (hit 1) dr))
      (when (and (<= 0 bx (- W 1)) (<= 0 br (- H 1)))
        (put ((c :grid) br) bx 0)
        (each i [0 1] (when (and (in-gorilla? ((c :gorillas) i) bx br) (not (index-of i dead))) (array/push dead i))))))
  {:path path :hit hit :dead dead})

# ---- the game

(defn gorillas []
  (def t (proc/tab))
  (def queue @[])        # [:key k] and [:packet p], oldest first
  (var peer nil)         # the other computer's address, during a game
  (var heard 0)          # when it was last heard from
  (var unacked nil)      # [index value] of our throw until it is acknowledged
  (var holes 0)
  (var me 0)             # this computer's address

  (defn status [colour s] (ui/text t 3 0 0 colour (string/format "%-52s" s)))
  (defn say [colour s] (ui/text t 4 0 16 colour (string/format "%-52s" s)))

  (bus/on (fn [p] (and (= :reply (p :type)) (>= (p :index) HELLO)))
          (fn [p]
            (when (= (p :source) peer) (set heard (now)))
            (if (= (op p) ACK)
              (when (and unacked (= (p :index) (+ ACK (band (unacked 0) 0x0F)))) (set unacked nil))
              (array/push queue [:packet p]))))
  (spawn (fn [] (forever
                  (sleep 1)
                  (when (and peer unacked) (bus/send peer :reply ;unacked))
                  (when (and peer (zero? (% (math/floor (now)) 3))) (bus/send peer :reply ALIVE 0)))))

  (defn next-event
    "The next key or packet; nil after timeout seconds. In a game, an error :gone if the other computer went away."
    [&opt timeout]
    (def until (+ (now) (or timeout 1e9)))
    (var e nil)
    (while (and (nil? e) (<= (now) until))
      (when (and peer (> (- (now) heard) 12)) (error :gone))
      (if (empty? queue)
        (sleep 0.05)
        (do
          (set e (queue 0))
          (array/remove queue 0)
          (if (and peer (= :packet (e 0)) (= HELLO (op (e 1))) (= peer ((e 1) :source)))
            (error :gone)))))    # it started again
    e)

  (defn field []
    (ui/clear t)
    (ui/raw t 1 0 1 W H (fn [k mods] (array/push queue [:key k])))
    (ui/box t 2 0 1 W H :blue)
    (set holes 0))

  (defn lobby
    "Look for a partner: returns [its address, the seed]."
    []
    (set peer nil)
    (set unacked nil)
    (field)
    (say :gray "")
    (def rng (math/rng (+ (ticks) me)))
    (var next-hello 0)
    (var found nil)
    (while (nil? found)
      (def [address on-bus] (bus/address))
      (set me address)
      (cond
        (not on-bus) (status :red "Gorillas: connect this computer to a bus first.")
        (zero? me) (status :red "Give this computer an address in its screen first.")
        (status :yellow (string "Looking for a partner on the bus (this is " me ")")))
      (when (and on-bus (pos? me) (>= (now) next-hello))
        (bus/send 0 :reply HELLO 0)
        (set next-hello (+ (now) 2)))
      (def e (next-event 0.5))
      (when (and e (= :packet (e 0)))
        (def p (e 1))
        (def s (p :source))
        (cond
          (= s me) (status :red (string "Another computer on this bus has address " me " too."))
          (and (= (op p) HELLO) (> me s))
          (let [seed (math/rng-int rng 16777216)]
            (bus/send s :reply START seed)
            (set found [s seed]))
          (= (op p) START) (set found [s (math/round (p :value))]))))
    found)

  (defn draw-city [c]
    (field)
    (eachp [i [bx w h col lit]] (c :buildings)
      (ui/box t (+ 10 i) bx (- (+ H 1) h) w h (colours col)))
    (var id 30)
    (each [bx w h col lit] (c :buildings)
      (for row 0 h
        (when (even? row)
          (def s (string/join (seq [i :range [0 w]] (if (and (odd? i) (lit (+ i (* row w)))) ":" " "))))
          (ui/text t id bx (+ (- (+ H 1) h) row) :yellow s)
          (++ id)))))

  (defn draw-gorilla [c i look]
    (def [gx gy] ((c :gorillas) i))
    (def [top bottom colour] (case look
                               :dead ["   " "x_x" :red]
                               :wins ["\\O/" " M " :orange]
                               [" O " "/M\\" :orange]))
    (unless (= look :alive)   # (made again, so it is drawn over the holes)
      (ui/remove t (+ 200 (* 2 i)))
      (ui/remove t (+ 201 (* 2 i))))
    (ui/text t (+ 200 (* 2 i)) gx (+ gy 1) colour top)
    (ui/text t (+ 201 (* 2 i)) gx (+ gy 2) colour bottom))

  (defn read-number
    "Keys until Enter: a whole number in lo..hi (Enter alone: last, if there was one)."
    [label lo hi last]
    (var typed "")
    (var n nil)
    (while (nil? n)
      (say :white (string label " (" lo "-" hi ")" (if last (string " [" last "]") "") ": " typed "_"))
      (def e (next-event))
      (when (= :key (e 0))
        (def k (e 1))
        (cond
          (and (= 1 (length k)) (<= 48 (k 0) 57) (< (length typed) 3)) (set typed (string typed k))
          (= k "backspace") (set typed (string/slice typed 0 (max 0 (- (length typed) 1))))
          (= k "enter")
          (let [x (if (empty? typed) last (scan-number typed))]
            (if (and x (<= lo x hi)) (set n x) (set typed ""))))))
    n)

  (defn wait-throw
    "The other side's throw for turn n: [angle speed]."
    [n]
    (var throw nil)
    (while (nil? throw)
      (def e (next-event))
      (def p (e 1))
      (when (and (= :packet (e 0)) (= (op p) THROW) (= (p :source) peer))
        (bus/send peer :reply (+ ACK (turn-of p)) 0)
        (when (= (turn-of p) (% n 16))
          (def v (math/round (p :value)))
          (set throw [(math/floor (/ v 1000)) (% v 1000)]))))
    throw)

  (defn animate [f]
    (ui/remove t 220)   # (the banana is made again for each throw, so it is drawn over the holes)
    (each [x r] (f :path)
      (if (and (>= r 0) (< x W))
        (ui/text t 220 x (+ r 1) :yellow ")")
        (ui/text t 220 0 0 :yellow ""))
      (sleep 0.05))
    (ui/text t 220 0 0 :yellow "")
    (when-let [[hx hr] (f :hit)]
      (when (< holes 40)    # (a tab holds 256 elements)
        (++ holes)
        (def id (+ 230 (* 3 holes)))
        (ui/box t (+ id 0) (- hx 1) (- (+ hr 1) 1) 3 1 :blue)
        (ui/box t (+ id 1) (- hx 2) (+ hr 1) 5 1 :blue)
        (ui/box t (+ id 2) (- hx 1) (+ hr 2) 3 1 :blue))
      (ui/remove t 221)
      (ui/text t 221 hx (+ hr 1) :red "*")
      (sleep 0.4)
      (ui/text t 221 0 0 :red "")))

  (defn game [seed]
    (def left (< me peer))
    (def mine (if left 0 1))
    (def scores @[0 0])
    (var n 0)
    (var last-angle nil)
    (var last-speed nil)
    (var round 0)
    (forever
      (def c (city (+ seed (* 7919 round))))
      (draw-city c)
      (draw-gorilla c 0 :alive)
      (draw-gorilla c 1 :alive)
      (var over false)
      (while (not over)
        (def who (% n 2))
        (def w (c :wind))
        (status :yellow (string/format " You %d : %d Them    wind %s" (scores mine) (scores (- 1 mine))
                                       (cond (pos? w) (string/repeat ">" w) (neg? w) (string/repeat "<" (- w)) "none")))
        (def [angle speed]
          (if (= who mine)
            (let [a (read-number (if (= mine 0) "Angle to the right" "Angle to the left") 0 180 last-angle)
                  s (read-number "Speed" 1 200 last-speed)]
              (set last-angle a)
              (set last-speed s)
              (set unacked [(+ THROW (% n 16)) (+ (* a 1000) s)])
              (bus/send peer :reply ;unacked)
              [a s])
            (do (say :gray "Their throw...") (wait-throw n))))
        (say :gray (string (if (= who mine) "You" "They") " threw at " angle " degrees, speed " speed))
        (def f (fly c who angle speed))
        (animate f)
        (++ n)
        (def dead (f :dead))
        (when (not (empty? dead))
          (def winner (if (index-of who dead) (- 1 who) who))
          (++ (scores winner))
          (draw-gorilla c (- 1 winner) :dead)
          (draw-gorilla c winner :wins)
          (say :lime (string (if (= winner mine) "You win the round!" "They win the round.") " Next round soon..."))
          (sleep 3)
          (++ round)
          (set over true)))))

  (forever
    (def [p seed] (lobby))
    (set peer p)
    (set heard (now))
    (try (game seed)
      ([err fib] (if (= err :gone)
                   (do (status :red "Your partner has gone. Looking again...") (sleep 5))
                   (propagate err fib))))))

(gorillas)
