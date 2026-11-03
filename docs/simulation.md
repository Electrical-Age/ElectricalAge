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
  The coupling is iterative: a split network settles geometrically, most within 100 steps, a few in a thousand in
  500 to 700 (`MnaPropertyTest.slowSettlersReachTheDcAnswer`). `RootSystem` walks its pending states and components
  in insertion order (LinkedHashSet), so the split, and the simulation, is the same from run to run (it used
  HashSets of identity-hashed objects: the split, and how fast it settled, varied with the JVM).
- `MnaConst`: `highImpedance` 1e9, `ultraImpedance` 1e16, `noImpedance` 1e-9.

## Thermal network (`ThermalLoad`, `ThermalConnection`, `Simulator.thermalStep`)

- Explicit Euler. `Tc` is K above 20 C (`PhysicalConstant.Tamb`: a fixed scale, so limits, saves and displays are
  absolute); each load relaxes through `Rp` to its `ambient` (same scale), which `Ambient` (biome x 25 C, cooler above
  y 80, blended into a depth curve underground, 90 C in the Nether; config `ambient`) stamps when the device joins the
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
- Burnt (`IBurnable.isBurnt`; Baughn: a damaged machine should tick, but should stop doing useful things): the node
  calls the element's `connectBurntJob` instead of `connectJob`. Only its thermal loads and the thermal connections
  with both ends among them join the simulation, so it cools to ambient through Rp; no process, watchdog or electrical
  part runs, and its faces carry nothing. Elements that run a process from `initialize` skip it while burnt (heat
  furnace, electrical furnace). Its light goes out. Right-click: a meter reads it, anything else repairs it or says
  the repair part (`Repair.onActivated`); `hasGui` is false, `NodeMenus.menu` refuses, an open screen closes
  (`ElnNodeMenu.stillValid`). Client: no looped sounds, no render `refresh` (animation, particles). `BurntCases`.
- Repair part (`Repair.defaultPart`): the descriptor's `repairPart` (item name) if set, else by kind: ELECTRONICS and
  electrical MACHINE failures a Cheap Chip, other MACHINE failures a Copper Thermal Cable, TRANSFORMER a Cheap
  Ferromagnetic Core, CAPACITOR a Dielectric, ROTATING an Electrical Motor, the rest one of the same item. Jade shows
  the cause and the part on a burnt device.

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

- Fluids: `src/main/java/mods/eln/sim/fluid/` (`FluidNetwork` implicit pressure solve, `Flash`, `Water`, `EaUnits`,
  `Species`, `SpeciesRegistry`), stepped by `src/main/java/mods/eln/fluid/FluidWorld.java` as a slow process; devices
  join with their node's connectJob. Tests: `FluidCoreTest`, `FluidPropertyTest`, `MultiFluidTest`,
  `MultiFluidPropertyTest`. See content.md for pipes/oil.
  Species (Baughn 2026-11-03: fluids are data): water (id 0) and air (1) are built in on `Water`'s numbers; others come
  from datapack JSON (`SpeciesLoader`, content.md). A node holds `mWater`, `mAir` and `extra[id - 2]`, plus energy.
  `Flash` splits each condensable species at T by Raoult within its miscibility group, from the node's whole
  composition (non-iterative: the gas space is the root of one piecewise-linear equation); liquids stack in layers by
  density (one per group); `pressureAt` weighs the layers above a port. A port draws the gas above the liquid, else the
  layer at its height, blended across a band at each interface. A pipe's open end on a side face is an opening as tall
  as its bore (`FluidPort.span`, `FluidEdge.spanA/B`): it passes liquid as far as it is wet, and the pressure through it
  is the mean over it (`FluidNode.pressureOver`), so a part-full run delivers at its far end (Baughn 2026-11-03: it held
  its puddle until half full). Device flanges are points. An opening coupled to a point port is two one-way edges
  (FluidWorld): out of the opening its wetted mix at the wet mean, into it what the other port gives (liquid at the wet
  mean, gas at the opening's height as a point: `FluidEdge.entrySpan`), so a dead-ended pipe drains while gas comes
  back over its liquid (Baughn 2026-11-03; `pipe_sealed_run_drains`). Liquid counts by volume against a node's room; each
  layer's volume moves at most half a band a step; a node gives at most half of each layer (and of its gas) a step.
  With water and air alone every step is bit-identical to the two-species core it replaced (checked on 400 random
  networks), and `mLiquid()`, `mVapour()`, `pVapour()` and `FluidEdge.fLiquid/fVapour/fAir` stay water's and air's;
  `FluidEdge.fSpecies` carries every species. Devices save a node with `FluidSaves` (W/A/U plus `<prefix>S`); fluids map
  to species in `fluid/FluidSpecies` (with kg per mB); `EaUnits.add/drainLiquid/drainVapour/vapourAvailable` move a
  species in and out; `FluidNode.energyAt(T)` is a node's whole contents' energy (heat exchange). Tanks' sight glasses
  draw each liquid layer in its species' colour.
  Per step (20 Hz): heat in; one compressibility per phase per node (the stiffer of a small and a 2% flash step); an
  implicit pressure solve, repeated if a check valve runs backwards or a flow runs against the side its packet was
  taken from; limiters bound nodes by their neighbours' solved pressures and cap liquid level moves; each node gives at
  most half of each phase it holds, net of inflow, and the rest of its outflow is its inflow passed through, with the
  mix and enthalpy from an implicit upwind solve (so pipe volume doesn't limit throughput or stability); then the
  transfer and a flash per node. Pumps are one-way edges from a sump with a head (`FluidEdge.boost`); a curved pump
  (the condensers' `CondensatePump`) is set each step to the chord from where it runs now on its curve to its shut-off
  point (boost H0, open the chord's slope). Steeper than the curve over that stretch, it neither overshoots the settled
  point nor pumps past the shut-off. A chord from free flow overshot every step near the shut-off against a tight feed,
  and the pipe flipped between two pressures (`CondensatePumpTest`, `CondensatePumpPropertyTest`). Iron pipe holds
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
