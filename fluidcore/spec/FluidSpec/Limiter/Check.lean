import FluidSpec.Limiter.Impl
/-
Runs the Lean limiter at `Float` on the calls dumped by the Java tests (LimiterTap, FLUIDCORE_VECTORS) and compares
scales, results and pass counts bit for bit. `--control` runs a limiter that is wrong in the last place instead, which must
fail.
-/
open FluidSpec.Limiter

def hexVal (c : Char) : Nat :=
  if '0' ≤ c ∧ c ≤ '9' then c.toNat - '0'.toNat else c.toNat - 'a'.toNat + 10

def parseHex (s : String) : Float :=
  Float.ofBits (UInt64.ofNat (s.foldl (fun n c => 16 * n + hexVal c) 0))

def parseIdx (s : String) : Option Nat :=
  if s.startsWith "-" then none else some s.toNat!

structure Vec where
  source : String
  C : Nat
  M : Nat
  down : Nat
  up : Nat
  slack : Float
  passes : Nat
  y0 : Array Float
  moves : List (Move Float)
  scale : Array Float
  y : Array Float

def parseVec (line : String) : Vec :=
  let t := (line.splitOn " ").toArray
  let C := t[2]!.toNat!
  let M := t[3]!.toNat!
  let n := t[8]!.toNat!
  let y0 := (Array.range M).map fun i => parseHex t[9 + i]!
  let mb := 9 + M
  let moves := (List.range n).map fun j =>
    { src := parseIdx t[mb + 5 * j]!, dst := parseIdx t[mb + 5 * j + 1]!, k := t[mb + 5 * j + 2]!.toNat!,
      amt := parseHex t[mb + 5 * j + 3]!, edge := parseIdx t[mb + 5 * j + 4]! : Move Float }
  let sb := mb + 5 * n
  { source := t[1]!, C, M, down := t[4]!.toNat!, up := t[5]!.toNat!, slack := parseHex t[6]!, passes := t[7]!.toNat!,
    y0, moves, scale := (Array.range M).map fun i => parseHex t[sb + i]!,
    y := (Array.range M).map fun i => parseHex t[sb + M + i]! }

/--
Doubles that divide by multiplying with the reciprocal, which rounds differently now and then: a limiter that is wrong
only in the last place. The control: the comparison must catch it.
-/
structure Skew where
  v : Float

instance : Zero Skew := ⟨⟨0⟩⟩
instance : One Skew := ⟨⟨1⟩⟩
instance : Add Skew := ⟨fun a b => ⟨a.v + b.v⟩⟩
instance : Sub Skew := ⟨fun a b => ⟨a.v - b.v⟩⟩
instance : Mul Skew := ⟨fun a b => ⟨a.v * b.v⟩⟩
instance : Div Skew := ⟨fun a b => ⟨a.v * (1 / b.v)⟩⟩
instance : LT Skew := ⟨fun a b => a.v < b.v⟩
instance : DecidableLT Skew := fun a b => inferInstanceAs (Decidable (a.v < b.v))

def edges (moves : List (Move Float)) : Nat :=
  moves.foldl (fun g mv => match mv.edge with | some e => max g (e + 1) | none => g) 0

/-- The Lean limiter on the call: results, scales, passes. -/
def run (v : Vec) : (Nat → Float) × (Nat → Float) × Nat :=
  apply v.C v.M (edges v.moves) v.slack v.down v.up (fun i => v.y0[i]!) v.moves

def runSkew (v : Vec) : (Nat → Float) × (Nat → Float) × Nat :=
  let r := apply (α := Skew) v.C v.M (edges v.moves) ⟨v.slack⟩ v.down v.up (fun i => ⟨v.y0[i]!⟩)
    (v.moves.map fun mv => { src := mv.src, dst := mv.dst, k := mv.k, amt := ⟨mv.amt⟩, edge := mv.edge })
  (fun i => (r.1 i).v, fun i => (r.2.1 i).v, r.2.2)

/-- The first disagreement with Java, if any. -/
def check (control : Bool) (v : Vec) : Option String :=
  let (y, scale, passes) := if control then runSkew v else run v
  if passes != v.passes then some s!"passes {passes}, Java {v.passes}"
  else
    (List.range v.M).firstM (fun i =>
      if (scale i).toBits != v.scale[i]!.toBits then some s!"scale[{i}] {scale i}, Java {v.scale[i]!}"
      else if (y i).toBits != v.y[i]!.toBits then some s!"y[{i}] {y i}, Java {v.y[i]!}"
      else none)

def kind (v : Vec) : String :=
  if v.passes == 0 then "unlimited" else if v.passes < v.down then "scaled"
  else if v.passes == v.down then "scaled at the last pass"
  else if v.passes < v.down + v.M then "silenced" else "silenced until the pass cap"

def main (args : List String) : IO UInt32 := do
  let control := args.contains "--control"
  let file := (args.filter (· != "--control")).head!
  let lines := (← IO.FS.lines file).filter (·.startsWith "V ")
  let mut bad := 0
  let mut counts : List (String × Nat) := []
  for line in lines do
    let v := parseVec line
    let key := kind v
    counts := match counts.find? (·.1 == key) with
      | some _ => counts.map fun (k, n) => if k == key then (k, n + 1) else (k, n)
      | none => counts ++ [(key, 1)]
    match check control v with
    | some msg =>
      bad := bad + 1
      if bad ≤ 3 then IO.println s!"MISMATCH ({v.source}, {key}): {msg}"
    | none => pure ()
  IO.println s!"{lines.size} calls: {counts}; mismatches {bad}"
  return if bad == 0 then 0 else 1
