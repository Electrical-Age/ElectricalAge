/-
The end of a fluid step: mods.eln.fluidcore.Limiter, operation for operation.

One definition for every number type. At `Float` it is run against values dumped from the Java (`Check.lean`), where it
must agree bit for bit; over ordered fields it is what the theorems in `Proofs.lean` are about.

Java's arrays are filled by one sweep over the transfers. Here each cell's value is its own fold over the transfers, in
the same order, which performs the same additions in the same order for that cell.
-/
namespace FluidSpec.Limiter

/-- A transfer: `amt` of component `k` from node `src` to node `dst` (`none`: a fixed node, or outside), on `edge`. -/
structure Move (α : Type) where
  src : Option Nat
  dst : Option Nat
  k : Nat
  amt : α
  edge : Option Nat

variable {α : Type} [Zero α] [One α] [Add α] [Sub α] [Mul α] [Div α] [LT α] [DecidableLT α]

/-- Java's `Math.max(0, x)`, for any x but NaN. -/
def pos (x : α) : α := if 0 < x then x else 0

/-- Java's `Math.min(1, x)`, for any x but NaN. -/
def min1 (x : α) : α := if x < 1 then x else 1

/-- Java's `x == 0`, for any x but NaN. -/
def isZero (x : α) : Bool := !decide (0 < x) && !decide (x < 0)

/-- The cell of a node's component: with C components a node, the last of them the energy. -/
def cell (C node k : Nat) : Nat := C * node + k

def srcCell (C : Nat) (mv : Move α) : Option Nat := mv.src.map (cell C · mv.k)

def dstCell (C : Nat) (mv : Move α) : Option Nat := mv.dst.map (cell C · mv.k)

/-- Java: `i % C != S`. -/
def isMass (C i : Nat) : Bool := i % C != C - 1

/-- A function's values on 0..M-1, computed once: Java's arrays. -/
@[noinline] def tab (M : Nat) (f : Nat → α) : Array α := Array.ofFn (n := M) (fun i => f i.val)

/-- Reading `tab M f` (and `f` itself beyond it, so that `look (tab M f) f` is `f`: see `look_tab`). -/
def look (a : Array α) (f : Nat → α) (i : Nat) : α := if h : i < a.size then a[i] else f i

/-- The scale of the move's sender, 1 where there is none. -/
def srcScale (C : Nat) (scale : Nat → α) (mv : Move α) : α :=
  match mv.src with
  | some f => scale (cell C f mv.k)
  | none => 1

/-- Java `sums`: `mv[3] * (from >= 0 && k < S ? scale[C * from + k] : 1)`. -/
def massAmt (C : Nat) (scale : Nat → α) (mv : Move α) : α :=
  mv.amt * (if mv.k < C - 1 then srcScale C scale mv else 1)

/-- Java `sums`: `in[i]`. -/
def inAt (C : Nat) (moves : List (Move α)) (scale : Nat → α) (i : Nat) : α :=
  moves.foldl (fun acc mv => if dstCell C mv = some i then acc + massAmt C scale mv else acc) 0

/-- Java `sums`: `out[i]`. -/
def outAt (C : Nat) (moves : List (Move α)) (scale : Nat → α) (i : Nat) : α :=
  moves.foldl (fun acc mv => if srcCell C mv = some i then acc + massAmt C scale mv else acc) 0

/-- Java: `Math.max(0, y0[i] + in[i])`. -/
def have_ (C : Nat) (y0 : Nat → α) (moves : List (Move α)) (scale : Nat → α) (i : Nat) : α :=
  pos (y0 i + inAt C moves scale i)

/-- The cell sends more than it has: Java's `out[i] > have * SLACK`, for a mass cell that sends at all. -/
def overSends (C : Nat) (slack : α) (y0 : Nat → α) (moves : List (Move α)) (scale : Nat → α) (i : Nat) : Bool :=
  isMass C i && !isZero (outAt C moves scale i)
    && decide (have_ C y0 moves scale i * slack < outAt C moves scale i)

/-- One pass of the first loop at a cell: `scale[i] = pass < DOWN_PASSES ? scale[i] * (have / out[i]) : 0` if over. -/
def cutAt (C : Nat) (slack : α) (y0 : Nat → α) (moves : List (Move α)) (silence : Bool) (scale : Nat → α)
    (i : Nat) : α :=
  if overSends C slack y0 moves scale i then
    if silence then 0 else scale i * (have_ C y0 moves scale i / outAt C moves scale i)
  else scale i

def anyOver (C M : Nat) (slack : α) (y0 : Nat → α) (moves : List (Move α)) (scale : Nat → α) : Bool :=
  (List.range M).any (overSends C slack y0 moves scale)

