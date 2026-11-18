# The Applications floppy: a menu of the programs on it, in a window of its own. Each program is a file of its own;
# open one with Files (the Project menu, or (files) in the Shell) to read it, change it, or press Run there.

(def dir (if (disk/exists? "a:/startup.janet") "a:/" "b:/"))
(def t (proc/tab))
(def programs
  [["Calculator" "calculator.janet" "a pocket calculator with a tape"]
   ["BBS" "bbs.janet" "message boards shared over the bus"]
   ["Snake" "snake.janet" "arrow keys; best on a nearby computer"]
   ["Gorillas" "gorillas.janet" "two computers, one bus: throw bananas"]])

(ui/title "Dynamo 1.0   Grid Cooperative Computing")
(ui/window t 4 3 56 12 "Applications")
(ui/place t 4 3 56 12)
(ui/clear t)
(ui/text t 1 1 1 :yellow "Applications")
(eachp [i [name file about]] programs
  (ui/button t (+ 10 i) 1 (+ 3 (* 2 i)) 12 name (fn [] (proc/start name run-file (string dir file))))
  (ui/text t (+ 20 i) 14 (+ 3 (* 2 i)) :light-gray about))
(ui/text t 2 1 -1 :gray "Snake and Gorillas: click the field first.")
(ui/menus t [["Project" ["Files" "f"] ["Shell" "s"] nil ["Close" "q"]]]
          (fn [menu item]
            (case item
              "Files" (files dir)
              "Shell" (ui/focus 1)
              "Close" (proc/close t))))
