# Switchboard, Dynamo's desk (Baughn 2026-11-18). The desk is made, never saved: the drives with a disk in them (A:,
# B:, and RAM:, the ramdisk) top left, then every application found on them: a directory NAME.app holding main.janet
# and icon.pix, at a disk's top or one directory down. Double-click an application to start it (one of each: a running
# one comes back instead), a drive to open it in a Filer window, the Shell to bring it back. The desk looks at the
# drives every two seconds and lays itself out again when what is in them changed.

(def t (proc/tab))
(def sys (if (disk/exists? "a:/System/backdrop.pix") "a:/System/" "b:/System/"))

(defn picture [id file]
  (try (do (ui/pixmap id (pix/load (string sys file))) id) ([_] 0)))
(def P-BACK (picture 1 "backdrop.pix"))
(def P-FLOPPY (picture 2 "floppy.pix"))
(def P-RAM (picture 3 "ram.pix"))
(def P-DRAWER (picture 4 "drawer.pix"))
(def P-SHELL (picture 5 "shell.pix"))

(ui/title "Dynamo 1.0")
(prefs/apply (prefs/load))   # (the text mode, pens, clock and station name saved by Prefs)
(ui/backdrop P-BACK)
(ui/window t 0 1 ui/screen-width (- ui/screen-height 1) "Switchboard" [:backdrop])
(ui/clear t)

# ---- the Filer: a drive's or directory's files in a window

(defn filer
  "A Filer window on dir (\"A:/\"): click a name to select it, click it again to open it (a directory goes in, an
  application starts, a Janet file opens in the editor). Its menus copy, rename, delete, make drawers, format."
  [dir]
  (def ft (proc/tab))
  (var here dir)
  (var names @[])
  (var chosen nil)
  (var chosen-at 0)
  (defn path-of [n] (string here n))
  (defn status [s] (ui/text ft 3 0 -1 :gray (string/format "%-48s" s)))
  (var open nil)   # (show and open call each other)
  (defn show []
    (set names (try (disk/list here) ([e] @[])))
    (ui/window ft 8 4 50 14 here)
    (def count (length names))
    (ui/text ft 1 0 0 :light-gray (string/format "%-48s" (string here "  " count (if (= count 1) " item" " items"))))
    (ui/list ft 2 0 1 :fill [:fill 1] names
             (fn [i]
               (def n (get names i))
               (when n
                 (if (and (= n chosen) (< (- (now) chosen-at) 2))
                   (open n)
                   (do (set chosen n) (set chosen-at (now)) (show) (status (string "selected: " n))))))
             (find-index |(= $ chosen) names)))
  (set open
       (fn [n]
         (def p (path-of n))
         (cond
           (string/has-suffix? ".app/" n) (app/launch p)
           (string/has-suffix? "/" n) (do (set here p) (set chosen nil) (show))
           (string/has-suffix? ".janet" n) (edit p)
           (status (string n ": no tool opens this")))))
  (defn copy-to [drive]
    (when (and chosen (not (string/has-suffix? "/" chosen)))
      (def to (string drive chosen))
      (try (do (spit to (slurp (path-of chosen))) (status (string "copied to " to)))
           ([e] (status (string e))))))
  (defn ask-line [prompt]
    (def l (string/trim (string (getline prompt))))
    (if (empty? l) nil l))
  (ui/menus ft [["File" ["Open" "o"] ["Copy to RAM:" ""] ["Copy to A:" ""] ["Copy to B:" ""] nil
                 ["Rename..." ""] ["Delete" ""] ["New drawer..." ""] nil ["Parent" "p"] ["Close" "q"]]
                ["Disk" ["Format..." ""]]]
            (fn [menu item]
              (case item
                "Open" (when chosen (open chosen))
                "Copy to RAM:" (copy-to "RAM:/")
                "Copy to A:" (copy-to "A:/")
                "Copy to B:" (copy-to "B:/")
                "Rename..." (when chosen
                              (when-let [n (ask-line (string "rename " chosen " to: "))]
                                (try (disk/rename (path-of chosen) (path-of n)) ([e] (status (string e))))
                                (set chosen nil) (show)))
                "Delete" (when chosen
                           (ui/ask ft 9 "Delete" (string "Delete " chosen "?") ["Delete" "Keep"]
                                   (fn [i] (when (= i 0)
                                             (try (disk/delete (path-of (string/trimr chosen "/"))) ([e] (status (string e))))
                                             (set chosen nil) (show)))))
                "New drawer..." (when-let [n (ask-line "new drawer: ")]
                                  (try (disk/mkdir (path-of n)) ([e] (status (string e)))) (show))
                "Parent" (let [parts (filter |(not (empty? $)) (string/split "/" here))]
                           (when (> (length parts) 1)
                             (set here (string (string/join (array/slice parts 0 -2) "/") "/"))
                             (show)))
                "Close" (proc/close ft)
                "Format..." (ui/ask ft 9 "Format" (string "Erase everything on " (first (string/split "/" here)) "?") ["Format" "Cancel"]
                                    (fn [i] (when (= i 0)
                                              (def d (case (string/ascii-lower (first (string/split ":" here))) "a" :a "b" :b "ram" :ram))
                                              (try (disk/format d) ([e] (status (string e))))
                                              (show)))))))
  (show))

