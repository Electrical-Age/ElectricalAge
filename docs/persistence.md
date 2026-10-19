# Persistence

Where Electrical Age (EA) state is saved, the NBT layout of nodes, the load/save lifecycle, save upgrades and the
ids that old worlds depend on. All of it is server side; clients get state only through publish packets
(see networking-gui.md).

## Where state lives

| State | Stored in | Written by |
|---|---|---|
| Nodes (six, transparent, voxel), element state, sim parts, inventories | `<world>/data/electricalAgeWorld<dim>.dat` key `nodes` | `server/ElnWorldSaves`, `node/NodeManager` |
| Ghost blocks (multiblock parts) | same file, key `ghost` | `ghost/GhostManager` |
| Save-wide config (`heatFurnaceFuel`, `electricalLampAging`, `batteryAging`, `infinitPortableBattery`, `reGenOre`, `cableRsFactor_lastUsed`) | same file, key `config`, dim 0 only | `server/SaveConfig` |
| Wind (`windHit`, `windTarget`, `windVariation`, `windTargetFiltred`) | same file, key `wind`, dim 0 only | `misc/WindProcess` |
| Oil reservoirs | vanilla SavedData `eln:oil_reservoirs` (codec keys `next_id`, `reservoirs`, `keys`) | `oil/ReservoirData` |
| Tutorial sign texts | `<world>/EA/tutorialSign.txt`, read only (the `.xml` reader is commented out) | `sixnode/tutorialsign/TutorialSignElement` via `Utils.readMapFile` |
| Block entities | nothing persistent | `node/NodeBlockEntity` |

- `<dim>` = `misc/Dimensions.id(level)`: overworld 0, nether -1, end 1, others the hash of the dimension id.
- Block entities (`NodeBlockEntity`, `SixNodeEntity`, `TransparentNodeEntity`, `VoxelNodeEntity`) have no
  `saveAdditional`: they look their node up in `NodeManager` by position. Their update tag carries only the
  publish packet under `elnDescription` (client sync, not saved).
- Decision (P1): nodes live outside chunk data, so unloaded chunks keep simulating, as in 1.12. (P1-DESIGN.md
  planned SavedData for nodes; the code uses EA's own file instead.)

## Lifecycle (`server/ElnWorldSaves`, registered in `Eln`)

- `LevelEvent.Load` (ServerLevel) -> `load`: parses `.dat`, else `.dat.bak`. A missing `.dat` is a save that
  crashed between its moves: then a `.dat.tmp` that is whole (read to its end, so the gzip length and CRC are
  checked) is the newest save and is preferred to the `.bak`; a `.tmp` that is not whole is ignored with a warning
  (not set aside). A `.tmp` next to a `.dat` is ignored. A file that exists but can't be parsed is renamed to
  `<name>.corrupt-<yyyyMMdd-HHmmss>` and logged as an error, so no save overwrites it (even when the `.bak` is
  good: else the next save would rotate the bad `.dat` over it); both bad -> a loud banner, the dim starts with no
  nodes. The dim is marked loaded (saved) unless a bad file could not be renamed. The parsed tag is applied once:
  `NodeManager.loadFromNbt(nodes, dim)`, `config` + `wind`, `GhostManager.loadFromNBT(ghost)`, each in its own
  try/catch. Node file before chunks: block entities find their node when chunks load.
- `LevelEvent.Save` -> `save`: only for dims marked loaded. Writes `.dat.tmp`, moves `.dat` -> `.dat.bak`,
  then `.tmp` -> `.dat` (atomic move when possible).
- `LevelEvent.Unload` -> `NodeManager.unload(dim)` (calls `NodeBase.unload()`, drops nodes and orphans of the
  dim) and `GhostManager.unload(dim)`.
- Chunk unload vs break (`NodeBlockEntity`): `onChunkUnloaded` sets `chunkUnloading`; `setRemoved` then leaves
  the node alone. Any other removal (break, replace, GameTest clear) calls `node.onBreakBlock()`.
- `node/NodeOrphans` (every 200 ticks): a six/transparent/voxel node whose chunk is loaded but whose block is not
  a `NodeBlock` is disconnected and removed, without drops. `/elnlab remove x y z` does it for one position.

## NodeManager (`node/NodeManager.java`)

- Node types by saved `tag` string (`registerUuid`, in `Eln`): `"s"` SixNode, `"t"` TransparentNode,
  `"v"` VoxelNode (`VoxelNode.UUID`). Never rename or unregister these.
- File layout: compound of `n0`, `n1`, ... each a node tag `{tag: <type>, ...node.writeToNBT}`. Load iterates
  all child compounds (`Utils.getTags`), key names do not matter.
- `loadFromNbt(nbt, dim)`: per node `upgradeTag` -> class lookup -> `newInstance` -> `readFromNBT` -> `addNode`
  -> `initializeFromNBT`; after all, `globalBoot()` on each. `saveToNbt(nbt, dim)` skips `mustBeSaved()==false`
  and other dims; `dim == Integer.MIN_VALUE` means all dims (tests).
- Save upgrades: `registerTagUpgrade(type, op)` / `TAG_UPGRADES`, applied before the type lookup. A throwing
  upgrade logs and keeps the tag. Only one today: `"ElnToOther"` (1.12/early 26.x simple-node FE exporter) ->
  `transparentnode/energyconverter/ExporterSaveUpgrade` (commit 6b105574). It moves `cx/cy/cz/cd/NBOpaque/NBWrap`
  into `node`, maps `SNdescriptorKey` (`EnergyConverterElnToOther{LVU,MVU,HVU}`) to `eid` (group 84 ids), copies
  all other root keys into `element`, and turns `SNfront` into `others` (front + 8, grounded). Unknown key or
  no coordinates: returned unchanged (-> orphan).
- Orphans (commit 2f54a6d5): a tag with an unknown type, or whose `readFromNBT` throws, is kept raw per dim
  (`Orphan(at, tag)`, position from `node/cx..`), logged, and written back unchanged by `saveToNbt` after the
  live nodes. `addNode` at the same position drops the orphan. `getOrphans(dim)` returns copies.
  `TransparentNode.readFromNBT` throws on an unknown `eid` on purpose so the device becomes an orphan.
- Kept six-node sides: in `SixNode.readFromNBT` a face whose device can't be built or loaded (unknown `EID`, any
  exception) is kept raw as `{EID, ED}` (`keptSides`), logged, and written back unchanged by `writeToNBT`; the rest of
  the block loads. The side stays taken (`getSideEnable`, `getIfSideRemain`, `VoxelNode.canPlaceElement`/`markUsed`):
  placing over it is refused with a chat message, because unlike a node orphan the block still stands and a player
  would naturally reuse the "empty" face and so delete the device. Breaking the whole block discards them (logged).
  A block left with only kept faces (and, for a voxel block, no parts or imitated block) breaks at the player's
  first hit, since nothing in it can be aimed at.
