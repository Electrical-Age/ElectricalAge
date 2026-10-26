# Simulation

Scope: the server-side simulator (electrical MNA, thermal network, process lists, failures), how devices plug into it,
its invariants and the tests that guard them. Everything here is **server** only; clients see published values.
Fluids, logic-chip equations and voxel cables get pointers only.

## Driver and timesteps

- `src/main/java/mods/eln/Eln.java`: `simulator = new Simulator(0.05, 1 / electricalFrequency, electricalInterSystemOverSampling, 1 / thermalFrequency)`.
  Defaults (static fields, no 26.x config yet): electrical 20 Hz (dt 0.05 s), inter-system oversampling 50, thermal 400 Hz (dt 1/400 s).
- `ServerAboutToStartEvent` -> `simulator.init()` (fresh `RootSystem`, lists cleared), then adds `WindProcess` and
  `FluidWorld.reset()` as slow processes. `ServerStoppedEvent` -> `simulator.stop()`.
- `ServerTickEvent.Pre` -> `simulator.tick()` (1.12 ran it in phase START). Registered before `NodeServer`'s Pre
  listener (`src/main/java/mods/eln/node/NodeServer.java`), so a tick simulates first, then publishes nodes.
- `Simulator.probe` (`TickProbe`) and `RootSystem.stageNs` are timing hooks for the gametest `CableBench`.

## One tick (`src/main/java/mods/eln/sim/Simulator.java`, `tick()`)

1. `slowPreProcessList`, dt 1/20.
2. Interleaved loop over `callPeriod` (0.05 s), scheduled by `electricalTimeout`/`thermalTimeout`:
   - electrical step: `mna.step()` then every `electricalProcessList` entry with `electricalPeriod`;
   - fast thermal step: `thermalStep(thermalPeriod, fast connections, thermalFastProcessList, fast loads)`.
   At defaults: 1 electrical step and 20 fast thermal steps per tick.
3. One slow thermal step, `thermalStep(0.05, ...slow lists)`.
4. `slowProcessList`, dt 0.05 (iterates a copy: processes may add/remove themselves).
5. Queued failures (see Failures). Only here may the simulation be changed safely.

Guard: `SimulatorTimingTest.simulatedTimeTracksGameTime`, `SimulatorTimingTest.stepsPerTickAreRegular`
(`src/test/java/mods/eln/sim/`). Gametest `SimTickCase` checks the sim on the real server tick.

## How devices join the simulation

| Element list (`SixNodeElement` / `TransparentNodeElement`) | Simulator call |
|---|---|
| `electricalLoadList` | `addElectricalLoad` -> `RootSystem.addState` |
| `electricalComponentList` | `addElectricalComponent` -> `RootSystem.addComponent` |
| `electricalProcessList` | `addElectricalProcess` (every electrical step) |
| `thermalLoadList` | `addThermalLoad` (fast or slow list by `ThermalLoad.isSlow()`) |
| `thermalConnectionList` | `addThermalConnection` |
| `thermalProcessList` (six) / `thermalFastProcessList` (transparent) | `addThermalFastProcess` |
| `thermalSlowProcessList` (six only; transparent has none) | `addThermalSlowProcess` |
| `slowProcessList` | `addSlowProcess` |

- `NodeBase.connect()` -> `connectJob()` -> element `connectJob()`; `disconnect()` -> `disconnectJob()` removes the same.
  Element `connectJob` is a no-op while the node `isDestructing()`; `TransparentNode` skips a burnt element.
