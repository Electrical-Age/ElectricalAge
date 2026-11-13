# Testing

How the 26.x port is tested: JUnit/jqwik unit tests (`src/test`), and the GameTest harness that runs the 1.12
self-test cases and the golden-data diffs on a headless server (`src/main/java/mods/eln/gametest`).

## Running

| Command | What |
|---|---|
| `nix develop` | Dev shell from `flake.nix`: JDK 25 (`JAVA_HOME`, `JDK25`) and JDK 21 (`JDK21`). `gradle.properties` reads both through `org.gradle.java.installations.fromEnv=JDK21,JDK25`; auto-download is off. Elsewhere, export those two variables. |
| `./gradlew test` | Unit tests (JUnit 5 platform; JUnit 4 via vintage; jqwik). Run under ModDevGradle `unitTest`, so Minecraft is on the classpath (sim NBT uses `CompoundTag`). |
| `./gradlew runGameTestServer` | Every registered GameTest, headless; exit code = number of failed required tests. Sets `neoforge.enabledGameTestNamespaces=eln` and `eln.golden=golden/1.12`. In game: `/test runall`. |
| `./gradlew runGameTestServer -PelnBench=result.json [-PelnBenchLabel=name]` | The cable benchmark only (no other test registered). Extra: `-PelnBenchConvert` (same scene through the voxel-cable converter), `-PelnBenchNoRthCache` (`DelayInterSystem2.cacheRth` off, 1.12 behaviour). |
| `./gradlew runClientData` | Datagen (lang, models, tags, recipes) into `src/generated/resources` (committed). There is no separate `runData` run. |

`test` task system properties (`build.gradle`): `eln.deterministic=true` (`Utils.rand` returns the midpoint of its
range; the 1.12 sim tests were written against that), `jqwik.database=build/jqwik-database` (failing seeds are kept
there and replayed first), `eln.guiDir` (LayoutTest), `-PfluidCases=N` -> `fluid.cases`. jqwik defaults to 200 tries
(`src/test/resources/junit-platform.properties`).

## Unit tests (`src/test/java/mods/eln/...`)

