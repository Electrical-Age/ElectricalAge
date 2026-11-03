# Architecture

Overview of the Electrical Age 26.x port (mod id `eln`, NeoForge 26.3, Java 25 + Kotlin): entry points, node kinds,
the descriptor/element/render triad, registries and the node lifecycle. Subsystem details live in the sibling pages.

## Entry points

| Class | Side | Role |
|---|---|---|
| `src/main/java/mods/eln/Eln.java` | both | `@Mod` ctor; global statics (balancing values, `simulator`, `nodeManager`, `ghostManager`, descriptor registries `sixNodeItem` / `transparentNodeItem` / `sharedItem` / `sharedItemStackOne`, packet ids, `CONTENT` batch list) |
| `src/main/java/mods/eln/client/ElnClient.java` | client | `@Mod(dist = CLIENT)`: BERs, item special model `eln:device`, `VoxelModel`, `eln:lit`, GUI layer, screens, keys, pipelines, frame hook (see rendering.md) |
| `src/main/java/mods/eln/server/ServerEventListener.java` | server | lightning list for wireless devices; `ServerTickEvent.Post` -> `ElnContent.serverTick()` per batch |
| `src/main/java/mods/eln/server/ElnWorldSaves.java` | server | `LevelEvent.Load/Save/Unload` -> per-dimension node file (see persistence.md) |
| `src/main/java/mods/eln/node/NodeServer.java` | server | `ServerTickEvent.Pre`: publish dirty nodes to tracking players (see networking-gui.md) |

`Eln` ctor order (matters: descriptors must exist before item registration):
1. `ElnConfig.SPEC` registered (SYNCED) -> `Simulator` -> `NodeManager` / `GhostManager` -> `ElnWorldSaves.register()`.
2. `NodeManager.registerUuid("s" SixNode, "t" TransparentNode, "v" VoxelNode)`; `registerTagUpgrade("ElnToOther", ExporterSaveUpgrade::upgrade)`.
3. Forge-bus listeners: `ServerAboutToStartEvent` (-> `ElnRegistry.afterRegistration()`, `ElnMachineRecipes.registerAll()`, `simulator.init()`, wind + `FluidWorld` slow processes, `CONTENT[].serverAboutToStart()`), `ServerStoppedEvent` (clear everything), `ServerTickEvent.Pre -> simulator.tick()` (1.12 ran it at tick START), `ServerTickEvent.Post -> NodeOrphans.tick`, kit/chisel/pipe player events.
4. `obj.loadAllElnModels()` (OBJ models, both sides) -> `ElnDeviceRegistry.registerBatch1()` -> `CONTENT[].preInit()` (fills the descriptor registries) -> `ElnRegistry.register(modBus)` (DeferredRegisters from those registries).
5. Capabilities (`RegisterCapabilitiesEvent`, all server-side only, null on client): fluid on transparent nodes and voxel pipe adapters, items, energy (FE exporter), item energy. See capabilities.md.

Tick data flow: `ServerTickEvent.Pre` -> `Simulator.tick()` (see simulation.md) -> `NodeServer.tick()` publishes -> `Post` -> `NodeOrphans.tick` (every 200 ticks) and batch `serverTick()`.

## Package map (`src/main/java/mods/eln/...`)

