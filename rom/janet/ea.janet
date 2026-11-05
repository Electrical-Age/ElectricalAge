# The computer's standard library: a scheduler of tasks (fibers) that sleeps the CPU while every task waits, the
# REPL on the terminal as one of those tasks, and the bus, device, light and drive calls. Compiled at boot into the
# root environment; the REPL and programs run in environments made from it.

(def bus/types "Packet types by number." {1 :signal 2 :press 3 :power 4 :write 5 :read 6 :reply 7 :fault 8 :arrive})
(def- type-ids (invert bus/types))
(def bus/colours "The 16 colour channels, as the dyes (0 white .. 15 black)."
  [:white :orange :magenta :light-blue :yellow :lime :pink :gray
   :light-gray :cyan :purple :blue :brown :green :red :black])
(def- colour-ids (invert bus/colours))

# ---- the scheduler

(def- ready @[])       # [fiber value] to resume next
(def- sleepers @[])    # [until fiber]
(def- waiters @[])     # [pred fiber until]: waiting for a packet
(def- handlers @[])    # [pred f]: a task for every packet that matches
(def- joiners @{})     # fiber -> the fiber waiting for it to end
(def- typed @"")       # keystrokes not yet read
(var- input-fiber nil) # the task waiting for keystrokes
(var- repl-eval nil)   # the REPL's form being evaluated (Break stops it)
(def- editor-in @"")   # bytes from the screen's editor not yet taken
(var- editor-fiber nil) # the editor task, waiting for them

(defn ticks "The computer's clock: game ticks it has run (20 a second)." [] (ea/ticks))
(defn now "The computer's clock in seconds." [] (/ (ea/ticks) 20))

(defn spawn
  "Run (f ;args) as a task of its own, beside the others; returns its fiber."
  [f & args]
  (def fib (fiber/new (fn [] (f ;args)) :yeri))   # yields, errors, Break (user8); its env inherited
  (array/push ready [fib nil])
  fib)

(defn sleep
  "Wait s seconds of the computer's time; other tasks run meanwhile."
  [s]
  (yield [:sleep (+ (ea/ticks) (max 1 (math/ceil (* 20 s))))]))

(defn- print-error [err fib]
  (if fib (debug/stacktrace fib err "") (eprint "error: " err))
  (flush))

(defn- finish [f result]
  (if-let [j (get joiners f)]
    (do (put joiners f nil) (array/push ready [j result]))
    (when (= :error (result 0)) (print-error (result 1) (result 2)))))

(defn- park [f req]
  (match req
    [:sleep until] (array/push sleepers [until f])
    [:packet pred until] (array/push waiters [pred f until])
    [:input] (set input-fiber f)
    [:editor] (if (empty? editor-in) (set editor-fiber f) (array/push ready [f nil]))
    [:join g] (if (= :new (fiber/status g)) (put joiners g f)
                (if (index-of (fiber/status g) [:dead :error :user8 :interrupted])
                  (array/push ready [f [:ok nil]])
                  (put joiners g f)))
    _ (array/push ready [f nil])))

(defn- step [f v]
  (def r (ea/run f v))
  (case (fiber/status f)
    :dead (finish f [:ok r])
    :error (finish f [:error r f])
    :user8 (finish f [:error "stopped by Break" f])
    :interrupted (finish f [:error "stopped by Break" f])
    (park f r)))

(defn- drop-fiber [f]
  (defn keep [l i] (def k (filter |(not= f ($ i)) l)) (array/clear l) (array/concat l k))
  (keep sleepers 1)
  (keep waiters 1)
  (when (= input-fiber f) (set input-fiber nil)))

(defn- on-break []
  (when (and repl-eval (not (index-of (fiber/status repl-eval) [:dead :error :user8 :interrupted])))
    (def f repl-eval)
    (drop-fiber f)
    (finish f [:error "stopped by Break" f])))

(defn- packet [t]
  {:target (t 0) :source (t 1) :type (get bus/types (t 2) (t 2)) :index (t 3) :value (t 4)})