- Links between neighbouring nodes: `NodeBase` (`tryConnectTwoNode`) creates an `ElectricalConnection` (an
  `InterSystem` resistor, R = sum of both loads' Rs) and a `ThermalConnection`, recorded in a `NodeConnection`.
- Voxel cables register per conductor in `src/main/java/mods/eln/node/voxel/CableNet.java` (`addToSimulation`):
  line-ready loads, slow thermal loads. See voxel-cables.md.
- `NodeBase.isAdded()`: node is in the simulation. `NodeBase.simulationVeto` (static, null in this repo; set by an
  external freezer such as Stasis Delvers): if it returns true, `connect()` only does `visualConnect()` and the node
  stays out. `disconnect()` of a non-added node still drops neighbours' links (`dropConnections`).
- `NodeBase.timeScale` (26.x, static, null in this repo; set by Stasis Delvers for its acceleration fields): how
  many times faster time runs at a node. `ElectricalMachineElement` checks it once a second and multiplies its speed-up
  by it (as boosters do: the resistor drops, so the machine draws that much more power and finishes that much sooner,
  for the same energy per item). Test: `wp10a_macerator_in_a_3x_acceleration_field`.

## MNA (`src/main/java/mods/eln/sim/mna/`)

- `RootSystem.step()`: `generate()` (only if `addComponents`/`addStates` non-empty: `generateLine` -> `generateSystems`
  -> `generateInterSystems`) -> `interSystemOverSampling` x every `IRootSystemPreStepProcess` -> `SubSystem.stepCalc`
  for all -> `stepFlush` for all -> root `ISubSystemProcessFlush` (Line inner voltages).
- Any add/remove calls `breakSystems` on the touched `SubSystem` (and its inter-system neighbours): its parts go back to
  the root's pending sets and are rebuilt lazily on the next step.
- `SubSystem`: one connected component, dense matrix, inverse by `QRDecomposition`, rebuilt when `matrixValid` is
  false (`matrixVersion` counts rebuilds). Singular matrix -> all states 0. `addToI` superposes (1.7.10 assigned).
  Diodes (`ResistorSwitch.isDiode`): up to `DIODE_PASSES = 3` re-solves per step on polarity change.
- Components (`mna/component/`): `Resistor`, `ResistorSwitch`, `VoltageSource` (adds a `CurrentState`), `Capacitor`,
  `Inductor` (backward Euler), `Delay`, `PowerSource`, `Transformer` (ideal, one SubSystem), `Monopole` (abstract,
  one pin), `InterSystem` (a `Resistor` that may be split), `Line`, `DelayInterSystem2`, `InterSystemAbstraction`.
  `mna/process/`: `PowerSourceBipole` (solar panel), `TransformerInterSystemProcess` (isolating transformers).
- States (`mna/state/`): `VoltageState`, `CurrentState`, `VoltageStateLineReady` (`ElectricalLoad` extends it).
  Flags: `setCanBeSimplifiedByLine(true)` (cables, poles, downlinks, CableNet), `setAsMustBeFarFromInterSystem()`
  (capacitors/inductors: their SubSystem is seeded first), `setAsPrivate()` (never merged with public states).
- Line collapse (`generateLine`, `Line.newLine`): a line-ready state with exactly two `Resistor`s is folded with its
  neighbours into one `Line` (R = sum); inner states are recomputed in `Line.simProcessFlush`.
- Size limit `maxSubSystemSize = 100`: past it, components with `canBeReplacedByInterSystem()` are left out; leftovers
  become `InterSystemAbstraction` (half-R + `DelayInterSystem2` to ground on each side, coupled by Thevenin
  equivalents in `DelayInterSystem2.ThevnaCalculator`; Rth cached per `matrixVersion`, `-Deln.bench.noRthCache` off).
  A non-resistor left straddling two SubSystems is logged and left unsimulated (1.7.10 threw ClassCastException).
- `MnaConst`: `highImpedance` 1e9, `ultraImpedance` 1e16, `noImpedance` 1e-9.

## Thermal network (`ThermalLoad`, `ThermalConnection`, `Simulator.thermalStep`)

- Explicit Euler. `Tc` is K above 20 C (`PhysicalConstant.Tamb`: a fixed scale, so limits, saves and displays are
  absolute); each load relaxes through `Rp` to its `ambient` (same scale), which `Ambient` (biome x 25 C, cooler above
  y 80, blended into a depth curve underground, 110 C in the Nether; config `ambient`) stamps when the device joins the
  simulation (`TransparentNodeElement`/`SixNodeElement.connectJob`, `CableNet.addToSimulation`). GameTests hold it at
  20 C (`Ambient.fixedC`; `AmbientCases`). `Rs` per connection half (flow `dT/(Rs1+Rs2)`), `C` J/K.
- Fast loads step 20x per tick at 1/400 s; slow loads (`setAsSlow()`: six-node and voxel cables, poles, downlinks, resistor, diode) once at 0.05 s.
- `addThermalConnection` between a fast and a slow load only prints `addThermalConnection ERROR` and drops it.
- `checkThermalLoad(Rs, Rp, C)` throws if `C < getMinimalThermalC = 3 * thermalPeriod * (Rs || Rp)`. It uses the
  fast period even for slow loads; slow cable constants are stable anyway (`AuditFixesTest.slowCableJunctionIsStable`).
- Heat input: `movePowerTo(W)` counts for one step of the load (call once per its step); `moveEnergyTo(J)` is folded
  in as J/dt at the load's next step, correct from any rate. `ThermalLoad.ambientSink()` = an infinite reservoir per device, at its ambient (`sinkAt`).

## Failures (`src/main/java/mods/eln/sim/process/destruct/`)

- `ValueWatchdog` subclasses (`ThermalLoadWatchDog`, `VoltageStateWatchDog`, `BipoleVoltageWatchdog`,
  `ResistorPowerWatchdog`, `ShaftSpeedWatchdog` in `Mechanical.kt`) burn a 2 s budget by overflow; one-tick "joker".
  All are put on `slowProcessList` in content code.
- Trip -> `IDestructable.fail(cause, value, severity)` -> `Failure` keeps the worst per device and calls
  `Simulator.queueDestruct`. End of tick: failures grouped by `FaultNetwork.of(node)` (nodes reachable via
  `nodeConnectionList`); only the highest `severity()` per network runs `destructImpl()`, the rest `cancelFail()`, then
  `FaultNetwork.relieve` re-arms every watchdog of that network and clamps temperatures to the limit. Decision
  (Baughn): one failure per network per tick, so a short burns the weakest cable, not the whole run.
- `Failure.outcome(kind, cause, ...)`: burn in place (`IBurnable`, repair via `Repair`) or pop/arc flash/explode.
- Element NBT: `burnt` (cause ordinal + 1), `burntKind`.

## NBT of sim parts

Elements save all lists with `INBTTReady.writeAll/readAll(nbt, lists...)` (`src/main/java/mods/eln/misc/INBTTReady.java`),
prefix `""`, so keys are `name + suffix` and names must be unique within one element:
`NbtElectricalLoad`/`NodeVoltageState` `<name>Uc` (float); `NbtThermalLoad` `<name>Tc` (float, NaN/inf -> 0);
`NbtResistor` `<name>R`; `ResistorSwitch` `<name>R`, `<name>State`; `VoltageSource` `<name>U`, `<name>Istate`;
`Inductor` `<name>Istate`; `PowerSource` `<name>P/Umax/Imax`; `PowerSourceBipole` `P`, `Umax`, `Imax` (no name);
`NbtBatteryProcess` `NBPQ`, `NBPlife` (no name); `NbtFurnaceProcess` `<name>Q`, `<name>gain`, + `Fire` `<name>fire`,
`<name>fireLighting`; `RegulatorProcess` `<name>errorIntegrated`, `<name>target`; `NbtElectricalGateOutputProcess`
`<name>highImpedance`, `<name>U`; `NodeElectricalGateInputHysteresisProcess` `<name>state`. See persistence.md.

## Other simulators (pointers)

- Fluids: `src/main/java/mods/eln/sim/fluid/` (`FluidNetwork` implicit pressure solve, `Flash`, `Water`, `EaUnits`),
  stepped by `src/main/java/mods/eln/fluid/FluidWorld.java` as a slow process; devices join with their node's
  connectJob. Tests: `FluidCoreTest`, `FluidPropertyTest`. See content.md for pipes/oil.
  Per step (20 Hz): heat in; one compressibility per phase per node (the stiffer of a small and a 2% flash step); an
  implicit pressure solve, repeated if a check valve runs backwards or a flow runs against the side its packet was
  taken from; limiters bound nodes by their neighbours' solved pressures and cap liquid level moves; each node gives at
  most half of each phase it holds, net of inflow, and the rest of its outflow is its inflow passed through, with the
  mix and enthalpy from an implicit upwind solve (so pipe volume doesn't limit throughput or stability); then the
  transfer and a flash per node. Pumps are one-way edges from a sump with a head (`FluidEdge.boost`). Iron pipe holds
  0.03 L per block (tunable, not geometry); saves record the volume and older ones are rescaled on load.
- `src/main/java/mods/eln/solver/Equation.java`: expression parser/evaluator for the math chip (`ElectricalMathElement`)
  and `AnalogChips.kt`; not part of the MNA.

## Invariants and their guards

| Commit | Invariant | Guard (`src/test/java/mods/eln/...`) |
|---|---|---|
| e0a454db | A `Line` ending at ground flushes ground as 0 V and is never an inter-system link | `sim/mna/TopologyTest.lineEndingAtGround`, `groundedLinesOnALargeChain`; also `MnaPropertyTest.lineCollapseKeepsThePhysics` |
| 5973f322 | A line-ready run grounded at both ends is not collapsed | `sim/mna/TopologyTest.lineGroundedAtBothEnds` |
| 94782ece | A removed `Monopole` leaves its pin (`breakConnection`) | `sim/mna/DcCircuitTest.removedMonopoleLeavesTheCircuit`, `MnaPropertyTest.monopoleIsAConductanceToGround` |
| 948ac491 | Solver = independent dense MNA + KCL; Line/InterSystem/topology changes keep physics; thermal conservation | `sim/mna/MnaPropertyTest` (6 properties, `RandomNet`), `sim/ThermalPropertyTest` (3) |
| 494ca0d4, 25827ba3 | Closed 3D grid at minimal C (Rp infinite) stays finite, spread never above 1.05x start, ends no higher (overshoot of ~1% is allowed, not "never overshoots") | `sim/ThermalPropertyTest.loadsAtTheirMinimalCapacityStayBounded` |
| b052e0a0 | One failure per network per tick | GameTest only: `src/main/java/mods/eln/gametest/cases/BurnChainCases.java` ("burn chain overheat" asserts one of 6 cables burns; "burn chain overvoltage" reports). No JUnit test covers `FaultNetwork`. |

## Unit tests covering the sim (`src/test/java/mods/eln/sim/`)

`SimHarness` (Eln defaults), `SimulatorTimingTest`, `ThermalTest`, `ThermalUnitsTest`, `ThermalPropertyTest`,
`AuditFixesTest`, `ElectricalDevicesTest` (diodes, transformers, resistor heating), `TransformerMotorTest`,
`XfmrDualSourceTest`, `FireTest`; `mna/DcCircuitTest`, `mna/TransientTest`, `mna/TopologyTest`, `mna/MnaPropertyTest`
(+ `mna/RandomNet`, test-only `mna/component/TestConductance`, `TestCurrentSource`); `fluid/FluidCoreTest`,
`fluid/FluidPropertyTest`. jqwik failures persist in `build/jqwik-database`. See testing.md.

## Gotchas

- Mixed fast/slow thermal connection is silently dropped (`ThermalTest.fastSlowConnectionIsSilentlyDropped`).
- Never change the simulation from inside a step (watchdog, process): queue it (`queueDestruct`) or defer to a slow process.
- `Simulator.tick` copies `slowPreProcessList`/`slowProcessList` before iterating; the other lists are not copied.
- Two INBTTReady parts with the same name (or two nameless ones) in one element overwrite each other's keys.
