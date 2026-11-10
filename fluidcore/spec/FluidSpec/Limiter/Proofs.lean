import Mathlib
import FluidSpec.Limiter.Impl
/-
What the limiter guarantees, for any list of transfers, over any ordered field (so the rationals and the reals).
-/
namespace FluidSpec.Limiter
open Finset

/-! ### The small functions -/

section Order
variable {α : Type} [LinearOrder α]

theorem pos_eq [Zero α] (x : α) : pos x = max 0 x := by
  unfold pos; split_ifs with h
  · exact (max_eq_right h.le).symm
  · exact (max_eq_left (not_lt.mp h)).symm

theorem pos_nonneg [Zero α] (x : α) : 0 ≤ pos x := by rw [pos_eq]; exact le_max_left _ _

theorem pos_mono [Zero α] {x y : α} (h : x ≤ y) : pos x ≤ pos y := by rw [pos_eq, pos_eq]; exact max_le_max le_rfl h

theorem pos_of_nonneg [Zero α] {x : α} (h : 0 ≤ x) : pos x = x := by rw [pos_eq]; exact max_eq_right h

theorem pos_of_nonpos [Zero α] {x : α} (h : x ≤ 0) : pos x = 0 := by rw [pos_eq]; exact max_eq_left h

theorem min1_eq [One α] (x : α) : min1 x = min x 1 := by
  unfold min1; split_ifs with h
  · exact (min_eq_left h.le).symm
  · exact (min_eq_right (not_lt.mp h)).symm

