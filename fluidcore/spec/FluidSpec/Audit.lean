import FluidSpec.Limiter.Proofs
import FluidSpec.Step
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