| File | Covers |
|---|---|
| `sim/mna/DcCircuitTest` | Resistive circuits vs Ohm/Kirchhoff; one step is exact. |
| `sim/mna/TopologyTest` | `RootSystem` splitting into `SubSystem`s, lazy rebuild, cable `Line` collapse, >100-state split at InterSystem resistors. |
| `sim/mna/TransientTest` | Capacitor/inductor backward-Euler recurrence (tight) and exp(-t/tau) (loose). |
| `sim/mna/MnaPropertyTest` (jqwik) | Random DC nets (`RandomNet`, own dense solver) vs `RootSystem`: KCL, add/remove transitions, Lines, InterSystem, Monopole, PowerSourceBipole. Helpers `mna/component/TestConductance`, `TestCurrentSource`. |
| `sim/ThermalTest`, `ThermalUnitsTest` | Explicit-Euler thermal network; `movePowerTo` (W, one step) vs `moveEnergyTo` (J). |
| `sim/ThermalPropertyTest` (jqwik) | Closed nets conserve heat and stay within extremes; open nets balance energy. |
| `sim/SimulatorTimingTest` | Electrical/fast-thermal interleave per 50 ms tick (1 electrical + 20 fast thermal steps). `SimHarness` = Eln defaults. |
| `sim/ElectricalDevicesTest` | Diode and transformer as their elements build them. |
| `sim/AuditFixesTest` | Regression tests for the 1.7.10 sim bugs fixed in the WP16 audit. |
| `sim/FireTest` | A fire's burn level lags its setting. |
| `sim/fluid/FluidCoreTest`, `FluidPropertyTest` | Water/steam/air properties, flash, turbine calibration; random networks conserve mass/energy, no NaN, settle. Not jqwik: own seeds, printed in the failure message. |
| `api/MachineSpecTest` | `MachineSpec` defaults (power on every face not given a signal or the bus) and every `build()` message. |
| `api/MachineCoreTest`, `MachineCorePropertyTest` (jqwik) | `MachineCore` with no circuit: misuse throws; power-up hold (a flickering line never starts it); an ask sizes the next step (exactly one tick of lag); a retried failed `tryTakeJoules` is one ask; refill; each trip and its cooldown, the overheat trip as a rise over ambient and its reset (at 40 and 90 C), a throwing trip listener. Properties over random specs (any voltage, floors 0.05..0.95, a heat model in a third) and line histories that dwell on every threshold: delivered == taken + buffered + lost exactly (producers: fed <= given, overdraw carried), buffer in bounds, load resistance >= Unom^2/rated and idle only below the floor, trips latch for the cooldown (also when another cause follows at once), undervoltage always recovers and never chatters, `Brownout.SLOW` never stops, droop and current limit. |
| `api/BenchTest` | Machines in the real simulator (`ElnTestKit.bench()`): a stiff supply gives what is asked; two machines on a weak line sag it and share; a 200 V machine on 50 V is UNDERVOLTAGE and takes nothing; overvoltage trip and its cooldown; a producer feeds a consumer (taken <= delivered <= fed <= given; the refill backs off rather than collapsing the line); a starved producer stops and restarts a `Brownout.STOP` mill and settles under a `Brownout.SLOW` one; parallel producers share; rows of 3-5 `Brownout.SLOW` machines of mixed ratings on one weak line, switched on one by one, off and on, the largest every 8 ticks, the line weakened and made stiff (checked every tick against resistors at their ratings: never lower, never starved; each phase settles, shares in proportion, recovers); the servo drive's spec in the Nether (90 C) idles far under its trip and trips 80 K over it; signals, bus, thread check. Golden trace `src/test/resources/mods/eln/api/step-load.trace` (a step load on a weak line): regenerate deliberately with `./gradlew test -PapiTraces` and review the diff. |
| `api/ElementHostTest` | The element host with a stub element: relative faces one-to-one for every front, masks and loads per face (power, gate in, gate out), an input's default until connected, outputs floating until set; parts in the element's lists (books first, plan in the pre-processes, one machine per element); the game's tick order in a bare `Simulator` (rated draw at 50 V, the old resistor's draw on a half-volt line, exact books); NBT under `api.machine.*` written through until the first tick; a burn goes to the element and a repaired element un-burns the core; `find`, `saveState`/`loadState`, a bus port (checked channels and levels, claims). `MachineCoreTest` also covers `respec`, `snapshot`/`restore` and `repair`; `MachineCorePropertyTest.respecConservesEnergy`. |
| `api/HostFacesTest`, `api/impl/RegistrationBookTest`, `api/impl/ClientMachineTest`, `api/ApiTestFixtureImportsTest` | Hosted machines: a block's facing is FRONT (none: north), UP stays up on a horizontal facing and LEFT/RIGHT are as seen facing the front; registration is idempotent, one function per type, closed with a message naming common setup, thread-safe; the client view reads back a synced snapshot whole, is UNPOWERED before one and throws on writes; the `apitest` fixture names no EA type outside `mods.eln.api`. |
| `api/MnaMachineTest` | `MnaMachine` in a bare simulator: a steady load on a weak line keeps its resistance tick to tick (no matrix rebuilds) with exact books; a producer given energy every third tick stays connected and opens 20 ticks after its queue runs dry; a simulator stepping other than once per tick is refused. |
| `api/impl/testkit/VerifySpecTest` | `ElnTestKit.verifySpec` passes on EA-shaped specs and a spread of others (both kinds, every tier, both brownouts, heat models, burns), runs the drive's heat model to its reset (at 20 and 90 C ambient), skips overheat without one saying why, and fails (with the trace in its report) when the bench runs a machine that doesn't match its spec: another floor, less power, a burn, a slower cooldown, a weaker producer. |
| `api/lowlevel/LowLevelBenchTest`, `api/impl/lowlevel/PortLinkTest`, `node/MaskedLoadTest` | The low level on a bench: Ohm's law on the bench line, a source sagging behind its ohms and two sharing in parallel, a signal source floating until set and driving a level, a watchdog's hold, single trip, reset and limits, every checked value and cross-circuit wiring, threads, a port link on a bench (NOT_HOSTED) and its removal, the debug trace only when on; each `PortStatus` from what a link finds, PortRef's codec keys and its refusal of unknown names; `NodeBase.getElectricalLoad(side, lrdu, mask)`. |
| `api/MetersTest` | `Meters` reads as `Utils.plot*` without padding (Celsius absolute); `Meters.number` (EA's one copy of the format) pinned at every band. |
| `api/ApiJarTest`, `api/spi/ImplementationsTest` | The eln-api jars (built before `test`): only the API's packages (`api`, `lowlevel`, `spi`; the testkit's), no EA class named in any class's constant pool outside them, the API jar naming nothing of the testkit, `Eln-Api-Version` equal to `ElnApi.API_VERSION`. The entry points reach EA's implementation, and without it say that EA isn't loaded and name `ModList.get().isLoaded("eln")`. The build's `compileApiConsumer`/`compileApiConsumerTest` (part of `check`) compile the docs' samples, their tests and the `apitest` fixture against the jars and Minecraft alone. |
| `api/docs/*` | docs/api.md's samples: `ApiDocSamplesTest` checks that every java block of the page equals a `// doc:begin`/`doc:end` region of a source here, that every region is shown, and that the samples name no EA type outside the eln-api jars (not `impl`, `spi` or `element`; the testkit only in tests; but `HeaterElement`, the EA-element sample); `MillTest` and `DocSamplesRunTest` run the samples (verifySpec on each spec, the mill on a bench, the dynamo, the gate's signals and bus, the kiln, the load bank's guard). |
| `node/NodeSaveLoadTest` | Unloadable node tags (also a `LinkageError`) are kept by `NodeManager` as orphans and written back unchanged; dropped by position; a node whose save throws keeps its last good tag. |
| `node/six/SixNodeLoadTest` | A six-node face that cannot load is kept and its side stays taken; load connects once; bad client packets ignored. |
| `server/ElnWorldSavesTest` | Bad save files set aside, unreadable ones left and saving blocked, a whole `.tmp` preferred to the `.bak`. |
| `transparentnode/energyconverter/ExporterSaveUpgradeTest` (jqwik) | Old simple-node `"ElnToOther"` exporter tags become transparent-node tags (`"node"`, `"element"`, `"others"`). |
| `node/voxel/CableArmsPropertyTest` (jqwik) | `CableConverter.plan` + `CablePart.arms`: one conductor, opens at edge middles, inner-corner joins. |
| `node/voxel/CableRouterTest`, `PipeRouterTest` | Routers on synthetic worlds. |
| `node/voxel/ChiselBrushTest` | Chisel brush boxes (cube, flat, tunnel) for every axis, size and hit voxel; brush validation. |
| `client/gui/RadialMenuTest` | Radial menu hit test (ring and segment under the mouse). |
| `gui/LayoutTest` | Layout engine; every `assets/eln/gui/*.json` has no overlaps and nothing outside its panel. |
| `item/xray/XRayRayMarchTest` | X-ray ray march. |
| `gametest/StructureFileTest` | The generated floor `.nbt` parses (3x3 stone floor). |
| `robotics/motion/SegmentPropertyTest` (jqwik), `DriveTest` (jqwik) | Servo segments are continuous, within limits, arrive; retargets are seamless. Top speed follows the line; the energy rule: a step goes no further than its energy pays for, a cut move starts where the paid step ended at the speed the supply keeps paying for. |
| `robotics/motion/StationsTest` | Stations from markers or spread evenly; setpoint bands and share; the panel's next stop; block-aligned stops. |
| `robotics/servo/RidersTest` | Who stands on a car (players a round trip late on the server); the client's carry of its own player (the pure core of `CarCarry`). |
| `robotics/servo/SegmentPayloadTest` | The segment payload's codec round trip; the client's segment matches the server's. |

