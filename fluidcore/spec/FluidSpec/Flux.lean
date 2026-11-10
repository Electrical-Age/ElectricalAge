import Mathlib
/-
What an edge carries. Network.flux builds each edge's flows from two streams (Network.streams), the shares each end
draws (NodeFluid.draw), conduction between the two nodes and the exchange of liquid for gas through an opening. Here
are the shapes of those pieces over the real numbers: signs, sums, what is zero at rest, what mirrors what. They say
that the flows are what the model means, not that the Java computes them (FluxShapeTest checks the Java in doubles
for what these lemmas assume of the fluids: shares not negative and summing to at most 1, windows in [0, 1]).
-/
namespace FluidSpec.Flux
open Finset

set_option linter.unusedSectionVars false

/-! ### The two streams of a pipe -/

/-- ln(1 + e^x): 0 far below zero, x far above. -/
noncomputable def softplus (x : ℝ) : ℝ := Real.log (1 + Real.exp x)

theorem softplus_pos (x : ℝ) : 0 < softplus x :=
  Real.log_pos (by linarith [Real.exp_pos x])

theorem softplus_gt (x : ℝ) : x < softplus x := by
  have h : Real.log (Real.exp x) < Real.log (1 + Real.exp x) :=
    Real.log_lt_log (Real.exp_pos x) (by linarith)
  rwa [Real.log_exp] at h

theorem softplus_le_exp (x : ℝ) : softplus x ≤ Real.exp x := by
  have h := Real.log_le_sub_one_of_pos (show 0 < 1 + Real.exp x by linarith [Real.exp_pos x])
  unfold softplus
  linarith

/-- The streams' mirror law: softplus(-x) = softplus(x) - x. -/
theorem softplus_neg (x : ℝ) : softplus (-x) = softplus x - x := by
  unfold softplus
  have hx : Real.exp x ≠ 0 := (Real.exp_pos x).ne'
  have h : 1 + Real.exp (-x) = (1 + Real.exp x) / Real.exp x := by
    rw [Real.exp_neg]
    field_simp
    ring
  rw [h, Real.log_div (by linarith [Real.exp_pos x]) hx, Real.log_exp]

/-- Java's softplus below its upper cut: e^x under -30 (where ln(1 + e^x) would round to it anyway). -/
noncomputable def spCode (x : ℝ) : ℝ := if x < -30 then Real.exp x else softplus x

theorem spCode_gt (x : ℝ) : x < spCode x := by
  unfold spCode
  by_cases h : x < -30
  · simp only [h, ↓reduceIte]
    linarith [Real.exp_pos x]
  · simp only [h, ↓reduceIte]
    exact softplus_gt x

theorem spCode_pos (x : ℝ) : 0 < spCode x := by
  unfold spCode
  by_cases h : x < -30
  · simp only [h, ↓reduceIte]
    exact Real.exp_pos x
  · simp only [h, ↓reduceIte]
    exact softplus_pos x

/-- `Network.streams` for a pipe of conductance g with the band S (SPLIT_DP) at the pressure difference dp:
(q+ from a, q- from b). More than 30 bands up there is no return stream. -/
noncomputable def streams (g S dp : ℝ) : ℝ × ℝ :=
  if 30 < dp / S then (g * dp, 0)
  else (g * S * spCode (dp / S), g * S * spCode (dp / S) - g * dp)

/-- The streams as the model means them, without the cuts. -/
noncomputable def smooth (g S dp : ℝ) : ℝ × ℝ :=
  (g * S * softplus (dp / S), g * S * softplus (dp / S) - g * dp)

/-- **The net flow is g dp**, exactly, whatever the streams. -/
theorem streams_net (g S dp : ℝ) : (streams g S dp).1 - (streams g S dp).2 = g * dp := by
  unfold streams
  by_cases h : 30 < dp / S
  · simp only [h, ↓reduceIte]
    ring
  · simp only [h, ↓reduceIte]
    ring

