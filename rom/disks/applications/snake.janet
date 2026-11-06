# Snake: arrow keys (or WASD) steer; Enter or a click starts again. Click the field first, so it gets the keys.
# Start it from the Games menu, or open a:/snake.janet in the Files tab and press Run.

(def W 30)
(def H 12)

(defn snake []
  (def t (proc/tab))
  (def rng (math/rng (ticks)))
  (var body @[])               # [x y] cells, head last
  (var dir [1 0])
  (var next-dir [1 0])
  (var food nil)
  (var score 0)
  (var alive false)
  (def rows (array/new-filled H nil))   # what each row showed last

  (defn place-food []
    (forever
      (def c [(math/rng-int rng W) (math/rng-int rng H)])
      (unless (find |(= $ c) body) (set food c) (break))))

  (defn start []
    (set body @[[3 6] [4 6] [5 6]])
    (set dir [1 0])
    (set next-dir [1 0])
    (set score 0)
    (set alive true)
    (place-food)
    (ui/text t 2 1 0 :yellow (string/format "%-28s" "score 0")))

  (defn draw []
    (def grid (seq [_ :range [0 H]] (buffer/new-filled W (chr " "))))
    (when food (put (grid (food 1)) (food 0) (chr "*")))
    (each [x y] body (put (grid y) x (chr "o")))
    (when-let [[x y] (last body)] (put (grid y) x (chr "@")))
    (for y 0 H
      (def s (string (grid y)))
      (unless (= s (rows y))
        (put rows y s)
        (ui/text t (+ 10 y) 1 (+ 1 y) :lime s))))

  (defn step []
    (set dir next-dir)
    (def [hx hy] (last body))
    (def h [(+ hx (dir 0)) (+ hy (dir 1))])
    (cond
      (or (< (h 0) 0) (>= (h 0) W) (< (h 1) 0) (>= (h 1) H) (find |(= $ h) body))
      (do (set alive false)
          (ui/text t 2 1 0 :red (string/format "%-28s" (string "game over: " score " (Enter)"))))
      (= h food)
      (do (array/push body h) (++ score) (place-food)
          (ui/text t 2 1 0 :yellow (string/format "%-28s" (string "score " score))))
      (do (array/push body h) (array/remove body 0))))

  (defn turn [d]
    (unless (and (= (d 0) (- (dir 0))) (= (d 1) (- (dir 1)))) (set next-dir d)))

  (ui/clear t)
  (ui/raw t 1 1 1 W H
    (fn [k mods]
      (case k
        "left" (turn [-1 0]) "a" (turn [-1 0])
        "right" (turn [1 0]) "d" (turn [1 0])
        "up" (turn [0 -1]) "w" (turn [0 -1])
        "down" (turn [0 1]) "s" (turn [0 1])
        "enter" (unless alive (start))
        nil))
    (fn [x y] (unless alive (start))))
  (start)
  (forever
    (when alive (step))
    (draw)
    (sleep 0.15)))

(snake)