(defn- deliver [p]
  (each [pred f] handlers
    (when (pred p) (spawn f p)))
  (def still @[])
  (each w waiters
    (if ((w 0) p) (array/push ready [(w 1) p]) (array/push still w)))
  (array/clear waiters)
  (array/concat waiters still))

(defn- wake [t]
  (def s @[])
  (each [until f] sleepers (if (<= until t) (array/push ready [f nil]) (array/push s [until f])))
  (array/clear sleepers)
  (array/concat sleepers s)
  (def w @[])
  (each x waiters (if (and (x 2) (<= (x 2) t)) (array/push ready [(x 1) nil]) (array/push w x)))
  (array/clear waiters)
  (array/concat waiters w))

(defn- next-deadline []
  (var d nil)
  (each [until _] sleepers (when (or (nil? d) (< until d)) (set d until)))
  (each x waiters (when (and (x 2) (or (nil? d) (< (x 2) d))) (set d (x 2))))
  d)

(defn- run []
  (forever
    (when (ea/break?) (on-break))
    (while (def t (ea/recv)) (deliver (packet t)))
    (buffer/push typed (ea/input))
    (def e (ea/editor-input))
    (unless (empty? e)
      (buffer/push editor-in e)
      (when editor-fiber
        (array/push ready [editor-fiber nil])
        (set editor-fiber nil)))
    (when (and input-fiber (not (empty? typed)))
      (def f input-fiber)
      (set input-fiber nil)
      (array/push ready [f (string typed)])
      (buffer/clear typed))
    (wake (ea/ticks))
    (if (empty? ready)
      (ea/wait (next-deadline))
      (let [batch (array/slice ready)]
        (array/clear ready)
        (each [f v] batch (step f v))))))

# ---- the bus

(defn- type-id [t] (if (number? t) t (or (type-ids t) (error (string "no packet type " t)))))
(defn- channel [c]
  (def n (if (number? c) c (colour-ids c)))
  (if (and n (<= 0 n 15)) n (error (string "no colour channel " c))))

(defn bus/address "The computer's own address (set in its screen), and whether it is on a bus." [] (ea/address))

(defn bus/send
  "Send a packet from this computer: target (0 everyone, 17-255 a device), type (:power, :write, ... or a number),
  index and value as the type has them."
  [target type &opt index value]
  (or (ea/send target (type-id type) (or index 0) (or value 0)) (error "too many packets this tick")))

(defn bus/raw-send
  "Send a packet with any source address (the raw API: it can pretend to be another device)."
  [target source type &opt index value]
  (or (ea/send target (type-id type) (or index 0) (or value 0) source) (error "too many packets this tick")))

(defn bus/level "A colour channel's level, 0..1 (:red, or 0-15)." [c] (ea/level (channel c)))

(defn bus/set
  "Set a colour channel's level, 0..1: it holds until someone else writes it."
  [c level]
  (bus/send (+ 1 (channel c)) :signal 0 level))

(defn bus/wait
  "Wait for a packet for this computer that pred accepts (any, if pred is nil); nil after timeout seconds."
  [&opt pred timeout]
  (yield [:packet (or pred (fn [_] true)) (if timeout (+ (ea/ticks) (math/ceil (* 20 timeout))))]))

(defn bus/on
  "Call (f packet) in a task of its own for every packet whose type is type (or that pred accepts, given a function)."
  [type-or-pred f]
  (def pred (if (function? type-or-pred) type-or-pred
              (let [t (get bus/types (type-id type-or-pred))] (fn [p] (= t (p :type))))))
  (array/push handlers [pred f])
  nil)

(defn bus/on-press
  "Call (f index) in a task of its own whenever the button (or panel) at address is pressed."
  [address f]
  (bus/on (fn [p] (and (= :press (p :type)) (= address (p :source)))) (fn [p] (f (p :index)))))

(defn bus/handlers-clear "Forget every bus/on and bus/on-press handler." [] (array/clear handlers))