theorem isZero_iff [Zero α] (x : α) : isZero x = true ↔ x = 0 := by
  unfold isZero
  simp only [Bool.and_eq_true, Bool.not_eq_true', decide_eq_false_iff_not, not_lt]
  constructor
  · rintro ⟨h1, h2⟩; exact le_antisymm h1 h2
  · rintro rfl; exact ⟨le_rfl, le_rfl⟩

theorem not_isZero_iff [Zero α] (x : α) : (!isZero x) = true ↔ x ≠ 0 := by
  rw [Bool.not_eq_true', ← Bool.not_eq_true, isZero_iff]

end Order

/-- The tables change nothing: they are there so that the compiled limiter computes each pass once. -/
theorem look_tab {β : Type} (M : Nat) (f : Nat → β) : look (tab M f) f = f := by
  funext i
  unfold look tab
  split_ifs with h
  · simp
  · rfl

/-! ### Folds as sums -/

section Ring
variable {α : Type} [CommRing α]

theorem foldl_cond_add {β : Type} (p : β → Prop) [DecidablePred p] (g : β → α) (l : List β) (a : α) :
    l.foldl (fun acc b => if p b then acc + g b else acc) a = a + (l.map fun b => if p b then g b else 0).sum := by
  induction l generalizing a with
  | nil => simp
  | cons b l ih =>
    simp only [List.foldl_cons, List.map_cons, List.sum_cons]
    rw [ih]
    split_ifs <;> ring

theorem inAt_eq (C : Nat) (moves : List (Move α)) (scale : Nat → α) (i : Nat) :
    inAt C moves scale i = (moves.map fun mv => if dstCell C mv = some i then massAmt C scale mv else 0).sum := by
  unfold inAt; rw [foldl_cond_add]; simp

theorem outAt_eq (C : Nat) (moves : List (Move α)) (scale : Nat → α) (i : Nat) :
    outAt C moves scale i = (moves.map fun mv => if srcCell C mv = some i then massAmt C scale mv else 0).sum := by
  unfold outAt; rw [foldl_cond_add]; simp

theorem sentAt_eq (C : Nat) (moves : List (Move α)) (scale : Nat → α) (g : Nat) :
    sentAt C moves scale g
      = (moves.map fun mv => if mv.edge = some g ∧ mv.k < C - 1 then mv.amt * srcScale C scale mv else 0).sum := by
  unfold sentAt; rw [foldl_cond_add]; simp

theorem askedAt_eq (C : Nat) (moves : List (Move α)) (g : Nat) :
    askedAt C moves g = (moves.map fun mv => if mv.edge = some g ∧ mv.k < C - 1 then mv.amt else 0).sum := by
  unfold askedAt; rw [foldl_cond_add]; simp

/-- A cell after the moves: what it had, less what left it, plus what reached it. -/
theorem applyAt_eq (C : Nat) (eff : Move α → α) (moves : List (Move α)) (y0 : Nat → α) (i : Nat) :
    applyAt C eff moves y0 i = y0 i - (moves.map fun mv => if srcCell C mv = some i then eff mv else 0).sum
      + (moves.map fun mv => if dstCell C mv = some i then eff mv else 0).sum := by
  unfold applyAt
  generalize y0 i = a
  induction moves generalizing a with
  | nil => simp
  | cons mv l ih =>
    simp only [List.foldl_cons, List.map_cons, List.sum_cons]
    rw [ih]
    split_ifs <;> ring

/-! ### T1: conservation -/

/-- The cell is one of `K` (and is a cell at all: not a fixed node, not outside). -/
def within (K : Finset Nat) : Option Nat → Prop
  | some i => i ∈ K
  | none => False

instance (K : Finset Nat) (o : Option Nat) : Decidable (within K o) := by
  cases o <;> unfold within <;> infer_instance

theorem sum_cond (K : Finset Nat) (o : Option Nat) (e : α) :
    ∑ i ∈ K, (if o = some i then e else 0) = if within K o then e else 0 := by
  cases o with
  | none => simp [within]
  | some j => simp [within]

theorem sum_cells (K : Finset Nat) (o : Move α → Option Nat) (e : Move α → α) (l : List (Move α)) :
    ∑ i ∈ K, (l.map fun mv => if o mv = some i then e mv else 0).sum
      = (l.map fun mv => if within K (o mv) then e mv else 0).sum := by
  induction l with
  | nil => simp
  | cons mv l ih => simp only [List.map_cons, List.sum_cons, Finset.sum_add_distrib, ih, sum_cond]

/--
**T1.** Over any set of cells, whatever each move ends up moving: the total afterwards is the total before, plus what
moves brought in from outside the set, less what they took out of it. Moves inside the set cancel.
-/
theorem conservation (C : Nat) (eff : Move α → α) (moves : List (Move α)) (y0 : Nat → α) (K : Finset Nat) :
    ∑ i ∈ K, applyAt C eff moves y0 i = ∑ i ∈ K, y0 i
      + (moves.map fun mv => if within K (dstCell C mv) then eff mv else 0).sum
      - (moves.map fun mv => if within K (srcCell C mv) then eff mv else 0).sum := by
  simp only [applyAt_eq, Finset.sum_add_distrib, Finset.sum_sub_distrib, sum_cells]
  ring

/-- **T1, closed.** If every move has both ends in the set or neither, the set's total does not change. -/
theorem conservation_closed (C : Nat) (eff : Move α → α) (moves : List (Move α)) (y0 : Nat → α) (K : Finset Nat)
    (h : ∀ mv ∈ moves, within K (srcCell C mv) ↔ within K (dstCell C mv)) :
    ∑ i ∈ K, applyAt C eff moves y0 i = ∑ i ∈ K, y0 i := by
  rw [conservation]
  have : (moves.map fun mv => if within K (dstCell C mv) then eff mv else 0)
      = (moves.map fun mv => if within K (srcCell C mv) then eff mv else 0) := by
    apply List.map_congr_left
    intro mv hmv
    simp only [h mv hmv]
  rw [this]; ring

end Ring

/-! ### Cells and their moves -/

section Field
variable {α : Type} [Field α] [LinearOrder α] [IsStrictOrderedRing α]
set_option linter.unusedSectionVars false

/--
What the proofs assume of a call: no negative amounts, components that exist, senders that are cells. (The Java tests
check this of every call they make: LimiterTap.)
-/
structure Valid (C M : Nat) (moves : List (Move α)) : Prop where
  amt_nonneg : ∀ mv ∈ moves, 0 ≤ mv.amt
  k_lt : ∀ mv ∈ moves, mv.k < C
  src_lt : ∀ mv ∈ moves, ∀ i, srcCell C mv = some i → i < M

theorem isMass_cell {C f k : Nat} (hk : k < C) : isMass C (cell C f k) = true ↔ k < C - 1 := by
  unfold isMass cell
  rw [Nat.mul_add_mod, Nat.mod_eq_of_lt hk]
  simp only [bne_iff_ne, ne_eq]
  omega

theorem src_facts {C : Nat} {mv : Move α} {i : Nat} (hk : mv.k < C) (h : srcCell C mv = some i) (scale : Nat → α) :
    (isMass C i = true ↔ mv.k < C - 1) ∧ srcScale C scale mv = scale i := by
  unfold srcCell at h
  unfold srcScale
  cases hs : mv.src with
  | none => rw [hs] at h; simp at h
  | some f =>
    rw [hs] at h
    simp only [Option.map_some, Option.some.injEq] at h
    subst h
    exact ⟨isMass_cell hk, rfl⟩

theorem dst_mass {C : Nat} {mv : Move α} {i : Nat} (hk : mv.k < C) (h : dstCell C mv = some i) :
    isMass C i = true ↔ mv.k < C - 1 := by
  unfold dstCell at h
  cases hs : mv.dst with
  | none => rw [hs] at h; simp at h
  | some f =>
    rw [hs] at h
    simp only [Option.map_some, Option.some.injEq] at h
    subst h
    exact isMass_cell hk

theorem massAmt_src {C : Nat} {mv : Move α} {i : Nat} (hk : mv.k < C) (h : srcCell C mv = some i)
    (hi : isMass C i = true) (scale : Nat → α) : massAmt C scale mv = mv.amt * scale i := by
  obtain ⟨h1, h2⟩ := src_facts hk h scale
  unfold massAmt
  simp only [h1.mp hi, ↓reduceIte, h2]

/-- Every move out of a mass cell carries the cell's scale: what it sends is its scale times what it was asked for. -/
theorem outAt_mass {C : Nat} {moves : List (Move α)} (hk : ∀ mv ∈ moves, mv.k < C) {i : Nat}
    (hi : isMass C i = true) (scale : Nat → α) :
    outAt C moves scale i = scale i * outAt C moves (fun _ => 1) i := by
  rw [outAt_eq, outAt_eq, ← List.sum_map_mul_left]
  congr 1
  apply List.map_congr_left
  intro mv hmv
  split_ifs with h
  · rw [massAmt_src (hk mv hmv) h hi, massAmt_src (hk mv hmv) h hi]; ring
  · ring

theorem srcScale_nonneg {scale : Nat → α} (hb : ∀ i, 0 ≤ scale i) (C : Nat) (mv : Move α) :
    0 ≤ srcScale C scale mv := by
  unfold srcScale; split
  · exact hb _
  · exact zero_le_one

theorem srcScale_le_one {scale : Nat → α} (hb : ∀ i, scale i ≤ 1) (C : Nat) (mv : Move α) :
    srcScale C scale mv ≤ 1 := by
  unfold srcScale; split
  · exact hb _
  · exact le_rfl

theorem srcScale_mono {s s' : Nat → α} (h : ∀ i, s i ≤ s' i) (C : Nat) (mv : Move α) :
    srcScale C s mv ≤ srcScale C s' mv := by
  unfold srcScale; split
  · exact h _
  · exact le_rfl

theorem massAmt_nonneg {scale : Nat → α} {mv : Move α} (hamt : 0 ≤ mv.amt) (hb : ∀ i, 0 ≤ scale i) (C : Nat) :
    0 ≤ massAmt C scale mv := by
  unfold massAmt
  apply mul_nonneg hamt
  split_ifs
  · exact srcScale_nonneg hb C mv
  · exact zero_le_one

theorem massAmt_mono {s s' : Nat → α} {mv : Move α} (hamt : 0 ≤ mv.amt) (h : ∀ i, s i ≤ s' i) (C : Nat) :
    massAmt C s mv ≤ massAmt C s' mv := by
  unfold massAmt
  apply mul_le_mul_of_nonneg_left _ hamt
  split_ifs
  · exact srcScale_mono h C mv
  · exact le_rfl

theorem inAt_nonneg {C : Nat} {moves : List (Move α)} {scale : Nat → α} (hamt : ∀ mv ∈ moves, 0 ≤ mv.amt)
    (hb : ∀ i, 0 ≤ scale i) (i : Nat) : 0 ≤ inAt C moves scale i := by
  rw [inAt_eq]
  apply List.sum_nonneg
  intro x hx
  rw [List.mem_map] at hx
  obtain ⟨mv, hmv, rfl⟩ := hx
  split_ifs
  · exact massAmt_nonneg (hamt mv hmv) hb C
  · exact le_rfl

theorem outAt_nonneg {C : Nat} {moves : List (Move α)} {scale : Nat → α} (hamt : ∀ mv ∈ moves, 0 ≤ mv.amt)
    (hb : ∀ i, 0 ≤ scale i) (i : Nat) : 0 ≤ outAt C moves scale i := by
  rw [outAt_eq]
  apply List.sum_nonneg
  intro x hx
  rw [List.mem_map] at hx
  obtain ⟨mv, hmv, rfl⟩ := hx
  split_ifs
  · exact massAmt_nonneg (hamt mv hmv) hb C
  · exact le_rfl

/-- Raising scales lowers no cell's inflow. -/
theorem inAt_mono {C : Nat} {moves : List (Move α)} {s s' : Nat → α} (hamt : ∀ mv ∈ moves, 0 ≤ mv.amt)
    (h : ∀ i, s i ≤ s' i) (i : Nat) : inAt C moves s i ≤ inAt C moves s' i := by
  rw [inAt_eq, inAt_eq]
  apply List.sum_le_sum
  intro mv hmv
  split_ifs
  · exact massAmt_mono (hamt mv hmv) h C
  · exact le_rfl

theorem outAt_eq_zero {C : Nat} {moves : List (Move α)} {i : Nat} (h : ∀ mv ∈ moves, srcCell C mv ≠ some i)
    (scale : Nat → α) : outAt C moves scale i = 0 := by
  rw [outAt_eq]
  apply List.sum_eq_zero
  intro x hx
  rw [List.mem_map] at hx
  obtain ⟨mv, hmv, rfl⟩ := hx
  simp only [h mv hmv, ↓reduceIte]

theorem have_nonneg (C : Nat) (y0 : Nat → α) (moves : List (Move α)) (scale : Nat → α) (i : Nat) :
    0 ≤ have_ C y0 moves scale i := pos_nonneg _

/-! ### Feasible scales -/

/--
Scales are feasible when no mass cell sends more than `slack` times what it has: its contents at the start plus what
the others send it at these scales, or nothing if that is negative. This is the check Java's first loop ends on.
-/
def Feasible (C : Nat) (slack : α) (y0 : Nat → α) (moves : List (Move α)) (scale : Nat → α) : Prop :=
  ∀ i, isMass C i = true → outAt C moves scale i ≤ have_ C y0 moves scale i * slack

theorem overSends_iff (C : Nat) (slack : α) (y0 : Nat → α) (moves : List (Move α)) (scale : Nat → α) (i : Nat) :
    overSends C slack y0 moves scale i = true ↔ isMass C i = true ∧ outAt C moves scale i ≠ 0
      ∧ have_ C y0 moves scale i * slack < outAt C moves scale i := by
  simp only [overSends, Bool.and_eq_true, decide_eq_true_eq, not_isZero_iff, and_assoc]

theorem feasible_of_not_anyOver {C M : Nat} {slack : α} {y0 : Nat → α} {moves : List (Move α)} {scale : Nat → α}
    (hv : Valid C M moves) (hs : 0 ≤ slack) (h : anyOver C M slack y0 moves scale = false) :
    Feasible C slack y0 moves scale := by
  intro i hi
  by_cases hiM : i < M
  · have ho : ¬ overSends C slack y0 moves scale i = true := by
      unfold anyOver at h
      rw [List.any_eq_false] at h
      exact h i (List.mem_range.mpr hiM)
    rw [overSends_iff] at ho
    by_cases hz : outAt C moves scale i = 0
    · rw [hz]; exact mul_nonneg (have_nonneg ..) hs
    · exact not_lt.mp fun hlt => ho ⟨hi, hz, hlt⟩
  · rw [outAt_eq_zero]
    · exact mul_nonneg (have_nonneg ..) hs
    · intro mv hmv hsrc; exact hiM (hv.src_lt mv hmv i hsrc)

theorem not_anyOver_of_feasible {C M : Nat} {slack : α} {y0 : Nat → α} {moves : List (Move α)} {scale : Nat → α}
    (h : Feasible C slack y0 moves scale) : anyOver C M slack y0 moves scale = false := by
  unfold anyOver
  rw [List.any_eq_false]
  intro i _ ho
  rw [overSends_iff] at ho
  exact not_lt.mpr (h i ho.1) ho.2.2

/-- A cut lowers a scale, and not below 0. -/
theorem cutAt_bounds {C M : Nat} {slack : α} {moves : List (Move α)} {scale : Nat → α} (hv : Valid C M moves)
    (hs : 1 ≤ slack) (hb : ∀ i, 0 ≤ scale i) (y0 : Nat → α) (silence : Bool) (i : Nat) :
    0 ≤ cutAt C slack y0 moves silence scale i ∧ cutAt C slack y0 moves silence scale i ≤ scale i := by
  unfold cutAt
  split_ifs with ho hsil
  · exact ⟨le_rfl, hb i⟩
  · rw [overSends_iff] at ho
    obtain ⟨_, hz, hlt⟩ := ho
    have hout : 0 < outAt C moves scale i := lt_of_le_of_ne (outAt_nonneg hv.amt_nonneg hb i) (Ne.symm hz)
    have hh : 0 ≤ have_ C y0 moves scale i := have_nonneg ..
    have hle : have_ C y0 moves scale i ≤ outAt C moves scale i := (le_mul_of_one_le_right hh hs).trans hlt.le
    exact ⟨mul_nonneg (hb i) (div_nonneg hh hout.le), mul_le_of_le_one_right (hb i) ((div_le_one hout).mpr hle)⟩
  · exact ⟨hb i, le_rfl⟩

/--
Java's first loop ends on feasible scales, each between 0 and where it started, whatever the moves: either on the
check itself, or (never reached) on all scales 0.
-/
theorem settle_spec {C M : Nat} {slack : α} {moves : List (Move α)} (hv : Valid C M moves) (hs : 1 ≤ slack)
    (y0 : Nat → α) (down : Nat) :
    ∀ (fuel pass : Nat) (scale : Nat → α), (∀ i, 0 ≤ scale i) →
      Feasible C slack y0 moves (settle C M slack y0 moves down fuel pass scale).1
        ∧ ∀ i, 0 ≤ (settle C M slack y0 moves down fuel pass scale).1 i
          ∧ (settle C M slack y0 moves down fuel pass scale).1 i ≤ scale i := by
  have h0 : (0 : α) ≤ slack := zero_le_one.trans hs
  intro fuel
  induction fuel with
  | zero =>
    intro pass scale hb
    rw [settle]
    split_ifs with ho
    · refine ⟨?_, ?_⟩
      · intro i hi
        rw [outAt_mass hv.k_lt hi]
        simp only [hi, ↓reduceIte, zero_mul]
        exact mul_nonneg (have_nonneg ..) h0
      · intro i
        dsimp only
        split_ifs
        · exact ⟨le_rfl, hb i⟩
        · exact ⟨hb i, le_rfl⟩
    · exact ⟨feasible_of_not_anyOver hv h0 (by simpa using ho), fun i => ⟨hb i, le_rfl⟩⟩
  | succ fuel ih =>
    intro pass scale hb
    rw [settle]
    split_ifs with ho
    · simp only [look_tab]
      have hc := cutAt_bounds hv hs hb y0 (decide (down ≤ pass))
      obtain ⟨hf, hb'⟩ := ih (pass + 1) (cutAt C slack y0 moves (decide (down ≤ pass)) scale) (fun i => (hc i).1)
      exact ⟨hf, fun i => ⟨(hb' i).1, (hb' i).2.trans (hc i).2⟩⟩
    · exact ⟨feasible_of_not_anyOver hv h0 (by simpa using ho), fun i => ⟨hb i, le_rfl⟩⟩

theorem settle_of_not_anyOver {C M : Nat} {slack : α} {y0 : Nat → α} {moves : List (Move α)} {scale : Nat → α}
    (h : anyOver C M slack y0 moves scale = false) (down fuel pass : Nat) :
    settle C M slack y0 moves down fuel pass scale = (scale, pass) := by
  cases fuel <;> rw [settle] <;> simp [h]

theorem raiseAt_cases (C : Nat) (y0 : Nat → α) (moves : List (Move α)) (raw s : Nat → α) (i : Nat) :
    raiseAt C y0 moves raw s i = s i
      ∨ (raw i ≠ 0 ∧ s i < raiseAt C y0 moves raw s i
        ∧ raiseAt C y0 moves raw s i = min1 (have_ C y0 moves s i / raw i)) := by
  unfold raiseAt
  dsimp only
  split_ifs with hc hlt
  · right
    simp only [Bool.and_eq_true, not_isZero_iff] at hc
    exact ⟨hc.2, hlt, rfl⟩
  · left; rfl
  · left; rfl

/-- A raising pass keeps feasible scales feasible, lowers none, and lifts none above 1. -/
theorem raiseAt_spec {C M : Nat} {slack : α} {y0 : Nat → α} {moves : List (Move α)} (hv : Valid C M moves)
    (hs : 1 ≤ slack) {s : Nat → α} (hf : Feasible C slack y0 moves s) (hb : ∀ i, 0 ≤ s i ∧ s i ≤ 1) :
    Feasible C slack y0 moves (raiseAt C y0 moves (outAt C moves fun _ => 1) s)
      ∧ ∀ i, s i ≤ raiseAt C y0 moves (outAt C moves fun _ => 1) s i
        ∧ raiseAt C y0 moves (outAt C moves fun _ => 1) s i ≤ 1 := by
  have hmono : ∀ i, s i ≤ raiseAt C y0 moves (outAt C moves fun _ => 1) s i
      ∧ raiseAt C y0 moves (outAt C moves fun _ => 1) s i ≤ 1 := by
    intro i
    rcases raiseAt_cases C y0 moves (outAt C moves fun _ => 1) s i with h | ⟨_, hlt, he⟩
    · rw [h]; exact ⟨le_rfl, (hb i).2⟩
    · refine ⟨hlt.le, ?_⟩
      rw [he, min1_eq]; exact min_le_right _ _
  refine ⟨?_, hmono⟩
  intro i hi
  have hin : have_ C y0 moves s i ≤ have_ C y0 moves (raiseAt C y0 moves (outAt C moves fun _ => 1) s) i :=
    pos_mono (by
      have := inAt_mono (C := C) hv.amt_nonneg (fun j => (hmono j).1) i
      linarith)
  have h0 : (0 : α) ≤ slack := zero_le_one.trans hs
  have hraw : 0 ≤ outAt C moves (fun _ => (1 : α)) i := outAt_nonneg hv.amt_nonneg (fun _ => zero_le_one) i
  rw [outAt_mass hv.k_lt hi]
  rcases raiseAt_cases C y0 moves (outAt C moves fun _ => 1) s i with h | ⟨hz, _, he⟩
  · rw [h, ← outAt_mass hv.k_lt hi]
    exact (hf i hi).trans (mul_le_mul_of_nonneg_right hin h0)
  · have hpos : 0 < outAt C moves (fun _ => (1 : α)) i := lt_of_le_of_ne hraw (Ne.symm hz)
    have h1 : raiseAt C y0 moves (outAt C moves fun _ => 1) s i * outAt C moves (fun _ => 1) i
        ≤ have_ C y0 moves s i := by
      rw [he, min1_eq]
      calc min (have_ C y0 moves s i / outAt C moves (fun _ => 1) i) 1 * outAt C moves (fun _ => 1) i
          ≤ have_ C y0 moves s i / outAt C moves (fun _ => 1) i * outAt C moves (fun _ => 1) i :=
            mul_le_mul_of_nonneg_right (min_le_left _ _) hpos.le
        _ = have_ C y0 moves s i := by field_simp
    exact (h1.trans hin).trans (le_mul_of_one_le_right (have_nonneg ..) hs)

/-- Any number of raising passes: still feasible, nothing lowered, nothing above 1. -/
theorem raise_spec {C M : Nat} {slack : α} {y0 : Nat → α} {moves : List (Move α)} (hv : Valid C M moves)
    (hs : 1 ≤ slack) (n : Nat) :
    ∀ s : Nat → α, Feasible C slack y0 moves s → (∀ i, 0 ≤ s i ∧ s i ≤ 1) →
      Feasible C slack y0 moves (raise C M y0 moves (outAt C moves fun _ => 1) n s)
        ∧ ∀ i, s i ≤ raise C M y0 moves (outAt C moves fun _ => 1) n s i
          ∧ raise C M y0 moves (outAt C moves fun _ => 1) n s i ≤ 1 := by
  induction n with
  | zero => intro s hf hb; rw [raise]; exact ⟨hf, fun i => ⟨le_rfl, (hb i).2⟩⟩
  | succ n ih =>
    intro s hf hb
    rw [raise]
    simp only [look_tab]
    obtain ⟨hf', hb'⟩ := raiseAt_spec hv hs hf hb
    obtain ⟨hf'', hb''⟩ := ih _ hf' (fun i => ⟨(hb i).1.trans (hb' i).1, (hb' i).2⟩)
    exact ⟨hf'', fun i => ⟨(hb' i).1.trans (hb'' i).1, (hb'' i).2⟩⟩

/-- A pass that raises nothing ends the raising: Java stops there, and going on would change nothing. -/
theorem raise_fixed {C M : Nat} {y0 : Nat → α} {moves : List (Move α)} {raw s : Nat → α}
    (h : raiseAt C y0 moves raw s = s) (n : Nat) : raise C M y0 moves raw n s = s := by
  induction n with
  | zero => rw [raise]
  | succ n ih => rw [raise]; simp only [look_tab]; rw [h, ih]

/-- **The scales the limiter uses are feasible, and between 0 and 1**, whichever way they were found. -/
theorem scales_spec {C M : Nat} {slack : α} {moves : List (Move α)} (hv : Valid C M moves) (hs : 1 ≤ slack)
    (down up : Nat) (y0 : Nat → α) :
    Feasible C slack y0 moves (scales C M slack down up y0 moves).1
      ∧ ∀ i, 0 ≤ (scales C M slack down up y0 moves).1 i ∧ (scales C M slack down up y0 moves).1 i ≤ 1 := by
  obtain ⟨hf, hb⟩ := settle_spec hv hs y0 down (down + M) 0 (fun _ => 1) (fun _ => zero_le_one)
  unfold scales
  dsimp only
  split_ifs with h
  · exact ⟨hf, hb⟩
  · simp only [look_tab]
    obtain ⟨hf', hb'⟩ := raise_spec hv hs up _ hf hb
    exact ⟨hf', fun i => ⟨(hb i).1.trans (hb' i).1, (hb' i).2⟩⟩

/-! ### T2: masses stay positive -/

theorem effAmt_mass {C : Nat} {mv : Move α} (h : mv.k < C - 1) (s sent asked : Nat → α) :
    effAmt C s sent asked mv = massAmt C s mv := by
  unfold effAmt massAmt
  simp only [h, ↓reduceIte]

/-- A mass cell after the step: what it had, less what it sent at its scale, plus what the others sent it at theirs. -/
theorem applyAt_mass {C : Nat} {moves : List (Move α)} (hk : ∀ mv ∈ moves, mv.k < C) {i : Nat}
    (hi : isMass C i = true) (y0 s sent asked : Nat → α) :
    applyAt C (effAmt C s sent asked) moves y0 i = y0 i - outAt C moves s i + inAt C moves s i := by
  rw [applyAt_eq, outAt_eq, inAt_eq]
  have h1 : (moves.map fun mv => if srcCell C mv = some i then effAmt C s sent asked mv else 0)
      = (moves.map fun mv => if srcCell C mv = some i then massAmt C s mv else 0) := by
    apply List.map_congr_left
    intro mv hmv
    split_ifs with h
    · exact effAmt_mass ((src_facts (hk mv hmv) h s).1.mp hi) ..
    · rfl
  have h2 : (moves.map fun mv => if dstCell C mv = some i then effAmt C s sent asked mv else 0)
      = (moves.map fun mv => if dstCell C mv = some i then massAmt C s mv else 0) := by
    apply List.map_congr_left
    intro mv hmv
    split_ifs with h
    · exact effAmt_mass ((dst_mass (hk mv hmv) h).mp hi) ..
    · rfl
  rw [h1, h2]

/--
**T2.** With feasible scales, a mass ends no lower than it started, or no lower than -(slack - 1) times what it had
available in this step, whichever is lower. Nothing about how the scales were found is used.
-/
theorem mass_bound {C M : Nat} {slack : α} {y0 : Nat → α} {moves : List (Move α)} {s : Nat → α}
    (hv : Valid C M moves) (hf : Feasible C slack y0 moves s) (hb : ∀ i, 0 ≤ s i) {i : Nat}
    (hi : isMass C i = true) (sent asked : Nat → α) :
    min (y0 i) (-((slack - 1) * have_ C y0 moves s i)) ≤ applyAt C (effAmt C s sent asked) moves y0 i := by
  rw [applyAt_mass hv.k_lt hi]
  have hfi := hf i hi
  have hin := inAt_nonneg (C := C) hv.amt_nonneg hb i
  unfold have_ at hfi ⊢
  by_cases h : 0 ≤ y0 i + inAt C moves s i
  · rw [pos_of_nonneg h] at hfi ⊢
    apply (min_le_right _ _).trans
    linarith
  · rw [pos_of_nonpos (not_le.mp h).le, zero_mul] at hfi
    apply (min_le_left _ _).trans
    linarith

/-! ### T5: energy follows mass -/

/-- What each move moves in the end, at the given scales: `apply`'s amounts. -/
def effOf (C : Nat) (moves : List (Move α)) (scale : Nat → α) : Move α → α :=
  effAmt C scale (sentAt C moves scale) (askedAt C moves)

theorem sent_bounds {C : Nat} {moves : List (Move α)} {s : Nat → α} (hamt : ∀ mv ∈ moves, 0 ≤ mv.amt)
    (hb : ∀ i, 0 ≤ s i ∧ s i ≤ 1) (g : Nat) :
    0 ≤ sentAt C moves s g ∧ sentAt C moves s g ≤ askedAt C moves g := by
  rw [sentAt_eq, askedAt_eq]
  constructor
  · apply List.sum_nonneg
    intro x hx
    rw [List.mem_map] at hx
    obtain ⟨mv, hmv, rfl⟩ := hx
    split_ifs
    · exact mul_nonneg (hamt mv hmv) (srcScale_nonneg (fun i => (hb i).1) C mv)
    · exact le_rfl
  · apply List.sum_le_sum
    intro mv hmv
    split_ifs
    · exact mul_le_of_le_one_right (hamt mv hmv) (srcScale_le_one (fun i => (hb i).2) C mv)
    · exact le_rfl

theorem edgeShare_bounds {sent asked : Nat → α} (h : ∀ g, 0 ≤ sent g ∧ sent g ≤ asked g) (mv : Move α) :
    0 ≤ edgeShare sent asked mv ∧ edgeShare sent asked mv ≤ 1 := by
  unfold edgeShare
  cases mv.edge with
  | none => exact ⟨zero_le_one, le_rfl⟩
  | some g =>
    dsimp only
    split_ifs with hz
    · exact ⟨zero_le_one, le_rfl⟩
    · have hne : asked g ≠ 0 := fun h0 => hz ((isZero_iff _).mpr h0)
      have hpos : 0 < asked g := lt_of_le_of_ne ((h g).1.trans (h g).2) (Ne.symm hne)
      exact ⟨div_nonneg (h g).1 hpos.le, (div_le_one hpos).mpr (h g).2⟩

/-- **T5.** With scales between 0 and 1, every move ends up moving between nothing and what it asked: none is reversed
or enlarged, energy included. -/
theorem effOf_bounds {C : Nat} {moves : List (Move α)} {s : Nat → α} (hamt : ∀ mv ∈ moves, 0 ≤ mv.amt)
    (hb : ∀ i, 0 ≤ s i ∧ s i ≤ 1) (mv : Move α) (hmv : 0 ≤ mv.amt) :
    0 ≤ effOf C moves s mv ∧ effOf C moves s mv ≤ mv.amt := by
  unfold effOf effAmt
  split_ifs
  · exact ⟨mul_nonneg hmv (srcScale_nonneg (fun i => (hb i).1) C mv),
      mul_le_of_le_one_right hmv (srcScale_le_one (fun i => (hb i).2) C mv)⟩
  · obtain ⟨h0, h1⟩ := edgeShare_bounds (fun g => sent_bounds (C := C) hamt hb g) mv
    exact ⟨mul_nonneg hmv h0, mul_le_of_le_one_right hmv h1⟩

/-- **T5.** An edge whose masses are all held back carries no energy either (the energy the stream carries: conduction
is on no edge). -/
theorem energy_held {C : Nat} {moves : List (Move α)} {s : Nat → α} {mv : Move α} {g : Nat}
    (hk : ¬ mv.k < C - 1) (he : mv.edge = some g) (hasked : askedAt C moves g ≠ 0)
    (h : ∀ m ∈ moves, m.edge = some g → m.k < C - 1 → srcScale C s m = 0) : effOf C moves s mv = 0 := by
  have hsent : sentAt C moves s g = 0 := by
    rw [sentAt_eq]
    apply List.sum_eq_zero
    intro x hx
    rw [List.mem_map] at hx
    obtain ⟨m, hm, rfl⟩ := hx
    split_ifs with hc
    · rw [h m hm hc.1 hc.2, mul_zero]
    · rfl
  have hz : ¬ isZero (askedAt C moves g) = true := (isZero_iff _).not.mpr hasked
  unfold effOf effAmt edgeShare
  simp only [hk, ↓reduceIte, he, hz, hsent, zero_div, mul_zero, Bool.false_eq_true]

/-- **T5.** An edge whose masses all go in full carries its energy in full. -/
theorem energy_full {C : Nat} {moves : List (Move α)} {s : Nat → α} {mv : Move α} (hk : ¬ mv.k < C - 1)
    (h : ∀ g, mv.edge = some g → ∀ m ∈ moves, m.edge = some g → m.k < C - 1 → srcScale C s m = 1) :
    effOf C moves s mv = mv.amt := by
  unfold effOf effAmt edgeShare
  simp only [hk, ↓reduceIte]
  cases he : mv.edge with
  | none => simp
  | some g =>
    dsimp only
    have hsent : sentAt C moves s g = askedAt C moves g := by
      rw [sentAt_eq, askedAt_eq]
      congr 1
      apply List.map_congr_left
      intro m hm
      split_ifs with hc
      · rw [h g he m hm hc.1 hc.2, mul_one]
      · rfl
    split_ifs with hz
    · rw [mul_one]
    · have hne : askedAt C moves g ≠ 0 := fun h0 => hz ((isZero_iff _).mpr h0)
      rw [hsent, div_self hne, mul_one]

/-! ### The whole of `apply` -/

theorem apply_eq (C M G : Nat) (slack : α) (down up : Nat) (y0 : Nat → α) (moves : List (Move α)) :
    apply C M G slack down up y0 moves
      = (applyAt C (effOf C moves (scales C M slack down up y0 moves).1) moves y0,
          (scales C M slack down up y0 moves).1, (scales C M slack down up y0 moves).2) := by
  unfold apply effOf
  simp only [look_tab]

/-- **T1 for `apply`.** If every move has both ends in a set of cells or neither, the set's total does not change:
each species and the energy of a network with no fixed nodes and no heat supplied, or of any closed part of one. -/
theorem apply_conserves (C M G : Nat) (slack : α) (down up : Nat) (y0 : Nat → α) (moves : List (Move α))
    (K : Finset Nat) (h : ∀ mv ∈ moves, within K (srcCell C mv) ↔ within K (dstCell C mv)) :
    ∑ i ∈ K, (apply C M G slack down up y0 moves).1 i = ∑ i ∈ K, y0 i := by
  rw [apply_eq]
  exact conservation_closed C _ moves y0 K h

/-- **T1 for `apply`, open.** The total over any set of cells changes by what crossed its boundary. -/
theorem apply_balance (C M G : Nat) (slack : α) (down up : Nat) (y0 : Nat → α) (moves : List (Move α))
    (K : Finset Nat) :
    ∑ i ∈ K, (apply C M G slack down up y0 moves).1 i = ∑ i ∈ K, y0 i
      + (moves.map fun mv => if within K (dstCell C mv)
          then effOf C moves (apply C M G slack down up y0 moves).2.1 mv else 0).sum
      - (moves.map fun mv => if within K (srcCell C mv)
          then effOf C moves (apply C M G slack down up y0 moves).2.1 mv else 0).sum := by
  rw [apply_eq]
  exact conservation C _ moves y0 K

/-- The scales `apply` reports are between 0 and 1. -/
theorem apply_scales {C M : Nat} {slack : α} {moves : List (Move α)} (hv : Valid C M moves) (hs : 1 ≤ slack)
    (G down up : Nat) (y0 : Nat → α) (i : Nat) :
    0 ≤ (apply C M G slack down up y0 moves).2.1 i ∧ (apply C M G slack down up y0 moves).2.1 i ≤ 1 := by
  rw [apply_eq]
  exact (scales_spec hv hs down up y0).2 i

/--
**T2 for `apply`.** Every mass ends no lower than it started, or no lower than -(slack - 1) times what the cell had
available in this step, whichever is lower.
-/
theorem apply_mass_bound {C M : Nat} {slack : α} {moves : List (Move α)} (hv : Valid C M moves) (hs : 1 ≤ slack)
    (G down up : Nat) (y0 : Nat → α) {i : Nat} (hi : isMass C i = true) :
    min (y0 i) (-((slack - 1) * have_ C y0 moves (apply C M G slack down up y0 moves).2.1 i))
      ≤ (apply C M G slack down up y0 moves).1 i := by
  rw [apply_eq]
  obtain ⟨hf, hb⟩ := scales_spec hv hs down up y0
  exact mass_bound hv hf (fun i => (hb i).1) hi ..

/-- **T2, exact arithmetic.** Without slack, a mass that starts at 0 or more ends at 0 or more. -/
theorem apply_mass_nonneg {C M : Nat} {moves : List (Move α)} (hv : Valid C M moves) (G down up : Nat)
    (y0 : Nat → α) {i : Nat} (hi : isMass C i = true) (h0 : 0 ≤ y0 i) :
    0 ≤ (apply C M G 1 down up y0 moves).1 i := by
  have h := apply_mass_bound hv le_rfl G down up y0 hi
  rw [sub_self, zero_mul, neg_zero, min_eq_right h0] at h
  exact h

/--
**T3.** If no cell would send more than it has with every move in full, every move goes in full: the limiter changes
nothing, in one pass, whatever the caps on its passes.
-/
theorem apply_unlimited {C M : Nat} {slack : α} {y0 : Nat → α} {moves : List (Move α)}
    (h : anyOver C M slack y0 moves (fun _ => 1) = false) (G down up : Nat) :
    apply C M G slack down up y0 moves = (applyAt C (fun mv => mv.amt) moves y0, fun _ => 1, 0) := by
  have hsc : scales C M slack down up y0 moves = (fun _ => 1, 0) := by
    unfold scales
    rw [settle_of_not_anyOver h]
    simp
  rw [apply_eq, hsc]
  have he : effOf C moves (fun _ => (1 : α)) = fun mv => mv.amt := by
    funext mv
    by_cases hk : mv.k < C - 1
    · unfold effOf effAmt srcScale
      simp only [hk, ↓reduceIte]
      cases mv.src <;> simp
    · exact energy_full hk (fun g _ m _ _ _ => by unfold srcScale; cases m.src <;> rfl)
  simp only [he]

/-! ### T4: from the solver's fluxes -/

/-- A signed flow of component `k` from node `a` to node `b`: Network.finish's `dt * f[k]` for an edge. -/
structure Flow (α : Type) where
  a : Option Nat
  b : Option Nat
  k : Nat
  x : α
  edge : Option Nat

/-- Network.move: the flow as a transfer of a positive amount, the other way if it was negative; none if zero. -/
def toMove (f : Flow α) : Option (Move α) :=
  if 0 < f.x then some ⟨f.a, f.b, f.k, f.x, f.edge⟩
  else if f.x < 0 then some ⟨f.b, f.a, f.k, -f.x, f.edge⟩
  else none

theorem toMove_amt_pos {f : Flow α} {mv : Move α} (h : toMove f = some mv) : 0 < mv.amt := by
  unfold toMove at h
  split_ifs at h with h1 h2
  · simp only [Option.some.injEq] at h; subst h; exact h1
  · simp only [Option.some.injEq] at h; subst h; exact neg_pos.mpr h2

/--
**T4.** The moves made from signed flows, all in full, take each flow from its first end and give it to its second:
the cells end at Y0 + dt F, with the same F the residual is made of.
-/
theorem applyAt_flows (C : Nat) (fl : List (Flow α)) (y0 : Nat → α) (i : Nat) :
    applyAt C (fun mv => mv.amt) (fl.filterMap toMove) y0 i
      = y0 i - (fl.map fun f => if f.a.map (cell C · f.k) = some i then f.x else 0).sum
        + (fl.map fun f => if f.b.map (cell C · f.k) = some i then f.x else 0).sum := by
  rw [applyAt_eq]
  have key : ((fl.filterMap toMove).map fun mv => if dstCell C mv = some i then mv.amt else 0).sum
      - ((fl.filterMap toMove).map fun mv => if srcCell C mv = some i then mv.amt else 0).sum
      = (fl.map fun f => if f.b.map (cell C · f.k) = some i then f.x else 0).sum
        - (fl.map fun f => if f.a.map (cell C · f.k) = some i then f.x else 0).sum := by
    induction fl with
    | nil => simp
    | cons f l ih =>
      have hf : ((toMove f).toList.map fun mv => if dstCell C mv = some i then mv.amt else 0).sum
          - ((toMove f).toList.map fun mv => if srcCell C mv = some i then mv.amt else 0).sum
          = (if f.b.map (cell C · f.k) = some i then f.x else 0)
            - (if f.a.map (cell C · f.k) = some i then f.x else 0) := by
        by_cases ha : f.a.map (cell C · f.k) = some i <;> by_cases hb : f.b.map (cell C · f.k) = some i <;>
          rcases lt_trichotomy 0 f.x with h1 | h1 | h1
        all_goals first
          | simp [toMove, ← h1]
          | (simp only [toMove, srcCell, dstCell, h1, not_lt.mpr h1.le, ha, hb, ↓reduceIte, Option.toList_some,
              List.map_cons, List.map_nil, List.sum_cons, List.sum_nil]; ring)
      rw [List.filterMap_cons]
      cases hm : toMove f with
      | none =>
        rw [hm] at hf
        simp only [Option.toList_none, List.map_nil, List.sum_nil] at hf
        simp only [List.map_cons, List.sum_cons]
        linarith
      | some mv =>
        rw [hm] at hf
        simp only [Option.toList_some, List.map_cons, List.map_nil, List.sum_cons, List.sum_nil] at hf
        simp only [List.map_cons, List.sum_cons]
        linarith
  linarith

end Field

end FluidSpec.Limiter