(defn open-filer [dir]
  (proc/start-window (string "Filer " dir) [8 4 50 14 dir ui/standard] filer dir))

# ---- the desk's icons

(def drives [[:a "A:/" "A:"] [:b "B:/" "B:"] [:ram "RAM:/" "RAM:"]])
(var shown nil)            # what the desk shows now, to notice a change
(def app-pictures @{})     # application directory -> its pixmap id
(var next-picture 100)

(defn app-picture [dir]
  (or (get app-pictures dir)
      (let [id (++ next-picture)]
        (try (do (ui/pixmap id (pix/load (string dir "icon.pix"))) (put app-pictures dir id) id)
             ([_] (put app-pictures dir 0) 0)))))

(defn survey []
  (def present (filter |(disk/present? ($ 0)) drives))
  [present (mapcat |(app/list ($ 0)) present)])

(defn open-shell
  "The Shell icon: the first Shell back from its icon, or, when it is up already, another Shell."
  []
  (if (ui/minimized? 1) (do (ui/restore 1) (ui/focus 1)) (shell/new)))

(defn layout [[present apps]]
  (ui/clear t)
  # drives down the left, top first; then the Shell
  (var y 0)
  (eachp [i [d root label]] present
    (ui/desk-icon t (+ 1 i) 1 y label (if (= d :ram) P-RAM P-FLOPPY) (fn [] (open-filer root)))
    (+= y 4))
  (ui/desk-icon t 9 1 y "Shell" P-SHELL open-shell)
  # applications in a grid to the right of them, clear of the bottom row (minimised windows' icons)
  (eachp [i dir] apps
    (def col (% i 7))   # (9 cells apart: 8-letter labels keep a gap)
    (def row (math/floor (/ i 7)))
    (ui/desk-icon t (+ 100 i) (+ 10 (* 9 col)) (* 5 row) (app/name dir) (app-picture dir) (fn [] (app/launch dir)))))

(var desk-menus nil)   # (set below: the menus mark the text mode in use)
(defn mode-item [w h] (string (if (= w ui/screen-width) "* " "  ") w " x " h))
(defn set-mode [w h]
  (ui/mode w h)
  (try (prefs/save (merge (prefs/load) {:mode [w h]})) ([_] nil))   # (the desk's Screen menu is remembered as Prefs' is)
  (ui/window t 0 1 ui/screen-width (- ui/screen-height 1) "Switchboard" [:backdrop])
  (desk-menus))
(set desk-menus
     (fn []
       (ui/menus t [["Desk" ["Open Shell" "s"] ["New Shell" "n"] ["Look at the drives" "r"] nil ["Open A:" ""] ["Open B:" ""] ["Open RAM:" ""]]
                    ["Screen" [(mode-item 80 25) ""] [(mode-item 120 40) ""]]]
                 (fn [menu item]
                   (case (string/trim item "* ")
                     "80 x 25" (set-mode 80 25)
                     "120 x 40" (set-mode 120 40)
                     "Open Shell" (do (ui/restore 1) (ui/focus 1))
                     "New Shell" (shell/new)
                     "Look at the drives" (do (set shown nil) (layout (survey)))
                     "Open A:" (open-filer "A:/")
                     "Open B:" (open-filer "B:/")
                     "Open RAM:" (open-filer "RAM:/"))))))
(desk-menus)

# (the desk's window can't be closed: its close box is the desk's to ignore)
(ui/on-close t (fn [] nil))

(forever
  (def now-shown (survey))
  (unless (deep= now-shown shown)
    (set shown now-shown)
    (layout now-shown))
  (sleep 2))