/-- **Neither stream is negative.** (So each end sends only its own draw.) -/
theorem streams_nonneg {g S : ℝ} (hg : 0 ≤ g) (hS : 0 < S) (dp : ℝ) :
    0 ≤ (streams g S dp).1 ∧ 0 ≤ (streams g S dp).2 := by
  have hdp : dp = S * (dp / S) := by field_simp
  unfold streams
  by_cases h : 30 < dp / S
  · simp only [h, ↓reduceIte]
    have : 0 ≤ dp := by
      rw [hdp]
      exact mul_nonneg hS.le (by linarith)
    exact ⟨mul_nonneg hg this, le_refl 0⟩
  · simp only [h, ↓reduceIte]
    refine ⟨mul_nonneg (mul_nonneg hg hS.le) (spCode_pos _).le, ?_⟩
    have h1 : g * S * spCode (dp / S) - g * dp = g * S * (spCode (dp / S) - dp / S) := by
      conv_lhs => rw [hdp]
      have : S * (dp / S) / S = dp / S := by field_simp
      rw [this]
      ring
    rw [h1]
    exact mul_nonneg (mul_nonneg hg hS.le) (by linarith [spCode_gt (dp / S)])

/-- At rest each stream is g S ln 2: equal, so where both ends draw the same nothing moves. -/
theorem streams_rest (g S : ℝ) : streams g S 0 = (g * S * Real.log 2, g * S * Real.log 2) := by
  unfold streams spCode softplus
  simp only [zero_div, Real.exp_zero, mul_zero, sub_zero]
  norm_num

/-- **The cuts cost at most g S e^-30** in either stream (1e-13 of g S; both streams move by the same, the net flow
by nothing). -/
theorem streams_close {g S : ℝ} (hg : 0 ≤ g) (hS : 0 < S) (dp : ℝ) :
    |(streams g S dp).1 - (smooth g S dp).1| ≤ g * S * Real.exp (-30) ∧
    (streams g S dp).2 - (smooth g S dp).2 = (streams g S dp).1 - (smooth g S dp).1 := by
  have hgS : 0 ≤ g * S := mul_nonneg hg hS.le
  have hdp : dp = S * (dp / S) := by field_simp
  refine ⟨?_, ?_⟩
  · unfold streams smooth
    by_cases h : 30 < dp / S
    · simp only [h, ↓reduceIte]
      have h1 : g * dp - g * S * softplus (dp / S) = -(g * S * softplus (-(dp / S))) := by
        rw [softplus_neg]
        conv_lhs => rw [hdp]
        have : S * (dp / S) / S = dp / S := by field_simp
        rw [this]
        ring
      rw [h1, abs_neg, abs_of_nonneg (mul_nonneg hgS (softplus_pos _).le)]
      refine mul_le_mul_of_nonneg_left ?_ hgS
      exact (softplus_le_exp _).trans (Real.exp_le_exp.2 (by linarith))
    · simp only [h, ↓reduceIte]
      unfold spCode
      by_cases h2 : dp / S < -30
      · simp only [h2, ↓reduceIte]
        rw [← mul_sub, abs_of_nonneg (mul_nonneg hgS (by linarith [softplus_le_exp (dp / S)]))]
        refine mul_le_mul_of_nonneg_left ?_ hgS
        have := softplus_pos (dp / S)
        have := Real.exp_le_exp.2 h2.le
        linarith
      · simp only [h2, ↓reduceIte, sub_self, abs_zero]
        exact mul_nonneg hgS (Real.exp_pos _).le
  · have h1 := streams_net g S dp
    have h2 : (smooth g S dp).1 - (smooth g S dp).2 = g * dp := by
      unfold smooth
      ring
    linarith