- Two meanings of "orphan": `NodeManager` orphans = saved tags this version cannot load; `NodeOrphans` = live
  nodes whose block is gone.

## NBT layout of a node

| Level | Keys | Source |
|---|---|---|
| NodeBase | `cx cy cz cd` (Coordonate prefix `c`), `NBOpaque`, `NBWrap` | `node/NodeBase`, `misc/Coordonate` |
| Node | + `lastLight` | `node/Node` |
| TransparentNode | `node` {NodeBase}, `eid` (short, 1.12 device id), `element` {element} | `node/transparent/TransparentNode` |
| SixNode | `node` {NodeBase}, `cacheBlockId` (always 0), `cacheBlockMeta`, `EID0..5` (short, 0 = empty face), `ED0..5` {element} | `node/six/SixNode` |
| VoxelNode | SixNode keys + NodeBase keys copied to the root (older versions read them there), `legacy` {six-node tag}, `host` {block state}, `carved` (long[]), `parts`, `p0..` {`kind` + part data} | `node/voxel/VoxelNode`, `VoxelPart.read` |
| Transparent element | `others` (front + 8 if grounded), `burnt`, `burntKind`, `inv`, sim parts | `node/transparent/TransparentNodeElement` |
| Six element | `sixFront`, `burnt`, `burntKind`, `inv`, sim parts | `node/six/SixNodeElement` |
| GhostElement | flat `elemCoordx/y/z/d`, `obserCoordx/y/z/d`, `UUID` | `ghost/GhostElement` |

- `burnt` = `FailureCause` ordinal + 1, absent if not burnt; `burntKind` = `Failure.Kind` ordinal, only with `burnt`.
- `inv` (`Utils.writeToNBT(nbt, "inv", inv)`): list of `{Slot: byte, item: ItemStack.CODEC}`; empty slots
  omitted. One class for both node kinds: `node/ElementInventory` (subclasses `SixNodeElementInventory`,
  `TransparentNodeElementInventory`).
- Sim parts: `INBTTReady.readAll/writeAll(nbt, lists...)` (commit fa67a291) visit every `INBTTReady` in the
  element's load/component/process lists with prefix `""`. Keys are `<name><suffix>`: `NbtElectricalLoad`
  `<name>Uc`, `NbtThermalLoad` `<name>Tc`, `NbtResistor` `<name>R` (missing -> high impedance). Elements add
  their own keys after `super`.

