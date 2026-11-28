# Voxel cables and pipes

Scope: the 26.x voxel node (a block whose 16x16x16 voxels hold cable pieces, pipes, fittings and hosted six-node
devices), the routers and kits, and the six-node -> voxel cable converter. Package `src/main/java/mods/eln/node/voxel/`
unless a path says otherwise. Fluid physics and the MNA itself: see simulation.md; NBT layout of other nodes: persistence.md.

## Key classes

| Class | Side | Role |
|---|---|---|
| `VoxelNode` | server | `extends SixNode implements FluidDevice`; uuid `"v"` (registered in `Eln.java` via `NodeManager.registerUuid`). Holds `parts`, a `used` voxel BitSet, `legacy` (six-node NBT for revert), `host` (imitated BlockState), `carved` |
| `VoxelPart` | both | Base of the parts; `read`/`deserialize` dispatch on `kind` |
| `CablePart` (`"cable"`) | both | Polyline on one face (`face` 0..5, 3D value) or a knuckle (`face == -1`, a point on an edge); `TIERS[]` maps tiers to the six-node cable descriptors by name (Signal, LV, MV, HV, VHV, Copper Thermal, Field Bus, then 1 kV at index 7, appended: tiers are stored by index in kits, windings and packets; compare power tiers by `CableSpec.rank`, voltage order `CableSpec.POWER_TIERS`) |
| `PipePart` / `ValvePart` / `SafetyValvePart` / `PipeSensorPart` / `AdapterPart` | both | Pipes and fittings (`PipeSensorPart`: a signal output joined to the nearest signal cable in its block (`VoxelNode.fittingCable`: nearest the line from the fitting to the floor under it; one cable, never all of them), as a control valve's input; no longer made since 2026-11-02, loaded and working where fitted); `AdapterPart.tanks` is the fluid capability (capabilities.md) |
| `CableNet` | server | Groups a node's cable pieces into `Conductor`s and builds their loads |
| `CableRouter`, `PipeRouter` | server | A* routers, pure Java (world behind a `Space` interface) |
| `RoutePlacer`, `CablePlacer`, `PipePlacer`, `KitPreview` | server | Kit UX, one flow for both kinds: `RoutePlacer` holds the route (start, waypoints, finish, undo, lapse); `CablePlacer.ROUTE` and `PipePlacer.ROUTE` are the kinds' specs (where a click ends, routing, laying, the preview's answer); `KitPreview` one preview through the spec, outlines sent as `Eln.packetKitPreview`; a failed click's reason (no route, can't reach a waypoint, can't lay) as `Eln.packetRouteNote`, drawn by client `RouteNoteHud` above the action bar for 3 s, since the preview's length line takes the action bar at once (Baughn 2026-11-18, #521) |
| `KitItem`, `KitRecipes`, `CableKitItem`, `PipeKitItem`, `CableKitRecipes`, `PipeKitRecipes` | both | Kits: length in custom data "kitLen" (1024 voxels max). One shared fill/unroll code for both kinds (`KitRecipes`): coils or plates into a kit, a lone kit back to one block of coil or plates, kits merge up to a full kit with the rest left in the grid. A craft takes one unit a slot. Kits of a kind merge wherever they meet (`KitItem.pour`: `tick` in a player's inventory, the Toolbag's slots, `KitMoveMixin` on shift-click, `onClose` in containers, `onPickup`, kit clicked on kit) |
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
- An electrical conductor's thermal load keeps C Rs at least the slow thermal step (`CableNet.SLOW_STEP`, 0.05 s; both
  raised alike): scaled by `share()`, C Rs falls with its square, and below one step the explicit update swings and grows
  once the piece is joined to a stiff thermal load (a relay's 2-voxel pin cable beside a Power Resistor burnt in half a
  second). Pieces of half a block and more are unchanged.
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

### Stored links (routing phase 3a)