(defn light/set "Light the light (or a button's lamp) at address, 0..1." [address level] (bus/send address :power 0 level))

(defn device/write "Set register reg of the device at address." [address reg value] (bus/send address :write reg value))

(defn device/read
  "Read register reg of the device at address; an error if it doesn't answer within a second."
  [address reg]
  (bus/send address :read reg 0)
  (def p (bus/wait (fn [p] (and (= :reply (p :type)) (= address (p :source)) (= reg (p :index)))) 1))
  (if p (p :value) (error (string "no reply from " address))))

# ---- the Linear Servo Drive (its registers: 0 target, 1 position, 2 speed limit, 3 state, 4 load)

(def drive/states "The drive's state register." {0 :idle 1 :in-position 2 :moving 3 :fault})

(defn drive/position "The car's position at the drive at address, in blocks above its foot." [address]
  (device/read address 1))

(defn drive/state "The drive's state: :idle, :in-position, :moving or :fault." [address]
  (get drive/states (math/round (device/read address 3)) :unknown))

(defn drive/move
  "Send the car of the drive at address to height (blocks above its foot) and wait until it is there; an error on a
  fault, or if it isn't there after timeout seconds (default 60)."
  [address height &opt timeout]
  (def until (+ (now) (or timeout 60)))
  (device/write address 0 height)
  (sleep 0.1)
  (forever
    # (a reply lost to a bus being rebuilt, as the car's parts come and go, is asked again)
    (def s (try (drive/state address) ([_] :no-reply)))
    (when (= s :fault) (error (string "drive " address ": fault")))
    (when (and (= s :in-position) (< (math/abs (- (try (drive/position address) ([_] math/inf)) height)) 0.01)) (break))
    (when (> (now) until) (error (string "drive " address ": stalled")))
    (sleep 0.1))
  height)

# ---- the floppies (A: and B:, FAT12): slurp, spit, file/open and import work on their paths, e.g. "a:/main.janet"

(defn disk/format "Format the floppy in drive (:a or :b), erasing it; label it if given (11 letters at most)."
  [drive &opt label]
  (if label (ea/disk-format drive label) (ea/disk-format drive)))
(defn disk/list "The names in a directory of a floppy (a path, or :a or :b; default A:/); directories end in /."
  [&opt path]
  (ea/disk-list (case path nil "A:/" :a "A:/" :b "B:/" path)))
(defn disk/label "The floppy's label, or nil." [drive] (ea/disk-label drive))
(defn disk/free "Bytes free on the floppy." [drive] (ea/disk-free drive))
(defn disk/delete "Delete a file (or an empty directory)." [path] (ea/disk-delete path))
(defn disk/rename "Rename or move a file on its floppy." [from to] (ea/disk-rename from to))
(defn disk/mkdir "Make a directory." [path] (ea/disk-mkdir path))
(defn disk/exists? "Is there a file at path?" [path]
  (try (do (file/close (file/open path :rb)) true) ([_] false)))

# file/open, slurp and spit say why a floppy refused (no disk, not formatted, write-protected)
(def- core-open file/open)
(defn file/open "Open a file (see the core file/open); on a floppy, an error says why it failed."
  [path &opt mode buffer-size]
  (def f (try (core-open path (or mode :r) ;(if buffer-size [buffer-size] []))
           ([err] (error (string err ": " (ea/disk-why))))))
  (or f (error (string "could not open " path ": " (ea/disk-why)))))
(defn slurp "Read a whole file into a buffer." [path]
  (with [f (file/open path :rb)] (file/read f :all)))
(defn spit "Write contents to a file (mode :wb, or :ab to append)." [path contents &opt mode]
  (with [f (file/open path (or mode :wb))] (file/write f contents)) nil)

# ---- the terminal

(defn alarm "Say something is wrong, on the terminal." [& msg]
  (print "ALARM: " ;msg)
  (flush))

(defn read-line "Wait for a line typed on the terminal." []
  (yield [:input]))

(defn- eval-form [form env]
  (def thunk (compile form env "repl"))
  (if (function? thunk) (thunk) (error (get thunk :error))))

