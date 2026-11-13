# Networking and GUI

Scope: EA's byte protocol on NeoForge payloads, the server -> client publish flow, client -> server GUI packets,
device menus (`eln:node`), screens and the JSON layout engine. Node internals: architecture.md; NBT: persistence.md.

## Channel (both sides)

| Class | Side | Role |
|---|---|---|
| `src/main/java/mods/eln/network/ElnNet.java` | both | One payload `ElnNet.Data(byte[])`, id `eln:data`, `playBidirectional`, version `"1+<build commit>"` (`BuildInfo`: the commit baked into the jar by build.gradle, so a client built from another commit is refused at login; Baughn 2026-10-25). The bytes are the 1.12 packet unchanged: first byte = EA packet id, then whatever a `DataOutputStream` wrote. |
| `src/main/java/mods/eln/bike/BikeCrash.java` | both | `eln:bike_crash`, client -> server on the same channel: the rider's client says it crashed the bike it rides (`docs/bike.md`). |
| `src/main/java/mods/eln/robotics/servo/SegmentPayload.java` | both | `eln:servo_segment`, server -> client (`playToClient` in `ElnNet`): an elevator car's motion, (entity id, origin of position 0, axis, rail side, segment from/v0/to/vmax/accel, ticks already elapsed). Sent with `PacketDistributor.sendToPlayersTrackingEntity` when a move starts or changes (call, sag retarget, stop, fault: a zero-velocity segment where it stopped), and in the car's pairing bundle (`ElevatorCar.sendPairingData`, after its spawn packet) so a player who starts tracking it sees the move. The client counts on from `elapsed` by its own clock (`robotics/client/CarCarry`); the car has no vanilla position sync (no update interval, its interpolation handler swallows position packets). Handler: `SegmentPayload.clientHandler`, set by `robotics/client/RoboticsClient`. |
| `src/main/java/mods/eln/network/ServerPacketHandler.java` | server | Sets `ElnNet.serverHandler`; hops to the server thread, then `handle()`. |
| `src/main/java/mods/eln/client/ClientPacketHandler.java` | client | `packetRx`, set as `ElnNet.clientHandler` by `client/ElnClient`; runs on the client thread. |

Packet ids are constants in `src/main/java/mods/eln/Eln.java` (`packetPlayerKey` 14 ... `packetChiselBrush` 24).
Handled today:
- client -> server: `packetPlayerKey` (to `server/PlayerKeys`), `packetPublishForNode` (GUI actions),
  `packetChiselBrush` (size byte, shape byte: `ChiselItem.brushPacket`, sent by `client/ChiselRadial`; the server's
  `ChiselItem.applyBrush` sets it on the player's main-hand stack only if that is a chisel and `ChiselBrush.valid`;
  anything else is ignored, the packet never names a stack).
- server -> client: `packetNodeSingleSerialized` (publish), `packetForClientNode` (node packet), `packetPlaySound`,
  `packetKitPreview` (`node/voxel/KitPreview` -> `client/render/KitOutline`). Other ids are defined but unused.

Send helpers: `ElnNet.sendToPlayer`, `ElnNet.sendToTracking(level, x, z, bos)` (players tracking the chunk),
`ElnNet.sendToServer` (client only). `Utils.sendPacketToClient` and `NodeBase.sendPacketToAllClient(bos, range)`
wrap `sendToPlayer`.

## Server -> client: publish

```
element.needPublish()  -> NodeBase.setNeedPublish(true)
NodeServer.tick (server tick)            node/NodeServer.java
  every flagged node      -> NodeBase.publishToAllPlayer() -> getPublishPacket() -> ElnNet.sendToTracking
  node behind an open INodeContainer (ElnNodeMenu) -> publishToPlayer every (1 + refreshRateDivider) ticks
getPublishPacket = [packetNodeSingleSerialized, x, y, z, dim(int), nodeUuid(UTF)] + node.publishSerialize
  SixNode.publishSerialize / TransparentNode.publishSerialize -> element.networkSerialize(stream)
client: ClientPacketHandler.forNode -> dimension + uuid check -> INodeEntity.serverPublishUnserialize
  -> SixNodeElementRender.publishUnserialize        (six-node)
  -> TransparentNodeElementRender.networkUnserialize (transparent)
```
- Naming trap: on the client, the transparent render's publish reader is called `networkUnserialize`; on the server,
  `networkUnserialize` is the client -> server GUI reader. Same name, opposite directions.