/-- The smooth streams mirror exactly: turned round, an edge's q+ is its q-. -/
theorem smooth_mirror {S : ℝ} (hS : S ≠ 0) (g dp : ℝ) : (smooth g S (-dp)).1 = (smooth g S dp).2 := by
  unfold smooth
  have h : -dp / S = -(dp / S) := neg_div S dp
  rw [show (g * S * softplus (-dp / S), g * S * softplus (-dp / S) - g * -dp).1 = g * S * softplus (-dp / S) from rfl,
    show (g * S * softplus (dp / S), g * S * softplus (dp / S) - g * dp).2 = g * S * softplus (dp / S) - g * dp from rfl,
    h, softplus_neg]
  have : g * S * (dp / S) = g * dp := by field_simp
  linarith [this, mul_sub (g * S) (softplus (dp / S)) (dp / S)]

/-- **The coded streams mirror to within 2 g S e^-30**, and exactly within 30 bands of rest. -/
theorem streams_mirror {g S : ℝ} (hg : 0 ≤ g) (hS : 0 < S) (dp : ℝ) :
    |(streams g S (-dp)).1 - (streams g S dp).2| ≤ 2 * (g * S * Real.exp (-30)) := by
  have a := (streams_close hg hS (-dp)).1
  have b := streams_close hg hS dp
  have m := smooth_mirror hS.ne' g dp
  rw [← b.2] at b
  have e : (streams g S (-dp)).1 - (streams g S dp).2 =
      ((streams g S (-dp)).1 - (smooth g S (-dp)).1) - ((streams g S dp).2 - (smooth g S dp).2) := by
    rw [m]
    ring
  rw [e]
  have b1 := b.1
  calc |(streams g S (-dp)).1 - (smooth g S (-dp)).1 - ((streams g S dp).2 - (smooth g S dp).2)|
      ≤ |(streams g S (-dp)).1 - (smooth g S (-dp)).1| + |(streams g S dp).2 - (smooth g S dp).2| := abs_sub _ _
    _ ≤ 2 * (g * S * Real.exp (-30)) := by linarith

theorem streams_mirror_band {g S : ℝ} (hS : 0 < S) {dp : ℝ} (hlo : -30 ≤ dp / S) (hhi : dp / S ≤ 30) :
    (streams g S (-dp)).1 = (streams g S dp).2 := by
  have h : -dp / S = -(dp / S) := neg_div S dp
  have e1 : streams g S (-dp) = smooth g S (-dp) := by
    unfold streams smooth spCode
    have n1 : ¬ 30 < -dp / S := by rw [h]; linarith
    have n2 : ¬ -dp / S < -30 := by rw [h]; linarith
    simp only [n1, n2, ↓reduceIte]
  have e2 : streams g S dp = smooth g S dp := by
    unfold streams smooth spCode
    have n1 : ¬ 30 < dp / S := by linarith
    have n2 : ¬ dp / S < -30 := by linarith
    simp only [n1, n2, ↓reduceIte]
  rw [e1, e2]
  exact smooth_mirror hS.ne' g dp

/-- `Network.streams` for a check valve: a pipe's forward stream alone, and nothing back. Against the valve it passes
the stream's tail, about g S e^(dp / S), at rest g S ln 2. (A hard max(0, dp) put a kink where a valve sits balanced,
and Newton stalled on it in the game's networks.) -/
noncomputable def checkStreams (g S dp : ℝ) : ℝ × ℝ := ((streams g S dp).1, 0)

