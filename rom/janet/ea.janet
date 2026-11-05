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
(def- tasks @{})       # every task alive -> true
(var- broke nil)       # a Break already stopped the task that was running
(var- break-pending nil) # Break was pressed; the screen's (break tab) says for which tab
(def- procs @{})       # tab -> @{:name :env :tab}: processes, each with a tab of its own
(var- root-env-of-os nil) # the environment the stdlib was compiled in (processes start from it)
(def- system @{})      # the REPL task, which Break leaves alone
(def- display-in @"")  # bytes from the screen not yet read as lines
(def- display-out @"") # messages for the screen not yet sent (its channel was full)
(def- outputs @[])     # [buffer tab log colour]: print output to move into a log
(def- ui-handlers @{}) # [tab id] -> the function for its events

(defn ticks "The computer's clock: game ticks it has run (20 a second)." [] (ea/ticks))
(defn now "The computer's clock in seconds." [] (/ (ea/ticks) 20))

(defn spawn
  "Run (f ;args) as a task of its own, beside the others; returns its fiber."
  [f & args]
  (def fib (fiber/new (fn [] (f ;args)) :yeri))   # yields, errors, Break (user8); its env inherited
  (put tasks fib true)
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
  (put tasks f nil)
  (if-let [j (get joiners f)]
    (do (put joiners f nil) (array/push ready [j result]))
    (when (= :error (result 0)) (print-error (result 1) (result 2)))))

(defn- park [f req]
  (match req
    [:sleep until] (array/push sleepers [until f])
    [:packet pred until] (array/push waiters [pred f until])
    [:input] (set input-fiber f)
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
    :user8 (do (set broke true) (finish f [:error "stopped by Break" f]))
    :interrupted (do (set broke true) (finish f [:error "stopped by Break" f]))
    (park f r)))

(defn- drop-fiber [f]
  (defn keep [l i] (def k (filter |(not= f ($ i)) l)) (array/clear l) (array/concat l k))
  (keep sleepers 1)
  (keep waiters 1)
  (when (= input-fiber f) (set input-fiber nil)))

(defn- stop-tasks [stop]
  (each f stop
    (drop-fiber f)
    (put tasks f nil)
    (put joiners f nil))
  (def keep (filter |(not (index-of ($ 0) stop)) ready))
  (array/clear ready)
  (array/concat ready keep))

(defn- on-break
  "The REPL's Break (tab 1): it stops the REPL's form if one is running; else every program's task, and the bus
  handlers they set. A Break that interrupted a running task has already stopped it."
  []
  (if broke (set broke nil)
  (if (and repl-eval (get tasks repl-eval))
    (let [f repl-eval]
      (drop-fiber f)
      (finish f [:error "stopped by Break" f]))
    (do
      (def stop (filter |(not (get system $)) (keys tasks)))
      (stop-tasks stop)
      (array/clear handlers)
      (print "\nBreak: stopped " (length stop) " task" (if (= 1 (length stop)) "" "s") " and the bus handlers.")
      (flush)))))

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

# ---- the screen's channel (the display protocol: one Janet form per line, both ways; the handbook's "The Screen
# Protocol"). Defined before the scheduler, which drains it.

(defn- display-drain "Send what is waiting for the screen, as far as its channel has room." []
  (unless (empty? display-out)
    (def n (ea/display-write display-out))
    (def rest (buffer/slice display-out n))
    (buffer/clear display-out)
    (buffer/push display-out rest)))

(def- quote-str ea/quote)

(defn- fmt [x]
  (cond
    (or (string? x) (buffer? x)) (quote-str x)
    (keyword? x) (string ":" x)
    (number? x) (string/format "%d" (math/round x))
    (= x true) "true"
    (= x false) "false"
    (nil? x) "nil"
    (indexed? x) (string "[" (string/join (map fmt x) " ") "]")
    (quote-str (string x))))

(defn ui/send
  "Send one message to the screen, e.g. (ui/send 'text 1 2 0 0 :white \"hi\"). Messages queue in RAM while the
  screen's channel is full; a task sending a lot waits for it to drain."
  [verb & args]
  (buffer/push display-out "(" (string verb))
  (each a args (buffer/push display-out " " (fmt a)))
  (buffer/push display-out ")\n")
  (display-drain)
  (when (and (> (length display-out) 32768) (fiber/current) (get tasks (fiber/current)))
    (while (> (length display-out) 4096) (sleep 0.05))))