- Element-to-client one-offs: `packetForClientNode` via `NodeBase.preparePacketForClient`, e.g.
  `TransparentNodeElement.sendStringToAllClient`; read by `serverPacketUnserialize` on the render.
- The render must consume the publish bytes exactly: six-node faces are read one after another, so a short read
  garbles the faces after it.

Initial sync / chunk load: `node/NodeBlockEntity.getUpdateTag` (server) puts the whole publish packet as a byte array
under `"elnDescription"` (`NodeBlockEntity.DESCRIPTION_KEY`); `getUpdatePacket` is a `ClientboundBlockEntityDataPacket`.
The client's `handleUpdateTag` / `onDataPacket` replay it through `applyDescription` (header check, then
`serverPublishUnserialize`). If `level` is still null it is kept in `pendingDescription` and applied in `onLoad`.
That key exists only in the network tag and is never saved to disk.

## Machines at other mods' block entities: attachment sync

A hosted machine (`ElnApi.machines()`) has no EA block entity to publish through (`MachineNode.publishToAllPlayer` only
clears the flag). Its client view comes from NeoForge's attachment sync of `eln:machine` (`MachineData.SYNC`):
initial data rides with the chunk, and `MachineNode` calls `be.syncData` after a step when the state changed, else at
most once a second (20 ticks) while a number moved more than 1% (`Snapshot.near`). Payload: a boolean (hosted), then
the state ordinal, volts, voltage ratio, amps, watts, buffer, buffer capacity, celsius (doubles) and the last trip
(boolean, state, measured, limit, burns). Chosen over an EA payload for its initial sync and tracking for free; the
cost: `syncData` goes through `blockChanged`, so each sync also sends a block update and the block entity's own update
packet (vanilla furnaces have none), hence the throttle. Client: `of(be)` is a `ClientMachine` over the snapshot
(UNPOWERED and zeros before the first); takes, gives, signals, bus and respec throw.

## Client -> server: GUI packets

Header: `NodeBlockEntity.preparePacketForServer` writes `packetPublishForNode, x, y, z, dim, nodeUuid`, then
- six-node: `SixNodeElementRender.preparePacketForServer` adds slot byte (SixNodeElement.slot; 0..5 = the face) + element id short;
- transparent: `TransparentNodeEntity.preparePacketForServer` adds the element id short.

