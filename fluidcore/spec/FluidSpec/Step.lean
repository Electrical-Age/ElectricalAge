import Mathlib
import FluidSpec.Limiter.Proofs
/-
A whole step. Network.step ends in one or more calls of Limiter.apply, each from where the last ended (the whole
step; or halves, quarters, ... when Newton did not converge; a call whose result is discarded is undone exactly). So
what holds of each call holds of the step. Before each call, Newton tries states; the rules that keep their masses
from going below zero are here too.
-/
namespace FluidSpec.Step
open FluidSpec.Limiter Finset

set_option linter.unusedSectionVars false

variable {α : Type} [Field α] [LinearOrder α] [IsStrictOrderedRing α]

private theorem ifPos {c : Prop} [Decidable c] {β : Type} {a b : β} (h : c) : (if c then a else b) = a := by
  simp only [h, ↓reduceIte]

private theorem ifNeg {c : Prop} [Decidable c] {β : Type} {a b : β} (h : ¬c) : (if c then a else b) = b := by
  simp only [h, ↓reduceIte]

/-! ### The calls of a step -/

/-- The cells after a step's calls of `apply`, each with its own moves, in order. -/
def run (C M : Nat) (slack : α) (down up : Nat) (calls : List (List (Move α))) (y0 : Nat → α) : Nat → α :=
  calls.foldl (fun y moves => (apply C M 0 slack down up y moves).1) y0