(defn- flush-outputs "Move what tasks printed into their logs." []
  (each [buf t id colour] outputs
    (unless (empty? buf)
      (def s (string buf))
      (buffer/clear buf)
      (ui/send 'append t id colour s))))

(defn- env-of? [f env]
  (var e (fiber/getenv f))
  (var found false)
  (while e
    (when (= e env) (set found true) (break))
    (set e (table/getproto e)))
  found)

(defn- proc-stop "Stop every task of the process in tab t." [t]
  (def p (get procs t))
  (when p
    (def stop (filter |(env-of? $ (p :env)) (keys tasks)))
    (stop-tasks stop)
    (length stop)))

(defn- proc-close "Stop the process in tab t and remove its tab." [t]
  (proc-stop t)
  (def p (get procs t))
  (put procs t nil)
  (def keep (filter |(not= t ($ 1)) outputs))
  (array/clear outputs)
  (array/concat outputs keep)
  (eachk k ui-handlers (when (= t (k 0)) (put ui-handlers k nil)))
  (ui/send 'untab t))

(defn- screen-break "Break for tab t: the REPL's (tab 1) or a process's." [t]
  (set break-pending nil)
  (if (get procs t)
    (do
      (if broke (set broke nil))
      (def n (proc-stop t))
      (def p (get procs t))
      (buffer/push ((p :env) :err) (string "\nBreak: stopped " n " task" (if (= 1 n) "" "s") ".\n")))
    (on-break)))

(defn- screen-event [m]
  (when (and (tuple? m) (> (length m) 2) (number? (m 1)) (number? (m 2)))
    (def f (get ui-handlers [(m 1) (m 2)]))
    (case (m 0)
      'submit (when (and f (string? (get m 4))) (spawn f (m 4) (get m 3 0)))
      'press (when f (spawn f))
      'select (when (and f (number? (get m 3))) (spawn f (m 3)))
      'save (when (and f (string? (get m 4)) (indexed? (get m 5))) (spawn f (get m 3 0) (m 4) (m 5)))
      nil))
  (when (and (tuple? m) (> (length m) 1) (number? (m 1)))
    (case (m 0)
      'break (screen-break (m 1))
      'close (when (get procs (m 1)) (proc-close (m 1)))
      nil)))

(defn- screen-events "Read the screen's events and run their handlers as tasks." []
  (buffer/push display-in (ea/display-input))
  (forever
    (def nl (string/find "\n" display-in))
    (unless nl (break))
    (def line (string/slice display-in 0 nl))
    (def rest (string/slice display-in (+ nl 1)))
    (buffer/clear display-in)
    (buffer/push display-in rest)
    (def [ok m] (protect (parse line)))
    (when ok (screen-event m))))

(defn- run []
  (forever
    (when (ea/break?) (set break-pending true))
    (while (def t (ea/recv)) (deliver (packet t)))
    (buffer/push typed (ea/input))
    (screen-events)
    (when (and input-fiber (not (empty? typed)))
      (def f input-fiber)
      (set input-fiber nil)
      (array/push ready [f (string typed)])
      (buffer/clear typed))
    (wake (ea/ticks))
    (flush-outputs)
    (display-drain)
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
  # a wire on the drive's setpoint pin overrides the bus (AdventurAgent's rig kept its trimmer): say so
  (def t (try (device/read address 0) ([_] nil)))
  (when (and t (> (math/abs (- t height)) 0.01))
    (error (string "drive " address " keeps target " t ": a wire on its setpoint pin overrides the bus")))
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

# ---- the screen's elements (see the handbook's "The Screen Protocol")

(def ui/width "The screen's width in character cells." 52)
(def ui/height "The screen's height in character cells." 17)

(defn ui/tab "Add tab t (1-255), or rename it." [t title] (ui/send 'tab t title))
(defn ui/untab "Remove tab t." [t]
  (eachk k ui-handlers (when (= t (k 0)) (put ui-handlers k nil)))
  (ui/send 'untab t))
(defn ui/focus "Bring tab t forward." [t] (ui/send 'focus t))
(defn ui/clear "Empty tab t." [t] (ui/send 'clear t))
(defn ui/box "A filled rectangle, in cells." [t id x y w h colour] (ui/send 'box t id x y w h colour))
(defn ui/text "A line of text; bg: a background colour." [t id x y colour s &opt bg]
  (if bg (ui/send 'text t id x y colour s bg) (ui/send 'text t id x y colour s)))
(defn ui/log "A scrolling transcript." [t id x y w h] (ui/send 'log t id x y w h))
(defn ui/append "Add text to a log (it may hold newlines)." [t id colour s] (ui/send 'append t id colour s))
(defn ui/line
  "An editable line: the player edits it on their own client; Enter calls (on-submit text seq) as a task."
  [t id x y w prompt on-submit]
  (put ui-handlers [t id] on-submit)
  (ui/send 'line t id x y w prompt))
(defn ui/busy "A line's busy mark for submission seq." [t id seq on] (ui/send 'busy t id seq on))
(defn ui/button "A button: a click calls (on-press) as a task." [t id x y w label on-press]
  (put ui-handlers [t id] on-press)
  (ui/send 'button t id x y w label))
(defn ui/list "A list of rows: a click calls (on-select index) as a task." [t id x y w h items on-select]
  (put ui-handlers [t id] on-select)
  (ui/send 'list t id x y w h items))
(defn ui/remove "Remove an element." [t id]
  (put ui-handlers [t id] nil)
  (ui/send 'remove t id))
(defn ui/dict "Names the screen may complete in tab t (0: every tab)." [t names] (ui/send 'dict t names))
(defn ui/output
  "Send what tasks running in env print (and eprint, in red) into log id of tab t."
  [env t id]
  (def out @"")
  (def err @"")
  (put env :out out)
  (put env :err err)
  (array/push outputs [out t id :white] [err t id :red])
  env)

(defn alarm "Say something is wrong, in red." [& msg]
  (eprint "ALARM: " ;msg))

(defn read-line "Wait for a line typed on the console." []
  (yield [:input]))

(defn spawn-in
  "Run (f ;args) as a task whose environment (its definitions, where it prints) is env."
  [env f & args]
  (def fib (spawn f ;args))
  (fiber/setenv fib env)
  fib)

(defn- eval-form [form env]
  (def thunk (compile form env "repl"))
  (if (function? thunk) (thunk) (error (get thunk :error))))

(defn- core-names [] (ea/names root-env))

(defn- repl-tab
  "The REPL: tab 1, a log over an editable line. The line is edited on the player's client; Enter sends it, the OS
  echoes it into the log, evaluates each form as a task, and prints the value."
  [env]
  (def t 1)
  (def p (parser/new))
  (ui/output env t 1)
  (ui/tab t "REPL")
  (ui/log t 1 0 0 ui/width (- ui/height 1))
  (var taken false)
  (defn on-submit [text seq]
    (put system (fiber/current) true)   # the REPL's own machinery: Break stops what it runs, not it
    (while taken (sleep 0.05))   # one submission at a time, in order
    (set taken true)
    (ui/busy t 2 seq true)
    (ui/append t 1 :gray (string (if (empty? (parser/state p :delimiters)) "> " "  ") text "\n"))
    (parser/consume p (string text "\n"))
    (while (parser/has-more p)
      (def form (parser/produce p))
      (set repl-eval (spawn-in env eval-form form env))
      (def res (yield [:join repl-eval]))
      (set repl-eval nil)
      (flush-outputs)
      (match res
        [:ok v] (do (put env '_ @{:value v}) (buffer/push (env :out) (string/format "%q\n" v)))
        [:error e f] (if (= e "stopped by Break")
                       (buffer/push (env :err) "stopped by Break\n")
                       (with-dyns [:err (env :err)] (debug/stacktrace f e "")))))
    (when (= :error (parser/status p))
      (buffer/push (env :err) (string "parse error: " (parser/error p) "\n"))
      (parser/flush p))
    (flush-outputs)
    (def open (parser/state p :delimiters))
    (ui/line t 2 0 (- ui/height 1) ui/width (if (empty? open) "> " (string open "> ")) on-submit)
    (ui/busy t 2 seq false)
    (ui/dict t (ea/names env))   # the REPL's own definitions, for completion
    (set taken false)
    (put system (fiber/current) nil))
  (ui/dict 0 (core-names))
  (ui/line t 2 0 (- ui/height 1) ui/width "> " on-submit))

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

(defn proc/new-tab
  "A tab for a process of its own, empty (proc/start puts a log in it): returns its number. The process's environment
  is ((proc/env t))."
  [name]
  (var t 2)
  (while (or (get procs t) (= t 1)) (++ t))
  (when (> t 255) (error "too many processes"))
  (put procs t @{:name name :env (make-env root-env-of-os) :tab t})
  (ui/tab t name)
  t)

(defn proc/env "The environment of the process in tab t." [t] (get-in procs [t :env]))

(defn proc/start
  "Start a process: (f ;args) as its first task, in an environment of its own, with a tab of its own (a log over the
  whole tab) where what it prints goes. Returns its tab. Break on that tab stops its tasks; closing the tab ends it."
  [name f & args]
  (def t (proc/new-tab name))
  (def env ((get procs t) :env))
  (ui/log t 1 0 0 ui/width ui/height)
  (ui/output env t 1)
  (spawn-in env f ;args)
  t)

(defn proc/list "The processes: [tab name tasks] each." []
  (seq [[t p] :pairs procs] [t (p :name) (length (filter |(env-of? $ (p :env)) (keys tasks)))]))

(defn proc/stop "Stop every task of the process in tab t (its tab stays)." [t] (proc-stop t))
(defn proc/close "End the process in tab t and remove its tab." [t] (proc-close t))

(defn proc/tab "The tab of the process this task belongs to, or nil (the REPL's own tasks: 1)." []
  (var found nil)
  (eachp [t p] procs (when (env-of? (fiber/current) (p :env)) (set found t)))
  found)

# ---- editing files: an editor tab (the editor runs on the player's client; a save sends hunks against a revision)

(defn- apply-hunks
  "Apply hunks [start deleted [lines...]] to text: line numbers of the text they were made against (0-based), top to
  bottom; each replaces deleted whole lines with its lines. Out of order or out of range: an error."
  [text hunks]
  (def lines (string/split "\n" text))
  (def out @[])
  (var at 0)
  (each h hunks
    (def [start del ins] h)
    (unless (and (number? start) (number? del) (indexed? ins) (<= at start) (>= del 0) (<= (+ start del) (length lines)))
      (error "a hunk out of range"))
    (array/concat out (array/slice lines at start))
    (each l ins (array/push out (string l)))
    (set at (+ start del)))
  (array/concat out (array/slice lines at))
  (string/join out "\n"))

(defn edit
  "Open a file in an editor tab (a new file if there is none). The player edits it on their own client; Ctrl+S saves
  (refused once if the file changed on the disk meanwhile), Run runs the saved file as a process, Close closes."
  [path]
  (def text (if (disk/exists? path) (string (slurp path)) ""))
  (when (> (length text) 32768) (error (string path " is too big to edit (32 KB at most)")))
  (def t (proc/new-tab (last (string/split "/" path))))
  (var rev (revision text))
  (ui/text t 1 0 0 :light-gray (string path " (Ctrl+S saves)"))
  (ui/button t 2 (- ui/width 13) 0 6 "Run" (fn [] (proc/start (last (string/split "/" path)) run-file path)))
  (ui/button t 3 (- ui/width 6) 0 6 "Close" (fn [] (proc/close t)))
  (put ui-handlers [t 4]
       (fn [seq base hunks]
         (try
           (do
             (def now (if (disk/exists? path) (string (slurp path)) ""))
             (if (not= base (revision now))
               (ui/send 'conflict t 4 seq (revision now) now)
               (do
                 (def new (apply-hunks now hunks))
                 (spit path new)
                 (set rev (revision new))
                 (ui/send 'saved t 4 seq rev))))
           ([err] (ui/text t 1 0 0 :red (string path ": " err))))))
  (ui/send 'editor t 4 0 1 ui/width (- ui/height 1) :janet path rev text)
  t)

(defn files
  "A tab listing a floppy's files (default A:/): pick one to edit it, or a directory to go into it."
  [&opt dir]
  (default dir "A:/")
  (def t (proc/new-tab (string "files " dir)))
  (defn show [dir]
    (def names (try (disk/list dir) ([e] @[(string e)])))
    (ui/text t 1 0 0 :light-gray (string dir "  (" (length names) ")"))
    (ui/list t 2 0 1 ui/width (- ui/height 1) names
             (fn [i]
               (def n (get names i))
               (when n
                 (if (string/has-suffix? "/" n) (show (string dir n)) (edit (string dir n)))))))
  (show dir)
  t)

(defn- autorun [env]
  (each p ["A:/startup.janet" "B:/startup.janet" "A:/main.janet" "B:/main.janet"]
    (when (disk/exists? p)
      (buffer/push (env :out) (string "Running " p " (its own tab)\n"))
      (proc/start (last (string/split "/" p)) run-file p)
      (break))))

(defn ea/start
  "Start the scheduler: the REPL tab, and a floppy's startup.janet (A: first, then B:) if there is one (the ROM does
  this at boot)."
  []
  (set root-env-of-os (curenv))
  (def env (make-env (curenv)))
  (ui/send 'screen ui/width ui/height)
  (repl-tab env)
  (autorun env)
  (run))
