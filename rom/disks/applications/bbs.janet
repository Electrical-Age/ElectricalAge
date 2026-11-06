# A bulletin board shared by every computer on the bus that runs it. Pick a board on the left, read, and type a post on
# the bottom line. Type /board NAME to open (or start) a board, /name NAME to sign your posts. Start it from the
# Applications menu, or open it in the Files tab and press Run.
#
# Posts are only ever added, never changed or removed, so the boards merge without conflicts: each computer keeps every
# post it has seen on the floppy (bbs/posts.jdn), and asks the others for what it is missing. A post is known by its
# station (4 bytes made at the floppy's first post) and its number there, and is ordered by a Lamport clock, so every
# computer shows a board in the same order.
#
# They talk in :data packets (64 bytes each). The index says what the packet is:
#   SUMMARY  broadcast every 10 s and after a new post: [station, the highest number held with none missing], 10 a packet
#   WANT     to a computer whose summary has more: [station, the first number wanted]; it answers with up to 8 posts
#   PART     a piece of a post: [station, number, piece, pieces] then up to 56 bytes of the post's record

(def SUMMARY 0x41)
(def WANT 0x42)
(def PART 0x43)
(def piece-size 56)
(def max-text 500)

(defn u16 [n] (string/from-bytes (band (brshift n 8) 255) (band n 255)))
(defn get-u16 [s at] (+ (* 256 (s at)) (s (+ at 1))))
(defn hex [s] (string/join (map |(string/format "%02x" $) s)))
(defn unhex [h] (string/from-bytes ;(seq [i :range [0 (/ (length h) 2)]] (scan-number (string/slice h (* 2 i) (+ 2 (* 2 i))) 16))))

