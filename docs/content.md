# Content and data

Scope: what EA registers (items, blocks, ores, oil, entities), where its data comes from (datagen, hand-written JSON,
scripts), configuration, and the content decisions made in the port. Device classes and the node model are in
architecture.md.

## Registration

| Class / file | Role |
|---|---|
| `src/main/java/mods/eln/registry/ElnRegistry.java` | `DeferredRegister`s for blocks, items, block entities, menus, recipe serializers, entity types, the creative tab. One item per descriptor, one block per transparent-node device. `ALL_ITEMS` sets the tab order (1.12 id order); hidden items (`DescriptorRegistry.isHiddenItem`) stay out of the tab. |
| `src/main/resources/eln-ids.json` | 1.12 `item@damage` -> 26.x registry name (`ElnRegistry.nameOf`). Generated once by `script/gen_ids.py`, then hand-edited. It is the source of truth: registry names must never change (saved worlds). |
| `Eln.sixNodeItem`, `Eln.transparentNodeItem`, `Eln.sharedItem`, `Eln.sharedItemStackOne` (`src/main/java/mods/eln/Eln.java`) | Descriptor registries by 1.12 id. Filled by `registry/ElnDeviceRegistry.registerBatch1` and the batches in `Eln.CONTENT` (`registry/batch/Wp9aContent` ... `Wp12eContent`). |
| `Eln.itemsByName` / `Eln.findItemStack(name)` | EA items by 1.12 display name (the first one registered wins). Recipes use it. |
| `ElnRegistry.afterRegistration()` | Runs the descriptors' `setParent` hooks at `ServerAboutToStartEvent`: ItemStacks cannot exist during registration. |

## Items

