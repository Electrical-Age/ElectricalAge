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
- Item components (`ElnRegistry.DATA_COMPONENTS`): `eln:tank_contents`, a broken tank's contents (`oil/TankContents`;
  Baughn 2026-11-25): an Iron or Alloy Tank broken drops with what it held, its tooltip is only its contents and
  pressure, worded as Jade's (Baughn 2026-11-25: no temperature, none of the tank's usual lines, which an empty tank
  keeps), and it places back with it; filled ones stack to 1, empty ones are plain (persistence.md).
  Recipes name a tank through NeoForge's component ingredient with `"components": {"!eln:tank_contents": {}}` (must not
  have it; `ElnRecipesBase.itemIngredient`): a filled tank is no ingredient, so crafting can't delete its contents.
- Tank Minecarts (`ElnRegistry.IRON_TANK_MINECART`, `ALLOY_TANK_MINECART`, entity `oil/TankCart`; Baughn 2026-11-25,
  todo #611): a minecart and an empty tank (shapeless); broken, a minecart and the tank with its contents. The Tank
  Loading Station (`TANK_STATION`, `oil/TankStationBlock`; iron, a hopper over a powered rail): a rail (block tags
  `minecraft:rails`, item tag too, `mineable/pickaxe`; loot table in `src/main/resources`) with a flange on each side
  across the track; a cart standing on it is a vessel on those flanges until it leaves; unpowered it holds the cart,
  powered it lets it go; a comparator reads 0 no cart, 1 no liquid, 2..14 by liquid, 15 at 90 %. The hidden
  `tank_cart_load` block is the cart's display block (its tank). Icons `tools/icons/vanilla/families/tank_carts.py`.
  Details: porting-notes.md.
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
Baughn 2026-10-28; none where the ground isn't flat, dry and natural). Tests: `gametest/cases/OilCases`.

Oil survey maps (todo #470): loot function `eln:oil_survey_map` (`oil/OilSurveyMapFunction`) turns a
filled map into one centred on an oil lake picked at random among those `min_distance`..`max_distance` blocks (600-3000)
from where it is made, marked with a decoration, the position in the lore. `oil/OilSurvey` finds lakes from the
structure set's placement arithmetic (no chunk loaded), skipping lakes under a quarter full and start chunks generated
without a lake. Sold by cartographers at level 3 (`data/eln/villager_trade/cartographer/3/`, added to the vanilla tag
`#minecraft:cartographer/level_3`; 12 emeralds + compass, 3 uses), by the wandering trader
(`data/eln/villager_trade/wandering_trader/`, in `#minecraft:wandering_trader/uncommon`; 14 emeralds, 1 use), and found
in chests: NeoForge global loot modifiers (`data/eln/loot_modifiers/`, `neoforge:add_table` with a `loot_table_id` and a
`random_chance` condition) add the table `eln:chests/oil_survey_map` to village cartographer chests (30 %), shipwreck map
chests (25 %), buried treasure (25 %), abandoned mineshafts (15 %), desert pyramids and ruined portals (10 % each). A
chest's map is made around the chest; with no origin in the loot context the map is discarded. Name and lore are lang
keys `item.eln.oil_survey_map` and `.lore` (ElnLang). Tests: `OilSurveyTest`, GameTests `oil_survey_map` and
`oil_survey_chest`. The tar sand separator's list is `Eln.separatorRecipes`.

Oil goes on EA pipes (Baughn 2026-11-03: data-driven multi-fluid pipes; species in "Fluid species" below): each oil
device keeps its own tank and puts it on a pipe through a small port node on its drawn flange (`fluid/PortBridge`):
out of the oil pump's discharge (right, 6 px up), the tar sand separator's outlet (back, 4 px up, 6 across), the
column base's residue nozzle (left; at the column's temperature: cold residue barely flows) and each segment's
draw-off (front; natural gas as a gas); into the column base's crude nozzle (right), the flare stack (front) and the
gas turbine's fuel flange (top). Out-ports push up to 200 mB/s against up to 6 bar (constants in each descriptor).
With no EA pipe on the flange, a device with a drawn outlet pushes its tank into whatever block there takes fluid
(`fluid/FluidExport`: another mod's pipe, a column base, a flare stack, an EA tank), and every device offers its
tanks on every face. Latex (the rubber chain, 2026-11-07): the 230V compressor (CompressorPortsElement) takes it in at its blue IN flange (back, 4 px up, 13 across) and presses it to Raw Rubber, and pushes the latex it presses from dandelions out of its red OUT flange (back, 9 px up); the Industrial Resin Collector (transparentnode/resincollector) pushes the latex it taps out of a flange on top (face centre). The Steam Cracker (transparentnode/steamcracker, 400 V, 4 kW) takes gasoline and steam at two blue flanges on its back (4 px up, at 4 and 12 across) and gives butadiene and natural gas out of two red flanges on its front (the same spots); its four tanks are fluid nodes (gasoline a breathing DeviceInlet, steam a sealed one, butadiene a sealed DeviceOutlet with a sump pump, natural gas a sealed gas-only one); it has no FluidExport fallback and no capability (the two outlets side by side would mix). The Polymeriser (transparentnode/polymeriser, 230 V, 1 kW) takes butadiene, liquid or vapour (a sealed DeviceInlet vessel; counted by mass), at a blue flange on its back, and with Soul Dust from above or the sides (one per 8) makes Raw Rubber (20 mB and 5 kJ each) into a hopper below. PipePlacer.portAt picks the port nearest the click where a face has several. The fuel heat furnace takes fuel on a flange low on its front into a two-bucket tank (gasoline,
diesel, heavy oil, residue, crude). Crude cokes its burner (residue at half that rate), and a coked burner fires that
much less: half after an hour at full power on crude, at most 90 % coked. A right click with wool cleans it. Burning
crude or residue puts out black smoke (Baughn 2026-11-03). Tests: `OilCases` oil_pump_into_pipe*, oil_on_pipes_*,
fuel_furnace_coke.

**Fuel quality** (refinery milestone M1, `sim/fluid/FuelQuality.java`; Baughn 2026-11-15) is one mechanism for the
furnace burner and the engines. The liquid layer a device burns is rated: its mass share boiling outside the engine's
window (a species without a boiling point, residue, is too heavy for any window) and its coking index. An engine loses
2 % of its power per 1 % outside the window (the fuel burns all the same: the heat goes out of the exhaust). Fuel whose
coking index is over the device's balance fouls it, up to 90 %, at `FOUL_PER_INDEX` x (index - balance) per 2 h of full
power; under the balance it clears at the same law; engines foul and clear 4 times slower (`ENGINE_SLOWER`, Baughn
2026-11-15: time to consider a fix); wool cleans it at once; nothing is damaged. Balances: furnace burner 0.05, engines 0.02 (diesel with over 7 % heavy oil or 1 % residue fouls an engine). From
a class's choke index (Baughn 2026-11-23; per class 2026-11-24: piston engines can't run heavy oil) the fuel chokes an engine: it
takes none and gives no power until the fuel in its vessel is diluted (piped full there is no room to: breaking it
empties it); no damage, nothing saved (the rule reads the
vessel). `TURBINE_CHOKE` 0.18, the Gas Turbine's: crude (0.216) and diesel with over ~17 % residue choke it; heavy oil
(0.15) doesn't. `PISTON_CHOKE` 0.05, the Fuel Generator's, from the Still Pot's tests: a sensibly run still's cuts
(gasoline 0.000, diesel 0.022) don't choke it, a pot overfired at 25 kW (0.10-0.17), heavy oil and a heavy cut do; a
15 kW overfired batch (0.03) only fouls it. The furnace burner never chokes. The Gas Turbine's window is
330 to 720 K, so gasoline, diesel and heavy oil all give full power, and heavy oil fouls it by ~10 % an hour at full
power (diesel clears 0.8 % an hour, gasoline 1.5 %). Jade on the turbine shows the rating, whether the fuel fouls or chokes it, the
fuel's viscosity (`EaUnits.viscosityText`), and the fouling; a choked turbine's Status says so. Saved as `foul`, `fuelLight`, `fuelHeavy`, `fuelCoking` (absent: clean). The Fuel
Generator keeps its fuel in a 2000 mB vessel that breathes (a `DeviceInlet` as the turbine's; pipe on its top flange, or
buckets as before) and burns as much per joule as its old two-bucket tank did; its window is 330 to 400 K (gasoline
and naphtha), so diesel or residue in its gasoline derate it, coking fuel fouls it by the engines' law, and choking fuel stops it (its
Jade State says "choked"; a start click is refused on the action bar). An old
save's tank (`tankLevel` x 2000 mB of `tankFluid`) goes into the vessel; an unknown fluid name (1.7.10 saved "lava")
becomes gasoline, as the old tank burnt every fuel alike. GameTests gas_turbine_off_spec_derates,
gas_turbine_heavy_oil_fouls, gas_turbine_crude_chokes, fuel_generator_residue_chokes, gas_turbine_clean_fuel_clears, fuel_generator_gasoline, fuel_generator_piped, fuel_generator_off_spec_derates, fuel_generator_diesel_derates, fuel_generator_restarts_after_dilution,
fuel_generator_clean_fuel_clears, fuel_generator_old_save; unit test FuelQualityTest.