/-- (Java's count of edges only sizes two arrays.) -/
theorem apply_edges (C M G G' : Nat) (slack : α) (down up : Nat) (y0 : Nat → α) (moves : List (Move α)) :
    apply C M G slack down up y0 moves = apply C M G' slack down up y0 moves := by
  rw [apply_eq, apply_eq]

/-- **A step conserves.** If every move of every call has both ends in a set of cells or neither, the set's total is
the same after the step. -/
theorem run_conserves (C M : Nat) (slack : α) (down up : Nat) (K : Finset Nat) (calls : List (List (Move α)))
    (h : ∀ moves ∈ calls, ∀ mv ∈ moves, within K (srcCell C mv) ↔ within K (dstCell C mv)) (y0 : Nat → α) :
    ∑ i ∈ K, run C M slack down up calls y0 i = ∑ i ∈ K, y0 i := by
  unfold run
  induction calls generalizing y0 with
  | nil => rfl
  | cons moves rest ih =>
    rw [List.foldl_cons, ih (fun m hm => h m (List.mem_cons_of_mem _ hm))]
    exact apply_conserves C M 0 slack down up y0 moves K (h moves List.mem_cons_self)

/-- **A step keeps masses positive (exact arithmetic).** Without slack, a mass that starts a step at 0 or more ends
it at 0 or more, however many calls the step took and whatever Newton reached in each. -/
theorem run_mass_nonneg {C M : Nat} (down up : Nat) (calls : List (List (Move α)))
    (hv : ∀ moves ∈ calls, Valid C M moves) {i : Nat} (hi : isMass C i = true) (y0 : Nat → α) (h0 : 0 ≤ y0 i) :
    0 ≤ run C M 1 down up calls y0 i := by
  unfold run
  induction calls generalizing y0 with
  | nil => exact h0
  | cons moves rest ih =>
    rw [List.foldl_cons]
    exact ih (fun m hm => hv m (List.mem_cons_of_mem _ hm)) _
      (apply_mass_nonneg (hv moves List.mem_cons_self) 0 down up y0 hi h0)

/-- `B` is at least what cell `i` had available in each of the calls. -/
def Available (C M : Nat) (slack : α) (down up : Nat) (i : Nat) (B : α) : List (List (Move α)) → (Nat → α) → Prop
  | [], _ => True
  | moves :: rest, y =>
    have_ C y moves (apply C M 0 slack down up y moves).2.1 i ≤ B
      ∧ Available C M slack down up i B rest (apply C M 0 slack down up y moves).1

/--
**A step keeps masses positive (with slack).** A mass ends the step no lower than it started, or no lower than
-(slack - 1) times the most the cell had available in any one call, whichever is lower: the shortfalls of the calls
do not add up.
-/
theorem run_mass_bound {C M : Nat} {slack : α} (hs : 1 ≤ slack) (down up : Nat) {i : Nat} (hi : isMass C i = true)
    (B : α) (calls : List (List (Move α))) (hv : ∀ moves ∈ calls, Valid C M moves) (y0 : Nat → α)
    (hB : Available C M slack down up i B calls y0) :
    min (y0 i) (-((slack - 1) * B)) ≤ run C M slack down up calls y0 i := by
  unfold run
  induction calls generalizing y0 with
  | nil => exact min_le_left _ _
  | cons moves rest ih =>
    rw [List.foldl_cons]
    obtain ⟨hB1, hB2⟩ := hB
    refine le_trans ?_ (ih (fun m hm => hv m (List.mem_cons_of_mem _ hm)) _ hB2)
    have h1 := apply_mass_bound (hv moves List.mem_cons_self) hs 0 down up y0 hi
    have h2 : -((slack - 1) * B)
        ≤ -((slack - 1) * have_ C y0 moves (apply C M 0 slack down up y0 moves).2.1 i) := by
      have : 0 ≤ slack - 1 := sub_nonneg.mpr hs
      nlinarith
    exact le_min (le_trans (le_min (min_le_left _ _) ((min_le_right _ _).trans h2)) h1) (min_le_right _ _)

/-! ### The states Newton tries -/

/--
Network.predict's factor for a node, from each of its masses and the change the fluxes at the start would make to it:
`if (dm < 0 && m + dm < 0) f = Math.min(f, m > 0 ? 0.9 * m / -dm : 0)`, from 1. (`c`: the 0.9.)
-/
def predictFactor (c : α) (masses : List (α × α)) : α :=
  masses.foldl (fun f p => if p.2 < 0 ∧ p.1 + p.2 < 0 then min f (if 0 < p.1 then c * p.1 / -p.2 else 0) else f) 1

theorem predictFactor_spec {c : α} (hc : 0 ≤ c) (masses : List (α × α)) :
    0 ≤ predictFactor c masses ∧ predictFactor c masses ≤ 1
      ∧ ∀ p ∈ masses, p.2 < 0 → p.1 + p.2 < 0 →
        predictFactor c masses ≤ if 0 < p.1 then c * p.1 / -p.2 else 0 := by
  unfold predictFactor
  suffices h : ∀ a : α, 0 ≤ a →
      0 ≤ masses.foldl (fun f p => if p.2 < 0 ∧ p.1 + p.2 < 0
          then min f (if 0 < p.1 then c * p.1 / -p.2 else 0) else f) a
      ∧ masses.foldl (fun f p => if p.2 < 0 ∧ p.1 + p.2 < 0
          then min f (if 0 < p.1 then c * p.1 / -p.2 else 0) else f) a ≤ a
      ∧ ∀ p ∈ masses, p.2 < 0 → p.1 + p.2 < 0 →
        masses.foldl (fun f p => if p.2 < 0 ∧ p.1 + p.2 < 0
          then min f (if 0 < p.1 then c * p.1 / -p.2 else 0) else f) a
          ≤ if 0 < p.1 then c * p.1 / -p.2 else 0 from h 1 zero_le_one
  induction masses with
  | nil => intro a ha; exact ⟨ha, le_rfl, fun p hp => absurd hp List.not_mem_nil⟩
  | cons q rest ih =>
    intro a ha
    rw [List.foldl_cons]
    have hterm : q.2 < 0 → 0 ≤ (if 0 < q.1 then c * q.1 / -q.2 else 0) := by
      intro hq
      split_ifs with hm
      · exact div_nonneg (mul_nonneg hc hm.le) (neg_nonneg.mpr hq.le)
      · exact le_rfl
    by_cases hq : q.2 < 0 ∧ q.1 + q.2 < 0
    · rw [ifPos hq]
      obtain ⟨h1, h2, h3⟩ := ih (min a (if 0 < q.1 then c * q.1 / -q.2 else 0)) (le_min ha (hterm hq.1))
      refine ⟨h1, h2.trans (min_le_left _ _), ?_⟩
      intro p hp hp1 hp2
      rcases List.mem_cons.mp hp with rfl | hp
      · exact h2.trans (min_le_right _ _)
      · exact h3 p hp hp1 hp2
    · rw [ifNeg hq]
      obtain ⟨h1, h2, h3⟩ := ih a ha
      refine ⟨h1, h2, ?_⟩
      intro p hp hp1 hp2
      rcases List.mem_cons.mp hp with rfl | hp
      · exact absurd ⟨hp1, hp2⟩ hq
      · exact h3 p hp hp1 hp2

/-- **The predicted state** (Newton's starting point) has no mass lower than at the start of the step, or than 0. -/
theorem predict_mass {c : α} (hc0 : 0 ≤ c) (hc1 : c ≤ 1) (masses : List (α × α)) {m dm : α}
    (hp : (m, dm) ∈ masses) : min m 0 ≤ m + predictFactor c masses * dm := by
  obtain ⟨hf0, hf1, hf⟩ := predictFactor_spec hc0 masses
  by_cases hdm : dm < 0
  · by_cases hlow : m + dm < 0
    · have h := hf (m, dm) hp hdm hlow
      dsimp only at h
      by_cases hm : 0 < m
      · rw [ifPos hm] at h
        have h2 : predictFactor c masses * -dm ≤ c * m := by
          have := mul_le_mul_of_nonneg_right h (neg_nonneg.mpr hdm.le)
          rwa [div_mul_cancel₀ _ (neg_pos.mpr hdm).ne'] at this
        apply (min_le_right _ _).trans
        nlinarith
      · rw [ifNeg hm] at h
        have hz : predictFactor c masses = 0 := le_antisymm h hf0
        rw [hz, zero_mul, add_zero]
        exact min_le_left _ _
    · apply (min_le_right _ _).trans
      nlinarith [not_lt.mp hlow]
  · apply (min_le_left _ _).trans
    nlinarith [not_lt.mp hdm]

/--
Newton's step for a mass it would take below zero: one holding more than the floor limits the whole step (`alphaOf`),
one at or under the floor is stepped on its own instead, `d = -0.99 * Math.max(0, m)`. (`c`: the 0.99.)
-/
def dampedStep (c floor m dm : α) : α :=
  if dm < 0 ∧ m + dm < 0 ∧ ¬ floor < m then -(c * pos m) else dm

/-- Newton's step length, from each mass's (floor, m, dm): `if (m > floor) alpha = Math.min(alpha, 0.99 * m / -dm)`
for those the step would take below zero, from 1. -/
def alphaOf (c : α) (masses : List (α × α × α)) : α :=
  masses.foldl (fun a p => if p.2.2 < 0 ∧ p.2.1 + p.2.2 < 0 ∧ p.1 < p.2.1 then min a (c * p.2.1 / -p.2.2) else a) 1

theorem alphaOf_spec (c : α) (masses : List (α × α × α)) :
    alphaOf c masses ≤ 1 ∧ ∀ p ∈ masses, p.2.2 < 0 → p.2.1 + p.2.2 < 0 → p.1 < p.2.1 →
      alphaOf c masses ≤ c * p.2.1 / -p.2.2 := by
  unfold alphaOf
  suffices h : ∀ a : α,
      masses.foldl (fun a p => if p.2.2 < 0 ∧ p.2.1 + p.2.2 < 0 ∧ p.1 < p.2.1
          then min a (c * p.2.1 / -p.2.2) else a) a ≤ a
      ∧ ∀ p ∈ masses, p.2.2 < 0 → p.2.1 + p.2.2 < 0 → p.1 < p.2.1 →
        masses.foldl (fun a p => if p.2.2 < 0 ∧ p.2.1 + p.2.2 < 0 ∧ p.1 < p.2.1
          then min a (c * p.2.1 / -p.2.2) else a) a ≤ c * p.2.1 / -p.2.2 from h 1
  induction masses with
  | nil => intro a; exact ⟨le_rfl, fun p hp => absurd hp List.not_mem_nil⟩
  | cons q rest ih =>
    intro a
    rw [List.foldl_cons]
    by_cases hq : q.2.2 < 0 ∧ q.2.1 + q.2.2 < 0 ∧ q.1 < q.2.1
    · rw [ifPos hq]
      obtain ⟨h2, h3⟩ := ih (min a (c * q.2.1 / -q.2.2))
      refine ⟨h2.trans (min_le_left _ _), ?_⟩
      intro p hp hp1 hp2 hp3
      rcases List.mem_cons.mp hp with rfl | hp
      · exact h2.trans (min_le_right _ _)
      · exact h3 p hp hp1 hp2 hp3
    · rw [ifNeg hq]
      obtain ⟨h2, h3⟩ := ih a
      refine ⟨h2, ?_⟩
      intro p hp hp1 hp2 hp3
      rcases List.mem_cons.mp hp with rfl | hp
      · exact absurd ⟨hp1, hp2, hp3⟩ hq
      · exact h3 p hp hp1 hp2 hp3

/--
**Every state Newton tries** (the step at any length from 0 to `alphaOf`: the line search halves it) has no mass lower
than the state it steps from, or than 0. With `predict_mass`: no state of a step has a mass lower than the step's
start, or than 0.
-/
theorem trial_mass {c : α} (hc0 : 0 ≤ c) (hc1 : c ≤ 1) (masses : List (α × α × α)) {floor m dm : α}
    (hp : (floor, m, dm) ∈ masses) (hfl : 0 ≤ floor) {t : α} (ht0 : 0 ≤ t) (ht : t ≤ alphaOf c masses) :
    min m 0 ≤ m + t * dampedStep c floor m dm := by
  obtain ⟨ha1, ha⟩ := alphaOf_spec c masses
  have ht1 : t ≤ 1 := ht.trans ha1
  unfold dampedStep
  by_cases hdm : dm < 0
  · by_cases hlow : m + dm < 0
    · by_cases hm : floor < m
      · have h := ht.trans (ha (floor, m, dm) hp hdm hlow hm)
        have h2 : t * -dm ≤ c * m := by
          have := mul_le_mul_of_nonneg_right h (neg_nonneg.mpr hdm.le)
          rwa [div_mul_cancel₀ _ (neg_pos.mpr hdm).ne'] at this
        rw [ifNeg (fun hh => hh.2.2 hm)]
        apply (min_le_right _ _).trans
        nlinarith [hfl.trans hm.le]
      · rw [ifPos ⟨hdm, hlow, hm⟩]
        by_cases hm0 : 0 ≤ m
        · rw [pos_of_nonneg hm0]
          apply (min_le_right _ _).trans
          have : t * c ≤ 1 := by nlinarith
          nlinarith
        · rw [pos_of_nonpos (not_le.mp hm0).le]
          apply (min_le_left _ _).trans
          simp
    · rw [ifNeg (fun hh => hlow hh.2.1)]
      apply (min_le_right _ _).trans
      nlinarith [not_lt.mp hlow]
  · rw [ifNeg (fun hh => hdm hh.1)]
    apply (min_le_left _ _).trans
    nlinarith [not_lt.mp hdm]

end FluidSpec.Step
