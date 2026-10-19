# NeoForge capabilities

Scope: which NeoForge capabilities (energy, fluid, item; block and item) Electrical Age exposes in 26.x, where they are
registered, the side/burn/client rules, cache invalidation, and the tests that guard them. Voxel pipes: voxel-cables.md.

## Registration

All registrations are lambdas on `RegisterCapabilitiesEvent` in `src/main/java/mods/eln/Eln.java` (mod bus, in the
constructor next to `NodeServer`); the item-energy one calls `ItemEnergyCapability::register`. There is no separate
capabilities class (the javadoc of `fluid/ElementFluidHandler.java` still names an `ElnCapabilities`: it does not exist).

| Capability | Target | Provider returns | Null-side | Client | Burnt |
|---|---|---|---|---|---|
| `Capabilities.Energy.BLOCK` | `TRANSPARENT_NODE_ENTITY` | `TransparentNode.getEnergyHandler(side)` -> element | null | null | null (`TransparentNode`) |
| `Capabilities.Item.BLOCK` | `TRANSPARENT_NODE_ENTITY` | `TransparentNode.getItemHandler(side)` -> element | null | null | null (node and element) |
| `Capabilities.Fluid.BLOCK` | `TRANSPARENT_NODE_ENTITY` | `TransparentNode.getFluidHandler()` if an `ElementFluidHandler` or a `ResourceHandler<FluidResource>` | allowed (side ignored) | null | NOT hidden |
| `Capabilities.Fluid.BLOCK` | `VOXEL_NODE_ENTITY` | `VoxelNode.adapterOn(face).tanks` (`node/voxel/AdapterPart.Tanks`) | null | null | n/a |
| `Capabilities.Energy.ITEM` | every item whose item or descriptor is an `IItemEnergyBattery` | `ItemEnergyCapability.Handler` | n/a | both | n/a |