theorem checkStreams_spec {g S : ℝ} (hg : 0 ≤ g) (hS : 0 < S) (dp : ℝ) :
    g * max 0 dp ≤ (checkStreams g S dp).1 ∧ 0 ≤ (checkStreams g S dp).1 ∧ (checkStreams g S dp).2 = 0 := by
  have hnn : 0 ≤ (streams g S dp).1 := (streams_nonneg hg hS dp).1
  have hge : g * dp ≤ (streams g S dp).1 := by
    unfold streams
    by_cases h : 30 < dp / S
    · simp only [h, ↓reduceIte, le_refl]
    · simp only [h, ↓reduceIte]
      have h1 : dp / S < spCode (dp / S) := spCode_gt _
      have h2 : dp < S * spCode (dp / S) := by
        have := mul_lt_mul_of_pos_left h1 hS
        rwa [mul_div_cancel₀ dp hS.ne'] at this
      nlinarith
  refine ⟨?_, hnn, rfl⟩
  unfold checkStreams
  rcases le_total 0 dp with h | h
  · rw [max_eq_right h]; exact hge
  · rw [max_eq_left h, mul_zero]; exact hnn

/-! ### What the streams carry -/

variable {ι : Type} [Fintype ι]

/-- The flow of component k from a to b: q+ of a's draw less q- of b's (`Network.flux`). -/
def carried (qp qm : ℝ) (ca cb : ι → ℝ) (k : ι) : ℝ := qp * ca k - qm * cb k

/-- **Where both ends draw the same, the edge carries the net flow of it**: nothing at rest, and no mixing by the
return stream. -/
theorem carried_same (qp qm : ℝ) (c : ι → ℝ) (k : ι) : carried qp qm c c k = (qp - qm) * c k := by
  unfold carried
  ring

/-- **An end sends only what it draws**: no flow of k from a if a's draw has none. -/
theorem carried_has {qp qm : ℝ} {ca cb : ι → ℝ} {k : ι} (hq : 0 ≤ qm) (hb : 0 ≤ cb k) (ha : ca k = 0) :
    carried qp qm ca cb k ≤ 0 := by
  unfold carried
  rw [ha, mul_zero, zero_sub]
  exact neg_nonpos.2 (mul_nonneg hq hb)

/-- The mass an edge moves: each stream times the sum of the shares it draws. -/
theorem carried_total (qp qm : ℝ) (ca cb : ι → ℝ) :
    ∑ k, carried qp qm ca cb k = qp * ∑ k, ca k - qm * ∑ k, cb k := by
  unfold carried
  rw [sum_sub_distrib, mul_sum, mul_sum]

/-- **The mass moved is the net flow less what the shares leave out.** A gas's shares leave out the air cushion (it
is never drawn), so an edge drawing gas moves a little less than the flow `Edge.flow` reports. -/
theorem carried_total_gap {qp qm : ℝ} {ca cb : ι → ℝ} (hp : 0 ≤ qp) (hm : 0 ≤ qm)
    (ha : ∑ k, ca k ≤ 1) (hb : ∑ k, cb k ≤ 1) :
    |∑ k, carried qp qm ca cb k - (qp - qm)| ≤ qp * (1 - ∑ k, ca k) + qm * (1 - ∑ k, cb k) := by
  rw [carried_total]
  have h1 : 0 ≤ qp * (1 - ∑ k, ca k) := mul_nonneg hp (by linarith)
  have h2 : 0 ≤ qm * (1 - ∑ k, cb k) := mul_nonneg hm (by linarith)
  rw [abs_le]
  constructor <;> nlinarith

theorem carried_total_whole (qp qm : ℝ) {ca cb : ι → ℝ} (ha : ∑ k, ca k = 1) (hb : ∑ k, cb k = 1) :
    ∑ k, carried qp qm ca cb k = qp - qm := by
  rw [carried_total, ha, hb, mul_one, mul_one]

/-! ### Conduction -/

/-- How much of an edge's conductance a node's contents carry: m / (m + c), c the contents at which it is half. -/
noncomputable def wetted (m c : ℝ) : ℝ := m / (m + c)

theorem wetted_bounds {m c : ℝ} (hm : 0 ≤ m) (hc : 0 < c) : 0 ≤ wetted m c ∧ wetted m c < 1 := by
  unfold wetted
  have h : 0 < m + c := by linarith
  exact ⟨div_nonneg hm h.le, (div_lt_one h).2 (by linarith)⟩

/-- Heat from a to b (`Network.conduction`). -/
def conduction (G wa wb Ta Tb : ℝ) : ℝ := G * wa * wb * (Ta - Tb)