(defn bbs []
  (def t (proc/tab))
  (def dir (string (if (disk/exists? "a:/bbs.janet") "a:/" "b:/") "bbs/"))
  (def posts @{})          # [station n] -> the post's record
  (def partial @{})        # [station n] -> @{piece bytes}
  (def heard @{})          # computers heard from -> when
  (var lamport 0)
  (var board "general")
  (var shown @[])          # the posts in the log, in order
  (var station nil)        # this floppy's station, made at its first post
  (var name nil)
  (var trouble nil)        # why the floppy can't keep posts, or nil

  (defn status []
    (def peers (length (filter |(< (- (now) $) 30) (values heard))))
    (ui/text t 1 0 0 (if trouble :red :yellow)
             (string/format "%-52s" (or trouble (string "BBS  " (or name station "") "  " (length posts) " posts  "
                                                        peers " other computer" (if (= 1 peers) "" "s"))))))

  # ---- the floppy

  (var made-dir false)
  (defn make-dir []
    (unless made-dir
      (try (disk/mkdir (string/slice dir 0 -2)) ([_] nil))   # (it may be there already)
      (set made-dir true)))

  (defn save-line [s]
    (make-dir)
    (try (spit (string dir "posts.jdn") (string s "\n") :ab)
      ([e] (set trouble (string "Not saved: " e)))))

  (defn valid? [r]
    (and (struct? r) (string? (r :s)) (= 8 (length (r :s))) (string/check-set "0123456789abcdef" (r :s))
         (number? (r :n)) (= (r :n) (math/floor (r :n))) (<= 1 (r :n) 65535)
         (number? (r :l)) (string? (r :b)) (<= 1 (length (r :b)) 16) (string? (r :a)) (<= (length (r :a)) 16)
         (string? (r :t)) (<= (length (r :t)) max-text)))

  (defn start-floppy []
    (try
      (do
        (when (disk/exists? (string dir "station")) (set station (string (slurp (string dir "station")))))
        (when (disk/exists? (string dir "name")) (set name (string (slurp (string dir "name")))))
        (when (disk/exists? (string dir "posts.jdn"))
          (each line (string/split "\n" (slurp (string dir "posts.jdn")))
            (def [ok r] (protect (parse line)))
            (when (and ok (valid? r))
              (put posts [(r :s) (r :n)] r)
              (set lamport (max lamport (r :l)))))))
      ([e] (set trouble (string "No floppy for the posts: " e)))))

  # ---- the boards on the screen

  (defn order [a b] (if (= (a :l) (b :l)) (< (a :s) (b :s)) (< (a :l) (b :l))))
  (defn board-posts [b] (sort (filter |(= b ($ :b)) (values posts)) order))
  (defn boards []
    (def count @{"general" 0})
    (each r posts (put count (r :b) (+ 1 (get count (r :b) 0))))
    (sort (keys count)))

  (defn show-post [r]
    (ui/append t 3 :gray (string (if (empty? (r :a)) (r :s) (r :a)) ":\n"))
    (ui/append t 3 :white (string (r :t) "\n")))

  (var draw-board nil)
  (defn draw-boards []
    (def names (boards))
    (ui/list t 2 0 1 14 (- ui/height 2)
             (map |(string (if (= $ board) "> " "  ") $ " " (length (board-posts $))) names)
             (fn [i] (when-let [b (get names i)] (set board b) (draw-board)))))

  (set draw-board (fn []
    (ui/remove t 3)
    (ui/log t 3 15 1 (- ui/width 15) (- ui/height 2))
    (ui/append t 3 :yellow (string "#" board "\n"))
    (ui/send 'line t 4 0 (- ui/height 1) ui/width (string "#" board "> "))   # (its prompt names the board posts go to)
    (set shown (board-posts board))
    (each r shown (show-post r))
    (draw-boards)
    (status)))

  (defn arrived [r]
    "A post new to this computer: keep it, and show it if its board is open."
    (put posts [(r :s) (r :n)] r)
    (set lamport (max lamport (r :l)))
    (when (= (r :b) board)
      (if (or (empty? shown) (order (last shown) r))
        (do (array/push shown r) (show-post r) (draw-boards) (status))
        (draw-board)))   # (an older post: the board is drawn again, in order)
    (unless (= (r :b) board) (draw-boards) (status)))

  # ---- the bus

  (defn send [target index data]
    (while (not (try (bus/send target :data index data) ([_] false))) (sleep 0.05)))

  (defn held
    "The highest post number of a station held with none missing."
    [s]
    (var n 0)
    (while (get posts [s (+ n 1)]) (++ n))
    n)

  (defn station-of [s at] (hex (string/slice s at (+ at 4))))

  (defn summary []
    (def stations (distinct (map |($ :s) (values posts))))
    (each part (partition 10 stations)
      (send 0 SUMMARY (string ;(map |(string (unhex $) (u16 (held $))) part)))))

  (defn send-post [target r]
    (def body (string/format "%j" r))
    (def pieces (math/ceil (/ (length body) piece-size)))
    (for i 0 pieces
      (send target PART (string (unhex (r :s)) (u16 (r :n)) (string/from-bytes i pieces)
                                (string/slice body (* i piece-size) (min (length body) (* (+ i 1) piece-size)))))))

  (defn on-summary [p]
    (def v (p :value))
    (for k 0 (math/floor (/ (length v) 6))
      (def s (station-of v (* 6 k)))
      (def n (get-u16 v (+ 4 (* 6 k))))
      (when (> n (held s))
        (send (p :source) WANT (string (unhex s) (u16 (+ 1 (held s))))))))

  (defn on-want [p]
    (def v (p :value))
    (when (= 6 (length v))
      (def s (station-of v 0))
      (var n (get-u16 v 4))
      (var sent 0)
      (while (and (< sent 8) (get posts [s n]))
        (send-post (p :source) (get posts [s n]))
        (++ n)
        (++ sent))))

  (defn on-part [p]
    (def v (p :value))
    (when (> (length v) 8)
      (def key [(station-of v 0) (get-u16 v 4)])
      (def [i pieces] [(v 6) (v 7)])
      (unless (or (get posts key) (>= i pieces))
        (unless (get partial key) (put partial key @{}))
        (def got (get partial key))
        (put got i (string/slice v 8))
        (when (= pieces (length got))
          (put partial key nil)
          (def [ok r] (protect (parse (string ;(seq [j :range [0 pieces]] (got j))))))
          (when (and ok (valid? r) (= key [(r :s) (r :n)]))
            (save-line (string/format "%j" r))
            (arrived r))))))

  (defn post [text]
    (unless station   # (the player's timing makes two floppies' stations differ)
      (def rng (math/rng (hash (string (ticks) " " ((bus/address) 0) " " text))))
      (set station (hex (string/from-bytes ;(seq [_ :range [0 4]] (math/rng-int rng 256)))))
      (make-dir)
      (try (spit (string dir "station") station)
        ([e] (set trouble (string "Not saved: " e)))))
    (def r {:s station :n (+ 1 (held station)) :l (+ 1 lamport) :b board :a (or name "") :t text})
    (save-line (string/format "%j" r))
    (arrived r)
    (summary))

  (defn on-submit [text seq]
    (ui/busy t 4 seq true)
    (def text (string/trim text))
    (cond
      (string/has-prefix? "/board " text)
      (let [b (string/trim (string/slice text 7))]
        (when (<= 1 (length b) 16) (set board b) (draw-board)))
      (string/has-prefix? "/name " text)
      (let [n (string/trim (string/slice text 6))]
        (when (<= 1 (length n) 16)
          (set name n)
          (try (spit (string dir "name") n) ([_] nil))
          (status)))
      (> (length text) max-text) (ui/append t 3 :red (string "A post holds " max-text " characters at most.\n"))
      (not (empty? text)) (post text))
    (ui/line t 4 0 (- ui/height 1) ui/width (string "#" board "> ") on-submit)
    (ui/busy t 4 seq false))

  (ui/clear t)
  (start-floppy)
  (draw-board)
  (ui/line t 4 0 (- ui/height 1) ui/width (string "#" board "> ") on-submit)
  (ui/focus t)
  (bus/on (fn [p] (and (= :data (p :type)) (index-of (p :index) [SUMMARY WANT PART])))
          (fn [p]
            (put heard (p :source) (now))
            (case (p :index)
              SUMMARY (on-summary p)
              WANT (on-want p)
              PART (on-part p))))
  (forever
    (summary)
    (status)
    (sleep (+ 8 (* 4 (math/random))))))

(bbs)
