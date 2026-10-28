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
Tests: `gametest/cases/OilCases`. The tar sand separator's list is `Eln.separatorRecipes`.

## Entities

The electric bike (`bike/ElectricBike`, `ElnRegistry.ELECTRIC_BIKE_ENTITY`, item `ELECTRIC_BIKE`; Baughn 2026-10-28):
see [bike.md](bike.md).

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
| `tools/bike/texture.py` | The electric bike's entity texture and item icon (`docs/bike.md`). |
| `tools/models/` | Code-CAD device models (Python -> OBJ, `mcmodel.py`); see `tools/models/README.md`. |
| `script/gen_ids.py` | Wrote `eln-ids.json` (run once). |
| `script/gen_model_index.py` | Writes `assets/eln/model/index.txt` for `misc/Obj3DFolder`; re-run after adding or removing OBJ models. |
| `script/gen_ore_data.py` | Ore loot, features, pickaxe tag, biome modifier (stale, see above). |
| `script/gen_test_structures.py` | GameTest floors `data/eln/structure/floor_WxHxD.nbt`. |
| `script/blender_export.py`, `pre-commit.sh`, `transifex.sh` | Blender OBJ export; git hook; translation sync. |

## Config

Not "none": `src/main/java/mods/eln/ElnConfig.java` is a SYNCED `ModConfigSpec` (`config/eln-synced.toml`):
`balancing.batteryLifeFactor` (25), `fire.*` lags and kindle times, `cables.convertOnLoad` (false),
`probe.details`, `bike.*` (the electric bike's tunables, [bike.md](bike.md)). It is registered in the `Eln` constructor and copied into statics on load and reload. Every other
1.12 config value is a plain static with its 1.12 default in `Eln.java` (power factors, lamp lives, ranges,
`xRayScannerRange` 7.5...).

## Decisions and removed content

- Copper Coil (0e58d068): new shared item `eln:copper_coil`, 4 copper ingots around a stick give 4; windings
  (inductor, resistor, rheostat, thermistor, DC-DC, 50V copper heating corps) use coils; Copper Cable by hand 3 -> 3.
- Removed: copper tools and armour (a4e409f4, vanilla has them); EA copper ore (a08d3985); EA copper ingot (vanilla
  ingot used everywhere) and the 50V/200V Condensators (771ed3aa, already obsolete in 1.12). The golden tests list
  these as intended differences (testing.md).
