# Prefs (Dynamo P2): the text mode, the pen set, the clock's hours and the station name. A choice shows at once;
# Save keeps it on the System disk (System/prefs.jdn) for every start, Use keeps it until the power goes, Cancel puts
# back what was there.

(def t (proc/tab))
(def before (prefs/load))
(def p (merge before {}))

(var draw nil)
(defn choose [k v]
  (put p k v)
  (prefs/apply p)
  (draw))

(defn mark [on label] (string (if on "* " "  ") label))

(set draw
  (fn []
    (ui/clear t)
    (ui/text t 1 1 0 :yellow "Screen")
    (each [i [w h]] (pairs ui/modes)
      (ui/button t (+ 10 i) (+ 1 (* 14 i)) 1 13 (mark (deep= (p :mode) [w h]) (string w " x " h)) (fn [] (choose :mode [w h]))))
    (ui/text t 2 1 3 :yellow "Pens")
    (each [i name] (pairs ui/pen-sets)
      (ui/button t (+ 20 i) (+ 1 (* 14 i)) 4 13 (mark (= (p :pens) name) name) (fn [] (choose :pens name))))
    (ui/text t 3 1 6 :yellow "Clock")
    (ui/button t 30 1 7 13 (mark (= (p :clock) 24) "24 hours") (fn [] (choose :clock 24)))
    (ui/button t 31 15 7 13 (mark (= (p :clock) 12) "12 hours") (fn [] (choose :clock 12)))
    (ui/text t 4 1 9 :yellow "Station name")
    (ui/text t 5 1 10 :light-gray (string/format "%-20s" (or (p :name) "(none)")))
    (ui/line t 6 22 10 20 "new: " (fn [text _]
                                     (def n (string/trim text))
                                     (choose :name (if (empty? n) nil (string/slice n 0 (min 20 (length n)))))))
    (ui/text t 7 1 11 :gray "The name BBS signs posts with, and others see on the bus.")
    (ui/button t 40 1 13 8 "Save" (fn []
                                    (try (do (prefs/save p) (proc/close t))
                                      ([e] (ui/text t 8 1 12 :red (string "Not saved: " e))))))
    (ui/button t 41 11 13 8 "Use" (fn [] (proc/close t)))
    (ui/button t 42 21 13 8 "Cancel" (fn [] (prefs/apply before) (proc/close t)))))

(ui/place t 6 3 46 15)
(ui/on-close t (fn [] (prefs/apply before) (proc/close t)))
(draw)
(ui/focus t)
