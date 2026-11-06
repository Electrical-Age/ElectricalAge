# A pocket calculator: click its keys, or type (digits . + - * / % Enter or =, Backspace, C to clear). The tape on the
# right keeps every sum. Start it from the Applications menu, or open it in the Files tab and press Run.

(def keys-layout
  [["MC" "MR" "M+" "M-" "C"]
   ["7" "8" "9" "/" "sqrt"]
   ["4" "5" "6" "*" "%"]
   ["1" "2" "3" "-" "1/x"]
   ["0" "." "+/-" "+" "="]])

(defn show-number [x]
  (if (or (nan? x) (= x math/inf) (= x math/-inf))
    "Error"
    (let [s (string/format "%.12g" x)]
      (if (= s "-0") "0" s))))

(defn calculator []
  (def t (proc/tab))
  (var entry "0")        # the number being typed (or the last result)
  (var acc nil)          # the number before the pending operation
  (var pending nil)      # "+" "-" "*" "/", or nil
  (var fresh true)       # the next digit starts a new number
  (var memory 0)

  (defn value [] (scan-number entry))
  (defn tape [colour s] (ui/append t 30 colour (string s "\n")))
  (defn draw []
    (ui/text t 3 1 2 :white (string/format "%34s" entry) :black)   # (right-aligned, as on a calculator)
    (ui/text t 4 1 1 :gray (string/format "%-4s%30s" (if (zero? memory) "" "M")
                                         (if pending (string (show-number acc) " " pending) "")) :black))

  (defn set-entry [x]
    (set entry (show-number x))
    (set fresh true))

  (defn unary
    "f of the number shown, onto the tape as name(x) = result."
    [name f]
    (def x (value))
    (def r (f x))
    (tape :light-gray (string name "(" (show-number x) ")"))
    (tape (if (nan? r) :red :white) (string "= " (show-number r)))
    (set-entry r))

  (defn operate [a op b]
    (case op "+" (+ a b) "-" (- a b) "*" (* a b) "/" (if (zero? b) math/nan (/ a b))))

  (defn finish
    "Do the pending operation, if any, and put it on the tape."
    []
    (when (and pending (not (= entry "Error")))
      (def b (value))
      (def r (operate acc pending b))
      (tape :light-gray (string (show-number acc) " " pending " " (show-number b)))
      (tape (if (nan? r) :red :white) (string "= " (show-number r)))
      (set-entry r)
      (set acc nil)
      (set pending nil)))

  (defn press [k]
    (def error? (= entry "Error"))
    (cond
      (= k "C") (do (set entry "0") (set acc nil) (set pending nil) (set fresh true))
      error? nil                                   # (only C clears an error)
      (and (= 1 (length k)) (<= 48 (k 0) 57))
      (if (or fresh (= entry "0"))
        (do (set entry k) (set fresh false))
        (when (< (length entry) 15) (set entry (string entry k))))
      (= k ".")
      (cond fresh (do (set entry "0.") (set fresh false))
            (not (string/find "." entry)) (set entry (string entry ".")))
      (= k "back")
      (unless fresh
        (set entry (if (<= (length entry) 1) "0" (string/slice entry 0 -2)))
        (when (= entry "-") (set entry "0")))
      (index-of k ["+" "-" "*" "/"])
      (do (if (and pending (not fresh)) (finish) (set fresh true))
          (unless (= entry "Error")
            (set acc (value))
            (set pending k)))
      (= k "=") (finish)
      (= k "+/-") (set entry (if (string/has-prefix? "-" entry) (string/slice entry 1)
                               (if (= entry "0") entry (string "-" entry))))
      (= k "sqrt") (unary "sqrt" |(if (neg? $) math/nan (math/sqrt $)))
      (= k "1/x") (unary "1/x" |(if (zero? $) math/nan (/ 1 $)))
      (= k "%") (unary "%" |(if (and pending (index-of pending ["+" "-"])) (/ (* acc $) 100) (/ $ 100)))
      (= k "MC") (set memory 0)
      (= k "MR") (set-entry memory)
      (= k "M+") (do (set memory (+ memory (value))) (set fresh true))
      (= k "M-") (do (set memory (- memory (value))) (set fresh true)))
    (draw))

  (ui/clear t)
  (ui/raw t 1 0 0 36 ui/height
          (fn [k mods]
            (press (case k
                     "enter" "=" "backspace" "back" "delete" "C" "c" "C" "x" "*" "r" "sqrt"
                     k))))
  (ui/box t 2 0 0 36 4 :black)
  (eachp [row ks] keys-layout
    (eachp [col k] ks
      (ui/button t (+ 100 (* 10 row) col) (+ 1 (* 7 col)) (+ 5 (* 2 row)) 6 k (fn [] (press k)))))
  (ui/text t 5 37 0 :gray "tape")
  (ui/log t 30 37 1 15 (- ui/height 1))
  (draw)
  (ui/focus t))

(calculator)
