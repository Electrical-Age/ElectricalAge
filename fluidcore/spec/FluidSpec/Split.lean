import Mathlib
import FluidSpec.Flux
/-
The phase split of one group of liquids that mix (MixFluid.split). A group holds n i moles of each species i, of
which the gas space would hold c i as saturated vapour (0 for a species that never boils). The split is three steps:

1. X, the group's saturation coordinate: the root of sum n i / sm (X + c i) = 1, sm a softplus as wide as 1 % of the
   group. Without the smoothing this is Rachford-Rice in negative-flash form, sum n i / (X + c i) = 1, whose root
   is the liquid's moles where positive, and below zero by what the vapour lacks to saturation.
2. N = sm X, the liquid's moles.
3. lam in (0, 1], the factor on the c i at which the shares x i = n i / (N + lam c i) sum to 1.

Here: that each step has exactly one answer, where it lies, and what the shares then satisfy.

Why every term is smoothed, and not only N: the unsmoothed root cannot pass below -c i of any species that is present
at all. A first trace of one with c i under sm's width then holds X near zero, and N = sm X makes about a width of
liquid out of a superheated vapour (found by asking for `soft_gap` below of the earlier form, which had no such
statement). Under sm a species' term is at most n i / sm (X + c i) at any X, and fades with n i.
-/
namespace FluidSpec.Split
open Finset

set_option linter.unusedSectionVars false

variable {ι : Type*} (s : Finset ι) (n c : ι → ℝ)

/-! ### The unsmoothed equation and its root X* -/

/-- sum n i / (X + c i): the liquid's shares' sum, were its amount X. -/
noncomputable def fracSum (X : ℝ) : ℝ := ∑ i ∈ s, n i / (X + c i)

/-- It never rises with X (where every X + c i is positive). -/
theorem fracSum_anti {X Y : ℝ} (hn : ∀ i ∈ s, 0 ≤ n i) (hX : ∀ i ∈ s, 0 < X + c i) (hXY : X ≤ Y) :
    fracSum s n c Y ≤ fracSum s n c X := by
  unfold fracSum
  apply sum_le_sum
  intro i hi
  exact div_le_div_of_nonneg_left (hn i hi) (hX i hi) (by linarith)

/-- And falls strictly where anything is present. -/
theorem fracSum_strictAnti {X Y : ℝ} (hn : ∀ i ∈ s, 0 ≤ n i) (hp : ∃ i ∈ s, 0 < n i)
    (hX : ∀ i ∈ s, 0 < X + c i) (hXY : X < Y) : fracSum s n c Y < fracSum s n c X := by
  unfold fracSum
  apply sum_lt_sum
  · intro i hi
    exact div_le_div_of_nonneg_left (hn i hi) (hX i hi) (by linarith)
  · obtain ⟨i, hi, hni⟩ := hp
    exact ⟨i, hi, div_lt_div_of_pos_left hni (hX i hi) (by linarith)⟩

/-- **The root is unique.** -/
theorem root_unique {X Y : ℝ} (hn : ∀ i ∈ s, 0 ≤ n i) (hp : ∃ i ∈ s, 0 < n i)
    (hX : ∀ i ∈ s, 0 < X + c i) (hY : ∀ i ∈ s, 0 < Y + c i)
    (rX : fracSum s n c X = 1) (rY : fracSum s n c Y = 1) : X = Y := by
  rcases lt_trichotomy X Y with h | h | h
  · have := fracSum_strictAnti s n c hn hp hX h
    linarith
  · exact h
  · have := fracSum_strictAnti s n c hn hp hY h
    linarith

theorem fracSum_continuousOn {a b : ℝ} (ha : ∀ i ∈ s, 0 < a + c i) :
    ContinuousOn (fracSum s n c) (Set.Icc a b) := by
  unfold fracSum
  apply continuousOn_finsetSum
  intro i hi
  apply ContinuousOn.div continuousOn_const (continuousOn_id.add continuousOn_const)
  intro x hx
  have h1 := ha i hi
  have h2 := hx.1
  show x + c i ≠ 0
  linarith

