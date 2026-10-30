# Voxel cables and pipes

Scope: the 26.x voxel node (a block whose 16x16x16 voxels hold cable pieces, pipes, fittings and hosted six-node
devices), the routers and kits, and the six-node -> voxel cable converter. Package `src/main/java/mods/eln/node/voxel/`
unless a path says otherwise. Fluid physics and the MNA itself: see simulation.md; NBT layout of other nodes: persistence.md.

## Key classes

| Class | Side | Role |
|---|---|---|
| `VoxelNode` | server | `extends SixNode implements FluidDevice`; uuid `"v"` (registered in `Eln.java` via `NodeManager.registerUuid`). Holds `parts`, a `used` voxel BitSet, `legacy` (six-node NBT for revert), `host` (imitated BlockState), `carved` |
| `VoxelPart` | both | Base of the parts; `read`/`deserialize` dispatch on `kind` |
| `CablePart` (`"cable"`) | both | Polyline on one face (`face` 0..5, 3D value) or a knuckle (`face == -1`, a point on an edge); `TIERS[]` maps tiers to the six-node cable descriptors by name (Signal, LV, MV, HV, VHV, Copper Thermal) |
| `PipePart` / `ValvePart` / `SafetyValvePart` / `PipeSensorPart` / `AdapterPart` | both | Pipes and fittings (`PipeSensorPart`: a signal output joined to the block's signal cables, as a control valve's input); `AdapterPart.tanks` is the fluid capability (capabilities.md) |
| `CableNet` | server | Groups a node's cable pieces into `Conductor`s and builds their loads |
| `CableRouter`, `PipeRouter` | server | A* routers, pure Java (world behind a `Space` interface) |
| `CablePlacer`, `PipePlacer`, `KitPreview` | server | Kit UX: start, waypoints, finish; preview outlines sent as `Eln.packetKitPreview` |
| `KitItem`, `KitRecipes`, `CableKitItem`, `PipeKitItem`, `CableKitRecipes`, `PipeKitRecipes` | both | Kits: length in custom data "kitLen" (1024 voxels max). One shared fill/unroll code for both kinds (`KitRecipes`): coils or plates into a kit, a lone kit back to one block of coil or plates, kits merge up to a full kit with the rest left in the grid |
| `Mimic`, `ChiselItem`, `ChiselBrush` | server (`ChiselBrush`: both, pure) | Imitate passive decorative blocks (stairs, slabs, fences, walls, panes, bars, carpets); carve plain blocks (see Chisel) |
| `CableConverter` | server | Six-node block -> voxel node and back |
| `VoxelGeometry` | both (pure) | Parts -> textured quads; no client classes |
| `VoxelNodeBlock`, `VoxelNodeEntity` | both | Block (shape = part boxes, waterloggable) and BE (`extends SixNodeEntity`; client keeps parts/host from publish) |
| `client/render/VoxelModel`, `client/render/KitOutline`, `client/render/ChiselOutline` | client | Chunk-mesh model; kit/chisel outlines |
| `client/ChiselRadial`, `client/gui/RadialMenu` | client | The chisel's brush menu (wrench key) |

Blocks: `voxel_node`, `voxel_node_wooden_fence`, `voxel_node_fence`, `voxel_node_wall` (fence/wall variants carry the
vanilla tags so neighbours join an imitated fence), one BE type `voxel_node` (`registry/ElnRegistry.java`).

## Voxel grid invariants

- Parts never overlap (`VoxelNode.canAdd` / `fits`), except cable pieces that touch a cable being added (joints).
- A hosted six-node element on face f occupies `VoxelNode.plate(f)`: 3 voxels thick, inset 2 from the edges, so edge
  midpoints (its ports) and inner-corner cables stay free.
- An empty node (`isEmptyNode`: no parts, no elements) removes its block, restoring `host` if it imitated one.
- Cable-only nodes are NOT added to `FluidWorld` (`connectJob` checks `hasFluid()`): the fluid world steps every device.

## Cables and the MNA

`connectJob` (on every connect): `connectElements()` -> `cables.build()` -> `addToSimulation()` ->
`connectElementsToCables()` -> per neighbour: `connectVoxel` (voxel neighbour) or `tryConnectTwoNode` (six-node /
transparent neighbour).