/--
Java's first loop from pass `pass` on, `fuel` passes before the last (pass `down + M`): the scales, and the pass at
which it stopped.
-/
def settle (C M : Nat) (slack : α) (y0 : Nat → α) (moves : List (Move α)) (down : Nat) :
    Nat → Nat → (Nat → α) → (Nat → α) × Nat
  | 0, pass, scale =>
    if anyOver C M slack y0 moves scale then (fun i => if isMass C i then 0 else scale i, pass) else (scale, pass)
  | fuel + 1, pass, scale =>
    if anyOver C M slack y0 moves scale then
      let a := tab M (cutAt C slack y0 moves (decide (down ≤ pass)) scale)
      settle C M slack y0 moves down fuel (pass + 1) (look a (cutAt C slack y0 moves (decide (down ≤ pass)) scale))
    else (scale, pass)

/-- One raising pass at a cell: `s = Math.min(1, Math.max(0, y0[i] + in[i]) / raw[i]); if (s > scale[i]) scale[i] = s`. -/
def raiseAt (C : Nat) (y0 : Nat → α) (moves : List (Move α)) (raw : Nat → α) (scale : Nat → α) (i : Nat) : α :=
  if isMass C i && !isZero (raw i) then
    let s := min1 (have_ C y0 moves scale i / raw i)
    if scale i < s then s else scale i
  else scale i

/-- `n` raising passes. (Java stops at the first that raises nothing; the rest would change nothing.) -/
def raise (C M : Nat) (y0 : Nat → α) (moves : List (Move α)) (raw : Nat → α) : Nat → (Nat → α) → (Nat → α)
  | 0, scale => scale
  | n + 1, scale =>
    let a := tab M (raiseAt C y0 moves raw scale)
    raise C M y0 moves raw n (look a (raiseAt C y0 moves raw scale))

/-- Java `scales`: each cell's scale, and `passes[0]`. -/
def scales (C M : Nat) (slack : α) (down up : Nat) (y0 : Nat → α) (moves : List (Move α)) : (Nat → α) × Nat :=
  let r := settle C M slack y0 moves down (down + M) 0 (fun _ => 1)
  if r.2 ≤ down then r
  else
    let raw := tab M (outAt C moves (fun _ => 1))
    (raise C M y0 moves (look raw (outAt C moves (fun _ => 1))) up r.1, r.2)

/-- Java `apply`: `sent[g]`, an edge's masses as limited. -/
def sentAt (C : Nat) (moves : List (Move α)) (scale : Nat → α) (g : Nat) : α :=
  moves.foldl (fun acc mv => if mv.edge = some g ∧ mv.k < C - 1 then acc + mv.amt * srcScale C scale mv else acc) 0

/-- Java `apply`: `asked[g]`, an edge's masses in full. -/
def askedAt (C : Nat) (moves : List (Move α)) (g : Nat) : α :=
  moves.foldl (fun acc mv => if mv.edge = some g ∧ mv.k < C - 1 then acc + mv.amt else acc) 0

/-- The share of its masses an edge sends: `g >= 0 && asked[g] != 0 ? sent[g] / asked[g] : 1`. -/
def edgeShare (sent asked : Nat → α) (mv : Move α) : α :=
  match mv.edge with
  | some g => if isZero (asked g) then 1 else sent g / asked g
  | none => 1

/-- What a move moves in the end: a mass by its sender's scale, an energy by its edge's share. -/
def effAmt (C : Nat) (scale sent asked : Nat → α) (mv : Move α) : α :=
  if mv.k < C - 1 then mv.amt * srcScale C scale mv else mv.amt * edgeShare sent asked mv

/-- A cell after the moves, each moving `eff`: Java's `y[from] -= amt; y[to] += amt` over the list. -/
def applyAt (C : Nat) (eff : Move α → α) (moves : List (Move α)) (y0 : Nat → α) (i : Nat) : α :=
  moves.foldl (fun acc mv =>
    let acc := if srcCell C mv = some i then acc - eff mv else acc
    if dstCell C mv = some i then acc + eff mv else acc) (y0 i)

/-- Java `apply`: the cells after the step, with the scales and the pass count (`G`: the number of edges). -/
def apply (C M G : Nat) (slack : α) (down up : Nat) (y0 : Nat → α) (moves : List (Move α)) :
    (Nat → α) × (Nat → α) × Nat :=
  let r := scales C M slack down up y0 moves
  let sent := tab G (sentAt C moves r.1)
  let asked := tab G (askedAt C moves)
  let eff := effAmt C r.1 (look sent (sentAt C moves r.1)) (look asked (askedAt C moves))
  let y := tab M (applyAt C eff moves y0)
  (look y (applyAt C eff moves y0), r.1, r.2)

end FluidSpec.Limiter