## Ids that must never change

- `src/main/resources/eln-ids.json`: `"<1.12 item>@<damage>"` (e.g. `eln:six_node@<id>`, `eln:transparent_node@<id>`,
  `eln:shared_item@<id>`, `eln:ore@<id>`) -> `{name, kind, display, descriptor}`. `ElnRegistry.nameOf` takes the
  26.x registry name from `name`; generated by `script/gen_ids.py` from `golden/1.12/inventory.json`. Registry names are what saved item stacks and
  blocks refer to.
- The 1.12 damage id is the device id: `eid` / `EID<n>` in node NBT and in client packets
  (`node/DescriptorRegistry.getDescriptor(int legacyDamage)`). Renumbering a device orphans every saved copy.
- Descriptor keys of 1.12 (`EnergyConverterElnToOther*`) are only read by `ExporterSaveUpgrade`; keep them.
- Block-entity alias: `eln:energy_converter_eln_to_other_entity` -> `eln:transparent_node` (`ElnRegistry`), for
  chunks saved while the exporter had its own BE type.

## CableConverter (six-node cables -> voxel cables)

- `node/voxel/CableConverter`: `plan(SixNode)` (dry run, or why skipped), `convert(SixNode)`, `revert(VoxelNode)`,
  `revertKnuckles(VoxelNode)`. Convert keeps the full six-node tag in `VoxelNode.legacy`; revert restores the
  cables from it and keeps the devices as they are now. Cable U/T/burnt carry over to the parts.
- On load: only if config `[cables] convertOnLoad` (default false, `ElnConfig`). `ChunkEvent.Load` queues the
  chunk and its neighbours; `ServerTickEvent.Post` converts up to 2000 blocks per tick. Skipped blocks stay six-node.
- Manual: `/elnlab voxelcables convert|dry|revert <r>` (`gametest/LabCommands`, report in `eln-voxelcables.txt`).

## Tests

- Every GameTest case: `ElnGameTests.persistence` after `measure()` runs `gametest/Reload.node` on each node in the
  bounds: save, unload, build a fresh node from the tag, save again; any difference except keys ending `Uc` or
  `State` (`Reload.isTransient`) fails. It calls `readFromNBT` directly, so it does not cover `upgradeTag` or orphans.
- `src/test/java/mods/eln/node/NodeSaveLoadTest.java`: unknown type kept and re-saved byte-equal over two round
  trips; unknown transparent `eid` throws and is kept; orphans stay in their dim and are dropped on `unload(dim)`.
  (Added in 948ac491 asserting the loss; reversed in 2f54a6d5.)
- `src/test/java/mods/eln/server/ElnWorldSavesTest.java`: unreadable `.dat` and `.dat.bak` are renamed aside;
  a bad `.dat` with a good `.bak` loads the backup and leaves it untouched; a missing `.dat` is not corruption; with
  no `.dat` a whole `.tmp` beats the `.bak`, a truncated one is ignored, and a `.tmp` beside a `.dat` is ignored.
- `src/test/java/mods/eln/node/six/SixNodeLoadTest.java`: `readFromNBT` does not connect (the loader does, once);
  a face with an unknown device id is kept, re-saved equal over two round trips, and its side stays taken.
- `src/test/java/mods/eln/transparentnode/energyconverter/ExporterSaveUpgradeTest.java` (jqwik): every field
  carried, extra keys go to `element`, unplaceable tags unchanged, `NodeManager` loads an old exporter and
  re-saves it as `"t"`.
- GameTest `wp11 legacy exporter save` (`gametest/cases/Wp11Cases`): old tag through `loadFromNbt` in a world.
- GameTest `voxel block with only a kept face can be broken` (`gametest/cases/SixNodeBreakCases`).
- GameTests `converted ...` / `reverted ...` (`gametest/cases/CableCases`, `ConvertCase`): convert and revert.

## Gotchas

- Renaming an NBT key, a sim part's `name`, or a `tag`/`eid` silently resets that state on old worlds. Add a
  fallback read or a `registerTagUpgrade` instead.
- Inventory sizes: `Utils.readFromNBT` drops slots `>=` the current size. Removed 1.12 slots stay as hidden slots
  (e.g. cable slot in `GroundCableElement`, `EnergyMeterElement`, `ElectricalSensorElement`,
  `ThermalSensorElement`, `PowerSocketElement`) so old items survive and drop on break.
- Unknown `VoxelPart` kinds read as null and are dropped from the next save.
- Nodes in unloaded chunks keep running; code touching neighbours must check `level.isLoaded`.
