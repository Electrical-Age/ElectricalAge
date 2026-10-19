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
| `gametest` | in-game tests and `/elnlab` lab commands (see testing.md) |

Kotlin vs Java: there is no separate Kotlin tree. ~46 `.kt` files sit next to the Java ones under `src/main/java`
(e.g. `node/published.kt`, `node/INodeElement.kt`, `mechanical/*.kt`, `sixnode/AnalogChips.kt`, `sound/LoopedSound.kt`).

## Node kinds

| Kind | Node class / uuid | Block | Block entity | Element(s) | Render (client) |
|---|---|---|---|---|---|
| Six-node | `node/six/SixNode` / `"s"` | one shared `eln:six_node` (`SixNodeBlock`, waterloggable) | `SixNodeEntity` | up to 6 `SixNodeElement`, one per face (`sideElementList[6]`) | `SixNodeElementRender` per face, BER `SixNodeRender` |
| Transparent | `node/transparent/TransparentNode` / `"t"` | one `TransparentNodeBlock` per device (`ElnRegistry.TRANSPARENT_BLOCKS`, via `descriptor::newBlock`) | shared type `transparent_node` (`TransparentNodeEntity`) | one `TransparentNodeElement` | `TransparentNodeElementRender`, BER `TransparentNodeRender` |
| Voxel | `node/voxel/VoxelNode extends SixNode` / `"v"` | `voxel_node` (+ fence/wall variants) | `VoxelNodeEntity` | voxel parts (cables, pipes) + hosted six-node elements | parts baked by `VoxelModel`; hosted elements via `SixNodeRender`. See voxel-cables.md |
| Grid | a TransparentNode whose element is `gridnode/GridElement` | per device | as transparent | `GridElement` + `GridLink` set | `GridRender` (catenaries) |

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
- Load: `NodeManager.loadFromNbt` -> `upgradeTag` -> class by `"tag"` -> `readFromNBT` -> `initializeFromNBT` -> `globalBoot`. Unknown type or a throwing load: kept as an orphan tag and saved back unchanged (`keepOrphan`); a node placed later at that spot replaces it.
- Burn/repair (`sim/process/destruct`): `Failure` decides by `Failure.Kind`; most devices `burn()` in place (`IBurnable`): disconnect, then reconnect with the element skipped by `connectJob`; publish bit `HEADER_BURNT`; drops nothing of the device itself. `Repair.onActivated` (right-click with `getRepairPart()`) -> `repair()`. Real explosions only for rotating/battery cases; `Eln.explosionEnable`.

## Tests

- `src/test/java/mods/eln/node/NodeSaveLoadTest.java`: unknown node types / devices kept and re-saved; orphans per dimension.
- `src/test/java/mods/eln/transparentnode/energyconverter/ExporterSaveUpgradeTest.java`: old exporter tags (jqwik).
- GameTests (`gametest/`): `Reload` round-trips every node of every case; `cases/SixNodeBreakCases` (one face per break); `cases/BurnChainCases` (one cable of a chain burns); `GoldenCase` (descriptor numbers vs `golden/1.12`).

## Gotchas

- Element ids in NBT and packets are the 1.12 damage values (`eid`, `EID<face>`); never renumber. Registry names come only from `eln-ids.json`.
- Many 1.12 names survive (`Coordonate`, `dimention`, `LRDU`, `getWaila`); the `NodeManager("caca")` / `GhostManager("caca2")` ctor strings are unused.
- `ElnContent.load()` names `mods.eln.registry.ElnContentImpl`, which does not exist; it is never called (dead code). Batches are listed directly in `Eln.CONTENT`.
- Client and server share static registries in single-player: guard with `level.isClientSide()`; capabilities return null on the client.
- `Eln.simulator` is static and touched by unit tests without Minecraft: keep `Eln`'s static init trivial.
- Some javadoc is stale (e.g. `NodeBlock` "models come in P3", `TransparentNodeEntity` "render comes with P3").