Every part has an id unique in its block, never reused (`VoxelPart.id`, `VoxelNode.nextPartId`, assigned in
`changed()` and after load). Every cable piece keeps joints (`CablePart.joints`, `CableLinks`): what each end (0, 1; a
knuckle's port index) or its body (-1) connects to: CONT (the next piece, here or across a face; on both pieces), SPLICE
(an end on another piece's line; on the branch), TERM_ELEMENT (a device in this or the neighbouring voxel block: slot,
edge, terminal offset), TERM_NEIGHBOUR (a machine in a non-voxel neighbour: EA's edge rule, a bus peer, a face pin),
TERM_FITTING (a valve's or pipe sensor's signal), TOUCH (bodies touching across a face). Networks are still built by
the rules above; the joints are kept alongside:
- **Implied** joints (flag `IMPLIED`): every rule that links a piece records one (`CableNet.build`, `connectElementsToCables`,
  `connectPins`, valves and sensors, `connectVoxel`, `connectTouching`, `pinToCables`, `newConnectionAt`, bus peers,
  `connectPeerPins`). `CableLinks.clearImplied` drops them at each `connectJob` (and the neighbours' into the block),
  so they say what the network had; joints toward a block out of the simulation and burnt pieces' are kept.
- **Editors' joints:** `Laying.lay` writes CONT/SPLICE between consecutive pieces of a route (`CableLinks.laid`; the
  route's free ends stay implied); `removePart` drops joints pointing at the piece (`forgetPiece`), a block broken or
  reverted drops all joints into it (`forget`); a device put on a cable hands the old piece's joints to the cut's pieces
  (`CableLinks.cut`); a recolour keeps id and joints (`copyStateFrom`).
- **Check:** `CableVerify` compares the joints' groups with the live conductors, and their links (electrical, thermal,
  bus; conductor to anything) with the blocks' own link lists; it also counts blocks out of the simulation, blocks never
  connected under stored links (`VoxelNode.linksRecorded`, NBT `lk`) and ends meeting a block out of the simulation with
  no joint. `/elnlab links verify [r]` (report `eln-links.txt`), and the GameTest harness after every test
  (`ElnGameTests.links`).
- **Networks from joints (routing phase 3b, config `[cables] linksBuild`, default true):** `connectFromJoints` runs the
  rules in record-only mode (`recordOnly`: joints, no simulator objects or flags); each joint they give is confirmed for
  this connect (`CableLinks.pass`). Then the links are made from confirmed joints only: `CableNet.buildFromJoints`
  (conductors = same-cable same-face pieces joined by same-block CONT/SPLICE; other same-block joints pair conductors,
  one link per pair), `elementsFromJoints` (face elements' ends, off-centre pins), `fittingsFromJoints`,
  `acrossFromJoints` (CONT one NodeConnection each, TERM_ELEMENT either side, pins across, TOUCH one per conductor pair).
  Links that aren't a cable's (pin to pin, element to element across a face; `nonCableOnly`) and links to non-voxel
  neighbours (`connectLegacy`: EA's edge rule, bus peers, face pins) stay rule-made. Editors' joints no rule gives now are
  logged (`[eln links]`), not linked. `linksBuild = false` is `connectByRules` (3a). Implied joints are still recorded
  afresh at every connect, so the network is the rules' (neutral); the "freeze" (joints persist, rules stop) is a later
  step. `ElnGameTests.linksNeutral` reconnects every test's voxel blocks with the switch off and on and compares links and
  connection flags (`gametest/LinksDiff`); `/elnlab links diff [r]` does the same on a world (`eln-links-diff.txt`).

## Devices off centre (chips)

- A voxel block has `SixNode.SLOTS` element slots: 0..5 are the faces (a device centred on face f, as six-node blocks
  have), the rest hold devices placed anywhere on a face (`SixNodeDescriptor.placeAnywhere()`: the logic and analog
  chips). Such an element keeps its face in `side` and its centre's offset from the face's centre in `ox/oy/oz` (world
  voxels); packets, menus, activation and breaking address it by `slot`.
- `VoxelNode.placeAnywhere` snaps the click to voxels, keeps the model on the face, and `placeError` applies Baughn's
  rule: the model box (`SixNodeDescriptor.footprint()`, chips drawn at `CHIP_SCALE` 0.75: a case 6 voxels across) is
  clear, with a voxel of air all round, except where a pin touches a cable of a matching kind or meets another
  device's pin tip to tip.
- A device that goes anywhere always goes off centre, a click in the middle of a free face too, and connects only at
  its pins: no face slot, so no cable stubs drawn to the face's edges and no edge-rule link to a machine beside it
  (Baughn 2026-11-18, #515: "I'd like all cables to be explicit"; a cable is laid to the machine). Devices placed
  earlier in a face slot stay there and keep working as they were.
- Ports are pins (`VoxelNode.pins`): a tip on the face plane at the model's edge, and a contact box just outside it.
  `connectPins` links a pin to every cable conductor whose boxes take its contact box, and to another device's pin
  that meets it. These devices draw no cable stubs to the block's edges. The cable kit ends at the nearest pin; routes
  keep their usual clearance (`CableRouter.CLEARANCE`). In the block's outline (what the crosshair hits) each pin's box
  is grown by `VoxelNodeEntity.PIN_AIM` (1 voxel), so a click just beside a pin still ends the kit there (Baughn
  2026-11-18, #515); the drawn box, the highlight and the used voxels stay the pin's own.
- A pin at the block's edge reaches the next voxel block (`connectPinsAcross`): a cable there taking its contact voxel,
  or a pin there tip to tip (a NodeConnection, dropped when either block disconnects).
- A device whose model is not a square round its centre gives its model's bounds across the face
  (`SixNodeDescriptor.bodyBounds`, px at `modelScale`) and the turn its render gives the model (`modelTurn`, as its
  `drawPlaced`); its body then reaches toward each edge as far as the turned model, rounded up to an odd number of voxels
  (`bodyExtents`: with the centre on an even voxel, each pin's tip is on an even voxel, where the kit can end), and the
  grey pin boxes are drawn from the model's edge. Without bounds the body is a square of `bodyHalf`. Since the body and
  pins follow the front, a wrench turn of a device off centre skips fronts where the turned device would not fit
  (`VoxelNode.turnError`) and goes to the next clockwise that does (Baughn 2026-11-18, #512); with none it stays and
  says why. A device whose pins are joined to cables turns to the next front at which pins of the same kinds sit where
  those are, so a two-pin device in line turns straight round, or not at all (`VoxelNode.wrenchTurn`). Placing anywhere and its preview honour a device's allowed sides (`allowedOnSide`: the power
  sockets and the anemometer, walls only).
- Placed anywhere (Baughn): chips, the small sensors and sources of a0d38750, and the relays (drawn and measured at half
  scale), the four probes, the wireless receiver, transmitter and repeater, the Electrical Timer, the LED vuMeter, the
  Signal Switch with LED, the pipe sensor devices (see Pipe sensor devices), the power sockets, the Ground Cable, the Hub and the anemometer; the Energy Meter and Advanced Energy Meter (in line on a power run: its sign follows
  which way it faces), the Weak 50V, 50V and 200V Battery Chargers (four power pins, one load) and the Nuclear and
  Standard Alarms; the in-line passives (Baughn, batch 3: the 10A, 25A and Signal Diodes, Power Capacitor, Power
  Inductor, Power Resistor, Rheostat, Thermistor and Electrical Fuse Holder), and the Electrical Breaker (Baughn
  2026-11-17, drawn as the fuse holder). Their models run their base and leads edge
  to edge for the face slot, which is kept as it was; off centre the same model is drawn with the base and leads
  stretched by `SixNodeDescriptor.SHORT_LEADS` (6/8) along them so they end at the pins, 6 voxels from the centre, and
  the body is the core (`inlineBounds`). Their renders don't call `super.draw()`, so they call `drawPins()` themselves.
  Not: the redstone converters
  (Baughn), the analog vuMeter and the power switches (they take the face). A thermal probe's pins join Copper Thermal
  Cable (thermal loads both ways); a Temperature Probe's back pin and a Hub's pins change with their slots after
  placement: the links follow the new kind (a mismatched cable simply isn't joined), but the placement rules are not
  checked again.
- Placed where cables already run (`DevicePlacer`, AdventurAgent: with a cable across the floor every place was
  refused). A click on a cable, or a click refused within 6 voxels of one, tries in order: in line (two opposite pins
  with the same mask, of the cable's kind: the device sits on the cable, turned so they lie along it; the cable is cut
  between the pin tips and the cut-out length comes back to the player as a kit of its tier, a converted piece's `paid`
  shared by length, so cable is neither made nor lost; a cut end on the block's side keeps 2 voxels, since a pin there
  would face the next block); on a free cable end inside the block (one pin of its kind on the end; tried first for a click within 5 voxels of
  an end or anywhere in the block past it, so every aim there gives the same place; if the device doesn't fit beyond the end, the end is drawn back 2
  voxels at a time and the cable comes back as a kit); beside the cable
  (one pin of its kind touching its side: a tap); then at the click as before, else the refusal says what to do. Where
  the cable leaves a choice the player's facing is tried first, then turned clockwise. The preview shows the same plan.
  Breaking a device in line leaves the gap; the two ends stay where they were. A click on a cable places on the face
  the cable lies on (`VoxelNode.placeFace`), not on the side of the block the click hit. Every refusal of
  `SixNodeItem.placeAt` says why on the action bar, and a use key held down (the client repeats it every 4 ticks) does
  nothing on the block just placed in while the repeats go on (each one within 6 ticks of the last), so no "No room"
  follows a placement.
- A cable kit ends at the nearest pin of its own kind (`CablePlacer.deviceEnd` with the kit's tier; a face-slot device's
  edges likewise): a Copper Thermal Cable kit at a Thermal Probe's side pins, a signal kit at its front. The pin boxes are
  coloured by their cable: copper for thermal, pale for signal, dark grey for power.
- Pins that change after placement (a Hub's with its slots, a Temperature Probe's back pin) are published when they
  change (`VoxelNode.pinsChanged`), and one that now lands on something is logged; links follow the kind as always.
- `placeErrorAt` and `DevicePlacer.masks` build a stand-in element to read its pins and unload it after. Devices join
  their static registries (wireless channels and spots, power socket channels) in `initialize`, which a stand-in never
  gets: a Wireless Signal Repeater made here used to join the spots (its empty spot crashed the receivers' process),
  and a Wireless Button left a phantom transmitter on "Default channel" for every probe the preview built.
  `VoxelDeviceCases.ProbesLeaveNoTraceCase` checks every device that goes anywhere.
- Holding such a device shows it see-through where a click would put it (`DevicePreview`, client
  `DevicePreviewRender`, `GlFrame.ghost`), reddish where it may not go. A press of the wrench key (C) with it in hand
  turns it before it is placed (Baughn 2026-11-18, #513; `DevicePreview.wrenchPressed`, sent as the key's down state):
  to the next front clockwise at which the preview shows another place it may go, as a wrench click on a placed device
  skips fronts that don't fit; the turn is kept per player for that kind of device and applied by every placement path
  (`DevicePreview.placeFront`, `SixNode.frontFor`, `DevicePlacer.plan`). Not while aiming at a device in a voxel block
  or another EA block (C there is for turning it). A click on a device in a voxel block places on
  that device's face (`placeFace`).
- `CableConverter.revert` refuses a block holding such devices.

## Pipe sensor devices

- `sixnode/pipetransmitter/PipeTransmitter{Descriptor,Element,Render}` (Baughn 2026-11-02): placed by the pipe sensor
  items (`PipeSensorItem.place`) on any face, always off centre (`VoxelNode.placeOffCentre`, a slot past the faces even
  at the face's centre) with its centre on the pipe routers' lattice (4, 8 or 12 across the face), the lattice point
  nearest the click where it fits, its front from the player's (as the Voltage Probe's) then turned clockwise. Refused
  in mid-air, on a pipe or fitting, and where its body or the column up past its port would come within a voxel of a
  pipe or fitting (`PipeSensorItem.nearPipe`: a lab test put one under a floor pipe, its port inside it).
- The pick (`VoxelPick`, below) gives the device, not the pipe, for a line of sight into its body grown by a voxel or
  within 3 voxels of its port (`PipeTransmitterDescriptor.aimBoxes`): the end of the pipe laid to it covered the body
  from most angles, so Jade and clicks got the pipe. Breaking picks the same way.
- A pipe kit route doesn't start or end at a device's port that already has a pipe (`PipePlacer.portTaken`; a lab test
  laid a second pipe beside the first). The same holds for every machine's fluid port, the Large Boiler's ghost ports
  included (`FluidPlacement.partners`: FluidWorld's one coupling rule, `FluidWorld.couples`, asked of the block in front of the port;
  AdventurAgent: a second route to a tank's piped port "gave up"), in the preview and on the click.
- Its kind shows on the body's top: a coloured plate (pressure blue, temperature red, quality green) with a raised white
  letter P, T or Q for colourblind players. Jade's three headline lines: the reading, the output volts, the full scale. Under them, when the run holds a liquid other than water, its viscosity
  (`EaUnits.viscosity`, todo #590). A pipe's own Jade has the same line (`EaUnits.putViscosity`), third, in the headline,
  when the liquid is thick (200x water or more), else after the contents.
- Its own hidden items are out of the creative tab (`addWithoutRegistry`) and JEI (`compat/JeiPages.hidden`).
- Its port is `PORT_HEIGHT` (4) voxels off the face over its centre (`PipeTransmitterElement.port`), clear of its
  2-voxel body. `PipePlacer.devicePort` makes it a routing end any way round (dir -1) when a pipe kit clicks the
  device, for a route's start and its finish (laid with `lay`; the piece ends capped at the port). `KitPreview` does
  the same.
- The element reads `VoxelNode.pipeAt(port)` (any pipe through or ending at the port) via `FluidWorld.nodeFor` each
  slow step, no fluid of its own; `PipeSensorPart.reading/nextScale/name/unit` are shared with the fitted sensor.

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
  before a write or rebuild), `"hang"`/`"hang2"` (drawn hang), `"j"` (joints: a list of int arrays, sorted; see Stored links).
- Each part `"id"` (in its `p<i>` compound), the node `"nextPartId"`.
- `PipePart`: `"tier"`, `"pts"`, `"burst"`, `"burstDir"`, `"w"`, `"a"`, `"u"`. `ValvePart`: `"at"`, `"open"`.
  `SafetyValvePart`: `"at"`, `"set"`, `"purge"` (a purge valve), `"breather"` (a breather valve; absent on the others, so their saves are as before). `PipeSensorPart`: `"at"` (x, y, z, outlet, bore, kind), `"scale"`, its output. `AdapterPart`: `"face"`. Unknown kinds load as nothing (dropped).
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

## Kits in devices (`CableItems`)

Baughn 2026-11-07: recipes take bare Copper Cable, hand recipes make cable kits, and the 1.12 LV/MV/HV/VHV cable
items leave the creative tab (`ElnDeviceRegistry`: `addWithoutRegistry`). Devices that took a cable item take a kit:
`node/voxel/CableItems` gives a stack's `ElectricalCableDescriptor` (a 1.12 item's, or the kit's spec:
`CableSpec.descriptor(tier)`, the preset's if it has none), its thermal descriptor, tier, blocks (16 voxels) and
`take(stack, voxels)` (a kit piece keeps the spec). Old items stay valid wherever they are, so saves need nothing.
- The breaker has no slot since 2026-11-17 (Baughn): its contacts are a Medium Voltage Cable's, its trip current is set
  in its GUI; a piece in an old breaker's slot sets that current once on load and drops with it.
- Hub slots, the temperature probe's: `SlotSpec.cablePiece` (kind `CablePiece`) takes a block of a kit
  (`ElnSlot.safeInsert` for clicks, `ElnNodeMenu.quickMoveStack` for shift-clicks); the spec sets contacts, trip
  current and thermals.
- Lamp supply: range + blocks (`AutoAcceptInventoryProxy.acceptCable`: a right click adds a block).
- DC-DC converters: a kit's blocks are turns (as the items were one a turn); windings are the efficient way; the
  one-time rewind skips kits.
- Utility poles: a kit of the pole's tier (any cross-section), `ceil(distance)` blocks taken; the line keeps the
  piece (dropped on a break) and its Rs from the spec. Lines saved with 1.12 items still load and drop them.
- Clients get only the item id (`Utils.serialiseItemStack`): a spec kit in a device draws as its tier's preset.
Tests: `gametest/cases/KitDeviceCases`.

## Routers

One router for cables and pipes (Baughn 2026-11-11): `Router` holds the A* core (queue, best costs, expansion limit,
wall-clock deadline) and the moves by the `RouteKind`'s placement; `RouteKind` carries what the search needs
(placement, cross-section, lattice, bend and block costs, clearance, margin, support). `CableRouter` and `PipeRouter`
are the kinds' entry points (same signatures) and output stages (`pieces`, `perBlock`). `RouterDifferentialTest`
holds them to frozen copies of the routers before the merge (`LegacyCableRouter`, `LegacyPipeRouter`).
- SURFACE kinds (cables): lattice 2 voxels, states (point, attach face, last dir), moves along a face / inner corner /
  outer corner (knuckle); `BEND` 4, `BLOCK_ENTERED` 1, `MAX_EXPANSIONS` 400000, budget `PipeRouter.TIME_BUDGET_MS`
  (200 ms).
- A device end (`End.dir` >= 0) is left straight out, or round an outer corner at the pin; it is reached arriving
  along `dir`, or round an outer corner at the pin from the face whose support lies toward `dir` (Baughn 2026-11-18,
  #514: a pin at the edge of a block on a pillar took routes from it, down the pillar's side, but none to it).
  `CableAirCases.ToPinOnPillarCase`.
- Air runs (Baughn 2026-11-11, D5): a SURFACE kind's `airTiers` let it leave its support: cables `RouteKind.CABLE_AIR`,
  up to 10 voxels off any support at +4 per step and up to 20 at +16, never further (the same for every tier, Baughn
  2026-11-12). The state gains `air` (voxels since the last support, carried across waypoint legs). An air stretch is
  one straight line: off an edge, or lifted off a face (a turn as if a wall stood straight ahead, only from a support),
  to where it lands (a supported step, or an inner corner onto a wall). No bends in the air: they made the search ~10x
  dearer. A state reached as cheaply with no more air behind it prunes this one. An air piece is an ordinary flat
  piece whose plane is off any surface; whether it is supported is read live (`CablePlacer.supported`): no device goes
  on its face (`VoxelNode.placeFace`) and no branch starts on it (`endAt`), so air allowances can't chain.
- How air cables hang (Baughn 2026-11-12: energy minimisation once at placement; route, preview and hitbox stay as
  routed): `CableHang` finds the free spans (from where the cable leaves a support to where it lands, round corners: a
  drop past an air block onto a floor is one span) and searches, for each, where it leaves the surface before (peeling
  back up to 8 voxels), where it lands after, and a curve between: a Hermite curve along the surfaces' directions there,
  lowered by a smooth bump. Score over the same stretch of route for every candidate: gravity per voxel of length,
  bending (turn squared per voxel), tension per voxel of length (so a short slant wins over a vertical drop and a
  right angle; length may exceed the route's by 5 %, so a straight bridge can sag), and a cost per voxel peeled;
  candidates whose cable would touch a blocked voxel are out (none fits: drawn as routed). Coarse grid, then a local
  search; ~5-10 ms a span. Each piece stores its share (`CablePart.hang2`: the arc range of its own polyline the curve
  replaces, the curve's points, normals and tangents; NBT `hang2`, its own stream slot after the old `hang` slot).
  `VoxelGeometry.cable` draws the boxes outside those ranges and `sweep` carries the cross-section along the curve with
  a turning frame. Cables laid by the 11-12 jar keep their `hang` (drawn through the same path).
- FREE kinds (pipes): lattice 4, `BEND` 6, unsupported steps cost more (`UNSUPPORTED`), off-lattice ports jog onto the
  lattice, waypoints as legs; gives up on the wall-clock budget (Baughn: it runs on the server thread).

- Pipe junctions (Baughn 2026-11-11, LabAgent's review): where a route joins a pipe's body or another pipe's end,
  `VoxelNode.addJoined` cuts the pipe and adds a `JunctionPart` at the point: its own fluid node, never lumped, 3/4
  block of its bore in volume (smaller nodes troubled the core: #374, #370), a wall as a pipe's, and filled with its
  share of the pipe it was cut into (after `FluidWorld.writeBackOf`). `fluidJoins` never joins at a junction;
  `fluidLinks` links each arm's end to it (the end's own conductance and height; a level end an opening as tall as the
  bore, the junction an opening as tall as its body, so a vertical arm gets the counter-flow pair: a sealed riser drains,
  `FluidDevice.Link.spanA/spanB`). So each branch is its own run. It takes up to four arms, isn't aimed at on its own
  (the pick gives the arm nearest the place looked at: `VoxelPick.armNear`), and goes when fewer than two arms are left, its contents with it (Baughn
  2026-11-25: given to the last arm, they squeezed its run to thousands of bar). Over its arms' rating an arm at it
  bursts. Old saves' coinciding ends (no part) stay lumped until the phase 3 converter.
  `FluidWorld` reports any node under `MIN_RELIABLE_VOLUME` (1e-6 m^3) once (Baughn). Tests: `JunctionCases`.

## Laying and the kit UX

- `Laying`: all or nothing for both kits. Every new part is checked against the world and the route's other parts in
  its block (pieces of one cable may overlap where they touch), then all are placed; a late failure takes back what
  was placed. A pipe's joins (`Laying.Join`) are part of the same lay: checked first (`VoxelNode.joinError`), put in
  after the pieces (`addJoined`), and a refused one takes the pieces back.
- Both kits are one flow, `RoutePlacer` (Baughn 2026-11-11: one kit UX; 2026-11-21: cables have the better UX, pipes
  move onto it): start at a port, on an old run (a branch) or any surface; waypoints are points on clicked surfaces
  (pipes: in the block in front, as near the face as the lattice allows, `PipePlacer.surfacePoint`); finish at a port,
  on a run, or by clicking the last waypoint again (the run ends there, a pipe capped); never back at the start spot,
  neither as the first waypoint nor as the finish; sneak-click takes back a waypoint (and draws the route left as
  particles) or cancels; a failed click's note says how to cancel the route; a route lapses (`RoutePlacer.lapse`: another
  dimension or item, a minute idle, too far), checked each tick and on each click. A player has one route, of either
  kind. The pipe kit's old one-straight-block click is gone (Baughn). A pipe route may start and end on pipes
  (`PipePlacer.layJoined` with both joins).
- Near a join (a route started or finished on an old run) the route may run inside the old run (`JoinRun`, todo #516;
  Baughn: "let the pathfinder pass through same-type voxels of the cable within 4-5 voxels of the waypoint"): within
  `JoinRun.REACH` (6) voxels along it from the join point (across block faces too), the old run's voxels are taken out of the
  used voxels the router sees (`RouteView.freeing`; the view's `parts` give the old run, a snapshot's too), and the route
  is cut where it leaves the old run's centre line (`Spec.route`, so the preview's outline, the length and the kit's cost
  are of what is laid). The new run starts or ends there: a splice, or at the old run's end its continuation (a pipe's
  join moves there, `Routed.startJoin/endJoin`; on a block's face it is a port coupling, no junction). Only the kit's own
  cable (same tier, spec and face) or any pipe (the join rule) is run through; a pipe's fittings and junctions are not.
  Before, a route from a point just short of a cable's end laid a piece over its last voxels, and a pipe hopped over a
  dead stub. Routes along the old run past its end from further away still run beside it (a parallel run, not
  overlapping): `JoinRouteCases`' unchecked rows. A route wholly inside the old run lays nothing: "That path is already laid" (`RoutePlacer.INSIDE`, wording Neon); a
  waypoint whose route so far lies inside it is taken (nothing laid yet). A route that comes back onto or across the old
  run away from its cut (after a waypoint; `JoinRun.crosses`) is routed again without the freed voxels, and if it still
  crosses (through the free cube round the join point) it is refused: "it crosses the cable it joins" (a pipe route that
  passes back through the joined pipe's block, over or under it, says the same for the pipe). A pipe route joining an
  old pipe doesn't rest against it within `JoinRun.REST_REACH` (2 blocks) of the join (`PipeRouter.Space.rests`, which
  is `blocked` elsewhere): the soft support drew a short loop's first leg along beside the old pipe for a block
  (AdventurAgent). A pipe's moved
  junction is checked when routed (`VoxelNode.joinError` on the view's parts and used voxels), so the preview refuses as
  the click does. A cut at an old pipe's end inside a block makes a two-armed junction there, as a join at that end
  always did, not a plain straight pipe. `JoinRun` walks the old run voxel by voxel on the route's view (a preview
  snapshot off the server thread too, through other branches' splices, at most `REACH` voxels); the toolbag shears'
  walk (`ToolbagShears.run`) follows whole pieces by their CONT joints on the live level and stops at splices, so the two
  stay separate. The snapshot shares the part objects; what JoinRun reads of them is final.
- Per kind, in its spec (`RoutePlacer.Spec`): the ends (`endAt`: a cable's connectors and edge rule, a pipe's ports,
  with "Its port already has a pipe" for a machine's piped port or a pipe sensor's), routing (surface or free), laying (cables with their hang,
  pipes with their junctions), the join rule (cables: the same tier or two power cables; pipes: any pipe tier, the
  junction takes the lower; Baughn 2026-11-21), the parts' outline for the preview (`boxes`), mending a burnt cable run
  by clicking it (cables; a pipe bursts), the particles' colour, what the start message names as a finish, and the
  preview's markers (a pipe's start always the port cube). A route's path is the spec's type (`Spec<P>`, `Routed<P>`).
  The placers' end is `CableRouter.End` for both (a pipe's: attach -1, dir as a face; `PipePlacer.end` adapts it,
  `PipeEndAdapterTest`); the routers and their direction orders are unchanged.
- The preview (`KitPreview.tick`, one for both): the start before a route, then the route to the aim on the preview
  thread over a snapshot (`KitPreview.Job`, one `KitPreview.answer` through the spec: "Can't reach: ...", "Too long:
  ...", or "Cable: 7.5 blocks") ending with what the click would do: " — connects" at a machine, " — joins", " — open
  end" at the last waypoint, or " — waypoint" (wording: Neon). It shows the click's refusals too: back at the start
  spot (`RoutePlacer.startRefusal`), a pipe sensor's port that has a pipe. Slow steps go to the log (`KitPreview.slow`) and the
  route debug kit (`RouteDebug`, `/elnlab debugkit` on a cable or pipe kit; a pipe's replay token starts `p_`) reports
  each preview, for both kinds.
- Breaking a pipe deletes what was in it (Baughn 2026-11-25), whichever way it goes (mined, the shears, `removePart`, the
  block destroyed): the rebuild writes the old run's lump into all its pieces and sums the new runs from those left, so
  the rest keep their shares at the same state and the piece's share is gone. Tests: `PipeBreakCases`
  (`pipe_break_deletes_<water|steam>_<player|nobody|shears|end_join|tee>`, `pipe_break_deletes_water_burst`: the runs left
  hold exactly their shares, at the same pressure and temperature, also after a reload).
- Tests: `RouteLeakCases` (lay/break/re-lay rounds leave nothing in the simulator or fluid world),
  `PipeCases` (`pipe_laying_all_or_nothing`, `pipe_kit_starts_anywhere_and_caps` with the start-spot refusal,
  `pipe_kit_branches_pipe_to_pipe`, `kit_preview_says_what_the_click_does`, `pipe_kit_refuses_a_taken_port`),
  `HandbookFixCases` (a cable route can't end where it started), `CableRouteLapseTest`, `JoinRouteCases` (routes started
  or finished on an old cable or pipe near its end, at its end, from the side: no piece over it, no dead stub, one
  network; rows logged unchecked for routes along it past its end and an MV kit on an LV run).

## Toolbag

- `ToolbagItem` (`eln:toolbag`; Baughn 2026-11-18, todos #510/#517): a bag of cable and pipe kits used in place of a
  kit. Its contents are a `DataComponents.CONTAINER` of `SLOTS` 16: kit slots 0..14 (three rows of five; cable and pipe
  kits only) and `SHEARS_SLOT` 15 (vanilla shears: the cutting below). `ToolbagMenu` /
  client `ToolbagScreen` as the battery belt's (written back on every change, its hotbar slot locked). The chosen kind
  (`Kind`: pipe or cable tier, a power cable's mm² index and colour) is in its custom data (`toolbagKind`).
- Placers read the main hand through `KitSupply` (a kit, or the bag's kits of its chosen kind taken together:
  `ToolbagItem.supply`, its length their sum, `take` from the first slots on, emptied kits removed).
- Right-click: the air opens it (sneaking: the radial); a cable or pipe (no route on) takes its kind (a cable laid
  without a spec is its tier's preset; colour must match) and starts or finishes a route from it as that kit would,
  else "No ... kit in the toolbag" (`KitPreview.note`); anywhere else with no route on, or sneaking, opens the radial
  (`packetToolbagRadial` -> client `ToolbagRadial`, `RadialMenu` with `stayOpen`): kind or voltage, then cross-section,
  then colour, only what it holds; a pick that leaves no choice beyond, or the centre, sends `packetToolbagChoice`.
  The click that opened it is kept (`PENDING`, 30 s, same dimension, within 10 blocks) and replayed as the route's
  start once the kind is chosen. With a route on the radial adds Undo and Cancel (the bag's sneak-click is the radial),
  and a new kind restarts the route from its start click (`STARTED`), its waypoints kept where they fit: a cable's for
  another cable kind, a pipe's for another pipe kind (a cable's face points and a pipe's block-middle points don't carry
  over between the two; "Restarted as ... — waypoints cleared"; within a family "Now ...; waypoints kept.").
- Client `ToolbagHud`: the chosen kind and a terse usage line above the route note while the bag is in hand.
- A kit or bag click must be within reach, its point on or in the clicked block (`CablePlacer.reachable`; else "Out
  of reach"; the player's reach plus `REACH_SLACK` 0.25, where the server itself takes a block more): AdventurAgent's
  clicks past reach made waypoints. With the bag in hand
  the client sends its sneak state before a click (`ClientKeys.beforeClick`), so a sneak-click opens the radial even
  when shift goes down in the same tick. Its tooltip lists the kits by kind only (the container's own list hidden).
- Shears (`ToolbagShears`, #517; the piece clicked or aimed at is the one the line of sight is on, the pick above: with
  two cables side by side, the one Jade names): with shears in the bag, a left-click on a cable or pipe (no block is broken with the
  bag) starts a cut; the run through that piece is found once (a cable by its CONT joints, stopping at a device
  joint, at an end where several pieces or a splice meet, and before a piece another's end splices onto; a pipe by its
  ends, in the block or across a face, stopping at fittings and where several pipes meet), and the span from the first
  piece to the one aimed at is outlined red (KitPreview's outline packet, sent from `KitPreview.tick` instead of the
  route preview) with its length on the action bar. A second left-click removes the span (`VoxelNode.partItem` for
  each: burnt pieces give nothing), puts it back into the bag (its kits of that kind topped up, then empty slots, the
  rest dropped at the player) and wears the shears by one. A right-click, or the bag leaving the hand, cancels.
- `ToolbagCases`: slots; a cable click lays its kind across two kits; a kind it lacks; the radial starts at the click
  and a new kind keeps the start; a pipe click; a new pipe kind keeps the waypoints, a cable kind doesn't; Cancel; reach, sneak on a pipe route, Undo; the shears (a span back
  into the bag, not past a junction; a full bag drops; no shears; cancel; a pipe run).

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

## The pick: what the crosshair is on

One function, `VoxelPick.pick(Scene, eye, look, hit)` (pure, both sides; Baughn 2026-11-28), answers it for Jade, the
outline (`client/render/VoxelOutline`), breaking and its tool (`VoxelNodeBlock.aimedBy`), right-click
(`VoxelNode.onBlockActivated`), middle-click, and every item clicked on the block: kits and their preview, the
Toolbag and its shears, valves, pipe sensors, devices. It returns `VoxelNode.Aimed(element slot, part index)`, one of
them set, or neither. Before, there were five copies (Jade and clicks, the outline, breaking, the breaking tool, the
kits' `elementAt`/`partAt` order): Jade named the last cable laid within 2 voxels of the hit point, the outline the
first.

- The `Scene` is the block's boxes: the parts, each slot's device boxes, a pipe sensor device's aim boxes, what's left
  of the imitated block (from the block entity's kept shapes on both sides). `VoxelNode.scene()` on the server,
  `VoxelNodeEntity.scene()` on the client (from the last publish; a device's boxes by `VoxelNode.deviceBoxes`).
  Callers use `VoxelNode.aimed(eye, hit)` / `VoxelNodeEntity.aimed(eye, hit)`, world coordinates.
- It follows the line of sight, not the hit point. The hit is on the block's outline, which stands 2 voxels proud of
  every part (`VoxelNodeBlock.outlineOf`), so seen at an angle the ray stops on one cable's outline while the crosshair
  is drawn over the next. The line runs from the hit point on (nothing of the block is in front of it), from the eye
  through the hit, to where it leaves the block or meets the imitated block.
- The eye is the one the ray was traced from. Client: the camera entity's at the frame's partial tick
  (`VoxelOutline.aimed`, for the outline and Jade's tooltip alike; the eye at the tick's end is up to a tick's walk
  away, and the line swung between neighbouring cables while the player moved). Server: `VoxelNode.eyeOf(player)`, a
  real player's eye (its idea of the look lags, the hit is the client's own, so eye to hit); `VoxelNodeBlock.aimedBy`
  traces its own ray for breaking and middle-click, which send none. Anything but a real player (a fake player, a
  deployer; GameTests' clicks): no eye, the point form.
- An eye that could not have traced the hit is not believed, and the pick is by the point (`VoxelPick.believable`,
  the one guard: another mod's fake `ServerPlayer`, a stale eye, a rider whose place on the server lags): farther
  than 8 blocks from the hit, or with one of the block's own boxes (a part's, a device's, the imitated block's)
  between it and the hit. The hit is where the ray first touched the outline and every box lies inside the outline,
  so an eye behind or inside what was hit fails that; it is the "does the clicked face look at the eye" test without
  needing the face. There is no "the picked part's outline must hold the hit" rule: nearly every angled pick breaks it.
- A click kept and replayed keeps its eye: the Toolbag's `Click` (the radial's choice, another kind chosen
  mid-route) holds the eye of the click and `RoutePlacer.use(…, eye, …)` takes it, so the route starts again from
  the same cable or pin wherever the player stands by then. The kit preview's cache key holds what is aimed at
  (`KitPreview.tick`), since that changes with the eye while the crosshair stays in one cell.
- Rule: the box it enters first (true boxes, grown 0.05); if it enters none, the part or device whose own box is
  nearest the line, among those whose outline (2.05 voxels) it passes through. Entered together or equally near: a
  pipe sensor device's aim box, a fitting (a valve's box takes in its pipe), a pipe or cable, a junction, a device;
  then the lower first box or slot. Never the order in the parts list: the client isn't sent part ids, and both sides
  must agree.
- Never nothing: a ray that stopped on the block's outline is on some part's or device's outline (the outline is
  the union of their boxes grown 2 voxels, and reach is counted the same way, per axis: a box grown 2.05, so the far
  corner of an outline, 3.46 voxels off in a straight line, is in), so the pick gives one, by the line and by the
  point alone; only with an imitated block can it give neither. GameTest `voxel_pick_never_nothing` (the edges, and
  every ray of a grid of eyes and directions on three blocks). What reads as a dead zone at the end of reach is not
  the pick: a ray that runs out of reach in the air of the block's cell is a MISS whose position is the voxel block
  (seen from 4 to 5 blocks, aimed at the floor just past the far cable: it ends a little over the outline), and on a
  miss vanilla draws no outline, asks Jade nothing and mines nothing, as for any block
  (`voxel_pick_at_the_end_of_reach`).
- A junction is never the answer: its arm whose first piece (from the junction to the pipe's next corner) is nearest
  the place the line met it (`armNear`).
- The imitated block hides what is behind it: a line that meets it first is on nothing of ours (its own outline and
  breaking). When the hit is on the imitated block's own face the line is that point alone, and the nearest-within-
  2.05 pass can still give a part: a cable in a channel carved into the block is named and mined from the block's
  face within 2 voxels of it, as before.
- The point form (`VoxelNode.aimed(x, y, z)`, `partAt(x, y, z)`; `aimed(null, hit)`) is the same rule for a point: the
  box holding it, else the nearest of those within 2.05 voxels. It is for a point with no viewer: a fake player's
  click, a click the code works out (`CablePlacer.endAt` beside a machine), GameTests. Item clicks pick WHAT is
  clicked by the line (`CablePlacer.endAt`/`isDevice`/`noEnd`/`repair`, `PipePlacer.joinPoint`/`deviceAt`,
  `ToolbagShears.pieceAt`, `ToolbagItem.kindAt`, `ValveItem`, `SafetyValveItem`, `PipeSensorItem`, `placeFace`,
  `DevicePreview`). WHERE on it (the branch point on a cable, the lattice point on a pipe for a join or a valve, the
  nearest pin) is nearest the place the line met the picked thing (`VoxelPick.Met`, `VoxelNode.met`: where it
  entered its box or came nearest), not the hit: the hit is on the outline of the first thing the ray touched, which
  may be another part's (a pipe crossing over a floor cable seen low: the pick is the cable, the hit on the pipe's
  outline at the other end). With no eye, and for a device being placed (its own spot on the face), it is the hit.
  For one cable or pipe alone in a block the place is the same or nearer what the crosshair covers (the hit stands 2
  voxels proud, so at an angle it was up to a few voxels short). So with a kit or the Toolbag in hand the shears
  cut, and the kit joins, the cable Jade names. A point 0.3 voxels off a
  device's pin and 1.7 from a cable is the device (the outline is fattened round pins for kit clicks, `PIN_AIM`).
  `VoxelNode.holdingForTests` (the exact pass alone) is for GameTests of a model box's extent; `placeFaceForTests`
  and `addSensorNearForTests` are the point forms GameTests use.
- Jade (`compat/jade/VoxelProbe`): the server sends an entry for every device and part of the block (`elnParts`: the
  device's `slot`, or the part's `id` and first `box`; title, lines, headline count), 64 at most, in the order of the
  asker's line of sight (`VoxelPick.ranked`: the one aimed at, then the nearest), and last a keyless entry for the
  block as a whole. The client chooses the entry each time the tooltip is built, with the call the outline makes
  (through the hook `VoxelProbe.clientAimed`, set by `client/ElnClient`: the plugin names no client class), so
  the tooltip follows the crosshair within a block without waiting for the server and names what is outlined. It
  matches a part by its first box (parts don't overlap). One with no entry (past 64, or laid since the answer) shows
  the block's lines as a whole until the next answer.
- The outline is still off while a kit, the Toolbag or the chisel is held.
- GameTests `gametest/cases/VoxelPickCases.java`: `voxel_pick_parallel_cables`, `_fittings`, `_junction`,
  `_behind_imitated_block`, `_device_beside_cable` (rows of lines of sight and points; each also checks the client's
  view: parts read back from their publish bytes, devices from their published place),
  `voxel_pick_never_nothing`, `voxel_pick_at_the_end_of_reach`,
  `voxel_pick_outline_jade_and_break_agree`, `voxel_pick_item_clicks_follow_the_line` (clicks with an eye: kit,
  shears, Toolbag replay, valve), `voxel_pick_jade_entry_per_part`; the sensor device's rays in
  `pipe_transmitter_lab_fixes`.

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
  + right-click puts back and keeps the brush, the brush packet: held chisel only, invalid ignored), "fence joins an imitated fence", "a short cable piece beside a resistor stays cool", "converted wall device round two corners"
  (every order), "converted cable under water", lamp round two corners (six-node / converted / reverted).
- `gametest/cases/VoxelDeviceCases.java`: the devices placed anywhere since a0d38750: sizes and pins of each (logged), a
  click in the middle of a free face still uses the face's slot, lopsided bodies turn with the front; relays switch a
  resistor's current, probes read through their pins, a thermal probe reads a hot Copper Thermal Cable, temperature probes
  by their slot, wireless link, timer, LED vuMeter and switch, ground and hub currents, sockets and anemometer walls only;
  devices put in line on an LV run (relays switch, a probe reads, the cable kept) and tapping a cable beside them.
- `gametest/cases/PipeCases.java`: `pipe_boiler_to_turbine`, `pipe_two_in_one_block`, `pipe_routed_over_a_wall`,
  `pipe_join_mid_run`, `pipe_kit_crafting`, `pipe_burst_vents`, `pipe_valve`, `pipe_adapter_capability`,
  `pipe_valve_by_player`, `pipe_kit_pickup_merges`.
- `gametest/cases/PipeTransmitterCases.java`: `pipe_transmitter_floor_pressure` (a route laid to the port, pressure,
  volts on its signal cable, Jade, scale step kept by NBT), `pipe_transmitter_wall_temperature` (wall, signal pin down,
  temperature; a ceiling one not connected), `pipe_transmitter_break_and_refuse` (lattice snap, drop, pipe stays,
  refused in mid-air and on a pipe), `pipe_transmitter_joins_a_run` (a route from its port joins a run mid-way), `pipe_transmitter_lab_fixes` (beside or under a
  pipe, aim, a second route refused, Jade's headline, the creative tab).
- Benchmark `gametest/bench/CableBench.java` (id `cable_bench`): `runGameTestServer -PelnBench=out.json
  [-PelnBenchLabel=x] [-PelnBenchConvert] [-PelnBenchNoRthCache]`; when set, it is the only test registered
  (`ElnGameTests`). ~20k cables; phases steady, churn, fault, teardown.

## Gotchas

- Javadocs cite `notes/cables/*.md` and `notes/pipes/DESIGN.md`; those design notes are not in the repo.
- `CableConverter`'s class javadoc still says blocks with anything but cables are skipped; since 1e0a6280 devices are
  hosted and only the reasons above skip.
- The light level of node blocks comes from a cache written on the server thread (`node/NodeBlock.java`): the light
  engine thread must not read nodes (a mass conversion killed the light worker, 7dea5329).