/-- **Conducted heat never lowers entropy**: Q (1/Tb - 1/Ta) >= 0, so heat goes from hot to cold. -/
theorem conduction_entropy {G wa wb Ta Tb : ℝ} (hG : 0 ≤ G) (ha : 0 ≤ wa) (hb : 0 ≤ wb) (hTa : 0 < Ta)
    (hTb : 0 < Tb) : 0 ≤ conduction G wa wb Ta Tb * (1 / Tb - 1 / Ta) := by
  have h : conduction G wa wb Ta Tb * (1 / Tb - 1 / Ta) = G * wa * wb * ((Ta - Tb) ^ 2 / (Ta * Tb)) := by
    unfold conduction
    field_simp
  rw [h]
  exact mul_nonneg (mul_nonneg (mul_nonneg hG ha) hb) (div_nonneg (sq_nonneg _) (mul_pos hTa hTb).le)

theorem conduction_rest (G wa wb T : ℝ) : conduction G wa wb T T = 0 := by
  unfold conduction
  ring

/-! ### The exchange through an opening: liquid down, gas up -/

/-- The exchanged volume flow (`Network.exchangeFlux`): none unless the liquid above is the denser. -/
def exchangeQ (X rhoL rhoG grav wl wg : ℝ) : ℝ := X * max 0 (rhoL - rhoG) * grav * wl * wg

theorem exchangeQ_nonneg {X rhoL rhoG grav wl wg : ℝ} (hX : 0 ≤ X) (hgr : 0 ≤ grav) (hl : 0 ≤ wl)
    (hg : 0 ≤ wg) : 0 ≤ exchangeQ X rhoL rhoG grav wl wg := by
  unfold exchangeQ
  exact mul_nonneg (mul_nonneg (mul_nonneg (mul_nonneg hX (le_max_left _ _)) hgr) hl) hg

theorem exchangeQ_pos {X rhoL rhoG grav wl wg : ℝ} (h : exchangeQ X rhoL rhoG grav wl wg ≠ 0) :
    rhoG < rhoL := by
  by_contra hn
  apply h
  unfold exchangeQ
  rw [max_eq_left (by linarith)]
  ring

/-- The flow of component k downwards: Q rhoL of the liquid's shares less Q rhoG of the gas's. -/
def exchanged (Q rhoL rhoG : ℝ) (liq gas : ι → ℝ) (k : ι) : ℝ := Q * rhoL * liq k - Q * rhoG * gas k

theorem exchanged_total (Q rhoL rhoG : ℝ) (liq gas : ι → ℝ) :
    ∑ k, exchanged Q rhoL rhoG liq gas k = Q * rhoL * ∑ k, liq k - Q * rhoG * ∑ k, gas k := by
  unfold exchanged
  rw [sum_sub_distrib, mul_sum, mul_sum]

/-- **The exchange never lifts mass**: with the liquid the denser, the net mass goes down. -/
theorem exchanged_down {Q rhoL rhoG : ℝ} {liq gas : ι → ℝ} (hQ : 0 ≤ Q) (hr : rhoG ≤ rhoL) (hrg : 0 ≤ rhoG)
    (hl : ∑ k, liq k = 1) (hg : ∑ k, gas k ≤ 1) : 0 ≤ ∑ k, exchanged Q rhoL rhoG liq gas k := by
  rw [exchanged_total, hl, mul_one]
  have h1 : Q * rhoG * ∑ k, gas k ≤ Q * rhoG * 1 := mul_le_mul_of_nonneg_left hg (mul_nonneg hQ hrg)
  have h2 : Q * rhoG ≤ Q * rhoL := mul_le_mul_of_nonneg_left hr hQ
  linarith

variable {κ : Type} [Fintype κ]

/-- The density of a mix of layers by shares of its mass w: the volumes add (`MixFluid.rhoLiquid`). -/
noncomputable def rhoMix (w rho : κ → ℝ) : ℝ := 1 / ∑ j, w j / rho j