(defn- repl [env]
  (def p (parser/new))
  (forever
    (def open (parser/state p :delimiters))
    (prin (if (empty? open) "> " (string open "> ")))
    (flush)
    (def s (yield [:input]))
    (prin s)
    (flush)
    (parser/consume p s)
    (while (parser/has-more p)
      (def form (parser/produce p))
      (set repl-eval (spawn eval-form form env))
      (def res (yield [:join repl-eval]))
      (set repl-eval nil)
      (match res
        [:ok v] (do (put env '_ @{:value v}) (pp v))
        [:error e f] (print-error e f)))
    (when (= :error (parser/status p))
      (print "parse error: " (parser/error p))
      (parser/flush p))))

# ---- the screen's editor: framed requests on the editor channel, "LENGTH\n" then fields joined by NUL:
# op, id, path, revision, body. The revision is the file's hash, so a save over a file changed meanwhile is refused.

(defn- editor-send [& fields]
  (def payload (string/join (map string fields) "\0"))
  (def frame (string (length payload) "\n" payload))
  (var i 0)
  (while (< i (length frame))
    (+= i (ea/editor-write frame i))
    (when (< i (length frame)) (sleep 0.05))))

(defn- revision [text] (string (hash text)))

(defn run-file
  "Run a Janet file's forms one after another in this task, so the program may sleep and wait on the bus at its top
  level (dofile would treat a wait as the end of a form). A fresh environment unless env is given."
  [path &opt env]
  (default env (make-env (curenv)))
  (def p (parser/new))
  (parser/consume p (slurp path))
  (parser/eof p)
  (while (parser/has-more p)
    (def form (parser/produce p))
    (def thunk (compile form env path))
    (if (function? thunk) (thunk)
      (error (string path ":" (get thunk :line "?") ": " (get thunk :error)))))
  (when (= :error (parser/status p)) (error (string path ": " (parser/error p)))))

(defn- editor-handle [env op id path rev body]
  (try
    (case op
      "list" (editor-send "list" id path "" (string/join (disk/list path) "\n"))
      "open" (do
               (unless (disk/exists? path)
                 (def why (ea/disk-why))
                 (error (if (= why "no such file") (string "no such file: " path) (string path ": " why))))
               (def t (string (slurp path)))
               (editor-send "file" id path (revision t) t))
      "save" (do
               (def now (if (disk/exists? path) (string (slurp path))))
               (if (and now (not= rev "") (not= rev (revision now)))
                 (editor-send "conflict" id path (revision now) "")
                 (do (spit path body) (editor-send "saved" id path (revision body) ""))))
      "run" (do (spawn run-file path (make-env env)) (editor-send "ran" id path "" ""))
      (editor-send "error" id path "" (string "unknown request " op)))
    ([err] (editor-send "error" id path "" (string err)))))

(defn- editor [env]
  (forever
    (yield [:editor])
    (forever
      (def nl (string/find "\n" editor-in))
      (unless nl (break))
      (def n (scan-number (string/slice editor-in 0 nl)))
      (unless (and n (>= (length editor-in) (+ nl 1 n))) (break))
      (def fields (string/split "\0" (string/slice editor-in (+ nl 1) (+ nl 1 n)) 0 5))
      (def rest (string/slice editor-in (+ nl 1 n)))
      (buffer/clear editor-in)
      (buffer/push editor-in rest)
      (when (= 5 (length fields)) (editor-handle env ;fields)))))

(defn- autorun [env]
  (each p ["A:/main.janet" "B:/main.janet"]
    (when (disk/exists? p)
      (print "Running " p)
      (spawn run-file p (make-env env))
      (break))))

(defn ea/start
  "Start the scheduler: a floppy's main.janet (A: first, then B:) if there is one, and the REPL on the terminal (the
  ROM does this at boot)."
  []
  (def env (make-env (curenv)))
  (autorun env)
  (spawn editor env)
  (spawn repl env)
  (run))
