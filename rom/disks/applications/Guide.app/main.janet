# Guide, Dynamo's hypertext reader. A .guide file is plain text in nodes:
#
#   @node NAME "Title"
#   Words wrap to the window. A link sits in the text as @{"its label" link OTHERNAME}.
#     Lines indented by two spaces are kept as they are (code, tables).
#   @endnode
#
# The first node is the contents page. Click a link to follow it; Retrace goes back the way you came; < and > browse
# the nodes in the file's order. It opens the System disk's manual, System/Dynamo.guide (from the desk, or (guide) at
# the Shell); its Project menu opens another file.

(def t (proc/tab))
(var file (or (find disk/exists? ["A:/System/Dynamo.guide" "B:/System/Dynamo.guide"]) "A:/System/Dynamo.guide"))

(defn parse-guide
  "A guide's nodes: an array of {:name :title :lines} in file order."
  [text]
  (def nodes @[])
  (var cur nil)
  (each line (string/split "\n" text)
    (def l (string/trimr line))
    (cond
      (string/has-prefix? "@node" l)
      (let [rest (string/trim (string/slice l 5))
            sp (or (string/find " " rest) (length rest))
            name (string/slice rest 0 sp)
            title (string/trim (string/slice rest sp) " \"")]
        (set cur @{:name (string/ascii-lower name) :title (if (empty? title) name title) :lines @[]})
        (array/push nodes cur))
      (string/has-prefix? "@endnode" l) (set cur nil)
      (and (string/has-prefix? "@" l) (not (string/has-prefix? "@{" l))) nil   # (@database and the like)
      cur (array/push (cur :lines) l)))
  nodes)

(defn pieces
  "A text line's pieces: [:word w] and [:link label node], in order."
  [l]
  (def out @[])
  (var rest l)
  (forever
    (def at (string/find "@{\"" rest))
    (def before (if at (string/slice rest 0 at) rest))
    (each w (string/split " " before) (unless (empty? w) (array/push out [:word w])))
    (unless at (break))
    (def close (string/find "\"" rest (+ at 3)))
    (def end (and close (string/find "}" rest close)))
    (unless end (array/push out [:word (string/slice rest at)]) (break))
    (def label (string/slice rest (+ at 3) close))
    (def target (string/trim (string/slice rest (+ close 1) end)))
    (def target (string/trim (if (string/has-prefix? "link" target) (string/slice target 4) target)))
    (array/push out [:link label (string/ascii-lower target)])
    (set rest (string/slice rest (+ end 1))))
  out)

(defn lay-out
  "A node's lines laid out to width w: an array of rows, each an array of [x kind text target]."
  [node w]
  (def rows @[])
  (var row @[])
  (var x 0)
  (defn newline [] (array/push rows row) (set row @[]) (set x 0))
  (each l (node :lines)
    (cond
      (empty? l) (do (when (> x 0) (newline)) (newline))
      (string/has-prefix? "  " l) (do (when (> x 0) (newline))
                                     (array/push row [0 :pre (string/slice l 0 (min (length l) w)) nil])
                                     (newline))
      (do
        (def ps (pieces l))
        (def alone (and (= 1 (length ps)) (= :link ((first ps) 0))))   # (a link on a line of its own keeps its line)
        (when (and alone (> x 0)) (newline))
        (each [kind text target] ps
          (def n (length text))
          (when (and (> x 0) (> (+ x n) w)) (newline))
          (def last-seg (last row))
          (if (and (= kind :word) last-seg (= :word (last-seg 1)) (= x (+ (last-seg 0) (length (last-seg 2)) 1)))
            (put row (dec (length row)) [(last-seg 0) :word (string (last-seg 2) " " text) nil])   # (words run together: one text)
            (array/push row [x kind text target]))
          (+= x (+ n 1)))
        (when alone (newline)))))
  (when (> x 0) (newline))
  rows)

(var nodes @[])
(var at 0)          # the node shown
(var top 0)         # its first row shown
(def trail @[])     # nodes come from, for Retrace

(defn node-index [name]
  (var found nil)
  (eachp [i n] nodes (when (= (n :name) name) (set found i)))
  found)

(var show nil)
(defn go [i &opt from-trail]
  (when (and i (< -1 i (length nodes)))
    (unless (or from-trail (= i at)) (array/push trail at))
    (set at i)
    (set top 0)
    (show)))

(set show
  (fn []
    (def [cw ch] (or (ui/size t) [70 18]))
    (def node (get nodes at))
    (ui/clear t)
    (ui/button t 1 0 0 10 "Contents" (fn [] (go 0)))
    (ui/button t 2 11 0 9 "Retrace" (fn [] (when-let [i (array/pop trail)] (go i true))))
    (ui/button t 3 21 0 3 "<" (fn [] (go (- at 1))))
    (ui/button t 4 25 0 3 ">" (fn [] (go (+ at 1))))
    (if (nil? node)
      (ui/text t 5 0 2 :red (string "No guide at " file))
      (do
        (def rows (lay-out node (- cw 1)))
        (def page (- ch 3))
        (set top (max 0 (min top (- (length rows) page))))
        (ui/text t 5 29 0 :yellow (string/slice (node :title) 0 (min (length (node :title)) (max 0 (- cw 40)))))
        (ui/button t 6 (- cw 11) 0 5 "Up" (fn [] (set top (- top (- page 1))) (show)))
        (ui/button t 7 (- cw 5) 0 5 "Dn" (fn [] (set top (+ top (- page 1))) (show)))
        (var id 10)
        (for r 0 page
          (when-let [row (get rows (+ top r))]
            (each [x kind text target] row
              (case kind
                :link (let [i (node-index target)]
                        (ui/button t id x (+ 2 r) (length text) text (fn [] (go i))))
                :pre (ui/text t id x (+ 2 r) :light-gray text)
                (ui/text t id x (+ 2 r) :white text))
              (++ id))))
        (when (> (length rows) page)
          (ui/text t 8 0 (- ch 1) :gray (string/format "%d-%d of %d lines" (+ top 1) (min (length rows) (+ top page)) (length rows))))))))

(defn load-file [path]
  (set file path)
  (set nodes (try (parse-guide (slurp path)) ([_] @[])))
  (array/clear trail)
  (set at 0)
  (ui/send 'window t 0 0 0 0 (string "Guide: " (last (string/split "/" path))) ui/standard)   # (its title only)
  (show))

(ui/place t 4 2 (min 76 (- ui/screen-width 6)) (min 22 (- ui/screen-height 4)))
(ui/menus t [["Project" ["Open..." "o"] nil ["Close" "q"]]
             ["Navigate" ["Contents" "c"] ["Retrace" "r"] ["Previous node" "p"] ["Next node" "n"]]]
          (fn [menu item]
            (case item
              "Close" (proc/close t)
              "Contents" (go 0)
              "Retrace" (when-let [i (array/pop trail)] (go i true))
              "Previous node" (go (- at 1))
              "Next node" (go (+ at 1))
              "Open..." (ui/line t 9 0 (- (get (or (ui/size t) [70 18]) 1) 1) :fill "Open guide: "
                                 (fn [answer _]
                                   (ui/remove t 9)
                                   (def a (string/trim answer))
                                   (unless (empty? a) (load-file (path/resolve a (string/slice file 0 (+ 1 (or (last (string/find-all "/" file)) 0)))))))))))
(ui/on-move t (fn [cw ch] (show)))
(load-file file)
(ui/focus t)