/-- **The liquid sent down fills the volume Q**, also where the opening sees several layers: layer j's part is
Q rhoMix w_j kg, of volume Q rhoMix w_j / rho_j. -/
theorem exchanged_liquid_volume {w rho : κ → ℝ} (hw : ∀ j, 0 ≤ w j) (hsum : ∑ j, w j = 1)
    (hrho : ∀ j, 0 < rho j) (Q : ℝ) : ∑ j, Q * rhoMix w rho * w j / rho j = Q := by
  have hpos : 0 < ∑ j, w j / rho j := by
    apply sum_pos'
    · intro j _
      exact div_nonneg (hw j) (hrho j).le
    · by_contra hn
      have hz : ∀ j ∈ (univ : Finset κ), w j = 0 := by
        intro j hj
        by_contra hne
        exact hn ⟨j, hj, div_pos (lt_of_le_of_ne (hw j) (Ne.symm hne)) (hrho j)⟩
      rw [sum_eq_zero hz] at hsum
      exact zero_ne_one hsum
  have e : ∀ j, Q * rhoMix w rho * w j / rho j = Q * rhoMix w rho * (w j / rho j) := fun j => by ring
  simp only [e]
  rw [← mul_sum]
  unfold rhoMix
  field_simp

/-- **The gas brought up is the share Q / Vg of the node's real gas**: the cushion c stays, as its share of the
draw is left out. -/
theorem exchanged_gas {mg c Vg : ℝ} (h : mg + c ≠ 0) (Q : ℝ) :
    Q * ((mg + c) / Vg) * (mg / (mg + c)) = Q / Vg * mg := by
  field_simp

/-! ### Windows: what a port draws -/

/-- t^2 (3 - 2 t): 0 at 0, 1 at 1, flat at both. -/
def poly (t : ℝ) : ℝ := t * t * (3 - 2 * t)

/-- `Network.smoothStep`: 0 up to -1, 1 from 1. -/
noncomputable def smoothStep (x : ℝ) : ℝ :=
  if x ≤ -1 then 0 else if 1 ≤ x then 1 else poly (0.5 * (x + 1))

theorem poly_mono {s t : ℝ} (hs : 0 ≤ s) (hst : s ≤ t) (ht : t ≤ 1) : poly s ≤ poly t := by
  unfold poly
  have h1 : 0 ≤ (t - s) := by linarith
  have h2 : 0 ≤ s * (1 - s) := mul_nonneg hs (by linarith)
  have h3 : 0 ≤ t * (1 - t) := mul_nonneg (by linarith) (by linarith)
  have h4 : 0 ≤ s * (1 - t) := mul_nonneg hs (by linarith)
  have h5 : 0 ≤ t * (1 - s) := mul_nonneg (by linarith) (by linarith)
  have key : t * t * (3 - 2 * t) - s * s * (3 - 2 * s) =
      (t - s) * (2 * (s * (1 - s)) + 2 * (t * (1 - t)) + s * (1 - t) + t * (1 - s)) := by ring
  have : 0 ≤ (t - s) * (2 * (s * (1 - s)) + 2 * (t * (1 - t)) + s * (1 - t) + t * (1 - s)) :=
    mul_nonneg h1 (by linarith)
  linarith

/-- The step is the polynomial of its argument held to [0, 1]. -/
theorem smoothStep_eq (x : ℝ) : smoothStep x = poly (max 0 (min 1 (0.5 * (x + 1)))) := by
  unfold smoothStep
  by_cases h1 : x ≤ -1
  · simp only [h1, ↓reduceIte]
    rw [min_eq_right (by linarith), max_eq_left (by linarith)]
    unfold poly
    ring
  · by_cases h2 : 1 ≤ x
    · simp only [h1, h2, ↓reduceIte]
      rw [min_eq_left (by linarith), max_eq_right (by norm_num)]
      unfold poly
      ring
    · simp only [h1, h2, ↓reduceIte]
      rw [min_eq_right (by linarith), max_eq_right (by linarith)]