## GameTest harness (`src/main/java/mods/eln/gametest`, server)

- `TestCase`: `id()`/`name()`, `width()=8`, `height()=4`, `depth()=3`, `floorDepth()=min(3, depth)`, `ticks()=40`,
  `build(ctx)`, `measure(ctx)`. The 26.x form of the 1.12 selftest's SelfTestCase.
- `TestContext`: `at(dx,dy,dz)` (relative to the structure; y=0 floor, devices at y=1), `floor(p)`, `placeSix(id, p[, side])`,
  `placeTransparent(id, p)` (as a FakePlayer click), `stream(writer)` (GUI packet bytes), `check`, `checkValue` (1%).
  Implementations: `GameTestContext` (fails the test at the end if any check failed), `WorldTestContext` (live
  world, log only; used by `LabCommands`).
- Device ids: the int passed to `placeSix`/`placeTransparent` is the 1.12 `sub + (id << 6)` key of the descriptor
  registries (`Eln.sixNodeItem.all()`, `Eln.transparentNodeItem.all()`), not an item damage value; every device is
  its own item, named from `src/main/resources/eln-ids.json`. New cases use `cases/Ids.six(name)` /
  `Ids.transparent(name)` (descriptor names as in `golden/1.12/inventory.json`).
- `ElnGameTests` (`register` called unconditionally from `Eln.java`): `cases()` lists every case; each becomes test
  `eln:<id>` in structure `eln:floor_<W>x<H>x<D>`, timeout `ticks()+40`, with `PADDING` (8) empty blocks round
  the structure. The test server keeps every batch's structures in one grid, 5 blocks apart; the padding puts 21
  blocks between neighbours (past a light's reach of 15 and a biome cell), so a test's outcome can't depend on which
  tests ran next to it (Baughn). Per case:
  0. `forceLoadPadding`: the chunks under the padding are forced too (the server forces only the structure's; the
     runner releases all forced chunks after each batch), so a case may write into its padding, e.g. a biome box.
     Padding is on every side and the server clears it to air, so the floor floats 8 blocks above stone (Stasis
     refills it; here that light work kept `port26_light_block`'s unsettled reading at 0 once);
  1. leaked-node check: any node already in the test bounds (same dimension) fails the case;
  2. lays the stone floor (`width x floorDepth`);
  3. `build` -> wait `ticks()` -> `measure` (exceptions become failed checks);
  4. `persistence`: `Reload.node` on every non-destructing, saved (`mustBeSaved`) node in the bounds (save -> unload -> fresh node from the
     tag -> save again; also compares `networkSerialize` bytes). Differences in keys ending `Uc` or `State`
     (`Reload.isTransient`) are allowed; anything else fails.
- Structures: `src/main/resources/data/eln/structure/floor_WxHxD.nbt`, made by `script/gen_test_structures.py WxHxD ...`
  (empty box, stone floor). A case's size must name an existing file.

### Case files

Top level: `SimTickCase`, `KotlinRuntimeCase.kt` (jarJar'd stdlib loads), `PublishRenderCase` (publish packet ->
fresh client render consumes it exactly, draws; then `BakeOnceCheck`, below), `GuiPacketCase` (client->server GUI packets via
`ServerPacketHandler.handle`, far player ignored), `MachineMenuCase`, `MenusOpenCase`, `AutoMinerSpinCase`,
`ItemDrawCase`, `XRayCase`, golden cases (below). `TestEnergySinks`: a lodestone at a test-registered position is an
FE receiver.

`BakeOnceCheck.check(ctx, name, blockEntity, pos, draw)`: the device with its unchanging parts in the chunk
(rendering.md) must show every quad exactly once. It draws the device without baking for reference, then with it
on a clock of its own (50 ms a frame) until the parts settle, and compares three frames against the reference:
before the chunk shows the parts (the renderer draws all), while it shows them (the renderer's frame plus what
`BakeMesh.write` gives for the settled calls: place within 1e-4, texture coordinates, state, light, and the colour
the entity shader would give, within 3 of 255), and after it stopped (the renderer draws all again). Then the device
is swept and nothing may be left settled. Only quads that the reference draws before and after agree on are
compared one by one (a part moving on its own differs between any two draws), and the frames' vertex counts must
add up. `publish_render` runs it for every device and fails if the chunk's share of all vertices falls below 90%
(98.9% when written; the INFO lines give it per device). `autominer_spin` runs it with a drill that stands until the
chunk shows it and then turns. A new device with a render is covered by `publish_render` without more work; a test
of a part that changes state can call the check with a draw that changes it.

`cases/`: `Batch1Cases`, `Wp9aCases` (six-node power), `Wp9bCases` (sensors, probes), `Wp9cCases` (logic, meters,
wireless), `Wp10aCases`, `Wp10bCases` (machines, generators, thermal), `Wp11Cases` (shafts, poles, converters),
`Wp12Cases`, `SolarCases` (first multiblock), `LogicTruthCases`, `ChipMoreCases`, `WirelessMoreCases`,
`WorldSensorCases`, `ShaftMoreCases`, `LampSupplyCases`, `LampLightCases`, `CableSlotCases` (devices work with an empty
inventory), `SixNodeBreakCases` (one face per break, kept-only voxel block, orphan block), `BurnChainCases` (one failure per network), `CableCases` (voxel vs six-node cables,
currents agree to 0.1%), `PipeCases`, `OilCases`, `TurretCases`, `DimensionCases` (wireless and poles across
dimensions, built in the Nether too), `AutoMinerCases` (mining below y = 0, refused blocks), `OreDropCases`, `ItemCapabilityCases` (hoppers/pipes),
`TooltipCases`, `PortHelperCases` (fuel, smelting, `eln:light`, waterlogged six-node block), `MachineApiCases` (the
electrical machines on the Machine API against their old resistor at 1x and 0.7x their volts, reload mid-item, an
old-format save, burn and repair), `HostedMachineCases` (machines at other mods' block entities, with the hidden
`apitest` fixture: works on a real supply, turns, keeps its node through LIT, breaks, reloads from its attachment,
leaf faces, bus face, the capability, thread and registration rules; a slow process added after the machine's
reads it right after the step, before the owner takes), `MachineApiCases.SpecsVerifyCase` (`api_ea_specs_verify`: every EA
device on the API passes `verifySpec` with the spec from its registered descriptor), `LowLevelCases` (a low-level
circuit at a vanilla barrel: a power face on a real line, turning, breaking; a `PortRef` link to a cable two blocks
away, its WRONG_KIND and NO_NODE neighbours, the cable broken and placed back; a watchdog whose listener opens the
load; one circuit per block entity, none on a machine's).
`GuiPacketFuzzCases` (`gui_packets_reject_bad_values_<element>`: one case per six-node and transparent element class
that has its own `networkUnserialize`, found by enumerating the registries, so a new handler is covered unlisted;
packet ids -128..127 with NaN, infinite, huge and negative numbers, out-of-range index bytes and a 3000-character
string; first, each value in `VALID` (one its screen sends) must land in the saved tag as sent; after each packet that
read a value, the element's processes step without throwing, its saved tag gains no NaN, infinity or string over 64
characters, and the values in `RANGES` are in their screens' ranges; a device whose check failed is removed before the
simulator ticks it; `..._saved_<element>` (`SAVED`: exporter, transporter, turret, wireless receiver, lamp supply): a
device saved with a bad value an older handler could store loads it in range and steps).

A case can run code part way (`ctx.after(ticks, r)`, as its measure runs: outside the simulator's tick) and reload a
node from an edited tag (`Reload.node(level, pos, tag -> ...)`: a save as an older version wrote it).
`ServoCases` (the elevator: driven over its RS485 bus and by its setpoint wire, slower on a sagging line, slower on a
starved one and still arriving, the energy books of a lift, stopped by an obstruction, reloaded mid-move with its
machine, floor ghost, a mob carried, the cabin's walls; structure `floor_11x12x9`).

### Golden data (`golden/1.12/*.json`, see `golden/README.md`)

- `GoldenCase` (`golden_descriptors`): every descriptor's primitive fields vs `inventory.json`. Lists: `IGNORED`
  (render/1.12-only fields), `RECREATED` (X-Ray Scanner: only listed fields compared), `CHANGED` (per item and field, with
  reason), `NEW_IN_26` (registry keys with no 1.12 twin), `NEW_IN_26_NAMES`.
- `GoldenRecipesCase` (`golden_recipes`): datagen recipes vs `recipes.json` (minecraft:/eln: ingredients only). Lists:
  `DEFERRED`, `CHANGED`, `REMOVED`, `ORES_TO_CHUNKS`, `NOW_VANILLA`.
- `GoldenSlotsCase` (`golden_slots`): every menu's slots (index, limit, accepted probes) vs `slots.json`. Lists:
  `KNOWN_MISSING` (reported as INFO), `REMOVED`, `removedCableSlots(descriptor)`, `GUI_REMOVED`.
- A deliberate difference goes into one of these lists with a reason, and into `golden/README.md`.

### Tools that are not tests

- `LabCommands` (`/elnlab`, op level 2, registered unconditionally): `gallery`, `burn`, `cables`, `machines`,
  `devicevolts`, `voxelcables convert|dry|revert`, `oil`, `netdump`, `remove`, `reconnect`, `inspect`.
- `NetDump` (`/elnlab netdump [pos [r]]`): the solved network (after Line collapse) to `eln-netdump/<stamp>.json` and
  `.falstad.txt` in the server directory.
- `bench/CableBench`: ~20k cable blocks; per-tick sim cost in steady, churn, fault and teardown phases, written as JSON.

## Adding a GameTest

1. Implement `TestCase` (usually a nested class in a `cases/*Cases.java` with an `addTo(List<TestCase>)`).
2. Add it to `ElnGameTests.cases()`.
3. If the size is new, run `script/gen_test_structures.py WxHxD` and commit the `.nbt`.
4. Place devices with `Ids` names; put any blocks you need (walls, supports) yourself with `ctx.floor` or `setBlock`.
5. Leave no node behind outside your bounds, and make sure saved state round-trips (the persistence pass runs anyway).

## Gotchas

- Structure blocks do not appear in the test world although the `.nbt` parses; the harness lays the floor itself.
  Anything else a case needs it must place.
- Tests share one level: a node left inside another test's box fails that test (leaked-node check).
- FakePlayers have no connection: `ElnNet.sendToPlayer` skips players with a null `connection` or without the
  channel, so cases cannot observe outbound packets. Inbound GUI packets are tested by calling
  `ServerPacketHandler.handle` directly (`GuiPacketCase`).
- Several cases read element fields by reflection on their names (`Wp10aCases`, `Wp11Cases` `field`, `Wp9cCases`
  `setField`); renaming such a field breaks the case at run time, not compile time.
- The harness, `/elnlab` and the structures ship in the main jar. Tests only run with
  `neoforge.enabledGameTestNamespaces=eln` (set by the Gradle runs); `TestEnergySinks` returns a null capability for
  every lodestone a test did not register.
- `GoldenCase` and friends skip (INFO line) when `eln.golden` is unset, e.g. under `/test runall` in a client.