/-- **The root exists**, between where the species k of least c would put it alone (n k - c k, where Java's Newton
starts) and the whole group's moles less that c. -/
theorem root_exists {k : ι} (hk : k ∈ s) (hn : ∀ i ∈ s, 0 ≤ n i) (hnk : 0 < n k) (hck : ∀ i ∈ s, c k ≤ c i) :
    ∃ X, n k - c k ≤ X ∧ X ≤ ∑ i ∈ s, n i - c k ∧ (∀ i ∈ s, 0 < X + c i) ∧ fracSum s n c X = 1 := by
  have hle : n k ≤ ∑ i ∈ s, n i := single_le_sum (f := n) hn hk
  have htot : 0 < ∑ i ∈ s, n i := lt_of_lt_of_le hnk hle
  have hdom : ∀ i ∈ s, 0 < (n k - c k) + c i := fun i hi => by linarith [hck i hi]
  have hlo : 1 ≤ fracSum s n c (n k - c k) := by
    unfold fracSum
    have h1 : n k / ((n k - c k) + c k) = 1 := by
      rw [sub_add_cancel]
      exact div_self hnk.ne'
    rw [← h1]
    exact single_le_sum (f := fun i => n i / ((n k - c k) + c i))
      (fun i hi => div_nonneg (hn i hi) (hdom i hi).le) hk
  have hhi : fracSum s n c (∑ i ∈ s, n i - c k) ≤ 1 := by
    unfold fracSum
    have h1 : ∑ i ∈ s, n i / (∑ j ∈ s, n j) = 1 := by
      rw [← sum_div]
      exact div_self htot.ne'
    rw [← h1]
    apply sum_le_sum
    intro i hi
    exact div_le_div_of_nonneg_left (hn i hi) htot (by linarith [hck i hi])
  have hab : n k - c k ≤ ∑ i ∈ s, n i - c k := by linarith
  have hivt := intermediate_value_Icc' hab (fracSum_continuousOn s n c hdom)
  obtain ⟨X, hX, rX⟩ := hivt ⟨hhi, hlo⟩
  exact ⟨X, hX.1, hX.2, fun i hi => by linarith [hdom i hi, hX.1], rX⟩

/-- **Saturation.** With every species volatile, the root is above zero (there is liquid) exactly when the group
holds more than its saturated vapour: sum n i / c i > 1. -/
theorem saturated_iff {X : ℝ} (hn : ∀ i ∈ s, 0 ≤ n i) (hp : ∃ i ∈ s, 0 < n i) (hc : ∀ i ∈ s, 0 < c i)
    (hX : ∀ i ∈ s, 0 < X + c i) (rX : fracSum s n c X = 1) : 0 < X ↔ 1 < ∑ i ∈ s, n i / c i := by
  have h0 : fracSum s n c 0 = ∑ i ∈ s, n i / c i := by
    unfold fracSum
    simp only [zero_add]
  have hd0 : ∀ i ∈ s, 0 < 0 + c i := fun i hi => by linarith [hc i hi]
  constructor
  · intro h
    have := fracSum_strictAnti s n c hn hp hd0 h
    linarith
  · intro h
    by_contra hneg
    have := fracSum_anti s n c hn hX (not_lt.mp hneg)
    linarith