/-- **A window is a share**: between 0 and 1. -/
theorem smoothStep_bounds (x : ℝ) : 0 ≤ smoothStep x ∧ smoothStep x ≤ 1 := by
  rw [smoothStep_eq]
  have h0 : (0 : ℝ) ≤ max 0 (min 1 (0.5 * (x + 1))) := le_max_left _ _
  have h1 : max 0 (min 1 (0.5 * (x + 1))) ≤ (1 : ℝ) := max_le (by norm_num) (min_le_left _ _)
  have a := poly_mono (le_refl 0) h0 h1
  have b := poly_mono h0 h1 (le_refl 1)
  unfold poly at a b ⊢
  constructor <;> linarith

/-- **A higher surface never draws less liquid.** -/
theorem smoothStep_mono : Monotone smoothStep := by
  intro x y hxy
  rw [smoothStep_eq, smoothStep_eq]
  have h0 : (0 : ℝ) ≤ max 0 (min 1 (0.5 * (x + 1))) := le_max_left _ _
  have h1 : max 0 (min 1 (0.5 * (y + 1))) ≤ (1 : ℝ) := max_le (by norm_num) (min_le_left _ _)
  exact poly_mono h0 (max_le_max (le_refl 0) (min_le_min (le_refl 1) (by linarith))) h1

/-- The window of a port at f under a layer whose top is at `top`, with the band B. -/
noncomputable def window (B f top : ℝ) : ℝ := smoothStep ((top - f) * (2 / B) - 1)

theorem window_mono {B : ℝ} (hB : 0 < B) (f : ℝ) {s t : ℝ} (h : s ≤ t) : window B f s ≤ window B f t := by
  unfold window
  apply smoothStep_mono
  have : 0 ≤ 2 / B := (div_pos (by norm_num) hB).le
  nlinarith

/-- What a port draws of layer k (`MixFluid.drawD`): its window less the window of the layer below; X k is the window
under the top of the k layers from the bottom (X 0 = 0: under the floor, nothing). The gas has the rest. -/
def layerShare (X : ℕ → ℝ) (k : ℕ) : ℝ := X (k + 1) - X k

theorem layerShare_nonneg {X : ℕ → ℝ} (hX : Monotone X) (k : ℕ) : 0 ≤ layerShare X k :=
  sub_nonneg.2 (hX (Nat.le_succ k))

/-- **The layers' shares and the gas's make one whole.** -/
theorem layerShare_total (X : ℕ → ℝ) (h0 : X 0 = 0) (L : ℕ) :
    ∑ k ∈ range L, layerShare X k + (1 - X L) = 1 := by
  unfold layerShare
  rw [sum_range_sub (f := X), h0]
  ring

/-- **A draw's shares sum to 1 less the gas part's cushion**: with each layer's shares summing to 1 and the gas's
to sG, the whole is 1 - (1 - X L) (1 - sG). Never over 1; exactly 1 under liquid. -/
theorem draw_total (X : ℕ → ℝ) (h0 : X 0 = 0) (L : ℕ) (sG : ℝ) :
    ∑ k ∈ range L, layerShare X k * 1 + (1 - X L) * sG = 1 - (1 - X L) * (1 - sG) := by
  have h := layerShare_total X h0 L
  simp only [mul_one]
  linarith

/-! ### Rest -/

/-- The backward Euler residual of a step from Y0 to Y: Y - Y0 - dt F(Y) (`Network.residual`). -/
def residual (F : (ι → ℝ) → ι → ℝ) (Y0 Y : ι → ℝ) (dt : ℝ) (i : ι) : ℝ := Y i - Y0 i - dt * F Y i

/-- **A state at rest stays put, whatever the step's length**: Y0 solves the step from Y0 exactly when every
net flux is zero there. -/
theorem rest_iff (F : (ι → ℝ) → ι → ℝ) (Y0 : ι → ℝ) {dt : ℝ} (hdt : dt ≠ 0) :
    (∀ i, residual F Y0 Y0 dt i = 0) ↔ ∀ i, F Y0 i = 0 := by
  unfold residual
  constructor
  · intro h i
    have := h i
    rw [sub_self, zero_sub, neg_eq_zero] at this
    exact (mul_eq_zero.1 this).resolve_left hdt
  · intro h i
    rw [h i]
    ring

end FluidSpec.Flux