- `src/main/java/mods/eln/generic/ElnItem.java`: the item of a plain (shared) descriptor; behaviour is delegated to the
  descriptor. The item bar shows energy for `IItemEnergyBattery` descriptors (red when empty to yellow when full, as
  1.12's `drawEnergyBare`) or `descriptor.itemBar(stack)` when that is >= 0.
- `src/main/java/mods/eln/generic/ItemNbt.java`: 1.12 item NBT lives in `DataComponents.CUSTOM_DATA`, content unchanged
  (e.g. `"energy"` in `item/electricalitem/BatteryItem.kt` and `ElectricalLampItem.kt`).
- `src/main/java/mods/eln/client/ItemTooltips.java` (client): the descriptor's 1.12 `addInformation` lines on
  `ItemTooltipEvent`. A descriptor that throws is logged once, then skipped. Test: `gametest/cases/TooltipCases`.
- Packages: `item/` (lamps, fuses, cores, upgrades, scanners...), `item/electricalitem/` (batteries, tools, E-Coal
  armour `ElectricalArmor`, lamp items, `ItemEnergyCapability`), `item/regulator/`, `item/xray/` (networking-gui.md).
- 26.x-only plain items: `ElnRegistry.RAW_LEAD`, `RAW_TUNGSTEN`, pipe kits/valves/adapter, `CABLE_KITS`, `CHISEL`
  (voxel-cables.md).

## Ores and worldgen

- Ores: `registry/batch/Wp12cContent.registerOre` registers lead, tungsten and cinnabar (`ore/OreDescriptor`,
  `ore/OreBlock`, `ore/OreItem`); `ElnRegistry.register` creates their blocks. Copper is vanilla's.
- Generation is plain data in `src/main/resources/data/eln/`: `worldgen/feature/{lead,tungsten}_ore.json`,
  `worldgen/placed_feature/...`, `neoforge/biome_modifier/ores.json` (overworld, `underground_ores`). Cinnabar does
  not generate (1.12 forced it off). Decision: lead generates (8 veins/chunk); batteries need lead and no other mod
  supplies it. Since 2026-10-20 (Baughn: y < 24 was 1.7.10's world) it lies in the middle of the deepslate range
  (`trapezoid` -64..0, peaking at -32), as `lead_ore` in stone-like blocks and `deepslate_lead_ore` (ore id 7, 26.x
  only; harder, deepslate sounds) in deepslate and tuff. All of it written by `script/gen_ore_data.py`; the deepslate
  texture by `tools/textures/deepslate_ore.py` (the ore's specks over vanilla deepslate).
- Drops: `loot_table/blocks/{lead,tungsten}_ore.json` give raw lead / raw tungsten (Fortune, explosion decay; silk
  touch keeps the ore), as vanilla iron (decision 41e71a8f). Test: `gametest/cases/OreDropCases` (`ore_drops_raw`).
- `script/gen_ore_data.py` once wrote these files, but it is stale. It would write "drops itself" loot tables and a
  pickaxe tag without `eln:well_casing`. Update it before running it.
- `Eln.genLead/genTungsten/genCinnabar` are 1.12 config mirrors; the data files decide what generates.

## Oil (`src/main/java/mods/eln/oil/`)

The 26.x oil chain. Fluids in `oil/Oil.java`: crude oil (bucket), reservoir oil (the still fluid of underground lakes,
a view of a `Reservoir`'s pooled volume), steam, and the fractions in `Oil.Cut` (natural gas, gasoline, diesel, heavy
oil, residue). `oil/OilWorldgen.java` registers the oil lake structure and its piece (`OilLakeStructure`,
`OilLakePiece`; data `worldgen/structure/oil_lake.json`, `structure_set/oil_lakes.json`), tar sand (placed by the
lakes) and well casing. Lake records are the saved data `eln:oil_reservoirs` (`ReservoirData`; codec fields
`next_id`, `reservoirs`, `keys`), kept in step with the blocks by `Reservoirs`. Devices (oil pump, boiler,
fractionating column, flare stack, electric heater, steam generator) are registered in `registry/batch/Wp10aContent`.
Over half the lakes (a fixed roll on the lake's key) a small surface pond, its own reservoir `oil-pond:<lake>`, rimmed
with tar sand, is dug once the chunks over the lake's middle are loaded, in new and old worlds alike (`oil/OilPonds`,
Baughn 2026-10-28; none where the ground isn't flat, dry and natural). Tests: `gametest/cases/OilCases`. The tar sand separator's list is `Eln.separatorRecipes`.

Oil goes on EA pipes (Baughn 2026-11-03: data-driven multi-fluid pipes; species in "Fluid species" below): each oil
device keeps its own tank and puts it on a pipe through a small port node on its drawn flange (`fluid/PortBridge`):
out of the oil pump's discharge (right, 6 px up), the tar sand separator's outlet (back, 4 px up, 6 across), the
column base's residue nozzle (left; at the column's temperature: cold residue barely flows) and each segment's
draw-off (front; natural gas as a gas); into the column base's crude nozzle (right), the flare stack (front) and the
gas turbine's fuel flange (top). Out-ports push up to 200 mB/s against up to 6 bar (constants in each descriptor).
With no EA pipe on the flange, a device with a drawn outlet pushes its tank into whatever block there takes fluid
(`fluid/FluidExport`: another mod's pipe, a column base, a flare stack, an EA tank), and every device offers its
tanks on every face. Latex (the rubber chain, 2026-11-07): the 230V compressor (CompressorPortsElement) takes it in at its blue IN flange (back, 4 px up, 13 across) and presses it to Raw Rubber, and pushes the latex it presses from dandelions out of its red OUT flange (back, 9 px up); the Industrial Resin Collector (transparentnode/resincollector) pushes the latex it taps out of a flange on top (face centre). The Steam Cracker (transparentnode/steamcracker, 400 V, 4 kW) takes gasoline and steam at two blue flanges on its back (4 px up, at 4 and 12 across) and gives butadiene and natural gas out of two red flanges on its front (the same spots); it has no FluidExport fallback and no capability (the two outlets side by side would mix). The Polymeriser (transparentnode/polymeriser, 230 V, 1 kW) takes butadiene, liquid or vapour (a sealed DeviceInlet vessel; counted by mass), at a blue flange on its back, and with Soul Dust from above or the sides (one per 8) makes Raw Rubber (20 mB and 5 kJ each) into a hopper below. PipePlacer.portAt picks the port nearest the click where a face has several. The fuel heat furnace takes fuel on a flange low on its front into a two-bucket tank (gasoline,
diesel, heavy oil, residue, crude). Crude cokes its burner (residue at half that rate), and a coked burner fires that
much less: half after an hour at full power on crude, at most 90 % coked. A right click with wool cleans it. Burning
crude or residue puts out black smoke (Baughn 2026-11-03). Tests: `OilCases` oil_pump_into_pipe*, oil_on_pipes_*,
fuel_furnace_coke.

### Fluid species (`src/main/java/mods/eln/fluid/SpeciesLoader.java`)
- Datapack JSON `data/<ns>/eln/fluid_species/<name>.json` defines species `<ns>:<name>` for the fluid core (fields in
  the loader's javadoc: kind curve/none/gas, group, density, heat capacities, molar mass, boiling point, latent heat,
  critical temperature, liquid viscosity at two temperatures, colour, the NeoForge fluids it is). Water and air are
  built in. EA ships crude_oil, gasoline, diesel, heavy_oil (hydrocarbons, one group), residue (never boils, denser
  than water), natural_gas, latex (the rubber chain: its own group, never boils; mods.eln.oil.Latex, not a column cut) and butadiene (the Steam Cracker's product: a hydrocarbon boiling at 268.7 K, ~2.3 bar at 20 °C, so liquid in pipes; liquid eln:butadiene and vapour eln:butadiene_gas, no bucket; mods.eln.oil.Butadiene). A modpack can add species or retune these; a file that fails its checks (the vapour's
  energy must stay above the liquid's below the critical point) is skipped with a log line. Optional `flammable`
  {flash_point, autoignition} (K): a leak's jet (`node/voxel/SteamJet.burn`) of a species that burns catches by itself
  at its autoignition temperature, over its flash point from a flame within 2 blocks (fire, lava, a torch, a lit
  campfire, furnace or candle), and sets fire along the jet and to what stands in it (Baughn 2026-11-03).

## Entities

The electric bike (`bike/ElectricBike`, `ElnRegistry.ELECTRIC_BIKE_ENTITY`, item `ELECTRIC_BIKE`; Baughn 2026-10-28):
see [bike.md](bike.md).

The electrical anvil (`anvil/ElectricalAnvil*`, transparent node `eln:transparent_node@5440`, `electrical_anvil`; Baughn
2026-10-28): see [anvil.md](anvil.md). Its companion, the capacitor bank (`transparentnode/capacitorbank`,
`eln:transparent_node@5444`, `capacitor_bank`) and its Capacitor Cell (`eln:capacitor_cell`), is in the same file.

The wire-drawing machines (`transparentnode/wiredrawing`, `eln:transparent_node@5632` `wire_drawing_machine_48v` and
`@5636` `wire_drawing_machine_230v`; Baughn 2026-11-06, Wires stage 4) draw copper (ingots or dust) and rubber into
cable kits of any `CableSpec` and into Windings (`eln:winding`, `node/voxel/WindingItem`: turns, tier, mm2, col in
custom data). The 48 V machine makes up to 6 mm² and 230 V insulation, the 230 V machine everything. Costs per block of
wire (a turn is 1/8 block): copper 1/3 ingot x mm²/2.5, rubber 2/3 x insulation layers (1, 1.5, 2, 4) x
sqrt(mm²/2.5), energy 1 kJ x sqrt(mm²/2.5). Whole items go into buffers (saved `copper`, `rubber`) that keep the
fractions; progress is `done` (blocks). A DC-DC converter slot takes cables (one a turn) or one Winding. Every converter
must fit its core's window (turns x mm² over both sides, a cable counting its tier's preset cross-section: 150 cheap,
300 average, 800 optimal), or it disconnects until corrected (Baughn: old converters too). JEI: `compat/jei/WireDrawingCategory`.
Both machines also draw Overhead Cable (`eln:overhead_cable`, `gridnode/OverheadCableItem`: blocks and mm2 in custom
data, up to 500 blocks; Baughn 2026-11-09): bare copper at a kit's copper cost, no rubber, the only thing utility poles,
downlinks and grid transformers are strung with (they refuse cable kits). A span's Rs is its cross-section's cable
resistance x its length; the poles' line is 11 kV, set by their insulators. Spans strung with kits before then load and
keep their kits' tier. Pieces of one cross-section combine by clicking one onto the other.

The servo layer (`robotics`, Baughn 2026-11-02: the servo layer first, 200 V only): the Linear Servo Drive (transparent
node `eln:transparent_node@5504`, `linear_servo_drive`), `eln:servo_rail` and `eln:station_marker` (plain blocks with
checked models, `ServoParts`), and the elevator car entity `eln:elevator_car` (not saved, not summonable). Recipes: the
drive is an Electrical Motor in a Machine Block with a Medium Voltage Cable, two Signal Cables and four iron plates; 8
rails from 6 iron ingots and 3 iron plates; a marker from an iron plate, a stone button and a Signal Cable. Behaviour:
porting-notes.md ("New in 26.x: the servo layer").

`entity/ReplicatorEntity` (`ElnRegistry.REPLICATOR`, spawn egg `REPLICATOR_SPAWN_EGG`), plus its AI classes in
`entity/`. `entity/ReplicatorPopProcess`: during thunderstorms it spawns replicators near players in the overworld
(dark air, within 100 blocks, at most 100 replicators). Client renderer: `client/entity/ReplicatorRenderer`. Damage
type `data/eln/damage_type/scald.json` (steam jets) is tagged `bypasses_armor`.

## Recipes

- Crafting and smelting: `src/main/java/mods/eln/datagen/ElnRecipes.java` (the 1.12 recipe methods, nearly unchanged)
  on top of `ElnRecipesBase.java`. 1.12 ore names become tags (`misc/OreDictTags`), 1.7.10 pattern quirks are rewritten,
  and recipe ids are `eln:<snake_case output>[_n]`. `ElnRecipeProvider` runs it from `GatherDataRegistryEntriesEvent`
  (`RecipeProvider.asBootstrap`). Any recipe problem fails the datagen run.
- Hand-written: `src/main/resources/data/eln/recipe/` (cable kits, pipe kits, valves, adapter, chisel). Kit
  merge and coil/plate conversions use the serializers in `ElnRegistry.RECIPE_SERIALIZERS`.
- Machine lists (macerator, compressor, plate machine, magnetizer): runtime code in
  `src/main/java/mods/eln/registry/ElnMachineRecipes.java`, filled at server start into `Eln.*Recipes`
  (`misc/RecipesList`, `misc/Recipe`). An input also matches other items in its 1.12 ore name's tag (so deepslate
  ores macerate too).
- Macerator rule (decision fc0a18ec, extended 41e71a8f): an ore block gives raw chunks, about like Fortune II
  (`Recipe.withExtra`: iron/gold/lead/tungsten 1-3, copper more); a raw chunk gives 1 dust, with a 20% chance of 2.
  1.12 gave 2 dust per ore. Every other ore block (`c:ores`: coal, redstone, lapis, diamond, emerald, quartz, other
  mods' ores) gives what a Fortune II pickaxe gets from its loot table, rolled per operation (`OreLootRecipe`, the
  macerator list's `RecipesList.fallback`; Baughn 2026-10-25). An ore that drops itself (ancient debris) has none.
- Golden comparison against 1.12 (crafting, smelting and the machine lists): `gametest/GoldenRecipesCase`
  (`golden_recipes`). See testing.md.
- JEI (Baughn 2026-11-06): every EA item JEI lists can be made by clicking its recipe, then its ingredients', down
  to vanilla. `compat/JeiPages` (no JEI classes) holds what the plugin `compat/jei/ElnJeiPlugin` shows beyond the
  game's recipes: the machine lists, resin, annealing and wire-drawing pages, information pages (ores, tar sand, oil
  and its cuts, the blown fuse, the data logger print, creative-only items; text `jei.eln.info.*`) and the items kept
  out of the list (dead ends: old 50 V/200 V turbines, ferrite ingot, the cinnabar chain; other-tier fuses; pipe
  sensor devices). JEI lists what the creative tabs list, so the fuel buckets are in the tab (`ElnRegistry.TAB_EXTRAS`).
  `gametest/cases/JeiAuditCases` (`jei_every_item_reachable`) grows the obtainable set from vanilla over all of it
  and names any listed item left out. A new item needs a recipe, a page, or a place in `hidden()`.

## Datagen

- `src/main/java/mods/eln/datagen/ElnDataGen.java`: `GatherDataEvent.Client` -> `ElnLang` (en_us), `ElnItemTags`,
  `ElnModels` (item definitions, item models, blockstates, block models). Recipes go through the registry-entries
  event above.
- The only datagen run is `clientData` in `build.gradle` (`gradlew runClientData`); there is no `data` run. Output:
  `src/generated/resources/`, committed and added as a resources dir. It holds 325 recipes and 325 recipe-unlock
  advancements, `assets/eln/lang/en_us.json` (the only lang file), and item, model and blockstate JSON.
- The guide advancements (Baughn's hints, 2026-10-20): `src/main/resources/data/eln/advancement/guide/*.json`, written
  by `tools/advancements/make_guide.py` (which also prints their lang lines for `ElnLang`). Rubber is the root; heat
  furnace, boiler and steam turbine come early (the only early power). All `inventory_changed` triggers, toasts only.
  Test: `guide_advancements_load`.
- Tags: `ElnItemTags` puts EA items into the tags of their 1.12 ore names (`OreDictTags.EA_ENTRIES`), and each
  `c:group/x` into `c:group`: 34 `c:` item tag files. Tungsten is EA-private (1.12 `dictionary.tungsten` = false):
  `eln:ores|ingots|dusts/tungsten`. Hand-written in `src/main/resources/data/`: `c:raw_materials/{lead,tungsten}`
  (note: `c:`, unlike the other tungsten tags), `minecraft:mineable/{pickaxe,shovel}`, fence/wall tags for voxel
  blocks that imitate them.
- Loot tables: hand-written for the ores, `tar_sand` and `well_casing`. Device blocks are nodes and drop through node
  code.

## Assets, tools and scripts

| Path | What |
|---|---|
| `src/main/resources/resourcepacks/schematic_icons` | Built-in optional resource pack (schematic 32 px icons), added by `addPackFinders` in `client/ElnClient`. |
| `tools/icons/` | Icon production, two sets: `vanilla/` (default 16 px) and `schematic/` (the pack). Rules: `tools/icons/CONTRACT.md`; generators `*/build.py`, per-icon scripts in `*/icons/`. |
| `tools/bike/texture.py` | The bikes' item icons; the entity texture is painted by `BikeTexture` (`docs/bike.md`). |
| `src/cadmodels/` | The device models (code-CAD in Java, generated at build time; `docs/cad.md`). The anvil's and capacitor bank's item icons, once drawn by their Python scripts, are committed PNGs. |
| `script/gen_ids.py` | Wrote `eln-ids.json` (run once). |
| `script/gen_model_index.py` | Writes `assets/eln/model/index.txt` for `misc/Obj3DFolder`; re-run after adding or removing OBJ models. |
| `script/gen_ore_data.py` | Ore loot, features, pickaxe tag, biome modifier (stale, see above). |
| `script/gen_test_structures.py` | GameTest floors `data/eln/structure/floor_WxHxD.nbt`. |
| `script/blender_export.py`, `pre-commit.sh`, `transifex.sh` | Blender OBJ export; git hook; translation sync. |

## Config

Not "none": `src/main/java/mods/eln/ElnConfig.java` is a SYNCED `ModConfigSpec` (`config/eln-synced.toml`):
`balancing.batteryLifeFactor` (25), `fire.*` lags and kindle times, `cables.convertOnLoad` (false),
`probe.details`, `bike.*` (the electric bike's tunables, [bike.md](bike.md)), `anvil.*` and `capacitorBank.*` (the electrical anvil's and the capacitor bank's, [anvil.md](anvil.md)). It is registered in the `Eln` constructor and copied into statics on load and reload. Every other
1.12 config value is a plain static with its 1.12 default in `Eln.java` (power factors, lamp lives, ranges,
`xRayScannerRange` 7.5...).

`src/main/java/mods/eln/client/ElnClientConfig.java` is the CLIENT spec (`config/eln-client.toml`), registered in the
`ElnClient` constructor: `rendering.staticPartsInChunks` (true), whether the chunk draws the devices' unchanging
parts ([rendering.md](rendering.md)); it applies as soon as the file changes.

## Decisions and removed content

- Copper Coil (0e58d068): new shared item `eln:copper_coil`, 4 copper ingots around a stick give 4; windings
  (inductor, resistor, rheostat, thermistor, DC-DC, 50V copper heating corps) use coils; Copper Cable by hand 3 -> 3.
- Removed: copper tools and armour (a4e409f4, vanilla has them); EA copper ore (a08d3985); EA copper ingot (vanilla
  ingot used everywhere) and the 50V/200V Condensators (771ed3aa, already obsolete in 1.12). The golden tests list
  these as intended differences (testing.md).