- Conductor = pieces of one tier, one face, touching (union-find in `CableNet.build`); knuckles are always alone.
- Each conductor: one `ElectricalLoad` with `setCanBeSimplifiedByLine(true)` (so runs collapse into MNA `Line`s,
  `sim/mna/component/Line.java`), scaled by `share()` = length/16 clamped to [0.25, 1]; knuckles use
  `KNUCKLE_SHARE` = 1e-3 and get no thermal load or watchdogs. Thermal tier: thermal load only (`load == null`).
- Conductors touching inside a block (inner corner, tier change) are joined by `ElectricalConnection`/`ThermalConnection`
  if their masks match (`NodeBase.compareConnectionMask`).
- Voxel-to-voxel: ends meet only at the exact same point across the face (`Port.mirrored()`), one `NodeConnection` each.
- Voxel-to-legacy: EA's (side, edge) rule; `VoxelNode.legacy(side, lrdu)` picks the conductor that a six-node cable on
  that face would be. No outer-corner wraps through air: an outer corner is a knuckle in the corner block.
- A straight block = 2 x Rs (`ElectricalConnection` adds both ends' Rs), as six-node cables.
- Hosted elements' edges are `End`s too (`VoxelNode.ends()`), linked to cables at the same point. An element may put
  several terminals on one edge (`SixNodeElement.edgePins()`: the three-channel data logger's inputs, board #45):
  each is an `End` of its own, offset voxels from the edge's middle toward `side.applyLRDU(lrdu.left())`, and that edge
  then has no middle point. In a plain six-node block such an element falls back to one connection an edge.

## Devices off centre (chips)

- A voxel block has `SixNode.SLOTS` element slots: 0..5 are the faces (a device centred on face f, as six-node blocks
  have), the rest hold devices placed anywhere on a face (`SixNodeDescriptor.placeAnywhere()`: the logic and analog
  chips). Such an element keeps its face in `side` and its centre's offset from the face's centre in `ox/oy/oz` (world
  voxels); packets, menus, activation and breaking address it by `slot`.
- `VoxelNode.placeAnywhere` snaps the click to voxels, keeps the model on the face, and `placeError` applies Baughn's
  rule: the model box (`SixNodeDescriptor.footprint()`, chips drawn at `CHIP_SCALE` 0.75: a case 6 voxels across) is
  clear, with a voxel of air all round, except where a pin touches a cable of a matching kind or meets another
  device's pin tip to tip.
- Ports are pins (`VoxelNode.pins`): a tip on the face plane at the model's edge, and a contact box just outside it.
  `connectPins` links a pin to every cable conductor whose boxes take its contact box, and to another device's pin
  that meets it. These devices draw no cable stubs to the block's edges. The cable kit ends at the nearest pin; routes
  keep their usual clearance (`CableRouter.CLEARANCE`).
- A pin at the block's edge reaches the next voxel block (`connectPinsAcross`): a cable there taking its contact voxel,
  or a pin there tip to tip (a NodeConnection, dropped when either block disconnects).
- Holding such a device shows it see-through where a click would put it (`DevicePreview`, client
  `DevicePreviewRender`, `GlFrame.ghost`), reddish where it may not go. A click on a device in a voxel block places on
  that device's face (`placeFace`).
- `CableConverter.revert` refuses a block holding such devices.

## Burn chain

Conductor watchdogs (`ThermalLoadWatchDog`, `VoltageStateWatchDog`, both in one slow process) -> `Failure(...).cable()`
-> `Simulator` keeps one failure per connected network per tick (`sim/process/destruct/FaultNetwork.java`; relieves the
others, including voxel nodes via `VoxelNode.slowProcesses()`) -> `Conductor.burn`: disconnect, mark every piece
`burnt`, reconnect (burnt conductors drop out of `live()`, i.e. an open circuit). Burnt pieces render brown
(`VoxelGeometry`) and drop nothing when broken. `Conductor.repair()` exists but nothing calls it (no in-game repair).

## Persistence (`VoxelNode.writeToNBT` / `readFromNBT`)

- SixNode layout (coordinates and hosted elements under `"node"`, `"EID<f>"`, `"ED<f>"`), PLUS the `"node"` compound's
  keys copied to the top level so a pre-hosting version still loads the node (f10fd800). Read accepts both.
- `"legacy"` (six-node NBT, converter), `"host"` (`NbtUtils.writeBlockState`), `"carved"` (long array),
  `"parts"` (count), `"p<i>"` (one compound per part, `"kind"` + the part's keys).
- `CablePart`: `"tier"` (descriptor name), `"face"`, `"pts"`, `"burnt"` (FailureCause name), `"conv"` (laid by the
  converter), `"paid"` (voxels returned on break), `"u"`, `"t"` (voltage, temperature: `CableNet.save()` copies them in
  before a write or rebuild).
- `PipePart`: `"tier"`, `"pts"`, `"burst"`, `"burstDir"`, `"w"`, `"a"`, `"u"`. `ValvePart`: `"at"`, `"open"`.
  `SafetyValvePart`: `"at"`, `"set"`. `PipeSensorPart`: `"at"` (x, y, z, outlet, bore, kind), `"scale"`, its output. `AdapterPart`: `"face"`. Unknown kinds load as nothing (dropped).
- Client publish: part count, then kind + length-prefixed bytes per part, then SixNode's elements
  (`publishBeforeElements`).

## CableConverter

- `plan(SixNode)` -> `Plan(parts, knuckles, cableFaces, skip)`: for each element, which edges are linked by EA's own
  rules (inner corner, same-plane neighbour, outer-corner wrap); cables -> `CablePart.arms(tier, face, dirs)` with
  state (`u`, `t`, burnt) copied and `paid` sharing the one 16-voxel item; devices stay hosted; outer corners -> a
  knuckle (signal or LV by the device's edge mask) in the wrap block.
- Skip reasons (`Plan.skip`): `already a voxel block`, `no level`, `next to an unloaded chunk`, `an outer corner goes
  through <block>` (not air nor a voxel node, e.g. a torch or ghost block), `a non-electrical outer corner (<Class>)`,
  `empty`. Waterlogged blocks convert (70dc2915).
- `convert`: writes the six-node NBT to `legacy`, strips cable elements, removes the node (disconnect, `unload`,
  `removeNode`) BEFORE `setBlock`, builds a `VoxelNode` from the stripped NBT, `globalBoot`, adds parts, places
  knuckles (`PipeKitItem.place`), skipping knuckles a neighbour's conversion already laid.
- `revert`: devices as they are now + cables from `legacy`; `revertKnuckles` removes `conv` pieces.
- Auto-convert: config `[cables] convertOnLoad` (`ElnConfig.java`, default false; decision: off until Baughn approves
  the conversion), at most `PER_TICK` = 2000 blocks per tick, chunk and its 8 neighbours queued on load. With it on,
  `SixNodeItem` treats old cable items as coils and puts new devices into voxel blocks.
- Commands: `/elnlab voxelcables convert|dry|revert <r>` (`gametest/LabCommands.java`).

## Routers

- `CableRouter`: lattice 2 voxels, states (point, attach face, last dir), moves along a face / inner corner / outer
  corner (knuckle); `BEND` 4, `BLOCK_ENTERED` 1, `MAX_EXPANSIONS` 400000, budget `PipeRouter.TIME_BUDGET_MS` (200 ms).
- `PipeRouter`: lattice 4, `BEND` 6, unsupported steps cost more (`UNSUPPORTED`), waypoints as legs; gives up on the
  wall-clock budget (Baughn: it runs on the server thread).

## Chisel

- Left-click carves the brush out of the block just inside the face hit (cancels mining, both sides:
  `ChiselItem.onLeftClick`); right-click puts the block's own material back into the block just outside it (not into
  parts). Sneak + right-click is the same put-back (the sneak cycling of sizes is gone, Baughn). A plain carvable block
  (`Mimic.carvable`) becomes a voxel node with `host` = the block and `carved` = the voxels taken; all carved away and
  empty: air; nothing carved and empty: the plain block again.
- Brush = size (1, 2, 4, 8) x shape, `ChiselBrush.box(shape, size, axis, vx, vy, vz)`: `CUBE` the grid-aligned size
  cube round the voxel hit; `FLAT` a 1-deep size x size layer at the hit voxel's depth, grid-aligned across the face
  (reliefs, trims); `TUNNEL` the size x size footprint through all 16 voxels along the face normal (the cable hole; put
  back fills the same column). Always inside one block. `ChiselItem.brushBox` turns a hit into that box; the client
  outline (`client/render/ChiselOutline`) draws the same box, so it shows the shape. There is no server-side chisel
  preview (`KitPreview` is kits only).
- Stored in the stack's custom data: `"brush"` (size, the pre-shape key) and `"shape"` (`ChiselBrush.Shape` ordinal,
  missing = cube). The name shows both: "Chisel (4, flat)" (`item.eln.chisel.shape.*`).
- Chosen in a radial menu: the wrench key (`key.eln.wrench`, C) with a chisel in the main hand opens
  `client/ChiselRadial` (inner ring sizes, outer ring shapes, centre = the brush so far) instead of reporting the
  wrench key to the server. Release the key over a segment (or click; clicks while held pick without closing; a click
  on the centre commits), Esc cancels. A changed brush is sent as `Eln.packetChiselBrush`; `ChiselItem.applyBrush` sets
  it on the server's main-hand stack only if that is a chisel and the size and shape are valid.

## Rendering pointers

`VoxelGeometry` (quads) -> `VoxelModel` (NeoForge dynamic block-state model, `{"type": "eln:voxel_node"}`,
registered in `client/ElnClient.java`), reading parts from `VoxelNodeEntity.getModelData()`; a publish that changes
parts re-meshes the section. Hosted elements are drawn by the six-node renderer. `cable/CableTerminals.java` (lugs at
device terminal plates) applies to six-node floor cables only, not voxel cables.

## Tests

- Unit (`src/test/java/mods/eln/node/voxel/`): `CableRouterTest` (floor, hole, wall climb, bump, sealed box gives up),
  `PipeRouterTest` (obstacles, ports, floor preference, sealed cube in/out gives up in time, slow world, waypoints),
  `CableArmsPropertyTest` (jqwik: arms are one conductor open exactly at linked edges; perpendicular cables touch iff
  linked both ways; knuckles sit on an edge), `ChiselBrushTest` (every shape, size, axis and hit voxel: inside the
  block, volumes, flat/tunnel boxes, brush validation). MNA lines ending at ground: `sim/mna/TopologyTest`
  (`lineEndingAtGround`, `lineGroundedAtBothEnds`), `MnaPropertyTest.lineCollapseKeepsThePhysics`.
- GameTests `gametest/cases/CableCases.java` (currents vs six-node to 0.1%): "voxel cable straight / inner corners /
  outer corners / tee", their "converted ..." and "reverted ..." variants, "voxel cable burn chain" (exactly one of 6
  burns), "routed cable over a wall", "cable kits", "six-node device hosted in a voxel block", "break a device in a
  mixed voxel block", "thermal cable converted", "cable through a fence and under a slab", "cable up a staircase",
  "chisel a cable hole through a wall", "chisel brush shapes" (flat 16 + 16 voxels, tunnel 256 and put back 64, sneak
  + right-click puts back and keeps the brush, the brush packet: held chisel only, invalid ignored), "fence joins an imitated fence", "converted wall device round two corners"
  (every order), "converted cable under water", lamp round two corners (six-node / converted / reverted).
- `gametest/cases/PipeCases.java`: `pipe_boiler_to_turbine`, `pipe_two_in_one_block`, `pipe_routed_over_a_wall`,
  `pipe_join_mid_run`, `pipe_kit_crafting`, `pipe_burst_vents`, `pipe_valve`, `pipe_adapter_capability`,
  `pipe_valve_by_player`, `pipe_kit_pickup_merges`.
- Benchmark `gametest/bench/CableBench.java` (id `cable_bench`): `runGameTestServer -PelnBench=out.json
  [-PelnBenchLabel=x] [-PelnBenchConvert] [-PelnBenchNoRthCache]`; when set, it is the only test registered
  (`ElnGameTests`). ~20k cables; phases steady, churn, fault, teardown.

## Gotchas

- Javadocs cite `notes/cables/*.md` and `notes/pipes/DESIGN.md`; those design notes are not in the repo.
- `CableConverter`'s class javadoc still says blocks with anything but cables are skipped; since 1e0a6280 devices are
  hosted and only the reasons above skip.
- The light level of node blocks comes from a cache written on the server thread (`node/NodeBlock.java`): the light
  engine thread must not read nodes (a mass conversion killed the light worker, 7dea5329).
