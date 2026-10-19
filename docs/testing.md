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
| `node/NodeSaveLoadTest` | Unloadable node tags (also a `LinkageError`) are kept by `NodeManager` as orphans and written back unchanged; dropped by position; a node whose save throws keeps its last good tag. |
| `node/six/SixNodeLoadTest` | A six-node face that cannot load is kept and its side stays taken; load connects once; bad client packets ignored. |
| `server/ElnWorldSavesTest` | Bad save files set aside, unreadable ones left and saving blocked, a whole `.tmp` preferred to the `.bak`. |
| `transparentnode/energyconverter/ExporterSaveUpgradeTest` (jqwik) | Old simple-node `"ElnToOther"` exporter tags become transparent-node tags (`"node"`, `"element"`, `"others"`). |
| `node/voxel/CableArmsPropertyTest` (jqwik) | `CableConverter.plan` + `CablePart.arms`: one conductor, opens at edge middles, inner-corner joins. |
| `node/voxel/CableRouterTest`, `PipeRouterTest` | Routers on synthetic worlds. |
| `gui/LayoutTest` | Layout engine; every `assets/eln/gui/*.json` has no overlaps and nothing outside its panel. |
| `item/xray/XRayRayMarchTest` | X-ray ray march. |
| `gametest/StructureFileTest` | The generated floor `.nbt` parses (3x3 stone floor). |

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
  `eln:<id>` in structure `eln:floor_<W>x<H>x<D>`, timeout `ticks()+40`. Per case:
  1. leaked-node check: any node already in the test bounds (same dimension) fails the case;
  2. lays the stone floor (`width x floorDepth`);
  3. `build` -> wait `ticks()` -> `measure` (exceptions become failed checks);
  4. `persistence`: `Reload.node` on every non-destructing node in the bounds (save -> unload -> fresh node from the
     tag -> save again; also compares `networkSerialize` bytes). Differences in keys ending `Uc` or `State`
     (`Reload.isTransient`) are allowed; anything else fails.
- Structures: `src/main/resources/data/eln/structure/floor_WxHxD.nbt`, made by `script/gen_test_structures.py WxHxD ...`
  (empty box, stone floor). A case's size must name an existing file.

### Case files

Top level: `SimTickCase`, `KotlinRuntimeCase.kt` (jarJar'd stdlib loads), `PublishRenderCase` (publish packet ->
fresh client render consumes it exactly, draws), `GuiPacketCase` (client->server GUI packets via
`ServerPacketHandler.handle`, far player ignored), `MachineMenuCase`, `MenusOpenCase`, `AutoMinerSpinCase`,
`ItemDrawCase`, `XRayCase`, golden cases (below). `TestEnergySinks`: a lodestone at a test-registered position is an
FE receiver.

`cases/`: `Batch1Cases`, `Wp9aCases` (six-node power), `Wp9bCases` (sensors, probes), `Wp9cCases` (logic, meters,
wireless), `Wp10aCases`, `Wp10bCases` (machines, generators, thermal), `Wp11Cases` (shafts, poles, converters),
`Wp12Cases`, `SolarCases` (first multiblock), `LogicTruthCases`, `ChipMoreCases`, `WirelessMoreCases`,
`WorldSensorCases`, `ShaftMoreCases`, `LampSupplyCases`, `LampLightCases`, `CableSlotCases` (devices work with an empty
inventory), `SixNodeBreakCases` (one face per break, kept-only voxel block, orphan block), `BurnChainCases` (one failure per network), `CableCases` (voxel vs six-node cables,
currents agree to 0.1%), `PipeCases`, `OilCases`, `TurretCases`, `OreDropCases`, `ItemCapabilityCases` (hoppers/pipes),
`TooltipCases`, `PortHelperCases` (fuel, smelting, `eln:light`, waterlogged six-node block).

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