The Still Pot (refinery M2, todo #591; `oil/StillPotElement`, model `cadmodels/StillPot`): a 2x2x2 kettle of 32 B for a
16-24 B batch, heated through its shell from one heat face, the back of its lower right block (Baughn 2026-11-24: one
is enough; its ghost cells `oil/GhostCells`, as the Large Boiler's), its
vapour off the top of its rear left block to a condenser and receivers that breathe (the breather valve). Constants from
the step-0 sketch (`sim/fluid/StillSketchTest`, gated: `-Psketch=still`): wall 60 W/K, shell 2800 J/K losing 2.5 W/K,
shell limit 850 K (its watchdog trips from 840 K on a 20 K s budget, so a pot boiled down to residue under a big burner
stops by 850 K). Front: a sight glass showing its liquid's level and layers live (`oil/SightGlass`), the liquid's temperature as a signal on the origin (1 V a 100 °C), the bottoms drain low right,
at a tank's low flange height with the pot's floor at its bottom edge (the vessel stands 3 px up, on its skid), so
it drains out completely; a hot drain into a closed receiver pushes back with the receiver's own air, so the
receiver needs a breather (the breather valve clicked on the receiver tank's top stands on a stub over its top port); the fill low on its left flank and the steam inlet low on its right flank (into the pot at their flanges'
height, through check valves). Boiled too hard it boils over: before each step, outside the core's solve, E = 2 (F / 0.10 - 1) kg of
its liquid (to 5) per kg of vapour moves into the vapour line (the node its vapour port couples to; a pipe run up to 30 % of it
liquid, a vessel up to its free volume), every species by its share with its enthalpy
(mass and energy exact), F the Souders-Brown factor of the smoothed vapour velocity over the node's cross-section; a
medium burner stays under it, a pushed one puts residue in the product, which then fouls engines. The vapour port is
an opening over the pot's top tenth (the charge never reaches it), so liquid in the vapour line runs back into the pot
while vapour leaves. Its fill and steam stubs lump into the pipe on their flanges, and are linked into the pot only
while something is coupled there: unpiped they add no node to its network. Lay the vapour
line no higher than the condenser's inlet: a line that rises above it is one lumped run whose lowest point is the pot's
flange, liquid in it can't reach the condenser and the vapour bubbles through it. A safety valve on its crown
(Baughn 2026-11-24: a copper pot at 13 bar, seen in a client check with both receiver valves shut and the coil flooded
over its gas breather, is unrealistic) relieves to the air at 0.5 bar gauge (`RELIEF_PA`, the fluid core's relief
regulator, a pipe safety valve's jet and whistle; what it lets out is booked by species in `ventedKg`); past 2 bar gauge
(`BURST_PA`) it bursts as a tank does. Jade: relieving (outlet blocked), boiling over, only residue left, under vacuum (below 0.9 bar); pressure, temperature, liquid (as a tank's),
what boils off ("Boiling now", while vapour leaves), shell, contents, viscosity, the signal. Its fluid
capability is on its origin block (ghost blocks expose none). A piped fill's float shuts it at the 24 B charge (as a
bucket stops there).

The Condenser Coil (`oil/CondenserCoilElement`, model `cadmodels/CondenserCoil`): a 1x1x2 drum of 2 B (node the lower
block, a ghost cell above), vapour in at the top, gas out at the upper front (high), condensate out underneath (draw 0)
and at the lower front (low); its coil passes 120 W/K from its contents to its shell, which gives it to thermal
dissipators on its back and flanks (both blocks) and ~2 W/K to the air. It may stand on a pipe. Jade: vapour in,
outlet, condensing and gas out (g/s, a second's mean), shell; "Vapour escaping, add cooling" over 60 °C while vapour
comes in. GameTests `condenser_coil_body`, `still_pot_sensible_batch` (a furnace, two
receivers switched through OpAmps and control valves by the pot's signal: gasoline cut 97.5 %; neither cut chokes a Fuel Generator), `still_pot_overfired_batch`
(15 kW: it boils over, residue reaches the receiver and fouls a turbine; every gram of residue accounted for),
`still_pot_calm`, `still_pot_overfire` (25 kW: its product chokes a Fuel Generator), `still_pot_residue`,
`still_pot_body`, `still_pot_sparge_layers` (water under the oil), `still_pot_steam_sparge`,
`still_pot_valves_shut_mid_boil` (both receivers shut: pressure rises, no stall), `still_pot_shut_in_relieves` (shut in
completely under 10 kW: it holds at its relief setting, venting, no burst, no stall), `still_pot_hot_residue_drains` (6 B of hot
residue drains out completely through a large pipe, cold loses ~2 %), `still_pot_second_pass` (the gasoline heart redistilled: 98 % or better), and the
breather's `pipe_breather_valve_*`. No vacuum-trap test: the receivers' breather valves let air in, so a cooling
pot can't pull its receivers into a vacuum (the pot's Jade warns if one is left unvented). Cost: the sensible batch's network has 12 active nodes (pot, coil, the two receivers and eight pipe runs)
and takes ~3.2 ms a step (4.8 iterations, 9.6 solves), flash-bound at ~0.3 ms a node.

The 230V Pipe Heater (`oil/PipeHeaterElement`, Baughn 2026-11-26, model `cadmodels/PipeHeater`): an Electric Heater whose
coils heat a pipe line, for oil too viscous to flow cold. Laid out as the Inline Pump: the line through its flanks at the
pump's height (4 px), placed along the player's view, power on its front; 4 kW. One node of ~2 B (two alloy pipe blocks'
worth, 2 to 6 px up) between the pipes on its flanks, not lumped into them: a lumped run hands its wall heat back to its
pieces by wall conductance alone, so the pipes' iron would pay part of the coils' heat. Its coils (1 kJ/K, ~2 W/K to the
air) heat it through the core's wall link at 50 W/K. The regulator slot and screen are the Electric Heater's, but the
regulator holds the contents' temperature (the screen shows that, `heldTc`); with none it heats at full power. Dry or
stagnant its coils pass 800 (Tc; 820 °C) and it burns out (repair: 230V Copper Heating Corp). Jade: power, contents and
their temperature, coils, viscosity. Sub-id 24 of the blower's group (transparent_node@5400, 230v_pipe_heater). It thins
what flows on from it: an edge's viscosity is its ends' by their shares of its resistance (porting-notes, option A').
GameTests `pipe_heater_thins_heavy_oil` (two lines of heavy oil from tanks at 8 bar: the heated one moves ~1.6x as
much by 500 ticks), `pipe_heater_holds_its_set_point` (the oil just over its 60 °C), `pipe_heater_burns_dry` (coils preset
past the limit with nothing to heat).

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
wire (a turn is 1/8 block): copper 1/3 ingot x mm²/2.5, rubber 2/3 x insulation layers (48 V 1, 230 V 1.5, 400 V 2, 1 kV 3, 11 kV 4) x
sqrt(mm²/2.5), energy 1 kJ x sqrt(mm²/2.5). Whole items go into buffers (saved `copper`, `rubber`) that keep the
fractions; progress is `done` (blocks). A DC-DC converter slot takes cables (one a turn) or one Winding. Every converter
must fit its core's window (turns x mm² over both sides, a cable counting its tier's preset cross-section: 150 cheap,
300 average, 800 optimal), or it disconnects until corrected (Baughn: old converters too). JEI: `compat/jei/WireDrawingCategory`.
Both machines also draw Overhead Cable (`eln:overhead_cable`, `gridnode/OverheadCableItem`: blocks and mm2 in custom
data, up to 500 blocks; Baughn 2026-11-09): bare copper at a kit's copper cost, no rubber, the only thing utility poles,
downlinks and grid transformers are strung with (they refuse cable kits). A span's Rs is its cross-section's cable
resistance x its length; the poles' line is 11 kV, set by their insulators. Spans strung with kits before then load and
keep their kits' tier. Pieces of one cross-section merge wherever they meet, as kits do.

The servo layer (`robotics`, Baughn 2026-11-02: the servo layer first, 200 V only): the Linear Servo Drive (transparent
node `eln:transparent_node@5504`, `linear_servo_drive`), `eln:servo_rail` and `eln:station_marker` (plain blocks with
checked models, `ServoParts`), and the elevator car entity `eln:elevator_car` (not saved, not summonable). Recipes: the
drive is an Electrical Motor in a Machine Block with a Medium Voltage Cable, two Signal Cables and four iron plates; 8
rails from 6 iron ingots and 3 iron plates (standing up the drive's top for an elevator, or lying along a flank for a level run); a marker from an iron plate, a stone button and a Signal Cable. Behaviour:
porting-notes.md ("New in 26.x: the servo layer").
The Track Kit (`eln:track_kit`, `node/voxel/TrackKitItem`, track B 2026-11-27): a length of servo track, 1 block per
Servo Rail (a shapeless recipe makes a 1-block kit from one rail; more rails click into it like plates into a pipe kit).
Laid on the shared placer (`TrackPlacer`) from a Linear Servo Drive with no rail on its top; saved on the drive.

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
  out of the list (dead ends: ferrite ingot, the cinnabar chain, the old six-node power cables;
  other-tier fuses; pipe sensor devices). JEI lists what the creative tabs list, so the fuel buckets are in the tab (`ElnRegistry.TAB_EXTRAS`).
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
- Removed: the 48V/230V heat turbines (Baughn 2026-11-23; uncraftable since 2026-10-14, the small steam turbines
  replaced them). Their ids and registry names stay reserved in `eln-ids.json`; old nodes are kept orphans
  (persistence.md).