Helpers (client, on the render): six-node `clientSetFloat` (1 or 2 floats), `clientSetDouble`, `clientSetString`,
`clientSetInt`, `clientSetByte`, `clientSend(id)`; transparent `clientSendBoolean`, `clientSendId`,
`clientSendString`, `clientSendFloat`, `clientSendInt`, `clientSetGrounded`. Kotlin screens use the private `send()`
in `src/main/java/mods/eln/client/gui/KotlinDeviceScreens.kt`. `NodeBlockEntity.sendPacketToServer` does nothing off
the client (tests' stand-in entities).

Server: `ServerPacketHandler.handle` -> node by coordinate, uuid must match -> player must be in the node's level and
within 16 blocks (`MAX_DISTANCE_SQ`, 26.x hardening) -> `NodeBase.networkUnserialize(stream, player)`:
- `SixNode.networkUnserialize`: side + element id must match -> `SixNodeElement.networkUnserialize`; else logs
  "sixnode unserialize miss".
- `TransparentNode.networkUnserialize`: element id must match -> `TransparentNodeElement.networkUnserialize`, which
  handles `unserializeGroundedId` itself and returns any other id byte for the subclass to switch on.

What the element does with the payload is its own check: a modified client sends what no screen would. Handlers read
through `src/main/java/mods/eln/network/ClientValues.java`: a value that is not finite changes nothing, a slider's
value is clamped to the slider's ends (the screen's numbers), a typed number is taken if finite, an index outside its
list is ignored, a string is cut to 64 characters (EaField's limit). Values an older handler may have saved are
clamped again on load (`ClientValues.load`). A handler that throws anyway is caught in `ServerPacketHandler.handle`
(one warning naming the player). `GuiPacketFuzzCases` sends every handler bad values.

## Menus (`eln:node`)

| Class | Side | Role |
|---|---|---|
| `src/main/java/mods/eln/gui/ElnNodeMenu.java` | both | The one menu type (`ElnRegistry.NODE_MENU`). Device slots from a `MenuSpec`, then player inventory 27 + 9. Open data: pos, slot (six-node: the element's slot, 0..5 = centred on that face; transparent: the side), kind (`SIX`/`TRANSPARENT`), descriptor id, the worn slots' keys. Client copy: `node == null`, device inventory is a stand-in `SimpleContainer` synced by vanilla slot sync. |
| `src/main/java/mods/eln/node/NodeMenus.java` | server (`open`, `menu`), both (`spec`, `descriptor`) | `NodeBase.onBlockActivated` -> `hasGui(side)` -> `NodeMenus.open`. Pads a too-small element inventory with a `SimpleContainer`. `openedHook` for GameTests. |
| `src/main/java/mods/eln/gui/MenuSpecs.java` | both | Descriptor class -> `MenuSpec` (walks superclasses); no entry = `MenuSpec.SCREEN_ONLY`. The 1.12 container constructors as data. |
| `src/main/java/mods/eln/gui/MenuSpec.java`, `SlotSpec.java`, `ElnSlot.java` | both | Slots, player-inventory position, panel size, `refreshRateDivider` (-1: no periodic publish). `SlotSpec` kinds: `any`, `descriptor`, `six`, `filter`, `battery`, `lamp`, `regulator`. `ElnSlot` = `mayPlace` from the filter, `getMaxStackSize` = spec limit. |
| `*Container` classes (23, e.g. `src/main/java/mods/eln/sixnode/hub/HubContainer.java`) | both | Only slot-id constants now; the elements index their inventories with them. |

Rules:
- `stillValid`: the node is not destructing, is still the node at `pos`, still has the element the menu was opened on
  (a six-node face broken while the block stays closes it), and the player is within 8 blocks.
- Shift-click (`ElnNodeMenu.quickMoveStack`): device slot -> a worn slot, else player inventory (from the end); worn
  slot -> device slots, else player inventory; player slot -> device slots (first accepting), then worn slots, else
  main inventory <-> hotbar. Returns `EMPTY` unless something moved, because 26.x repeats the move while the result
  is non-empty. Covered for the charger's worn slots by `charger_worn_slots`.
- Worn slots (`MenuSpec.wearing`, only the battery charger; Baughn, todo #177): after the player inventory, the
  player's Curios `belt` and `head` slots (`CuriosCompat.WEARABLES`: Curios' own `CurioSlot`, so Curios' rules on
  what goes in and out apply), at most `wear().size()`. The server picks them; their keys (`belt#0`) ride at the end
  of the open data, so the client builds the same number of slots (`ElnNodeMenu.NoSlot` for a key it lacks). Layout
  ids `wear0`.. place them. `ElnNodeMenu.wearables` is a hook so the GameTest can stand in for Curios.
- Server authoritative: the client menu has no node. Slot contents use vanilla menu sync; every other GUI value goes
  out as an EA packet and comes back in the next publish. Screens never change device state locally.

## Screens (client)

- `client/ElnClient` registers `ElnScreens::create` for `NODE_MENU` and `XRayScannerScreen::new` for `XRAY_MENU`, calls
  `ClientScreens.registerAll()`, and sets `ElnNodeMenu.clientLayout = ElnLayouts::apply`. That is how the client menu
  takes its slot positions and panel size from the layout; the server keeps `MenuSpec`'s positions.
- `src/main/java/mods/eln/client/gui/ElnScreens.java`: factory by descriptor class (superclass walk). It needs the
  element render at `menu.pos`/`slot`, otherwise it falls back to the generic `ElnScreen` (slots only).
- `src/main/java/mods/eln/client/ClientScreens.java`: every layout name and screen factory. Analog chips share
  `AnalogChipDescriptor`, so they are chosen by element / render class. `info(...)` devices use `InfoScreen` (value
  lines computed from the slots).
- `src/main/java/mods/eln/client/gui/ElnScreen.java`: base screen. Builders by layout id (`button`, `field`, `slider`,
  ...) throw if the layout lacks the id. `containerTick` -> refreshers -> `update()` (read live values from the render).
  `EaField` commits on Enter, on focus loss, or before a click elsewhere on the screen acts (so a button sees the typed value); `EaSlider` sends on release or a key press, and follows the device while idle.
- Code editor (board #45): `client/gui/CodeEditor.java`, a widget over `gui/code/CodeBuffer` (pure Java: lines,
  cursor, selection, undo/redo; tested by `CodeBufferTest`) with a `gui/code/LanguageAdapter` (tokens to colour,
  diagnostics to mark, rules to draw). Text is drawn in fixed 6 px cells, each glyph of the game font centred in its
  cell, so it is monospaced and scales with the GUI. The signal processor's adapter is `sixnode/electricalmath/SpLanguage`;
  its screen (`ElectricalMathScreen`, laid out by hand, no JSON) adds the reference pane `client/gui/ReferencePane`
  fed by `FunctionReference`.
- Device screens live next to their devices (e.g. `sixnode/hub/HubScreen.java`, `oil/ElectricHeaterScreen.java`); the
  Kotlin devices' screens are in `KotlinDeviceScreens.kt`.

Layout engine: `src/main/java/mods/eln/gui/layout/Layout.java` (pure Java, no client classes). Rows and columns of
self-sizing leaves (`slot`, `text`, `button`, `slider`, `field`, `gauge`, `progress`, `inventory`, `canvas`, `space`),
so nothing is placed by hand. The grammar is in its Javadoc. It returns `Result` with `problems()` (overlaps etc.).
JSON: `src/main/resources/assets/eln/gui/*.json` (53 files). `client/gui/ElnLayouts` reads them fresh from the resource
manager every time a screen opens (resource packs can override them) and measures with the client font. Slot leaves
are named by slot index. A slot wider than 18 gets the big skin.

## Radial menu (client)

`src/main/java/mods/eln/client/gui/RadialMenu.java`: a generic, non-pausing screen of rings of `Segment(label, current,
pick)` round a centre text, for a held tool's modes. Opened while a key is held (`ClientKeys` -> `ChiselRadial` for
the chisel, on the wrench key): releasing the key picks the hovered segment and runs the commit; clicks pick while the
key is held, otherwise pick and commit; a centre click commits; Esc cancels. `Gui.setScreen` releases all key
mappings, so the screen watches `keyReleased`/`mouseReleased` against the key itself. A tap (no hold seen) leaves it
open until a click, the key again, or Esc. The commit sends whatever packet the tool needs.

## Item-held screen: X-ray scanner

`src/main/java/mods/eln/item/xray/XRayScannerMenu.java` (both; `ElnRegistry.XRAY_MENU`), opened on sneak + use by
`XRayScannerDescriptor` (server `openMenu`, open data = inventory slot of the scanner, `OFFHAND` = 40). There are 4
filter slots, 1 item each, stored in the scanner stack's `DataComponents.CONTAINER` on every change. `stillValid`
needs the stack in that slot to still be a scanner. Screen: `client/gui/XRayScannerScreen`; the picture is
`client/XRayScan` (client only).

## Tests

| Test | Guards |
|---|---|
| `src/test/java/mods/eln/gui/LayoutTest.java` | layout maths; `everyShippedLayoutIsSound`: every `assets/eln/gui/*.json` lays out without problems |
| `gametest/GuiPacketCase` (`gui_packets`) | GUI bytes through `ServerPacketHandler.handle` reach six-node and transparent elements; a too-far player is ignored (bypasses the real channel) |
| `gametest/cases/GuiPacketFuzzCases` (`gui_packets_reject_bad_values_*`) | every six-node and transparent element with a GUI handler (by enumeration): ids -128..127 with NaN, infinities, huge and negative numbers, out-of-range indices and a 3000-character string; no throw, no NaN/infinity or over-long string in the saved tag, the review's named values in range; values the screens send are stored as sent; devices saved with bad values (exporter, transporter, turret, aggregators) load clean |
| `gametest/MenusOpenCase` (`menus_open`) | every device with a screen opens an `eln:node` menu; the open data rebuilds the same slots |
| `gametest/GoldenSlotsCase` (`golden_slots`) | menu slots vs `golden/1.12/slots.json` |
| `gametest/MachineMenuCase` (`machine_menu`) | a click puts iron ore into the macerator input (single click, not shift-click) |
| `gametest/PublishRenderCase` (`publish_render`) | every render consumes its publish packet exactly and draws |
| `gametest/XRayCase` (`xray_scanner`) | scanner menu and filters |
| `gametest/cases/CableCases` (`chisel_brush_shapes`) | `packetChiselBrush` through `ServerPacketHandler.handle`: applied to a held chisel; bad size, bad shape, short packet or no chisel ignored |
| `src/test/java/mods/eln/client/gui/RadialMenuTest.java` | radial menu hit test: centre, gaps, segments clockwise from the top |

## Gotchas

- The old "ElnNet sends without a hasChannel check" problem is fixed (3a4dd2ce): `sendToPlayer` and `sendToTracking`
  skip players without the channel (vanilla clients, mock players). `sendToServer` has no such check.
- Packets carry no version beyond the payload version `"1"`. Publish and GUI formats are positional: change writer and
  reader together, and keep `PublishRenderCase` green.
