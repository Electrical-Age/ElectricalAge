import FluidSpec.Limiter.Proofs
import FluidSpec.Step
import FluidSpec.Flux
import FluidSpec.Split
/-
The axioms each headline theorem rests on (check.sh fails on anything but Lean's three standard ones, which is how an
unfinished proof would show).
-/
open FluidSpec.Limiter

#print axioms conservation
#print axioms apply_conserves
#print axioms apply_balance
#print axioms scales_spec
#print axioms apply_scales
#print axioms apply_mass_bound
#print axioms apply_mass_nonneg
#print axioms apply_unlimited
#print axioms applyAt_flows
#print axioms effOf_bounds
#print axioms energy_held
#print axioms energy_full
#print axioms raise_fixed
#print axioms FluidSpec.Step.run_conserves
#print axioms FluidSpec.Step.run_mass_nonneg
#print axioms FluidSpec.Step.run_mass_bound
#print axioms FluidSpec.Step.predict_mass
#print axioms FluidSpec.Step.trial_mass
#print axioms FluidSpec.Flux.streams_net
#print axioms FluidSpec.Flux.streams_nonneg
#print axioms FluidSpec.Flux.streams_rest
#print axioms FluidSpec.Flux.streams_close
#print axioms FluidSpec.Flux.streams_mirror
#print axioms FluidSpec.Flux.streams_mirror_band
#print axioms FluidSpec.Flux.checkStreams_spec
#print axioms FluidSpec.Flux.carried_same
#print axioms FluidSpec.Flux.carried_has
#print axioms FluidSpec.Flux.carried_total_gap
#print axioms FluidSpec.Flux.conduction_entropy
#print axioms FluidSpec.Flux.wetted_bounds
#print axioms FluidSpec.Flux.exchangeQ_nonneg
#print axioms FluidSpec.Flux.exchanged_down
#print axioms FluidSpec.Flux.exchanged_liquid_volume
#print axioms FluidSpec.Flux.exchanged_gas
#print axioms FluidSpec.Flux.smoothStep_bounds
#print axioms FluidSpec.Flux.smoothStep_mono
#print axioms FluidSpec.Flux.window_mono
#print axioms FluidSpec.Flux.layerShare_nonneg
#print axioms FluidSpec.Flux.draw_total
#print axioms FluidSpec.Flux.rest_iff
#print axioms FluidSpec.Split.fracSum_strictAnti
#print axioms FluidSpec.Split.root_unique
#print axioms FluidSpec.Split.root_exists
#print axioms FluidSpec.Split.saturated_iff
#print axioms FluidSpec.Split.single_root
#print axioms FluidSpec.Split.softSum_exact
#print axioms FluidSpec.Split.single_soft
#print axioms FluidSpec.Split.soft_unique
#print axioms FluidSpec.Split.soft_exists
#print axioms FluidSpec.Split.soft_terms
#print axioms FluidSpec.Split.soft_holds
#print axioms FluidSpec.Split.soft_covers
#print axioms FluidSpec.Split.soft_le_total
#print axioms FluidSpec.Split.soft_gap
#print axioms FluidSpec.Split.soft_smooth
#print axioms FluidSpec.Split.soft_strictMono
#print axioms FluidSpec.Split.soft_continuous
#print axioms FluidSpec.Split.soft_low
#print axioms FluidSpec.Split.softCode_exact
#print axioms FluidSpec.Split.softCode_smooth
#print axioms FluidSpec.Split.lam_unique
#print axioms FluidSpec.Split.lam_exists
#print axioms FluidSpec.Split.lam_pos
#print axioms FluidSpec.Split.liquid_total
#print axioms FluidSpec.Split.liquid_bounds
#print axioms FluidSpec.Split.split_exists