Not exposed: six-node blocks (only voxel adapters), ghost blocks (`ghost/GhostBlock.java` has no block entity: other
mods must address a multiblock's master block), simple nodes (gone; the exporter is a transparent node now).

## Energy (FE)

- Block: `transparentnode/energyconverter/EnergyConverterElnToOtherElement.java` (LVU/MVU/HVU exporters). Handler only
  on the back face (`front.back()`); `ForgeEnergy` is extract-only (`insert` returns 0), a `SnapshotJournal<Double>` over
  `energyBuffer` so aborted transactions roll back; `extract` also returns 0 while burnt.
- The exporter also pushes: `PushProcess` once per simulator tick, into `Capabilities.Energy.BLOCK` of the block behind,
  in a root `Transaction`; skipped if the target chunk is not loaded (`getChunkNow`), since EA simulates unloaded chunks
  and a lookup would load them.
- Ratio: `Eln.ElnToTeConversionRatio` (config `balancing.ElnToThermalExpansionConversionRatio`, FE per J).
- Old saves: simple-node exporters (`tag:"ElnToOther"`) are rewritten by
  `transparentnode/energyconverter/ExporterSaveUpgrade.java` via `NodeManager.registerTagUpgrade` (see persistence.md).
- Items: `item/electricalitem/ItemEnergyCapability.java`. Insert up to one tick (0.05 s) of `getChargePower`, extract
  only from `BatteryItem` (portable batteries/condensators; tools like the mining drill are receive-only). Energy lives
  in the stack via the descriptor; changes go through `ItemAccess.exchange` inside the transaction. The durability-bar
  display is separate (`generic/ElnItem.isBarVisible`), not a capability.

## Fluid

- Transparent devices override `TransparentNodeElement.getFluidHandler()` (returns `Object`): one tank via
  `fluid/ElementFluidHandler.java` (whitelist via `setFilter`, applies while empty) or a multi-tank
  `ResourceHandler<FluidResource>` (e.g. the boiler: water in, steam out). Overrides: `mechanical/Turbines.kt`,
  `transparentnode/FuelHeatFurnace.kt`, `transparentnode/electricalmachine/OilSeparatorElement.java`, and in `oil/`:
  `BoilerElement`, `SteamGeneratorElement`, `OilPumpElement`, `FlareStackElement`, `ColumnBaseElement`,
  `ColumnSegmentElement`.
- Buckets: `TransparentNode.onBlockActivated` -> `fluid/FluidBuckets.use` on the same handler.
- Pipe adapter: `AdapterPart.Tanks` (water `WATER_MB` 8000, steam `STEAM_MB` 32000) is a view of the pipe run behind
  the adapter's face; what other mods' pipes put in or take out is synced into the run at the next fluid step
  (`VoxelNode.syncAdapters`) and before a save. EA's own voxel pipes do not use the capability; they couple through
  `fluid/FluidWorld` ports.
- Consumer: `sixnode/Scanner.kt` reads `Fluid.BLOCK` for fill level (items through `Container` only).

## Item (414cc625, "Hoppers and pipes can feed and empty EA machines")

`TransparentNodeElement.getItemHandler(side)`: null for the null side or while burnt; else, if `getInventory()` is a
`WorldlyContainer` with slots on that face, `new WorldlyContainerWrapper(wc, side)`. Sided rules come from the
inventories (`compat/ISidedInventoryCompat.java` maps `getSlotsForFace` / `canPlaceItemThroughFace` /
`canTakeItemThroughFace` to the 1.12-style `getAccessibleSlotsFromSide` / `canInsertItem` / `canExtractItem`).

| Machine (inventory) | Top | Other faces (incl. bottom) | Insert valid only if |
|---|---|---|---|
| Macerator, compressor, magnetizer, plate machine (`transparentnode/electricalmachine/ElectricalMachineInventory.java`) | input slot, insert only | output slots, extract only | a recipe takes it (`hasRecipe`) |
| Electrical furnace (`transparentnode/electricalfurnace/ElectricalFurnaceInventory.java`) | input, insert only | output, extract only | it has a smelting result |
| Heat furnace (`transparentnode/heatfurnace/HeatFurnaceInventory.java`) | fuel slot | fuel slot | fuel (`Utils.getItemEnergie > 0`); never extract |
| Egg incubator (`transparentnode/eggincubator/EggIncubatorInventory.java`) | egg slot | egg slot | `Items.EGG`; never extract |

Everything else uses `node/transparent/TransparentNodeElementInventory.java` (no slots on any face), so capacitors,
inductors, transformers, batteries, turret, etc. stay closed and pipes can't pull cores or upgrades. The wrapper defers
`setChanged` (and so `inventoryChange`) to the outermost transaction commit.

AutoMiner output (`transparentnode/autominer/AutoMinerSlowProcess.java`): `getDropHandler()` looks 1 then 2 blocks
forward and 1 down (1 forward wins), via `Capabilities.Item.BLOCK` from `UP`, else a vanilla `Container` through
`VanillaContainerWrapper`; `drop()` inserts the whole stack in a root transaction or nothing (stays queued).

## Invalidation

`TransparentNode.invalidateCapabilities()` calls `ServerLevel.invalidateCapabilities(pos)`. Called from:
`TransparentNode.initializeFromThat` (element bound), `initializeFromNBT`, `onBreakBlock`, `unload`, and
`TransparentNodeElement.burn` / `repair`. Rule: call it whenever the set of handlers a face returns can change without
the block entity being replaced (bind, burn, repair, a face changing role). Block entity placement/removal itself is
NeoForge's job.

Missing today: `VoxelNode` never invalidates, so a `BlockCapabilityCache` held by another mod's pipe will not see an
adapter added to or removed from an existing voxel block until something else invalidates. The fluid capability on
transparent nodes ignores burn state, so it needs no invalidation on burn.

## Tests (GameTests, by name)

- `wp11 energy converter` (`gametest/cases/Wp11Cases.java`): FE capability on the back face only.
- `wp11 legacy exporter save`: old exporter loads as a transparent node, pushes FE behind, capability on the back face.
- `wp12 forge energy` (`gametest/cases/Wp12Cases.java`): FE on the portable battery, aborted insert leaves energy,
  drill receive-only, no FE on plain items.
- `item_capability` (`gametest/cases/ItemCapabilityCases.java`): macerator top/side/bottom slots, null side, refused
  stick, aborted insert, hopper feed, closed devices (power capacitor, power inductor, DC-DC converter), egg incubator,
  burnt macerator clears a held `BlockCapabilityCache` and repair restores it.
- `autominer_drop_capability`: autominer fills a macerator below; a refused stick stays queued.
- `pipe_adapter_capability` (`gametest/cases/PipeCases.java`): water in and out through the adapter face, only there.
- `port26 fluid handler` (`gametest/cases/PortHelperCases.java`): tank semantics; fuel heat furnace exposes its tank.
- `oil_separator`, `oil_boiler`, `oil_column` (`gametest/cases/OilCases.java`): fluid capability lookups (null side).
- Unit: `src/test/java/mods/eln/transparentnode/energyconverter/ExporterSaveUpgradeTest.java` (tag upgrade).
