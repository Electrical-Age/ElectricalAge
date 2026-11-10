# The fluid core's spec, machine-checked

What the fluid core guarantees, stated and proven in [Lean 4](https://lean-lang.org) with Mathlib. The solver's
safety rests on one design choice: every step ends in `Limiter.apply`, which moves amounts between nodes whatever
Newton reached. So the guarantees are theorems about that function and about how a step calls it, and they hold
whether or not Newton converged.

The Lean limiter (`FluidSpec/Limiter/Impl.lean`) is one definition used twice:

- **proven** over any ordered field (the rationals, the reals), and
- **run** on doubles against calls recorded from the Java tests, where its scales and results must equal Java's bit
  for bit. That is what ties the proofs to the Java.

## Checking it

`fluidcore/spec/check.sh` (needs `lake` from elan, and the JDKs the Gradle build needs; a few minutes once Mathlib's
prebuilt files are fetched, which it does the first time). It:

1. records a sample of `Limiter.apply` calls from `LimiterTest` and the random-network tests (with rings; once with
   16 scaling passes, once with 2 so that silencing and raising are reached often);
2. builds the proofs and the worked examples, and fails if any theorem rests on anything but Lean's three standard
   axioms (an unfinished proof would show there as `sorryAx`);
3. runs the Lean limiter on the recorded calls and compares bits;
4. runs it again with one division computed as a multiplication by the inverse, which must be noticed.

Last run: 1258 calls (153 unlimited, 759 scaled, 25 scaled until the last allowed pass, 321 with nodes silenced and
scales raised again), no difference; the changed arithmetic differed on 237. One branch is in no recorded call: the
limiter's last resort, all scales to zero when silencing has run as many passes as there are cells. Java's comment
says it is never reached; the proofs cover it all the same.

## What is proven

Cells are (node, component); a node's last component is its energy, the others are masses. A *move* takes an amount
from one cell to another, or to or from outside (a fixed node, a heater). `Limiter.apply` scales each cell's sends
of a mass by a factor from 0 to 1 and applies the moves.

| | Statement | Lean | Java |
|---|---|---|---|
| T1 | **Conservation.** For any set of cells, the total after = the total before + what moves brought in from outside the set - what they took out. Whatever the scales; energy included. If every move has both ends in the set or neither, the total is unchanged. | `conservation`, `apply_balance`, `apply_conserves` | `Limiter.apply` |
| T2 | **Positivity.** Every mass ends no lower than min(where it started, -(SLACK - 1) x what the cell had available). Without slack: a mass that starts at 0 or more ends at 0 or more. | `apply_mass_bound`, `apply_mass_nonneg` | `Limiter.apply` |
| | The scales used pass the check (no cell sends more than SLACK x what it has), and are between 0 and 1, whichever way they were found: scaling down, silencing, raising, any pass counts. T2 follows from the check alone. | `scales_spec` (`settle_spec`, `raise_spec`) | `Limiter.scales` |
| | Raising stops early in Java when a pass changes nothing; going on would change nothing. | `raise_fixed` | the `break` in `scales` |
| T3 | **No interference.** If no cell over-sends at full scale, every scale is 1, no pass runs and every move is applied in full. | `apply_unlimited` | `Limiter.apply` |
| T4 | **Flux form.** Applied in full, the moves that `Network.move` makes of signed flows change each cell by exactly the sum of the flows into it minus those out of it: the step ends at Y0 + dt F(Y_iter). | `applyAt_flows` | `Network.finish`, `Network.move` |
| T5 | **Energy follows mass.** A limited move is never reversed or enlarged (0 <= moved <= asked), energy included. The energy an edge's flow carries is not moved at all if none of that edge's masses are, and is moved in full if all of them are. | `effOf_bounds`, `energy_held`, `energy_full` | `Limiter.apply` |
| S1 | **A whole step conserves.** A step is a sequence of `apply` calls, each from where the last ended (the whole step, or halves, quarters, ... after a failed solve). T1 holds across all of them. | `Step.run_conserves` | `Network.step`, `substep` |
| S2 | **A whole step keeps masses positive.** As T2, and the slack does not add up over the calls: the bound is -(SLACK - 1) x the most the cell had available in any one call. | `Step.run_mass_nonneg`, `Step.run_mass_bound` | `Network.step`, `substep` |
| S3 | **The predicted state** has no mass lower than at the step's start, or than 0. | `Step.predict_mass` | `Network.predict` |
| S4 | **Every state Newton tries** has no mass lower than the state it stepped from, or than 0 (at any length the line search can pick). With S3: within one solve, the fluid's equations of state are never asked about a mass below min(its value when the solve started, 0). A solve starts from the step's start or from an earlier call's result, so with S2 that is never below min(the step's start, -(SLACK - 1) x what the cell had available in a call). | `Step.trial_mass` | the "fraction to the boundary with a floor" loop in `Network.newton` |

`FluidSpec/Limiter/Examples.lean` works small cases in exact fractions (checked at build time): an over-sending
transit node, an empty ring, a ring one node feeds, and the energy following its mass.

## What the theorems assume

- Of the moves (`Valid`): amounts are 0 or more, components are below the count per node, and sending cells are
  within the cells given. `Network.move` makes only such moves; `LimiterTap` checks it on every call in the tests.
- SLACK is 1 or more (it is 1 + 1e-15).
- T1 and T4 assume nothing.

## Exact numbers and doubles

The proofs are about exact arithmetic. In doubles:

- T1: a move subtracts and adds the same double, so what is lost is the rounding of each cell's running sum: about
  1e-16 of the cell's throughput per step. The Java tests check each run's totals at the end against the start.
- T2: a cell that sends all it has ends within rounding of zero, on either side: about 1e-16 of what passed through
  it. The scaling and silencing branches end on a check computed in doubles; the raising branch does not re-check
  (in exact arithmetic it cannot break the check; in doubles it can by rounding).
- S3 and S4 have margins of 10 % and 1 % of the mass, far above rounding.
- The comparison ran on x86-64, where Java and compiled Lean both do plain IEEE double operations in the same order.

## Not proven

- That Newton converges, or how fast. The tests are the evidence.
- That entropy never falls over a step.
- Anything about the Java outside `Limiter` except through the recorded calls. S1 to S4 are about the step as
  transcribed here, and these links to the Java are by reading, not cross-checked:
  - S1 and S2: each call starts from exactly what the last kept call left. (`Network.set` then `pack` returns the
    same doubles; a failed solve is undone by `restore` with the array it started from.)
  - S3 and S4: the formulas of `predict` and of `newton`'s damping loop.
  - T4: `finish` builds its moves with `move` from `flux`, the same fluxes the residual uses.

## Files

| | |
|---|---|
| `FluidSpec/Limiter/Impl.lean` | The limiter, generic in the number type. Lean core only. |
| `FluidSpec/Limiter/Check.lean` | `limitercheck`: runs it on doubles against the recorded calls. |
| `FluidSpec/Limiter/Proofs.lean` | T1 to T5. |
| `FluidSpec/Limiter/Examples.lean` | Worked cases in fractions. |
| `FluidSpec/Step.lean` | S1 to S4. |
| `FluidSpec/Audit.lean` | The axioms each theorem rests on, for `check.sh`. |