| Package | Contents |
|---|---|
| `node` | NodeBase/Node, NodeManager, NodeBlock/NodeBlockEntity, ElementInventory, DescriptorRegistry, ElnRenders, NodeOrphans; `node/six`, `node/transparent`, `node/voxel` |
| `sixnode` | wall-mounted devices (cables, gates, lamps, sensors, wireless...) |
| `transparentnode` | full-block machines (battery, furnaces, turbines, transformers, FE exporter `energyconverter`...) |
| `gridnode` | utility poles, grid transformer, downlink: `GridElement`, `GridLink`, `GridRender` catenaries |
| `ghost` | multiblock filler blocks: GhostBlock, GhostElement, GhostGroup, GhostManager, GhostObserver |
| `sim` | simulator, loads, processes; `sim/mna` (circuit solver), `sim/nbt`, `sim/process` (incl. `destruct`: Failure/Repair), `sim/fluid` |
| `mechanical` | shafts, turbines, generator (Kotlin) |
| `oil`, `fluid` | 26.x oil chain devices; fluid handlers, buckets, `FluidWorld` |
| `item`, `generic`, `ore`, `entity` | item descriptors (tools, batteries, X-ray), `ElnItem` + damage-descriptor base classes, ores, replicator |
| `registry` | `ElnRegistry`, `ElnDeviceRegistry`, `ElnMachineRecipes`, `registry/batch/Wp*Content` |
| `client`, `gl`, `cable`, `sound` | client setup, screens, render glue; GL11 facade; cable drawing; sounds (see rendering.md) |
| `gui`, `network` | `ElnNodeMenu`, menu specs, layout; `ElnNet`, `ServerPacketHandler` (see networking-gui.md) |
| `server`, `compat` | saves, tick listener, player keys; NeoForge shims (`WorldCompat`...) and `compat/jade` |
| `misc`, `fsm`, `solver`, `i18n`, `wiki` | utilities (Coordonate, Direction, LRDU, Obj3D, Utils), state machines, signal-math expression parser, `I18N.tr`, wiki data |
| `datagen` | lang, tags, recipes, item models -> `src/generated/resources` (committed) |
| `robotics` | the servo layer, kept apart (own registers in `Robotics`, an `ElnContent` in `Eln.CONTENT`) so it can become a subproject: `motion` (pure: `Segment`, `Drive` with its energy rule, `Stations`), `servo` (Linear Servo Drive node, a Machine API machine, `ServoRailBlock`, `StationMarkerBlock`, `ElevatorCar` + `ElevatorPanel` part, `Riders`, `SegmentPayload`), `client` (car renderer, `CarCarry`), `mixin` (floating kick) |
| `gametest` | in-game tests and `/elnlab` lab commands (see testing.md) |
| `api` (source set `src/api`, the eln-api jar) | the machine API for other mods and new devices, compiled against Minecraft and NeoForge alone (build.gradle: the `api` source set; `compileApiConsumer` compiles the docs' samples and `apitest` against the jar alone; `ApiJarTest`): `MachineSpec` (built and checked; nominal volts are a plain number), `Tier` (a convenience: 50, 200, 800, 3200 V), `RelativeFace`, `CablePosition` (a signal's cable on a side face's floor or ceiling, so two signals can share a face), `MachineState`, `TripCause`, `Brownout` (STOP by default, or SLOW), `BurnHandler` (what a burn does to another mod's block: the spec's `burnsOnOvervoltage(handler)`), `Machine`/`MachineView`/`MachineBus` (`Machine.respec` swaps in other ratings: upgrades; `Machine.signalConnected` tells a wired input from an unwired one; `MachineView` reads the buffer's capacity and the energy books since built or loaded: `joulesDelivered`, `joulesTaken`, `joulesGiven`, `joulesLost`); `ElnApi` (`API_VERSION`, semantic, in the jar's manifest; `apiVersion()`, the installed one; `machines()`: `MachineRegistry`, machines at other mods' block entities; `MACHINE`, the `eln:machine` block capability); `Meters` (EA's meter formatting for other mods' GUIs; `Meters.number` is the one copy of it, `Utils.plotValue` calls it); no internal types. The user guide is [api.md](api.md) |
| `api/impl` | the API's implementation: `MachineCore`, one machine's pure controller (buffer, state machine, heat model with the spec's reset band, cooling to the ambient its host gives: EA's `Ambient` at an element, the bench's setting; plain numbers, no circuit types); `MnaMachine` puts a core in the circuit at a terminal load (consumer: resistor to ground; producer: source behind its droop resistor, set at root pre-step from the Thevenin equivalent); hosts call `applyBeforeStep()` before the electrical step and `readAfterStep()` after it (it refuses a simulator not stepping once per tick); a `Brownout.SLOW` consumer learns its line's Thevenin resistance from two steps and plans no more than the line's maximum power, on its stiff side (planning on the sagged volts alone collapsed a weak line to the machine's rated resistance); a consumer's resistor has a 1% dead-band (under-draw only) and a producer stays connected 20 ticks after its queue empties, so steady machines don't rebuild the matrix every tick; `ReadOnlyView`; `MachineLines` (a machine's Jade lines from its view: an element's, a hosted machine's); `ElementHost` (a machine on an EA element: its parts in the element's lists, faces by `Faces`, state saved under `api.machine.*`; its loads named `power` and by the spec's signal names, which introspect lists as the ports, behind index-based NBT keys `api.machine.in<i>` / `out<i>` (`NbtElectricalLoad(name, nbtName)`); an optional bus port, `PortBus`) behind `api/element`'s `Machines.forElement` / `ElementMachine`; the foreign block entity host (see "Machine API hosts"): `HostedMachines` (`ElnApi.machines()`, the lifecycle, the capability), `MachineNode`, `HostedMachine` (the server's handle, inert and `DETACHED` once its block entity goes), `ClientMachine` (the client's read-only view), `MachineData` (the `eln:machine` attachment), `RegistrationBook`, `HostFaces`, `ServerThread`, `ApiTrace` (the debug trace, `-Deln.api.debug=true`), `mixin/ServerLevelMixin` |
| `apitest` | the hidden `eln:api_test_machine` (FACING, LIT; no item, so in no creative tab or JEI; a furnace's look and the name "API Test Machine" from datagen), written on `mods.eln.api` alone like another mod: the GameTests' fixture for hosted machines; `ApiTestMachineEntity.useSpec` hosts one with another spec (`BURNING_SPEC`, whose burn handler books the burn and leaves the block) |
| `api/element` | EA-internal, not in the eln-api jar: `Machines.forElement` and `ElementMachine` for EA's own elements (faces and masks per side, or per side and LRDU for signals at a cable position; `wailaLines()` for the element to place among its own Jade lines); they name EA types (`TransparentNodeElement`, EA's `Direction` and `LRDU`, `ElectricalLoad`) |
| `api/spi` (`src/api`) | how the API's classes reach EA's implementation: `Implementations` finds `ElnApiProvider` (`api/impl/ElnApiImpl`: `machines()`, `Circuit.create`) by class name at first use, in the API's class loader; without EA it throws `IllegalStateException` naming `ModList.get().isLoaded("eln")` (the soft-dependency pattern, api.md "Without Electrical Age") |
| `api/testkit` (source set `src/apiTestkit`, the eln-api-testkit jar) | interfaces, implemented in `api/impl/testkit` (`TestKitImpl`, found as `testkit/spi/TestKitProvider` by name; `BenchImpl`, `BenchMachineImpl`, `SpecSuite`): `ElnTestKit.bench()`: `Bench` (supply, line, a resistive `load`, machines in EA's own simulator with `tickFieldBuses` off, ticked in the game's order, trace per tick; `attach`/`connect` run low-level circuits) and `BenchMachine` (a `Machine` plus owner code, signal inputs/outputs, a shared field bus); `ElnTestKit.verifySpec(spec)`: `SpecSuite`'s canned checks, a `SpecReport` or a `SpecFailure` |
| `api/lowlevel` (`src/api`) | EA's circuit parts for the few mods the Machine API can't serve (see api.md): `Circuit`, `Load`, `SignalLoad`, `SignalSource`, `ResistorLoad`, `VoltageSource` (always behind ohms), `Branch`, `Watchdogs`/`Watchdog`/`WatchdogTrip`, `PortRef` (with a Codec), `PortPosition`, `PortKind`, `PortLink`, `PortStatus`, `FaceConnection`, `HostedNode`; no internal types |
| `api/impl/lowlevel` | `CircuitImpl` (parts, attach/detach to a `Site`: the game's node or a bench), `PortLinkImpl`, `CircuitNode` (the unsaved node at a block entity), `CircuitHosts` (its lifecycle, run from `HostedMachines.tick`) |

Kotlin vs Java: there is no separate Kotlin tree. ~46 `.kt` files sit next to the Java ones under `src/main/java`
(e.g. `node/published.kt`, `node/INodeElement.kt`, `mechanical/*.kt`, `sixnode/AnalogChips.kt`, `sound/LoopedSound.kt`).

## Node kinds

| Kind | Node class / uuid | Block | Block entity | Element(s) | Render (client) |
|---|---|---|---|---|---|
| Six-node | `node/six/SixNode` / `"s"` | one shared `eln:six_node` (`SixNodeBlock`, waterloggable) | `SixNodeEntity` | up to 6 `SixNodeElement`, one per face (`sideElementList[6]`) | `SixNodeElementRender` per face, BER `SixNodeRender` |
| Transparent | `node/transparent/TransparentNode` / `"t"` | one `TransparentNodeBlock` per device (`ElnRegistry.TRANSPARENT_BLOCKS`, via `descriptor::newBlock`) | shared type `transparent_node` (`TransparentNodeEntity`) | one `TransparentNodeElement` | `TransparentNodeElementRender`, BER `TransparentNodeRender` |
| Voxel | `node/voxel/VoxelNode extends SixNode` / `"v"` | `voxel_node` (+ fence/wall variants) | `VoxelNodeEntity` | voxel parts (cables, pipes) + hosted six-node elements | parts baked by `VoxelModel`; hosted elements via `SixNodeRender`. See voxel-cables.md |
| Grid | a TransparentNode whose element is `gridnode/GridElement` | per device | as transparent | `GridElement` + `GridLink` set | `GridRender` (catenaries) |
| Hosted machine | `api/impl/MachineNode extends GhostNode` / `"h"`, never saved | another mod's | another mod's | an `ElementHost` | none (the block's own; publishing is off) |

- `NodeBase` (abstract): coordinate, `nodeConnectionList`, connect/disconnect, publish, NBT. `Node extends NodeBase` adds light (`lastLight`) and redstone. `NodeManager` keeps `Coordonate -> NodeBase` (all dimensions, one instance).
- Multiblocks: `TransparentNodeDescriptor.getGhostGroup(front)` -> `GhostGroup.plot` places `GhostBlock`s; `GhostManager` maps ghost coord -> `GhostElement` (observer = the device), the element is a `GhostObserver` (`ghostDestroyed`, `ghostBlockActivated`). Breaking any ghost breaks the device. `misc/GhostPowerNode.kt` gives ghosts electrical ports.
- FE exporter (`transparentnode/energyconverter/`): was a SimpleNode, now an ordinary transparent element (ids group 84: `ExporterSaveUpgrade.LVU_ID/MVU_ID/HVU_ID`), its own block class (`EnergyConverterElnToOtherBlock`) with a baked `neoforge:obj` model. Old saves: node tag `"ElnToOther"` rewritten by `ExporterSaveUpgrade.upgrade`; old BE id `eln:energy_converter_eln_to_other_entity` aliased to `transparent_node` in `ElnRegistry`.

## Descriptor / element / render

| Part | Side | Lives in | Notes |
|---|---|---|---|
| Descriptor (`SixNodeDescriptor`, `TransparentNodeDescriptor`, both extend `generic/GenericItemBlockUsingDamageDescriptor`) | both | `Eln.sixNodeItem` / `Eln.transparentNodeItem` (`DescriptorRegistry`, keyed by 1.12 damage id) | static data: ratings, model parts, ghost group, `ElementClass`; draw code (`draw`, `drawItem`) also here |
| Element (`*NodeElement`) | server | the node (`TransparentNode.element`, `SixNode.sideElementList`) | built by reflection `ElementClass(node, descriptor)`; owns loads/processes, inventory (`ElementInventory`), `burn/repair` |
| Render (`*NodeElementRender`) | client instance, server-safe class | block entity (`elementRender`, `elementRenderList[6]`) | created from the publish packet by `ElnRenders` factory, registered in `client/ClientRenders.registerAll()` (descriptor class or element class -> ctor) |

- `NodeBlockEntity.getNode()` returns null on the client; render classes must not import client classes (`UtilsClient.hooks` does the client calls).
- Kotlin helpers: `published<T>` delegate (`node/published.kt`) marks a field for publish/reconnect on change.

## Registry

- `registry/batch/Wp9aContent` .. `Wp12eContent` (`ElnContent` impls, 1.12 order) + `ElnDeviceRegistry.registerBatch1()` add descriptors with their 1.12 ids (`addDescriptor`, hidden: `addWithoutRegistry`).
- `ElnRegistry.register`: one block + item per transparent device, one item per six-node device, shared items (`ElnItem`), ores; name = `ElnRegistry.nameOf(legacy, id)` from `src/main/resources/eln-ids.json` (key `"eln:six_node@<id>"` / `"eln:transparent_node@<id>"` / `"eln:shared_item@<id>"` -> `name`, `display`, `descriptor`). Missing entry = `IllegalStateException` at startup.
- Lookup: by stack `Eln.transparentNodeItem.getDescriptor(stack)` (item implements `IDescriptorItem`); by id `getDescriptor(eid)`; by 1.12 display name `Eln.findItemStack("Cheap Chip")` (first registered wins).
- `afterRegistration()` runs descriptors' `setParent` at server start (ItemStacks can't exist during registration).
- 26.x-only items (cable/pipe kits, chisel, raw ores, oil) are plain `DeferredItem`s in `ElnRegistry`. See content.md.

## Lifecycle

- Place, transparent: `TransparentNodeItem.placeAt` -> spawn delta rotated by front -> `checkCanPlace` -> ghosts plotted -> `new TransparentNode().onBlockPlacedBy` (adds to `NodeManager`, builds element) -> `setBlock` -> `checkCanStay(true)`.
- Place, six-node: `SixNodeItem.useOn` -> new or existing `SixNode` (or `VoxelNode` host) -> `createSubBlock(stack, face, player)`.
- Connect: `NodeBase.connect()` -> `connectJob()` -> `tryConnectTwoNode` per matching face/LRDU (`compareConnectionMask`) -> `ElectricalConnection` / `ThermalConnection` added to the simulator. `NodeBase.simulationVeto` (26.x, for a time-stop integration) keeps a node out and only computes the visual connections.
- Neighbour change: `NodeBlock` -> `NodeBlockEntity.onNeighborBlockChange` -> `NodeBase.onNeighborBlockChange` -> `neighborBlockRead()` + `reconnect()` if added.
- Break: `NodeBlockEntity.setRemoved` (not on chunk unload) -> `onBreakBlock` -> disconnect + `NodeManager.removeNode`. Six-node breaks one face per click (`playerAskToBreakSubBlock`). `NodeOrphans` removes nodes whose block is gone (every 10 s, loaded chunks only; also `/elnlab remove`).
- Chunk unload: nothing. Nodes live outside chunk data and keep simulating (1.12 semantics). Only dimension unload calls `NodeManager.unload(dim)`.
- Load: `NodeManager.loadFromNbt` -> `upgradeTag` -> class by `"tag"` -> `readFromNBT` -> `initializeFromNBT` -> `globalBoot`. Unknown type or a throwing load: kept as an orphan tag and saved back unchanged (`keepOrphan`); a node placed later at that spot replaces it, and removing the block there drops it (`dropOrphans`).
- Burn/repair (`sim/process/destruct`): `Failure` decides by `Failure.Kind`; most devices `burn()` in place (`IBurnable`): disconnect, then reconnect with the element skipped by `connectJob`; publish bit `HEADER_BURNT`; drops nothing of the device itself. `Repair.onActivated` (right-click with `getRepairPart()`) -> `repair()`. Real explosions only for rotating/battery cases; `Eln.explosionEnable`.

## Machine API timing (`api`)

- One game tick: the slow pre-processes (each machine plans its load, or its source, for the step), the electrical
  step, then the slow processes: each machine books what the step delivered (state, heat, buffer) and then its owner
  takes or gives energy. A consumer's asks in tick N size its load for the step of tick N+1: exactly one tick later.
- The buffer holds two ticks of rated energy. An idle machine (no asks) draws only its idle watts and doesn't refill
  its buffer, so the first ask of a run usually gets nothing, or only what was left in the buffer: work starts a tick
  after the first ask. This is inherent (the step that would pay for it has already run). Owners either start their
  work a tick late (the Linear Servo Drive stands still a tick and goes on), ask in the tick before they need the
  energy, or keep asking a little while ready. While asks continue, a refill of up to a quarter of a tick's rated
  energy keeps the buffer ahead of them.
- Loading: a machine saved running runs on at its first step if the line is in range; otherwise a machine starts
  UNPOWERED and runs after the line has held for half a second.
- Books (`MachineView`): `joulesDelivered`, `joulesTaken`, `joulesGiven`, `joulesLost` since the machine was built or
  loaded; a consumer's delivered = taken + buffer + lost, a producer's given = delivered + buffer + lost.

## Machine API hosts (`api/impl`)

- Registration: `ElnApi.machines().register(type, be -> spec)` at common setup (common setups run in parallel: the
  book is thread-safe). `HostedMachines` closes it in `RegisterCapabilitiesEvent` (registries freeze before common
  setup; the capability for each type is the real deadline): later calls throw, naming `FMLCommonSetupEvent`. The
  same type with the same function again is a no-op, with another it throws.
- Load: NeoForge has no block entity load event. `mixin/ServerLevelMixin` (config `eln.api.mixins.json`, required)
  hooks `ServerLevel.onBlockEntityAdded`, which both `LevelChunk.addAndRegisterBlockEntity` (placement, `setBlock`,
  `setBlockEntity`) and `registerAllBlockEntitiesAfterLevelLoad` (chunk load) call; a registered type's block entity
  is only queued there (the chunk is mid-promotion). `HostedMachines.tick` (`ServerTickEvent.Pre`, high priority, so
  before `simulator.tick()`): retires hosted block entities that are `isRemoved()` (broken, replaced, chunk
  unloaded: `clearAllBlockEntities` calls `setRemoved`), hosts the queue, then `followFacing` on each. `of(be)` on
  the server hosts at once if it comes first (an owner's first tick).
- Hosting: a `MachineNode` at the block (a stale node there is retired first), `neighborBlockRead` (loaded neighbours
  only), `NodeManager.addNode`, `connect()`, then `reconnect()` on its added EA neighbours (Argon's trap 4: cables stay
  in NodeManager while the block's chunk is unloaded; also re-runs a voxel neighbour's bus links). The node is tied
  to the block entity object, so a block state change that keeps it (a furnace's LIT: trap 1) does nothing;
  `followFacing` reconnects only when the front from the facing changed (trap 2: `HostFaces`, a property named
  `facing` or `horizontal_facing` with Direction values, else north; then `Faces.resolve`).
- Leaf faces (trap 3): each power face has its own terminal load (cables connect to it); one `ElectricalConnection`,
  the selector, joins one terminal to the host's power node. A slow pre-process (first, before the host plans the
  step) keeps the active face while it has its cable and, for a consumer, its line (2% of nominal); else the connected
  face with the highest volts takes over; a connected face with a dead line is kept unless another face is live (no
  switching between two live lines: two cables from one grid would flip-flop). Unselected terminals dangle (no
  current), so no power ever passes between faces and removing the node never cuts a line.
- Bus: the bus face's mask is 0 (no NodeConnection); the node links its `BusPort` to a voxel bus cable beside it
  (`VoxelNode.busPortAt`), and a voxel node links to a `BusPeer` neighbour in its own `connectJob`.
- Retire: the state goes into the attachment, then `onBreakBlock` (or only `disconnect` when its dimension already
  dropped it), and the `HostedMachine` handle detaches (`DETACHED`, inert; it logs once if used).
- Burn (Baughn: "a callback. Let the API user choose how to handle it"): the spec's `burnsOnOvervoltage(handler)`.
  `MachineNode`'s site queues the handler through the simulator's destruct queue (end of the tick; skipped if the block
  entity went; a throw is logged). The core stays burnt (`OVERVOLTAGE_TRIPPED`), saved as `api.machine.burnt` and
  restored with the site's `burning` set before the first step, so the handler runs once per block entity. The builder
  has no burn without a handler, so `of()` has nothing to refuse; EA's elements pass `Machines.BURNS_OUT` (EA's
  burn-out, `ElementBurn`), the one handler `forElement` accepts (at a block entity it does nothing: the machine stays
  burnt). `respec` can't change the burn, nor swap EA's burn-out for another handler.
- Not saved: `mustBeSaved()` is false (the GameTest persistence pass skips such nodes); its state lives in the
  block entity's `eln:machine` attachment (persistence.md), synced to clients (networking-gui.md).
- The capability `eln:machine` (capabilities.md) reads `of(be).view()`.
- Jade: `compat/jade/ElnJadePlugin`'s machine provider (UID `eln:machine`, every block entity but EA's node blocks,
  whose lines are their node's) sends `MachineLines.at(level, pos)` from the server: `MachineLines.hosted`, the element
  lines (`MachineLines.element`, which `ElementMachine.wailaLines` gives) plus the state, buffer and temperature.

## Machine API low level (`api/lowlevel`, `api/impl/lowlevel`)

- A `Circuit` (`CircuitImpl`) keeps its parts: an `ElectricalLoad` per load (its series resistance set at attach from
  the site: a low-voltage cable node's for power, the signal cable's for signals), resistors, sources (an
  `ElectricalLoad` for the EMF, an ideal `VoltageSource` to ground and a `Resistor` to the output), gate outputs. `attach(site)`
  adds them all to the site's simulator with two processes: a slow pre-process that resolves the port links, and a
  slow process that checks the watchdogs (a deterministic hold time, latched until reset). `detach()` removes them.
- Hosting: `hostAt(be, faces)` queues a `CircuitNode` (a `GhostNode`, never saved) in `CircuitHosts`; the next
  `HostedMachines.tick` (before the simulator's step) places it as a hosted machine's node is placed, and retires it
  when the block entity is `isRemoved()`. Connecting the node attaches the circuit, disconnecting detaches it. A
  machine's block entity, or one already hosting, refuses a circuit. Server stop abandons circuits without touching
  the stopped simulator.
- Port links: `PortLinkImpl.resolve` runs before every step. The link's `ElectricalConnection` is not in the port
  node's `nodeConnectionList`, so the node dropping its links doesn't drop it, and a link left on a load removed from
  the simulation would pull that load into the next solve: each tick it checks that the node at the place is in the
  simulation and gives the same load through `NodeBase.getElectricalLoad(side, lrdu, mask)` (the two-argument load
  when the side's connection mask shares a kind bit), and drops and re-resolves it before any step if not.
  `PortLinkImpl.classify` turns what it found into a `PortStatus`.

## Tests

- `src/test/java/mods/eln/node/NodeSaveLoadTest.java`: unknown node types / devices kept and re-saved; orphans per dimension and dropped by position; a failing save keeps the last good tag.
- `src/test/java/mods/eln/transparentnode/energyconverter/ExporterSaveUpgradeTest.java`: old exporter tags (jqwik).
- GameTests (`gametest/`): `Reload` round-trips every node of every case; `cases/SixNodeBreakCases` (one face per break, kept-only voxel block, orphan block); `cases/BurnChainCases` (one cable of a chain burns); `GoldenCase` (descriptor numbers vs `golden/1.12`).

## Gotchas

- Element ids in NBT and packets are the 1.12 damage values (`eid`, `EID<face>`); never renumber. Registry names come only from `eln-ids.json`.
- Many 1.12 names survive (`Coordonate`, `dimention`, `LRDU`, `getWaila`); the `NodeManager("caca")` / `GhostManager("caca2")` ctor strings are unused.
- `ElnContent.load()` names `mods.eln.registry.ElnContentImpl`, which does not exist; it is never called (dead code). Batches are listed directly in `Eln.CONTENT`.
- Client and server share static registries in single-player: guard with `level.isClientSide()`; capabilities return null on the client.
- `Eln.simulator` is static and touched by unit tests without Minecraft: keep `Eln`'s static init trivial.
- Some javadoc is stale (e.g. `NodeBlock` "models come in P3", `TransparentNodeEntity` "render comes with P3").