/-- **One species alone**: the root is n - c, and the split WaterAir's. -/
theorem single_root {n0 c0 X : ℝ} (hX : 0 < X + c0) : n0 / (X + c0) = 1 ↔ X = n0 - c0 := by
  rw [div_eq_one_iff_eq hX.ne']
  constructor <;> intro h <;> linarith

/-! ### Steps 1 and 2: the smoothed root and the liquid's moles -/

/-- What the split needs of the smoothing sm: positive, never below its argument, and rising no faster than it. -/
structure Smooth (sm : ℝ → ℝ) : Prop where
  pos : ∀ X, 0 < sm X
  ge : ∀ X, X ≤ sm X
  lip : ∀ X d, 0 ≤ d → sm (X + d) ≤ sm X + d

variable (sm : ℝ → ℝ)

/-- sum n i / sm (X + c i): what step 1 sets to 1. -/
noncomputable def softSum (X : ℝ) : ℝ := ∑ i ∈ s, n i / sm (X + c i)

/-- Where the smoothing changes no term that is there, the equation is the unsmoothed one. -/
theorem softSum_exact {X : ℝ} (h : ∀ i ∈ s, sm (X + c i) = X + c i) : softSum s n c sm X = fracSum s n c X := by
  unfold softSum fracSum
  exact sum_congr rfl (fun i hi => by rw [h i hi])

/-- **One species alone**: X = n - c where the smoothing leaves n itself alone (Java's does from 30 widths up, and
the width is 1 % of n), so N = sm (n - c): WaterAir's split. -/
theorem single_soft {n0 c0 : ℝ} (hn0 : 0 < n0) (hex : sm n0 = n0) : n0 / sm ((n0 - c0) + c0) = 1 := by
  rw [sub_add_cancel, hex]
  exact div_self hn0.ne'

theorem softSum_strictAnti {X Y : ℝ} (hs : Smooth sm) (hm : StrictMono sm) (hn : ∀ i ∈ s, 0 ≤ n i)
    (hp : ∃ i ∈ s, 0 < n i) (hXY : X < Y) : softSum s n c sm Y < softSum s n c sm X := by
  unfold softSum
  apply sum_lt_sum
  · intro i hi
    exact div_le_div_of_nonneg_left (hn i hi) (hs.pos _) (hm.monotone (by linarith))
  · obtain ⟨i, hi, hni⟩ := hp
    exact ⟨i, hi, div_lt_div_of_pos_left hni (hs.pos _) (hm (by linarith))⟩

/-- **X is unique** (for a smoothing that rises). -/
theorem soft_unique {X Y : ℝ} (hs : Smooth sm) (hm : StrictMono sm) (hn : ∀ i ∈ s, 0 ≤ n i)
    (hp : ∃ i ∈ s, 0 < n i) (rX : softSum s n c sm X = 1) (rY : softSum s n c sm Y = 1) : X = Y := by
  rcases lt_trichotomy X Y with h | h | h
  · have := softSum_strictAnti s n c sm hs hm hn hp h
    linarith
  · exact h
  · have := softSum_strictAnti s n c sm hs hm hn hp h
    linarith

/-- **X exists** (for a continuous smoothing that comes as near zero as one likes), at or below the group's moles.
Unlike the unsmoothed root it needs no species of least c to stand on. -/
theorem soft_exists (hs : Smooth sm) (hcont : Continuous sm) (hlow : ∀ ε, 0 < ε → ∃ X, sm X ≤ ε)
    (hn : ∀ i ∈ s, 0 ≤ n i) (hp : ∃ i ∈ s, 0 < n i) (hc : ∀ i ∈ s, 0 ≤ c i) :
    ∃ X, X ≤ ∑ i ∈ s, n i ∧ softSum s n c sm X = 1 := by
  obtain ⟨k, hk, hnk⟩ := hp
  have hle : n k ≤ ∑ i ∈ s, n i := single_le_sum (f := n) hn hk
  have htot : 0 < ∑ i ∈ s, n i := lt_of_lt_of_le hnk hle
  obtain ⟨Xl, hXl⟩ := hlow (n k) hnk
  have hlo : 1 ≤ softSum s n c sm (Xl - c k) := by
    unfold softSum
    have h1 : 1 ≤ n k / sm ((Xl - c k) + c k) := by
      rw [sub_add_cancel, le_div_iff₀ (hs.pos _)]
      linarith
    exact h1.trans (single_le_sum (f := fun i => n i / sm ((Xl - c k) + c i))
      (fun i hi => div_nonneg (hn i hi) (hs.pos _).le) hk)
  have hhi : softSum s n c sm (∑ i ∈ s, n i) ≤ 1 := by
    unfold softSum
    have h1 : ∑ i ∈ s, n i / (∑ j ∈ s, n j) = 1 := by
      rw [← sum_div]
      exact div_self htot.ne'
    rw [← h1]
    apply sum_le_sum
    intro i hi
    exact div_le_div_of_nonneg_left (hn i hi) htot (by linarith [hs.ge (∑ j ∈ s, n j + c i), hc i hi])
  have hab : Xl - c k ≤ ∑ i ∈ s, n i := by linarith [hs.ge Xl, hc k hk]
  have hc' : ContinuousOn (softSum s n c sm) (Set.Icc (Xl - c k) (∑ i ∈ s, n i)) := by
    unfold softSum
    apply continuousOn_finsetSum
    intro i hi
    have hd : ContinuousOn (fun x => sm (x + c i)) (Set.Icc (Xl - c k) (∑ i ∈ s, n i)) :=
      (hcont.comp (continuous_id.add continuous_const)).continuousOn
    exact ContinuousOn.div continuousOn_const hd (fun x _ => (hs.pos _).ne')
  obtain ⟨X, hX, rX⟩ := intermediate_value_Icc' hab hc' ⟨hhi, hlo⟩
  exact ⟨X, hX.2, rX⟩

/-- **No term is above 1 at the root**: n i <= sm (X + c i) for every species. Java's Newton starts where the
largest term is 1, which is therefore at or below the root. -/
theorem soft_terms {X : ℝ} {i : ι} (hs : Smooth sm) (hn : ∀ i ∈ s, 0 ≤ n i) (hi : i ∈ s)
    (rX : softSum s n c sm X = 1) : n i ≤ sm (X + c i) := by
  have h : n i / sm (X + c i) ≤ 1 := by
    rw [← rX]
    unfold softSum
    exact single_le_sum (f := fun j => n j / sm (X + c j)) (fun j hj => div_nonneg (hn j hj) (hs.pos _).le) hi
  rwa [div_le_one (hs.pos _)] at h

/-- **The liquid holds all of what never boils**: N = sm X is at least n i where c i = 0. -/
theorem soft_holds {X : ℝ} {i : ι} (hs : Smooth sm) (hn : ∀ i ∈ s, 0 ≤ n i) (hi : i ∈ s) (hci : c i = 0)
    (rX : softSum s n c sm X = 1) : n i ≤ sm X := by
  have h := soft_terms s n c sm hs hn hi rX
  rwa [hci, add_zero] at h

/-- **N is not below the unsmoothed liquid.** At N = sm X the unsmoothed equation's left side is at most 1, so its
root (the liquid's moles at exact equilibrium) is at or below N. This is what step 3 needs, and where "rising no
faster than its argument" is used. -/
theorem soft_covers {X : ℝ} (hs : Smooth sm) (hn : ∀ i ∈ s, 0 ≤ n i) (hc : ∀ i ∈ s, 0 ≤ c i)
    (rX : softSum s n c sm X = 1) : fracSum s n c (sm X) ≤ 1 := by
  rw [← rX]
  unfold fracSum softSum
  apply sum_le_sum
  intro i hi
  exact div_le_div_of_nonneg_left (hn i hi) (hs.pos _) (hs.lip X (c i) (hc i hi))

/-- **N is no more than the group's moles.** -/
theorem soft_le_total {X : ℝ} (hs : Smooth sm) (hm : Monotone sm) (hn : ∀ i ∈ s, 0 ≤ n i)
    (hc : ∀ i ∈ s, 0 ≤ c i) (rX : softSum s n c sm X = 1) : sm X ≤ ∑ i ∈ s, n i := by
  have h : softSum s n c sm X ≤ ∑ i ∈ s, n i / sm X := by
    unfold softSum
    apply sum_le_sum
    intro i hi
    exact div_le_div_of_nonneg_left (hn i hi) (hs.pos _) (hm (by linarith [hc i hi]))
  rw [rX, ← sum_div, le_div_iff₀ (hs.pos X), one_mul] at h
  exact h

/-- **A trace changes a trace.** X is the root without some species, X' the root with nd of it, of capacity cd. Then
X' is not below X, and between them the others' sum falls by exactly the newcomer's term, which is at most
nd / sm (X + cd): nd over a number the newcomer has no part in. As nd goes to 0, X' goes to X. (Unsmoothed, the
newcomer's term is nd / (X' + cd) and X' > -cd however small nd is.) -/
theorem soft_gap {X X' nd cd : ℝ} (hs : Smooth sm) (hm : StrictMono sm) (hn : ∀ i ∈ s, 0 ≤ n i)
    (hp : ∃ i ∈ s, 0 < n i) (hnd : 0 ≤ nd)
    (rX : softSum s n c sm X = 1) (rX' : softSum s n c sm X' + nd / sm (X' + cd) = 1) :
    X ≤ X' ∧ softSum s n c sm X - softSum s n c sm X' = nd / sm (X' + cd) ∧
      nd / sm (X' + cd) ≤ nd / sm (X + cd) := by
  have hpos : 0 ≤ nd / sm (X' + cd) := div_nonneg hnd (hs.pos _).le
  have hle : X ≤ X' := by
    by_contra h
    have := softSum_strictAnti s n c sm hs hm hn hp (not_le.mp h)
    linarith
  exact ⟨hle, by linarith, div_le_div_of_nonneg_left hnd (hs.pos _) (hm.monotone (by linarith))⟩

/-! ### The smoothings -/

/-- The model's: w softplus(X / w). -/
noncomputable def soft (w X : ℝ) : ℝ := w * Flux.softplus (X / w)

theorem soft_smooth {w : ℝ} (hw : 0 < w) : Smooth (soft w) where
  pos X := mul_pos hw (Flux.softplus_pos _)
  ge X := by
    have h := Flux.softplus_gt (X / w)
    have h2 : w * (X / w) < w * Flux.softplus (X / w) := mul_lt_mul_of_pos_left h hw
    rw [mul_div_cancel₀ X hw.ne'] at h2
    exact h2.le
  lip X d hd := by
    unfold soft Flux.softplus
    have hb : 0 ≤ d / w := div_nonneg hd hw.le
    have h1 : 1 + Real.exp ((X + d) / w) ≤ Real.exp (d / w) * (1 + Real.exp (X / w)) := by
      rw [add_div, Real.exp_add]
      have := Real.one_le_exp hb
      nlinarith [Real.exp_pos (X / w), Real.exp_pos (d / w)]
    have h2 : Real.log (1 + Real.exp ((X + d) / w)) ≤ d / w + Real.log (1 + Real.exp (X / w)) := by
      have h3 := Real.log_le_log (by linarith [Real.exp_pos ((X + d) / w)]) h1
      rwa [Real.log_mul (Real.exp_pos _).ne' (by linarith [Real.exp_pos (X / w)]), Real.log_exp] at h3
    have h4 := mul_le_mul_of_nonneg_left h2 hw.le
    rw [mul_add, mul_div_cancel₀ d hw.ne'] at h4
    linarith

theorem soft_strictMono {w : ℝ} (hw : 0 < w) : StrictMono (soft w) := by
  intro X Y hXY
  unfold soft Flux.softplus
  apply mul_lt_mul_of_pos_left _ hw
  apply Real.log_lt_log (by linarith [Real.exp_pos (X / w)])
  have : X / w < Y / w := div_lt_div_of_pos_right hXY hw
  linarith [Real.exp_lt_exp.mpr this]

theorem soft_continuous {w : ℝ} : Continuous (soft w) := by
  unfold soft Flux.softplus
  apply Continuous.mul continuous_const
  apply Continuous.log
  · exact continuous_const.add (Real.continuous_exp.comp (continuous_id.div_const w))
  · intro x
    have := Real.exp_pos (x / w)
    show 1 + Real.exp (id x / w) ≠ 0
    simp only [id]
    linarith

/-- It comes as near zero as one likes. -/
theorem soft_low {w : ℝ} (hw : 0 < w) : ∀ ε, 0 < ε → ∃ X, soft w X ≤ ε := by
  intro ε hε
  refine ⟨w * Real.log (ε / w), ?_⟩
  unfold soft
  rw [mul_div_cancel_left₀ _ hw.ne']
  have h := Flux.softplus_le_exp (Real.log (ε / w))
  rw [Real.exp_log (div_pos hε hw)] at h
  have h2 := mul_le_mul_of_nonneg_left h hw.le
  rwa [mul_div_cancel₀ ε hw.ne'] at h2

/-- Java's (D.softplus, MixFluid.soft): X itself more than 30 widths up, w e^(X / w) more than 30 down. -/
noncomputable def softCode (w X : ℝ) : ℝ :=
  if 30 < X / w then X else if X / w < -30 then w * Real.exp (X / w) else soft w X

/-- Above its upper cut it is exact: what `single_soft` asks. -/
theorem softCode_exact {w X : ℝ} (h : 30 < X / w) : softCode w X = X := by
  unfold softCode
  simp only [h, ↓reduceIte]

theorem softCode_pos {w : ℝ} (hw : 0 < w) (X : ℝ) : 0 < softCode w X := by
  unfold softCode
  by_cases h : 30 < X / w
  · simp only [h, ↓reduceIte]
    have : 0 < X / w := by linarith
    exact (div_pos_iff_of_pos_right hw).mp this
  · simp only [h, ↓reduceIte]
    by_cases h2 : X / w < -30
    · simp only [h2, ↓reduceIte]
      exact mul_pos hw (Real.exp_pos _)
    · simp only [h2, ↓reduceIte]
      exact (soft_smooth hw).pos X

theorem softCode_ge {w : ℝ} (hw : 0 < w) (X : ℝ) : X ≤ softCode w X := by
  unfold softCode
  by_cases h : 30 < X / w
  · simp only [h, ↓reduceIte, le_refl]
  · simp only [h, ↓reduceIte]
    by_cases h2 : X / w < -30
    · simp only [h2, ↓reduceIte]
      have hX : X < 0 := by
        have : X / w < 0 := by linarith
        by_contra hX
        have := div_nonneg (not_lt.mp hX) hw.le
        linarith
      linarith [mul_pos hw (Real.exp_pos (X / w))]
    · simp only [h2, ↓reduceIte]
      exact (soft_smooth hw).ge X

/-- Below its upper cut Java's is at or above the model's. -/
theorem soft_le_code {w X : ℝ} (hw : 0 < w) (h : ¬ 30 < X / w) : soft w X ≤ softCode w X := by
  unfold softCode
  simp only [h, ↓reduceIte]
  by_cases h2 : X / w < -30
  · simp only [h2, ↓reduceIte]
    unfold soft
    exact mul_le_mul_of_nonneg_left (Flux.softplus_le_exp _) hw.le
  · simp only [h2, ↓reduceIte, le_refl]

theorem softCode_lip {w : ℝ} (hw : 0 < w) (X d : ℝ) (hd : 0 ≤ d) : softCode w (X + d) ≤ softCode w X + d := by
  have hb : 0 ≤ d / w := div_nonneg hd hw.le
  have hadd : (X + d) / w = X / w + d / w := add_div X d w
  by_cases h : 30 < (X + d) / w
  · rw [softCode_exact h]
    linarith [softCode_ge hw X]
  · have hX : ¬ 30 < X / w := by
      intro hc
      apply h
      linarith
    by_cases h2 : (X + d) / w < -30
    · have hX2 : X / w < -30 := by linarith
      have e : softCode w (X + d) = w * Real.exp ((X + d) / w) := by
        unfold softCode
        simp only [h, h2, ↓reduceIte]
      have e' : softCode w X = w * Real.exp (X / w) := by
        unfold softCode
        simp only [hX, hX2, ↓reduceIte]
      rw [e, e', hadd]
      have h3 : Real.exp (X / w) = Real.exp (X / w + d / w) * Real.exp (-(d / w)) := by
        rw [← Real.exp_add]
        congr 1
        ring
      have h4 : 1 - d / w ≤ Real.exp (-(d / w)) := by
        have := Real.add_one_le_exp (-(d / w))
        linarith
      have h5 : Real.exp (X / w + d / w) ≤ 1 := Real.exp_le_one_iff.mpr (by linarith)
      have h6 : 0 < Real.exp (X / w + d / w) := Real.exp_pos _
      have h7 : Real.exp (X / w + d / w) ≤ Real.exp (X / w) + d / w := by
        rw [h3]
        nlinarith [mul_le_mul_of_nonneg_left h4 h6.le, mul_le_mul_of_nonneg_right h5 hb]
      have h8 := mul_le_mul_of_nonneg_left h7 hw.le
      rw [mul_add, mul_div_cancel₀ d hw.ne'] at h8
      exact h8
    · have e : softCode w (X + d) = soft w (X + d) := by
        unfold softCode
        simp only [h, h2, ↓reduceIte]
      rw [e]
      linarith [(soft_smooth hw).lip X d hd, soft_le_code hw hX]

theorem softCode_smooth {w : ℝ} (hw : 0 < w) : Smooth (softCode w) where
  pos := softCode_pos hw
  ge := softCode_ge hw
  lip := softCode_lip hw

/-! ### Step 3: the factor lam and the shares -/

variable (N : ℝ)

/-- The shares' sum at the factor l: sum n i / (N + l c i). -/
noncomputable def shareSum (l : ℝ) : ℝ := ∑ i ∈ s, n i / (N + l * c i)

/-- **lam is unique.** -/
theorem lam_unique {l l' : ℝ} (hN : 0 < N) (hn : ∀ i ∈ s, 0 ≤ n i) (hc : ∀ i ∈ s, 0 ≤ c i)
    (hp : ∃ i ∈ s, 0 < n i ∧ 0 < c i) (hl : 0 ≤ l) (hl' : 0 ≤ l')
    (r : shareSum s n c N l = 1) (r' : shareSum s n c N l' = 1) : l = l' := by
  have key : ∀ {a b : ℝ}, 0 ≤ a → a < b → shareSum s n c N b < shareSum s n c N a := by
    intro a b ha hab
    unfold shareSum
    apply sum_lt_sum
    · intro i hi
      have h1 : 0 < N + a * c i := by nlinarith [hc i hi]
      exact div_le_div_of_nonneg_left (hn i hi) h1 (by nlinarith [hc i hi])
    · obtain ⟨i, hi, hni, hci⟩ := hp
      have h1 : 0 < N + a * c i := by nlinarith
      exact ⟨i, hi, div_lt_div_of_pos_left hni h1 (by nlinarith)⟩
  rcases lt_trichotomy l l' with h | h | h
  · have := key hl h
    linarith
  · exact h
  · have := key hl' h
    linarith

/-- **lam exists in [0, 1]** when N is no more than the group's moles (`soft_le_total`) and no less than the
unsmoothed liquid (`soft_covers`). -/
theorem lam_exists (hN : 0 < N) (hc : ∀ i ∈ s, 0 ≤ c i)
    (htot : N ≤ ∑ i ∈ s, n i) (hcov : fracSum s n c N ≤ 1) :
    ∃ l, 0 ≤ l ∧ l ≤ 1 ∧ shareSum s n c N l = 1 := by
  have h0 : 1 ≤ shareSum s n c N 0 := by
    unfold shareSum
    simp only [zero_mul, add_zero]
    rw [← sum_div, le_div_iff₀ hN]
    linarith
  have h1 : shareSum s n c N 1 ≤ 1 := by
    unfold shareSum
    unfold fracSum at hcov
    simpa only [one_mul] using hcov
  have hcont : ContinuousOn (shareSum s n c N) (Set.Icc 0 1) := by
    unfold shareSum
    apply continuousOn_finsetSum
    intro i hi
    apply ContinuousOn.div continuousOn_const
      (continuousOn_const.add (continuousOn_id.mul continuousOn_const))
    intro x hx
    have h2 := hx.1
    show N + x * c i ≠ 0
    nlinarith [hc i hi, mul_nonneg h2 (hc i hi)]
  obtain ⟨l, hl, rl⟩ := intermediate_value_Icc' (zero_le_one' ℝ) hcont ⟨h1, h0⟩
  exact ⟨l, hl.1, hl.2, rl⟩

/-- With less liquid than the group's moles the factor is not zero: some of every volatile is vapour. -/
theorem lam_pos {l : ℝ} (hN : 0 < N) (htot : N < ∑ i ∈ s, n i) (r : shareSum s n c N l = 1) : l ≠ 0 := by
  intro h
  rw [h] at r
  unfold shareSum at r
  simp only [zero_mul, add_zero] at r
  rw [← sum_div, div_eq_one_iff_eq hN.ne'] at r
  linarith

/-- Species i's share of the liquid. -/
noncomputable def share (l : ℝ) (i : ι) : ℝ := n i / (N + l * c i)

/-- **The liquid is N**: the species' parts sum to it. -/
theorem liquid_total {l : ℝ} (r : shareSum s n c N l = 1) : ∑ i ∈ s, share n c N l i * N = N := by
  unfold shareSum at r
  unfold share
  rw [← sum_mul, r, one_mul]

/-- **No species is more liquid than there is of it, or less than none**; what is not liquid is vapour at
lam x i times its saturation (Raoult's law, scaled by lam). -/
theorem liquid_bounds {l : ℝ} {i : ι} (hN : 0 < N) (hl : 0 ≤ l) (hn : 0 ≤ n i) (hc : 0 ≤ c i) :
    0 ≤ share n c N l i * N ∧ share n c N l i * N ≤ n i ∧
      n i - share n c N l i * N = share n c N l i * (l * c i) := by
  unfold share
  have hd : 0 < N + l * c i := by nlinarith
  refine ⟨mul_nonneg (div_nonneg hn hd.le) hN.le, ?_, ?_⟩
  · rw [div_mul_eq_mul_div, div_le_iff₀ hd]
    nlinarith [mul_nonneg hn (mul_nonneg hl hc)]
  · field_simp
    ring

/-! ### The whole split -/

/-- **The split exists**, whatever the group holds (something of it present): an X, and at N = sm X a factor lam in
[0, 1] at which the shares sum to 1. For the model's smoothing. -/
theorem split_exists {w : ℝ} (hw : 0 < w) (hn : ∀ i ∈ s, 0 ≤ n i) (hp : ∃ i ∈ s, 0 < n i)
    (hc : ∀ i ∈ s, 0 ≤ c i) :
    ∃ X l, softSum s n c (soft w) X = 1 ∧ 0 ≤ l ∧ l ≤ 1 ∧ shareSum s n c (soft w X) l = 1 := by
  obtain ⟨X, -, rX⟩ := soft_exists s n c (soft w) (soft_smooth hw) soft_continuous (soft_low hw) hn hp hc
  obtain ⟨l, hl0, hl1, rl⟩ := lam_exists s n c (soft w X) ((soft_smooth hw).pos X) hc
    (soft_le_total s n c (soft w) (soft_smooth hw) (soft_strictMono hw).monotone hn hc rX)
    (soft_covers s n c (soft w) (soft_smooth hw) hn hc rX)
  exact ⟨X, l, rX, hl0, hl1, rl⟩

end FluidSpec.Split
