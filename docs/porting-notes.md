# Porting notes (1.12 -> 26.x)

Decisions taken for the 26.x port, what differs from 1.12 for players and contributors, what was fixed, and what is
deferred or still open. Subsystem detail lives in the other pages; this is the "why is it like this" page.

## Platform

| Item | 26.x | Where |
|---|---|---|
| Loader | NeoForge `26.3.0.75-beta`, pinned (`minecraft_version_range=[26.3]`); a beta, bump deliberately | `gradle.properties` |
| Java | 25 toolchain (Mojang ships 25); JDK 21 also needed by the NeoForge runs | `build.gradle`, `flake.nix` |
| Kotlin | 2.4.21 stdlib **jarJar'd** in the mod jar (also commons-math3). No Kotlin for Forge dependency: KFF 6.3.0 stops at 26.2. NeoForge picks the newest copy if several mods ship it | `build.gradle`; guarded by `gametest/KotlinRuntimeCase.kt` |
| Names | Mojang (26.x is unobfuscated) | |
| Registry | One block/item per device; no damage sub-items. The 1.12 `sub + (id << 6)` ints survive only as keys of the descriptor registries; registry names come from `src/main/resources/eln-ids.json` (generated once, now the source of truth) | `registry/ElnRegistry.java`, `registry/ElnDeviceRegistry.java`, `registry/batch/*Content.java` |
| Save/load | Block entities use 26.x `ValueInput`/`ValueOutput`; node data still lives in EA's per-dimension save (see persistence.md) | `node/NodeBlockEntity.java`, `server/ElnWorldSaves.java` |
| Rendering | No raw GL: 1.12 draw code runs on a GL11 facade that records into `GlFrame`, submitted through 26.x pipelines; also renders the same under Vulkan (see rendering.md) | `gl/GL11.java`, `gl/GlFrame.java`, `client/render/GlSubmit.java` |
| GUIs | One menu type (`eln:node`, `gui/ElnNodeMenu` + `gui/MenuSpecs`), vanilla-style screens on a layout engine with JSON layouts in `assets/eln/gui/*.json` (see networking-gui.md) | `gui/layout/Layout.java`, `client/gui/ElnScreen.java` |
| Data | Recipes, lang (`en_us` only), item tags (1.12 ore dictionary -> `c:` tags, `misc/OreDictTags`), models by datagen into `src/generated/resources` (committed); machine recipes built at runtime, tag-aware; ore and oil worldgen as data | `datagen/ElnDataGen.java`, `registry/ElnMachineRecipes.java` |
| Config | `ElnConfig` (`eln-synced.toml`, SYNCED): `balancing`, `fire`, `cables`, `probe`. Every other balancing value is still a 1.12 default in `Eln` statics | `ElnConfig.java` |
| WAILA/TOP | Jade plugin (compile-only dependency); no TheOneProbe | `compat/jade/ElnJadePlugin.java` |

## Baughn's decisions

- Keep the 1.12 OBJ models; a remodel is a separate later project.
- Macerator: vanilla ore blocks (and EA lead/tungsten ore) -> raw chunks about like Fortune II (iron/gold 1-3,
  copper 3-8); a raw chunk -> one dust, 20% chance of two. 1.12 gave two dust per ore. EA lead and tungsten ores drop
  raw pieces like iron ore (`OreDropCases`).
- Burnt electrical cables are drawn burnt through (geometry: stubs, charred cut, copper core), since dyed cables
  cannot show soot.
- X-Ray Scanner recreated, not ported: 4 coloured filter slots (an ore block or what it drops, by tags only:
  `ores/`, `raw_materials/`, `gems/`, `dusts/`, `nuggets/`, `ore_drops/` in `c:` and `eln:`; coal is
  `eln:ore_drops/coal`; tungsten ore is also `c:ores/tungsten`, Baughn 2026-10-24), picture on the held item; range 7.5 (1.12: 5). `item/xray/`, `XRayCase`.
- Copper Coil (`eln:copper_coil`, new): thin winding wire; windings (inductor, resistors, rheostat, thermistor,
  DC-DC, 50V copper heating corps) take coils; copper cable is 1 per ingot.
- Thermal Insulation takes any colour of wool (`#minecraft:wool`; 1.12: white only; Baughn 2026-10-26).
- Ambient temperature by biome (Baughn 2026-10-26): loads cool towards the local ambient (`sim/Ambient`), not 20 C;
  limits stay absolute temperatures. Fluid nodes fill at it; other mods' water enters pipes and boilers at it.
- 50V/200V steam turbines work by the expansion (Baughn 2026-10-26): 0.7 x the isentropic drop from the inlet to the
  exhaust (Water.isentropicDrop), rated at 10 bar venting to air (3.5 g/s for 1 kW), casing bursts over 25 bar. WAILA
  shows the pressure efficiency; loaded under 5 bar for 30 s gives the placer the advancement Under Pressure (use a
  valve). Heat furnace fuel x2.5 (config heatFurnaceFuelFactor), stone heat furnace 4 combustion chambers, submersible
  pumps 6 / 15 bar. `oil_steam_turbine*`.
- Steam condensers (new, Baughn 2026-10-26): against a turbine's exhaust side (right of its front); a vessel cooled
  through its shell (own air cooler to the ambient, thermal cables on its sides), air ejector, condensate pump on the
  turbine's shaft out of its front flange, makeup in on top; the models join (turbine parts vent / exhaust,
  condenser ducts). Over half full it floods the turbine. `oil_steam_turbine_condenser`, `oil_condenser_floods`.
- Condensate pump curve (Baughn 2026-11-03: a turbine at its rated pressure works on its own, an overclocked one needs
  a pipe pump). Both condensers' pumps follow Q = speed x Qmax x sqrt(1 - dp / H0) (`CondensatePump`), dp the outlet
  over the vessel: H0 = 2 x the turbine's rated inlet, and 1.5 x its condensate at full power at 1.5 x the rating
  (a governed turbine's boiler sits above its rating: ~14 bar for a 10 bar turbine, AdventurAgent on live 2026-11-03;
  the first cut, shut-off at 1.3 x, flooded those condensers). Small/Steam Condenser (50V/200V turbines, 10 bar):
  shut-off 20 bar, ~4.2 / 16.8 g/s at 10 bar, 3.0 / 11.9 at 15 (5.9 / 23.8 g/s free; their turbines condense
  ~2.0 / 7.9 g/s), short of full-power condensate above ~18 bar. Plant Steam Condenser (HP, 20 bar): shut-off 40 bar,
  84 g/s at 20 bar, 59 at 30 (118 free; a full HP makes 39.5 g/s); it was 15 bar and 0.1 kg/s, which
  couldn't feed the HP's own 20 bar boiler. Its motor load now goes with the pressure it really pumps against. Speed
  still scales the flow only (the head stays at any speed). Was a straight line to a 15 bar shut-off for all.
  `CondensatePumpTest`, `oil_condensate_return_*`, `plant_condenser_pump_into_*`.
  Jade on both condensers, "Pump outlet": the pressure it works against and its flow there as a share of its rated
  (free) flow at its speed, or "shut off, needs a pipe pump" (Baughn 2026-11-03).
- Small Steam Condenser / Steam Condenser vessels hold 12 buckets, as a tank block (Baughn 2026-11-03: "133 m^3 is a
  bit much for a 1m^3 block"); they were 5 / 20 L (74 000 / 296 000 mB). Saves without the "tank3" tag load trimmed:
  water over the float valve's shut level (25 %) and air over 1 atm's worth go, at the temperature they had (kept, a
  full old vessel was over FLOOD at once and burnt its turbine). The Plant Steam Condenser keeps its 60 L
  (~889 000 mB, 74 buckets). `oil_condenser_old_save_trimmed`.
- Steam condenser makeup signal (Baughn 2026-11-03: no interlock to the boiler, which the condenser can't know; the
  boiler's analog fill, through a comparator, can say when the plant wants water): a 0-50 V input on its left flank
  scales what its float valve lets in, 0 V shut; unconnected, the float valve alone as before. That flank takes no
  thermal cable now. Jade's Makeup valve says whether its signal holds it. `oil_condenser_makeup_signal`.
- Shaft redo, part 1 (Baughn 2026-10-29: the old shaft turbines had no steam input; redo them on the new steam logic).
  The shaft Steam Turbine (same id) takes steam by EA pipe on top and lets it out of its back (piped on, or into the
  air); work = 0.75 x the isentropic drop from the inlet to the exhaust's pressure, onto the shaft; 16 kW at 20 bar
  venting to air (~39 g/s); automatic: 5 % droop governor (full open at 800 rad/s, shut at 840), trip over 880 (manual
  or external control: neither; Baughn: whoever takes manual control duplicates the safeties), windage 2 % of
  rated at rated speed (an unloaded turbine can't run away on the valve's leak); the 50V/200V turbines' controls and
  screen (`SteamValveControl`); casing bursts over 60 bar; water strips its blades, repaired with a cheap Turbine
  Blade Set. No fluid capability; an old turbine's tank is dropped on load. New: an 8 kW Generator (the 4 kW one is
  drawn thinner) and a 200V Shaft Motor (spins a shaft to a signal's speed, 0-50 V: 0-800 rad/s, 2 kW, 85 %).
  `shaft_steam_turbine_*`, `shaft_motor_spins_up_generator`.
- Shaft redo, part 2: plumbing (Baughn 2026-10-29). Pipe Pressure Valve (a ValvePart mode, one way along its arrow;
  relief holds the pressure before it, reducing the pressure after it; set point on its screen or a 0-50 V signal for
  0-150 bar; done as a negative boost on its link, which the network solves within the step: no overshoot). Pipe Steam
  Quality Sensor (PipeSensorPart QUALITY: % of the run's water that is liquid). Iron / Alloy Tank (3 L, 10/40 and
  100/300 bar; vapour ports on top and high on three sides, liquid underneath and low; sight glass). 200V Inline Pump
  (line low on its flanks, head 25 bar x speed^2, 0.1 kg/s free-flowing, 500 W; gas in its line: it primes itself
  through a casing vent; water within 0.2 bar of boiling at its inlet: it cavitates and rattles). `PlumbingCases`.
- Shaft redo, part 3 (Baughn 2026-10-29). LP Steam Turbine (transparent_node@274; ShaftSteamTurbineElement with
  SteamTurbineSpec.LP: 9 kW from ~1.5 bar into ~0.1 bar, big bore, H 3 s): wet steam (water over 1 % of its inlet or
  backed into its exhaust) brakes the shaft (up to half its rating) and knocks; over 10 % for 12 s at speed it strips
  its blades (Turbine Blade Set); its drain throws 3 %/s. Plant Steam Condenser (transparent_node@5300): a
  CondenserVessel (shared with the 50V/200V condensers' CondenserElement, unchanged in behaviour and NBT) fed at its
  top, back or right face centre (an LP turbine's exhaust set straight against it), 60 L, shell 40 kW at 20 K plus
  thermal cables; condensate pump and air ejector on a Shaft Motor against its side (no motor: no vacuum; air leaks
  in at 2 mg/s at full vacuum); over half full its tubes drown (vacuum lost, LP power sags). Measured: HP + LP +
  condenser +50 % work per kg over the HP venting to air, on 20 bar steam. `ShaftPlantCases`.
- Round 4 (Baughn 2026-10-29): tanks hold 12 buckets a block, and tank blocks of a tier that fill a box (A x B x C)
  are one vessel (TankGroup; any other shape stays separate one-block tanks); ports on the box's outer faces, drawing
  at their height in the box; each block keeps its share by volume (written back before a change and before saving,
  to float precision), so a broken block takes its share and saved state survives; the box draws as one tank from its
  lowest corner (shell, sight glass and water scaled; no flanges). Plant condenser: cooling-water tubes in low on its
  left, out low on its right, UA 4 kW/K. `tank_box_*`, `plant_condenser_cooling_water_full_hp`.
- Tank flanges (Baughn 2026-11-03): a tank, lone or a box, draws a flange only on a port a pipe or device meets (a lone
  tank drew all seven); the ports themselves are unchanged (kits still end at them) and Jade lists where they are and
  how many are piped. Each flange's nozzle runs in past the shell all round its bore and is closed at its end (it was
  cut at the shell's centre line, so the curving shell left gaps at its sides one could look into).
  `tank_flanges_where_piped`.
- Crude is a mixture (Baughn 2026-11-10): `eln:crude_oil` stays a fluid (buckets, the pump's tank, other mods), but in a
  node it is heavy oil 35 / diesel 25 / gasoline 20 / residue 15 / natural gas 5 % by mass (the species JSON's "mixture";
  sulphur counts in the residue). Live, with its gas, only from the oil pump's sealed line; from a bucket or any
  capability it goes in dead ("the buckets don't have lids"), the four liquids renormalised; crude a save holds loads
  dead. A pumped crude line needs a purge valve (or a pipe taking its gas). Out of a node, a mixed liquid is named by its
  main species and taken whole (EaUnits.drainLayer), so a bucket can't distil a mix. The old column takes the cuts' mix as
  crude until it is a still. `fluid_crude_is_a_mixture`.
- Fuel heat furnace on a fluid node (Baughn 2026-11-10: "mixed on the machines"; real numbers): its 2-bucket tank is a
  fluid node (flange 4 px up on its front; buckets put any species' liquid in). Its breather on top is two vents: gas
  out (gas only, its float shut once the liquid nears the top) and air in (a vacuum breaker), each opening only past
  300 Pa, so live crude's gas leaves and air comes in as it burns, but a standing tank doesn't breathe its fuel away
  and a full one spills nothing (an open vent lost ~44 mB of gasoline an hour idle and spilled ~4 buckets when filled
  from a pressurised tank). `fuel_furnace_idle_breather`, `fuel_furnace_overfill`. The burner draws the bottom liquid layer whole and gets each species' `heating_value` (J/kg, new
  in the species JSON: gasoline 44, diesel 43, heavy oil 41, residue 40, natural gas 50 MJ/kg) times
  `heatFurnaceFuelFactor`, so a mix burns as the mix. Gasoline gives ~32 % more than the 1.12 table (a unit slip there);
  the others within 5 %. Coking follows the residue share of what burns (fouling from unburnt residue), scaled so dead
  crude cokes as before (`COKE_PER_RESIDUE` = 1/0.158; residue alone cokes ~6x faster, refined cuts never). Water at
  the burner boils off at the flame's cost and gives no heat. Other mods' fuels with no species no longer go in (none
  on Incognito). `fuel_furnace_mixed_fuel`, `fuel_furnace_coke_by_residue`, `fuel_furnace_water_boils_off`,
  `fuel_furnace_legacy_tank`.
- Pipe Purge Valve (Baughn 2026-11-10: "a valve that lets out only gas, never liquid ... it goes by phase, not species;
  it'll let out steam"): a safety valve with a float (shapeless: safety valve + bowl; blue cap). It vents the gas of the
  run under it to the air above its set pressure (1.1 bar to start, the safety valve's slider), shuts as the liquid
  reaches its outlet, and lets nothing back in (a gas-only, one-way vent: `Network.Edge.gasOnly`).
  `pipe_purge_valve_bleeds_the_gas`, `pipe_purge_valve_keeps_the_liquid`.
- Tanks need no floor (Baughn 2026-11-10): a tank, lone or a box, stays when the block under it goes, so a pipe can
  run to its underside port (it used to break). `tank_stands_without_a_floor`.
- Tank sight glass (Baughn 2026-11-03: "a viewglass"): a 3 px window in brass on the tank's front (was a 1.5 px slit
  in a texture half pale), the water in blue to its level and, when the vessel's gas is mostly steam, steam above it
  (else the dark of an empty glass: air). A box draws one glass on its front, the box's height but its own width.
  Tanks hold only water, steam and air (the fluid core's species), so there is no other liquid to colour. The render
  reads the fill and a steam flag (published when either changes). `tank_sight_glass`.
- Oil outlets (Baughn 2026-11-03: "the oil separator lacks a pipe output"): the tar sand separator pushes its crude out
  of the outlet pipe drawn on its back, and the oil pump out of its discharge flange (its right), up to 200 mB/s into
  whatever block there takes fluid (`fluid/FluidExport`). EA pipes still don't carry oil (the fluid core is water,
  steam and air), so the outlet feeds other mods' pipes or the next device directly. `oil_separator_outlet`,
  `oil_pump_discharge`.
- Tar sand separator basin (Baughn 2026-11-03): the trough is drawn open, its oil rising from the floor to the froth
  line as its crude tank fills (the fill is published after the machine's own data). `oil_separator_basin`.
- Pipe sizes (Baughn 2026-10-29): iron and alloy pipes drawn at a quarter of their old 4 and 6 voxels (1 and 1.5) and
  taking 2 voxels on the grid (occupancy, hitbox, routing; even, so centred on the lattice; their physics unchanged);
  the Large Alloy Pipe (tier 2) drawn and taking 6, 16 x the alloy's inner area (Baughn): volume 16 x, conductance
  32 x (d^2.5, turbulent; the network's linear form would give d^4), 40 / 120 bar. Fittings sized from the pipe's grid width;
  ones saved before (their "at" arrays held 4 or 6, no "g" key) load at 2. LP turbine, HP exhaust, plant condenser
  and tank ports take a large pipe (PipePart.LARGE_END_K). Kit: 8 alloy plates in a ring. `PipeSizeCases`,
  `shaft_plant_large_crossover_full_flow` (42 g/s, HP 13.7 + LP 15.0 kW on one shaft, 33 m/s in the crossover).
- Shaft redo, part 4 (Baughn 2026-10-29). Gas Turbine (same id, GasTurbineElement): fuel (FuelRegistry's gasoline,
  gas and diesel lists, by energy per mB: e.g. gasoline 1.74 kJ/mB, diesel 2.64, natural gas 2.43) from a 16-bucket tank
  by any pipe or capability; an open cycle, 28 % of the fuel's heat onto the shaft at rated speed, none under half speed
  (it must be spun up: a Shaft Motor); 10 kW; H 1 s, fuel valve 0.3 s; governor/trip/windage as the steam turbines, a
  signal on its front sets the valve by hand; the rest of the heat leaves as air at 500 C out of its back (a large
  pipe's port) or vents. Counterflow Heat Exchanger (transparent_node@5304): blocks stack 2 x 2 across and up to 8 long along their
  fronts into one exchanger (HeatExchangerGroup, as tank boxes: merge, split on break, shares saved per block), 50 W/K
  a 2 x 2 slice (Baughn 2026-10-29), 8 L a side a block; a lone block or any other shape is a quarter slice (12.5 W/K).
  Effectiveness-NTU for counterflow from last step's inflows (a boiling or condensing side, steam nearly all of its
  pressure, takes any heat at saturation); ports on the two end faces, A at their centres, B up high: A in at the back
  end, B in at the front. Separate jobs (economizer, steam raiser, reheat) are separate exchangers piped in series.
  `GasPlantCases`.
- Shaft redo, art pass (Baughn 2026-10-29). The HP/LP Steam Turbine, Gas Turbine, both Generators, Shaft Motor,
  Joint, Joint Hub, Flywheel, Tachometer, Plant Steam Condenser and Heat Exchanger have their own models
  (docs/models/shaft.md) instead of the 1.12 Turbine/Generator/GasTurbine/... ones: one shaft line for all, a
  half coupling at each shaft face, rotor/valve/needle parts that move. Hitboxes: a model may carry NAME.hitbox beside its
  OBJ; a descriptor that returns it from `modelHitbox()` (`ModelHitbox`) gets that outline and collision turned with
  the block's front (read from the block entity), instead of its collision boxes (`shaft_hitbox_per_facing`).
  Sounds: turbines play rotor tones, steam hiss or the new gas_turbine_roar, and an overspeed_whistle over 105 %; the
  LP's wet-steam knock (steam_knock) and the inline pump's cavitation (pump_cavitation) replace the vanilla anvil and
  grindstone. Jade's first line on turbines, the gas turbine and the plant condenser says why it isn't working.
- Placement of the shaft and steam machines (Baughn 2026-10-31, replacing the 2026-10-29 rule): shaft parts and the
  steam and fluid machines are placed facing the player (a shaft part's shaft runs across the view), and placing one
  never turns its neighbours. The wrench key with a click turns a shaft part, turbine, gas turbine, plant condenser, lone
  tank, lone heat exchanger, 50V/200V steam turbine or steam condenser a quarter turn; its shaft re-couples keeping its speed, its ports follow
  (`TransparentNodeElement.wrenchTurn`, `FluidPlacement.turn`). Tanks and exchangers in a box or stack don't turn. Before,
  placing turned the device and its neighbours to meet the most ports, and nothing turned them back.
  `machines_face_the_player_and_the_wrench_turns_them`.
- Battery Belt (new, Baughn 2026-10-27; `item/belt`, first shipped in Stasis Delvers, which aliases its old id
  `stasisdelvers:battery_belt` to this one): a Curios belt (optional dependency) with four pockets for EA portable
  batteries/condensators and `#eln:battery_belt_items`; right-click opens them, sneak-right-click wears it. An EA item
  battery standing for its contents (charged whole; gives only from what can discharge). Worn, it joins the player's
  item energy (ItemEnergyInventoryProcess) and shows a HUD bar (client BeltHud; `BatteryBeltItem.hudAbove` for a mod's bar
  above it). `battery_belt_pockets_and_charge`.
- DC-DC converter (Baughn 2026-10-27): connects to nothing (no masks, no loads) unless both winding slots and the core
  slot are filled (1.12 kept its sides connected through high-impedance loads); its screen frames the primary slot green
  and the secondary yellow, as the model's marks. `wp10a_transformer_connects_only_when_complete`.
- Safety valve (Baughn 2026-10-27): its set pressure on a screen with a slider (1.5-150 bar, log scale; the
  empty-hand right-click opens it, it no longer steps presets), and it acts on the pressure averaged over 2 s, wide
  open 10% over its setting (was the instant pressure and 5%: a boiler's tick-to-tick jitter made it pulse shut/open).
  `fluid_safety_valve_*`.
- Pipe Pressure Sensor and Pipe Temperature Sensor (new, Baughn 2026-10-27; `PipeSensorPart`): fitted on a straight pipe
  like the safety valve, one per block; the run's absolute pressure or temperature as 0-50 V on every signal cable in the
  block (full scale stepped by an empty-hand right-click: 2-100 bar, 100-500 C); the dial goes green to red.
  `pipe_pressure_and_temperature_sensors`.
- Pipe sensors as devices (Baughn 2026-11-02: fitted on a pipe the sensor ended up in mid-air, where signal cables
  can't go): the three pipe sensor items now place a small device (`sixnode/pipetransmitter`, hidden six-node ids
  `6402`-`6404` beside the probes) anywhere on a floor, wall or ceiling of a voxel block, its centre on the pipe
  routers' 4-voxel lattice. It has a pale signal pin on its front and a pipe port 4 voxels over its centre; a pipe kit
  route starts or ends at the port when the device is clicked (`PipePlacer.devicePort`), and the pipe ends capped
  there. It reads the run of the pipe through or ending at its port, same modes, full scales, right-click stepping and
  0-50 V as before; without a pipe it says "not connected to a pipe" and gives 0 V. It holds no fluid, so the fluid
  sim is unchanged; breaking it drops the sensor item and leaves the pipe (the player's own, paid for from the kit).
  Chosen over a stub the device would lay and own: a free stub dropped a pipe kit when mined (free pipe each place and
  break), had to reach a block face with free space to start a route, and lost its identity once a route joined it.
  Wears the Voltage Probe's model with a drawn iron stem up to the port. The items no longer fit a sensor on a pipe;
  sensors already fitted keep working. After the first client check: refused where it would touch a pipe, the device
  wins the crosshair over the pipe end at its port, a second route to a piped port is refused, each kind carries a
  coloured plate with a letter (P, T, Q), Jade's headline is reading, volts and full scale, and its own items are out of
  JEI. `pipe_transmitter_*`.
- Battery protection chips (Baughn 2026-10-27, after a protected, nearly empty battery burnt on a 50 V bus): the
  overheating chip is a charge limiter, run every electrical step (BatteryInventoryProcess.Step): charging held to the
  rated current (electricalStdI) by the cut switch's resistance, tapering to none as it nears the cut; the cut is at
  ~56 C and reconnects at ~45 C (1.12: on/off every 0.05 s with 1.6 K hysteresis, so an empty battery's inrush heated
  it past its limit before the check). The overvoltage chip adds a low-voltage disconnect (no discharge under 10%,
  charging still goes in, back over 15%; by energy, the share the battery's screen shows). Jade shows what the protection does. `battery_protection_*`.
- Battery chargers take power on any edge (Baughn 2026-10-27; 1.12: the front edge only, so a voxel cable along another
  edge gave nothing). `wp9a_power_socket_battery_charger`.
- Signal processor `daytime` is the time of day (Utils.dayFraction: clock mod 24000 / 23999); 1.12 divided the total
  clock time, so after the first day it only grew and `daytime>0.5` stayed true. `wp9c_signal_processor`.
- Plates in EA recipes (Baughn 2026-10-27): crafting recipes and pipe kits take `eln:high_quality_plates/<metal>`
  (OreDictTags.recipeTag), which holds EA's own plates, instead of `c:plates/<metal>` or the exact item; a modpack can
  add plates to it. EA's plates say so in their tooltip. `recipes_take_high_quality_plates`.
- 50V Compact Steam Turbine (new, Baughn 2026-10-27: the bootstrap option, eln:transparent_node@5284): heat on its left
  flank, water by bucket / top flange (float valve) / capability, 50 V front and back, no settings. 10% of the heat
  (Baughn) up to 300 W, so a stone heat furnace with 4 chambers runs it at full; governor from 105 to 140 C, bypass to
  its built-in condenser over 150 C (3.5 kW, costs fuel), safety valve over 180 C (costs water); 3% makeup water.
  Dry it stops without failing. `compact_turbine_*`.
- Pipe Control Valve (signal 0-50 V on its actuator's top) and Pipe Throttle Valve (slider screen), kinds of ValvePart
  (Baughn 2026-10-26). `pipe_throttle_and_control_valves`.
- 50V/200V steam turbines take water damage: over 10% of the inlet liquid while the shaft turns above 20% speed burns them out (`FailureCause.WATER`, repairable; the repair drains the inlet). Baughn 2026-10-26; `oil_steam_turbine_floods`. A casing drain throws away 0.5% of the inlet volume a second in liquid water (Baughn 2026-10-29: condensate must get out, slugs stay fatal).
- Cable slots removed: meters, electrical/voltage/thermal probes and lamp sockets have a built-in cable (LV, MV for the
  200V socket, copper thermal cable); Ground Cable has no GUI. Kept: transformer (turns), hub (side selection), breaker
  (trip rating); lamp supply and temperature probe slots are optional. Old cables stay in the hidden slot and drop on
  break. Guarded by `CableSlotCases`, `GoldenSlotsCase.removedCableSlots`.
- Six-node blocks (and voxel blocks) are waterloggable (`SixNodeBlock`, `VoxelNodeBlock`; test
  `port26_waterlogged_cable` in `PortHelperCases`). Shorting in water: not done.
- Failure: one per network of connected nodes per tick, the worst; the rest are relieved
  (`sim/Simulator.java`, `sim/process/destruct/FaultNetwork.java`; `BurnChainCases`).
- Removed: copper tools and armour, EA copper ore and copper ingot (vanilla copper), 50V/200V condensators, the
  50V/200V heat turbines (replaced by steam turbines with built-in generators), the Modbus TCP server.
- New in 26.x: oil (lakes, tar sand, pumps, heaters, boiler, column, flare stack), pipes and the fluid core, steam
  turbines, voxel cables (see voxel-cables.md), cable kits, chisel, item capabilities on machines (`ItemCapabilityCases`).
- Lamp sockets remove monsters that appear within 9 blocks only while lit (Baughn 2026-10-27; 1.12 did it for every
  socket, lit or not). Mobs already near a lamp when it lights stay (`MonsterPopFreeProcess`, `LampMonsterCases`).
- New in 26.x: the Electrical Engineer's Ledger (`eln:handbook`, Baughn 2026-10-27), a GuideME guide (`eln:handbook`;
  GuideME optional). Book + 4 copper cables; every player gets one once, when a slot is free (persisted NBT
  `eln_handbook_given`). EA ships a stub start page; a content mod adds pages under `guides/eln/handbook/` in its own
  namespace and calls `Handbook.setStartPage` (`mods/eln/handbook`, `HandbookCases`).
- New in 26.x: the servo layer (`mods/eln/robotics`; Baughn 2026-11-02: the servo layer first, 200 V only, players ride
  the carriage now: "an elevator would be the earliest truly useful output"). The first device is an elevator:
  - A Linear Servo Drive (`eln:transparent_node@5504`) with a Servo Rail mast on its top, 2 to 32 blocks, moves an
    elevator car up and down on the drive's front side: a cabin 3 x 3 x 3 outside (Baughn 2026-11-05; about 2.5
    inside) over the 3 x 3 blocks in front of the drive, its back wall on the rail side, the side walls and roof solid,
    the front (away from the rail) open. The plate is the car entity (3 x 3 x 1/16); the walls and roof are its part
    entities (`CabinWall`, thin boxes from the car's frame), solid as the plate is: on the client to its own player,
    on the server to every creature but players. The whole cabin sweeps the shaft (a block or a creature outside it in
    the way stops it); creatures already in the cabin go with it. A stopped car has floor ghosts under all 9 blocks.
    GameTest `servo_elevator_cabin_walls_hold`.
  - The car's panel (`ElevatorPanel`, a thin board inside the back wall; Baughn 2026-11-05: real parts in it): 3 x 4
    slots, each empty or holding an Addressable Button or Light, put in by clicking a slot with one in hand (sneak with
    an empty hand: it comes back). Slot i's address is the panel's first address (the drive's screen, "Car panel
    from") plus i, left to right and top to bottom as a rider sees them. A button slot sends `press` from its address
    when clicked; `power` to a slot's address lights its lamp. The drive keeps the slots (NBT `panel`, `panelBase`,
    `panelLit`; they drop when it's broken) and a bus port per filled slot on its own bus (the travelling cable:
    nothing moves block to block); the car shows them (synced entity data). GameTest `servo_elevator_car_panel`.
  - Floor Sensor (`sixnode/busdevice`, id `6 + (117 << 6)`; Baughn 2026-11-05: floors found by sensors, not markers):
    the proximity sensor's model, an address in its screen, a bus pin in a voxel block. Register 0 is 1 while a car
    stands with its floor level with the floor of the sensor's block, up to 4 blocks across; when that starts it sends
    `arrive` (index 0) to everyone from its address. Recipe: an Electrical Entity Sensor, a cheap chip and Field Bus
    Cable. GameTest `servo_elevator_floor_sensor`. The car's floor
    stops level with the drive's foot at the bottom and with the top rail block at the top.
  - Commands (Baughn 2026-11-05: the drive is a plain actuator, an addressed RS485 device; no Station Markers, no
    setpoint bands, no panel stepping): its setpoint wire, a share of the travel to the nearest block, while connected;
    else its registers on the bus: 0 target (blocks above its foot), 1 position, 2 speed limit (m/s, 0 none), 3 state
    (idle, in position, moving, fault), 4 load (kg). It takes `write` (0 and 2), answers `read` with `reply` to the
    asker, and sends `fault` (its code) to everyone when a fault starts. Its address (17..255, NBT `addr`) is set in
    its screen (an empty hand). Every planned stop is on a whole block. The Station Marker stays registered (worlds
    that hold one load cleanly) but has no recipe, isn't in the tab, and a click says it's no longer used; the old
    `proportional` NBT key is ignored. The car's panel says how the elevator is driven (it has no buttons yet).
  - Connections: 200 V on the back. Every pin is on a flank's floor edge (Baughn 2026-11-05: a ceiling pin can't be
    wired with the rail on top; several pins per edge, as the displays): seen facing the flank, the left flank has the
    setpoint in the middle, in-position on its left and position (share of the travel) on its right; the right flank
    enable in the middle (unconnected counts as enabled), fault on its left and the bus on its right. A plain cable
    meets only the middles; voxel cables meet each pin (Machine API 1.1: `CablePosition.FLOOR_LEFT`/`FLOOR_RIGHT`,
    `bus(face, position)`, `FacePins`, `VoxelNode.connectPeerPins`). GameTests `servo_elevator_driven_over_the_bus`,
    `servo_elevator_follows_its_setpoint_wire`.
  - Electrics: a Machine API consumer (`Machines.forElement`, the API's first new device): 200 V, rated ~3 kW (40 W
    idle plus lifting 120 kg net, two riders over the counterweight, at full speed; tsugumi-lab's choice),
    `Brownout.SLOW`. Each tick it asks for its step's work against the net weight over 80 % (a counterweight balances
    the car and 40 kg; players count 80 kg, other creatures by size): a car carrying more than 40 kg pays to go up, an
    empty or lighter one pays to go down (it lifts the counterweight), and the other way costs only the idle draw. It
    moves only as far as what it got pays for: short, it goes on from there at the speed paid for, its new top speed what the line keeps
    delivering (continuous in position; the speed steps down, a lift braked by its load). So a weak or starved line, or
    a load over its rating, makes a slower car that still arrives; the machine's buffer (two ticks of rated power) can
    pay for a faster first second. Top speed also follows the line: 2 m/s at 200 V, 1 m/s^2, a sag or recovery
    mid-move retargets it (up only once energy piles up in the buffer, so a starved car doesn't surge and fall back);
    below 30 % of 200 V it holds, and runs again after half a second over 35 %. On a line weaker than it, the API keeps
    it at the line's maximum power (about half the volts) instead of pulling the line down.
  - Heat and trips (the API's heat model): 4 kJ/K, 4 W/K to the ambient where it stands (EA's Ambient), heated by
    the idle draw and the motor's losses; 80 K over its ambient (as the old drive: 100 C at 20 C, 170 C in the
    Nether, where it idles at 100 C) it trips, faults and holds, and runs again 3 s after it is 30 K under that (as the
    old drive's 50 K over ambient). The API's trip is a rise over ambient, so a hot biome costs it no margin. Over 1.3x 200 V it trips (fault "overvoltage") and
    holds until the line has been under 1.1x for 3 s: a servo amplifier's protection, so it never burns. A trip stops
    the car dead, as any fault; a dead line brakes it where it can (lifting, it stops at once). Reloaded mid-move, its machine runs
    on (the API keeps a running machine's state).
  - Safety: before each step the car and its riders are swept to the next position; a block or a creature that isn't
    riding stops it dead (fault; it clears on the next call or an enable toggle). Disabled, it stops at the next whole
    block it can; a dead line brakes it where it can.
  - Sync (design: Argon's review): the drive owns the motion (a Segment on its own saved clock, which stops while its
    chunks are unloaded) and places the car on the server; clients get the segment (`SegmentPayload`) and run it on a
    local clock, carrying their own player after its tick. The car has no vanilla position sync and is not saved. On
    the server it is solid to mobs and items, which it carries, but not to players, whose positions there are a round
    trip late. A stopped car has a floor ghost under its plate (a player logging in on it stands on that). A mixin
    keeps a rider from the floating kick when allow-flight is off.
  - Departures from the design: the panel is a part entity (the car's collider is only its plate, so the plate can't
    also be the panel's hitbox; its id is the server's entity counter's, sent in the car's spawn packet, never assumed
    to be the car's + 1); joiners get the segment in the car's pairing bundle rather than in startSeenByPlayer
    (which runs after the spawn bundle); on the server, riders' sweep is clipped to the car's column (a rider's
    overhang meets the landing, it doesn't fault the car) and a player up to 1 m off the top (a round trip late) still counts as a rider.
  - Tests: `ServoCases`, `DriveTest`, `SegmentPropertyTest`, `StationsTest`, `RidersTest`, `SegmentPayloadTest`.
- Tools (Baughn 2026-11-03): a pickaxe breaks machines, devices and pipes as fast as stone, shears break cables as fast
  as wool; in a block holding both, the part under the crosshair decides. No part breaks in under two ticks, so with
  vanilla's pause after each block it is at best one part per 7 ticks (`node/BreakTool`, `BreakToolCases`).

### Multi-fluid pipes (2026-11-03)
- Baughn: EA pipes carry oil "the complex way": fluids are data (datapack species JSON), Raoult mixing within a
  miscibility group, fluids no species names refused at EA's capabilities, viscosity depending on temperature now
  (solidifying later, if ever), crude as one species, fire from hot leaks as a last phase.
- The fluid core carries any species (simulation.md); water and air alone are bit-identical to the two-species core.
- Oil devices on EA pipes through PortBridge (content.md, Oil). The separator's outlet flange moved from 2.7 to 4 px up
  so a pipe end can meet it (Baughn: "definitely move it"); the pipe router reaches flanges off its lattice by a jog.
- A pipe's open end passes liquid as far as it is wet (Baughn: a part-full run held its puddle; water too).

- **Real-world voltages and realistic converters** (2026-11-06, board #55): tiers 48 V / 230 V / 400 V / 11 kV (1.12: 50 /
  200 / 800 / 3200 V), signals 0-5 V (1.12: 0-50 V), names follow ("48V Macerator"); the 230 V cable carries 3 kW. DC-DC
  converters take up to 64 turns a side; a core has a volts-per-turn limit (cheap 4, average 8, optimal 16 V): past it the
  core saturates, draws a magnetising current and heats, and the converter burns; a winding over 1.3 x its cable's
  current heats it too. The Variable DC-DC Converter taps its secondary by a signal on its front. Old converters are
  rewound once and kept as built (no saturation) until changed (docs/persistence.md).

## Behaviour changes vs 1.12

- Meters, probes and power sockets no longer burn at their (former slot) cable's voltage: those watchdogs are off.
- Fires lag their setting (coal ~3 min, oil ~20 s), go out below 15%, take time to relight (`sim/Fire.java`, `FireTest`).
- Battery wear divided by `batteryLifeFactor` (default 25), doubled every 10 C above ambient, up to 4x at low charge.
- Burnt devices (Baughn: they kept their temperature, made their sounds and opened their GUI) keep only
  their thermal loads in the simulation: they cool to ambient and do nothing else, make no working sound or
  animation, give no light, and a right-click only says what repairs them (a meter still reads them); no GUI.
  Contents drop when broken. See simulation.md "Failures"; `BurntCases`.
- Repair parts cost less than the device (Baughn: a stone heat furnace took a Machine Block, which cost more than the
  furnace). Jade shows a burnt device's cause and repair part.

  | Device | Old repair part | New |
  |---|---|---|
  | Stone Heat Furnace, Fuel Heat Furnace | Machine Block | Combustion Chamber (4 stone) |
  | 50V / 200V Electric Heater | Cheap Chip (overvoltage), Machine Block (overheat) | its 50V / 200V Copper Heating Corp |
  | Large Rheostat | Machine Block | Power Resistor |
  | Other machines that overheat (thermal dissipators, smelter, boiler, column base, compact steam turbine, heat turbines) | Machine Block | Copper Thermal Cable |
  | Machines on overvoltage (macerator, compressor, magnetizer, plate machine, exporter, pumps, ...) | Cheap Chip | unchanged |
  | Electronics, transformers, capacitors, shaft machines, cables, batteries, HV gear | by kind | unchanged |
- The FE exporter is a transparent node: it burns out in place and can be repaired instead of vanishing; old
  simple-node saves are upgraded on load (`transparentnode/energyconverter/ExporterSaveUpgrade.java`,
  `ExporterSaveUpgradeTest`). `SimpleNode` and its block/BE/item are gone.
- Nodes that cannot load (including a `LinkageError`) are kept as orphans and written back unchanged; an orphan is
  dropped, with a warning, when its block is removed, and placing a six-node device on its block is refused
  (`node/NodeManager.java`, `NodeSaveLoadTest`, `SixNodeBreakCases`). A node whose save throws is written with its
  last good data instead of being left out. Nodes whose block is gone leave the simulation (`node/NodeOrphans.java`,
  `/elnlab remove`).
- A six-node face whose device cannot load is kept as saved data and the face stays taken (placing on it is refused
  with a message; breaking the block discards it, also a voxel block holding nothing else) instead of being dropped
  from the next save (`SixNodeLoadTest`, `SixNodeBreakCases`).
- An `electricalAgeWorld<dim>.dat` or `.dat.bak` with bad content is renamed to `*.corrupt-<time>` instead of being
  overwritten by the next save; one that can't be read at all (permissions, I/O error) is left in place and the
  dimension is not saved that session (an error at every skipped save). With no `.dat`, a whole `.dat.tmp` is loaded
  before the older `.bak` (`server/ElnWorldSaves.java`, `ElnWorldSavesTest`).
- A loaded six-node connects once, not twice; a client packet for a bad or empty six-node side is ignored instead of
  throwing (`SixNodeLoadTest`). Voxel blocks invalidate capability caches when pipe adapters come and go.
- Portable batteries charge items in the offhand too. Sounds: render loops play client-side (`sound/LoopedSoundPlayer`).

- Electrical oven: above 280 °C (Baughn: 300 first; an iron element tops out at 300) food (anything with a food component) chars to a charcoal nugget instead of cooking
  (Baughn 2026-10-23; `ElectricalFurnaceProcess.CHAR_T`). Charcoal nuggets: four to a charcoal and back; fuel for 400
  ticks (a `cooking_fuel` component). Test `wp10a_electrical_oven_chars_food_above_280_c`.

- New: the Smelter (`transparentnode/smelter`, Baughn 2026-10-23): heat from a thermal cable on its back, 20 W of
  50 V for its controls on a side; smelts at the electrical oven's rate for its temperature (same PfT table), drawing
  that heat; hoppers: top/sides in, bottom out. Model `mods.eln.cad.models.Smelter`. Test `wp10a_smelter`.

- Stone heat furnace remodelled as a coal oven (Baughn 2026-11-03, model `mods.eln.cad.models.HeatFurnace`, id
  `stonefurnace`): a firebrick oven with a cast-iron firebox door whose glass window shows the fire, as high as its
  burn level and from dull red to orange-yellow with its power (against all four chambers' 3 kW), an ash door, and a
  flue to an iron pipe's flange at the top face's centre (drawn only; no port yet). The 1.12 drawer that opened when a
  player came near, and the fuel item drawn in it, are gone. Test `wp10b heat furnace model`.

- Fuel heat furnace remodelled as a small oil-fired heater (Baughn 2026-11-03, model `mods.eln.cad.models.FuelHeater`,
  id `fuelheater`, parts renamed lower case): a painted housing with a cast-iron front plate, the installed burner gun
  (big, medium or small) on a mounting flange, the power and heat lamps above it, a sight port whose flame shows as
  high as the fire and from dull orange to bright yellow-white with the power (against the big burner), an iron pipe's
  flange for the fuel low on the front (centre y 4, z 8) and a flue to one at the top face's centre (both drawn only;
  no ports yet). Test `wp10b fuel heat furnace model`.

- New: the Steam Oven (`oil/SteamOven*`, Baughn 2026-10-23): steam into a port on top (as the turbine's), a 10 W fan
  on 50 V; cooks food only, at up to 280 W (twice the electrical oven's best food speed), condensing 280 W / 25% of the
  steam's heat (~500 mB/s at full speed; the rest vents as a plume); hoppers: sides in, bottom out. Model
  `mods.eln.cad.models.SteamOven`. Test `steam_oven_cooks_with_steam`.
- Devices (transparent and six-node) can be placed against any full face, not only opaque cubes (Baughn): a top
  slab, the underside of a bottom slab or stair, a stair's back, glass (`TransparentSupportCases`).
- Egg incubator: its windows are glass (the 1.7.10 texture left them fully transparent, so the model looked like a bare
  frame; Baughn), and the egg sits higher and larger in the nest (it was half sunk in the bowl).
- Boiler: a feedwater pipe flange at the centre of its right flank (`front.right()`), into the vessel through a check
  valve (Baughn). Device links can be check valves now (`FluidDevice.Link.kind`); a shut one is left out of the
  network's pressure limiter (`boiler_feedwater_flange`, `FluidCoreTest.checkValvePassesOneWayOnly`). A float valve on
  that inlet closes over the last 10 % of the gauge glass (Baughn 2026-11-04: a boiler shouldn't overfill; a condensate
  pump had fed one eight times its rated fill from a makeup tank). Jade shows the vessel's real water, "overfull" past
  the rating.
- Boiler: a level signal on its front (Baughn: where the dials are), analog since 2026-11-01 (Baughn: so a boiler can
  in principle run on little water, with the real downsides): the gauge glass's reading (fill of the 8 B tank) as a
  share of the signal range, which a logic input still reads high above half. Below a quarter full its heating surface
  uncovers in proportion and passes only 5 % as much heat into steam as into water, so the shell runs hot and, boiled
  dry, burns out (`BoilerElement.heatingShare`). Heat goes in on the back and flanks only. The gauge glass shows the
  fill and the dial the water temperature (0..300 C) (`boiler_phase_signal`).
- Submersible pumps, 50V (100 W) and 200V (400 W) (Baughn 2026-10-25): placed in a water source (transparent-node
  blocks are waterloggable now, as six nodes were), they push the water they stand in up a pipe from the flange on
  top: 3.5 turbines' worth of feedwater (57 / 230 mB/s), falling to nothing at 3 bar against the pipe. Power on the
  flanks, a signal on the front sets the speed (no cable there: full speed). Model `mods.eln.cad.models.SubmersiblePump`
  (`submersible_pump_feeds_a_boiler`, `submersible_pump_water_and_signal`).

- Logic and analog chips are drawn at 0.75 (a case 6 voxels across; Baughn) and, in voxel blocks, go anywhere on a
  face; pins that touch connect (docs/voxel-cables.md, devices off centre).

- The macerator, compressor, magnetizer and plate machine (50 V and 200 V; the tar sand separator shares their
  element) and the FE exporter run on the Machine API (`Machines.forElement`; Baughn: the four main machines and the
  exporter first, to test the API). Same nominal volts, rated power, energy per item and FE per joule; at or under
  nominal volts they draw exactly what their resistor drew (`MachineApiCases` runs that model alongside, at 1x and
  0.7x the line). Differences: above nominal volts they no longer speed up past their rating (the exporter takes no more than its
  maximum power); they start half a
  second after power comes (the API's power-up hold); overvoltage (over 1.3x nominal, either polarity for the
  machines) burns them at once instead of after the watchdog's budget, and a repair brings them back; the exporter
  no longer burns on a reverse voltage under -0.2x nominal; its machine holds up to two ticks of power in front of
  the FE buffer (lost when it is broken, as the buffer is). A machine's progress on the item in hand is saved now
  (it restarted on every load). Jade's "Power consumption" is the power drawn (it showed the power times the
  boosters' efficiency). Their terminal's series resistance is the LV cable's for every voltage: a 200 V machine's is
  0.025 instead of the MV cable's 0.1 ohm, at its 10 A 0.75 V less drop (under 1% more power).
  On a line weaker than the machine (its resistance above the machine's), they now draw the line's maximum power at
  about half its volts instead of their resistor's power far below that: the API's slow consumers plan on the line
  the solver shows them (its Thevenin equivalent at their terminal, read after each step), so alone they never pull it down past its maximum power (found with the Linear Servo Drive). A row of
  them on one weak line shares it as their resistors did (in proportion to their ratings, settling at their rated
  resistances when only the row together is heavier than the line); one heavier than the line takes the most it can
  beside the others (`BenchTest.slowConsumersShareAWeakLine`).

## Bugs fixed

- Shafts stored mass^2 x 0.25 J per rad/s (linear in speed): a line's inertia grew with the square of its total mass,
  and a joule bought as much speed at 10 rad/s as at 800. Now E = 1/2 I w^2, I summed over the blocks, each sized by
  an inertia constant (Baughn 2026-10-29: realistic values now that boilers are slow); `ShaftNetwork.drive` limits a
  machine's torque from rest. Only the speed is saved, so worlds keep their shaft speeds. `wp11 shaft network`.

From 1.7.10/1.12 (carried in from the 1.12 branch, before the port): the WP16 sim audit (`AuditFixesTest`), fuse and
breaker trip points, generator energy accounting, a truth table per PAL chip (every PAL shared one).
In the port:
- 1.12 `ItemStackFilter.OreDict` masked damage to 8 bits (Mercury, Gold Plate passed as Coal Dust;
  `GoldenSlotsCase.KNOWN_MISSING`).
- Power sockets could never be connected in survival (1.12 container had no slot).
- `while(true)` hang traps in `SixNode` connection/disconnection and the thermal cable check now log and continue;
  disconnecting a node outside the simulation still drops its links.
- Six-node breaking takes one element and drops it (`SixNodeBreakCases`).
- A transparent device that loses its floor, ceiling or wall is broken like a block and drops with its contents; it
  used to vanish through the failure path (`TransparentSupportCases`).
- From the AGE fork's history (Baughn 2026-10-25, board #32): µ readings were 100x too small (plotValue; M added);
  a cut spinning shaft stopped dead past the cut; an empty flashlight switched on; VCA and Animal Filter had no recipe;
  NaN resistances poisoned whole subsystems (now an open circuit, reported by NanGuard); tall six-node lamps vanished
  off screen; an open breaker at 1e16 ohm on the 51.2 kV grid solved to -3e20 V (ultraImpedance is 1e12 now); the power
  socket was 1.12's stub and is now AGE's wireless outlet fed by a lamp supply's channel.
- Six-node devices (also those hosted in voxel blocks) need their wall too (Baughn): when the face they sit on stops
  being full, they drop with their contents; a six-node block with nothing left goes (`SixNodeBreakCases.LostSupport`).
  1.12 never checked: they floated.
- MNA: a cable Line ending at ground crashed the step; a removed Monopole left its pin.
- Turret shots asked for light 25 and crashed the light block (now clamped; `TurretCases`).
- Item capabilities (hoppers, pipes) never worked in any EA version; now they do.
- Right-clicking an item into a device slot (battery charger) made a new stack and copied only its energy: an X-ray
  scanner lost its filters and a used lamp came back new (lamp sockets use the same path). The stack goes in as it is (`XRayCase`).

- Three portable batteries made an empty Portable Battery Pack (1.12's plain shapeless recipe; Baughn 2026-10-31).
  `BatteryPackRecipe` sums their charge into the pack; `golden_recipes` accepts a custom recipe in a shapeless one's
  place when the 1.12 grid makes the same item. Test: `battery_pack_keeps_charge`.

- A six-node device's screen stayed open after that face was broken while the block stayed (another device or a cable
  on it, a voxel block), and its contents were both on the ground and in the screen: a dupe, or a loss for anything put
  in after. The screen now closes with its device, and dropped contents leave the inventory
  (`six_node_device_menu_closes_when_its_face_is_broken`).

- Two devices on adjacent faces of one block shared heat only on some corners: `SixNode.tryConnectTwoInternalElement`
  paired the wrong thermal loads (1.12). Test: `six_node_inner_corner_a_temperature_probe_reads_a_wall_cable_s_heat`.

- GUI packets were trusted (review of 2026-11-09): a modified client could store a NaN FE exporter power factor
  (its process then threw in every simulator tick, and the factor was saved: the server crashed again on start), a
  negative teleporter or turret power (a negative resistance), an index past the signal hub's links, the wireless
  receiver's or a lamp supply's aggregators (a throw in the tick, saved), sensor bounds of Infinity (a NaN signal;
  the stock text box sends Infinity for "1e39"), NaN through the `max(lo, min(hi, x))` clamps of the heater, gate
  source and steam valve, and names of any length. Every handler now goes through `ClientValues` (slider values
  clamped, typed numbers finite, indices in range, strings cut to 64 characters); bad saved values are clamped on
  load; the Modbus RTU caps its Tx and Rx entries at 32 each. Two bugs the same test found: a Modbus RTU with two
  or more transmitters threw on unload, and a fuel heat furnace's switch turned on with no burner was off after a
  reload (it now turns on only with a burner, as loading kept it). Test: `gui_packets_reject_bad_values_*`.
- One NaN input (0/0 from two pins at 0 V) stuck in a signal processor's `rc`, `integrate`, `pid` or `derivate` for
  good, and was saved: the program output 0 V until it was retyped. A step whose new state would not be finite is
  skipped (the state holds, as `ramp` already did), and a NaN in a save loads as 0. A watchdog that saw one NaN sample
  never fired again; it now skips the sample and reports it through NanGuard. Inductors and voltage sources load a
  non-finite saved value as 0. Tests `StatefulNanTest`, `ValueWatchdogNanTest`, `NanGuardTest`.
- A turret ended its search at the first entity in its window that its filter spared, so a cow in front of a zombie
  kept it from firing (1.12). It skips that entity now. Test `turret_looks_past_a_spared_entity`.
- Range checks ignored the dimension (1.12 `trueDistanceTo` compared x, y and z): a wireless transmitter or lamp supply
  in the Nether reached devices at the same coordinates in the Overworld, and two utility poles in different dimensions
  could be linked for a few blocks of cable. The distance between dimensions is infinite now, a starting pole in
  another dimension is forgotten, and so is a player's starting pole on logout. Test
  `dimensions_keep_wireless_and_poles_apart`.
- Auto Miner below y = 0 (1.12 never went there): each pipe segment's ghost id was its height, so the segment at
  y = -1 had the machine's own id and breaking it broke the miner. Pipe segments share one id now. The stacks waiting
  for room in the chest are saved and dropped when the miner is broken (1.12 lost them). Test `autominer_below_zero`.
- The Auto Miner mined anything but air, liquids, obsidian and bedrock, and with silk touch handed out spawners, budding
  amethyst and end portal frames as items. It leaves alone blocks with a destroy speed below 0, the
  `#minecraft:wither_immune` and `#minecraft:features_cannot_replace` blocks (spawners, trial spawners, vaults, chests,
  reinforced deepslate, command blocks) and budding amethyst: in the shaft they end it as bedrock does, around it they
  are skipped. A finished or waiting miner looks for work once a second (1.12: every tick, rescanning its whole layer).
  Test `autominer_refuses_unbreakable_blocks`.

## Deferred

- Sound redesign: synthesize machine sound client-side from device state; one-shot sounds are still server packets.
- Modbus RTU: element ported, render and GUI not; registered without a creative entry while `Eln.modbusEnable` is false.
- Other languages: only the generated `en_us`; 1.12's 40+ `.lang` files to convert.
- Config: the rest of the 1.12 balancing values.
- Chunk baking of static device geometry (voxel nodes are already baked into the chunk mesh); only if playtests need it.
- In-game wiki guide (`wiki/Data.java` only collects descriptor groups), ore regeneration.
- Servo layer: horizontal rails (the car and its payload carry an axis; the drive only scans up), larger platforms, bus
  I/O on the drive, the drive's own screen. Other players and mobs riding a car are drawn by vanilla's interpolation,
  so they may trail the car by a tick or two; only the local player is carried in step with it.

## Open questions

- Breaker: optional cable slot (empty = voltage trip only) instead of the required one?
- Should open, unloaded cables still burn from overvoltage (`BurnChainCases`' 1000 V case burns two)?
- Voxel cable conversion on load (`[cables] convertOnLoad`) is off until approved.
- `LampSupplyElement` (`sixnode/lampsupply`): `setWirelessName` registers the wireless name as a power channel
  (1.7.10 behaviour, listed as a bug to fix, unchanged).

## History and next steps

The 1.12 tree is `upstream/ports/1.12` (local `port-1.12`); this port lives on `develop` (it was `ports/26.3` until 2026-11-04). Phase Q (after
parity and Baughn's sign-off): port the age-series fork's features (github.com/age-series/ElectricalAge, ~654 commits
on 1.7.10) onto 26.x.

- Power Capacitor (six-node) leaks with RC 30 min, not 1.12's 33 h (Baughn 2026-10-28; config
  `capacitorBank.powerCapacitorLeakTauSeconds`, `GoldenCase.CHANGED`). With the compressor's four Dielectrics a sand (was
  one) a row of them would have been a cheap, non-wearing bulk store; batteries stay the only sensible one. Saved
  capacitors keep their charge and take the new leak on load. GameTest `power_capacitor_leaks_like_the_bank`.

- Oil lakes (todo #138, Baughn 2026-10-30, after Argon's review): new lakes are `oilLakes.sizeMultiplier` (2) times as
  big, all by depth (same footprint; the scale is saved in the lake's structure piece, 1 for older lakes). Below
  `oilLakes.tailShare` (20 %) of its capacity an oil lake refills by seepage, `seepShare` (18 %) of a 230V oil pump's
  nominal rate (~5.6 mB/s), into the lake and back up to the tail level (Baughn 2026-11-08: the seepage refills the
  reservoir, so more pumps don't mean more oil; until then it went to the pumps directly and the lake stayed at its tail
  level). It is caught up lazily by game time (`seep_time` per lake in `eln:oil_recovery`). Surface ponds (`oil-pond:`)
  don't refill (Baughn 2026-11-08). GameTest `oil_lake_refills_to_its_tail`. Steam pushed down a
  second well casing through the Steam Injection Wellhead (`eln:transparent_node@5200`, new; above the casing's head of
  crude, ~0.08 bar a block) pays for crude at `steamKgPerBucket` (0.2 kg), and the lake's pumps run at full rate on it
  (credit per lake in SavedData `eln:oil_recovery`, at most a pump tank's worth). Water lakes still run dry. Residue is a
  fuel for the fuel heat furnace (~41.7 MJ/l); heavy oil already was. GameTests `oil_lake_seepage`, `oil_lake_steam`,
  `oil_lake_weak_steam`, `oil_heavy_cuts_burn`, `oil_lake_sizes`.

- Safety valve and pipe sensor (board #44, Baughn 2026-10-30): drawn to the pipe they sit on as drawn (iron 1 px,
  alloy 1.5 px), not the old 4-voxel pipe: they stand on it from its axis (they floated half a voxel above the thin
  pipes), half the old size on iron, a little more on alloy, the old size on the large alloy pipe. The cap and the dial
  are stacked on the body and stem; the safety valve's cap used to end in the body's top plane and z-fought. Hitboxes
  are unchanged. VoxelGeometry is built on the checked model library (docs/cad.md); everything else draws exactly as
  before (VoxelGeometryCadTest).

- Bikes (board #44, Baughn 2026-10-30): the battery stands on the down tube's drawn surface on both bikes (it floated
  0.3 px over the bike's tube and nearly met the terrain bike's fatter one); it stays in the BASE group, so the frame
  dye doesn't tint its stripe. Its green stripe is now one row of texels round its sides and ends, the same row on
  every side face, so it meets at the edges (the old one was painted across the region without regard to the box's
  unwrap, half on the top). Vanilla 26.x doesn't truncate fractional box sizes in the unwrap (`ModelPart.Cube` takes
  them as floats; only the texture corner is an int), but a fractional face edge falls inside a texel, which then shows
  on both faces. The bike is built from BikeShape's solids (docs/cad.md) and draws exactly as before otherwise
  (BikeModelCadTest); the texture is painted by BikeTexture (`./gradlew bikeTexture`), its noise reseeded.

- Z-fights the checker found (board #44, Baughn 2026-10-30), fixed with the least change to the look:
  - cable bends, tees and crosses: a cable box is cut back where an earlier one in the block covers its whole
    cross-section, so their tops no longer overlap (the texture is unchanged: it's mapped by position);
  - a pipe ending under a pipe adapter draws no flange or end cap of its own there (they lay in the adapter's face);
  - two bends of a large pipe close together: the later fitting ring is turned half a side (12-sided, 15°);
  - bikes: every other rim segment narrower (0.1 px a side; the terrain tyre's 0.2, clear of its knobs), the seat stays
    0.1 px further in than the chain stays, the top tube 1.3 px instead of 1.5, the saddle's rear box 0.1 px lower on
    top and higher underneath.

- Bikes, joined up (board #44, Baughn 2026-10-30: every piece attached through a real cross-section, checked by
  Checker.connectivity): a fork crown under the head tube holds the blades' tops (they met the head tube beside it,
  18 % of their section); the pedals run through the cranks' ends (they hung off the side, 28 %); the stem's top goes
  0.3 px into the bars (45 %, hidden); the rack braces sit at x 1.4 instead of 1.9, ending inside the seat stays (they
  touched the stays' sides, 26 %). The voxel parts needed nothing.

- Signal processor programs (Baughn 2026-10-30, board #45): the expression may be a program, `name = expression`
  lines (or ;-separated statements) and a last line with the output; an old single expression is still the output line
  (= in it is still "equals"). Components are computed once a tick in order, definition before use; a stateful function
  in one keeps one state, saved under the component's name (the output's keeps the old "equation" keys). Cost: each
  line's operators once, paid in cheap chips, ceil(N / 4); the slot takes cheap chips only, and a processor saved with
  redstone loads with ceil(count / 4) chips. Programs are capped at 4096 characters and 40 lines; the server refuses
  bigger ones. `rs` now steps once a tick in its process (it stepped when read: never inside an if's branch not taken).
  Tests `ProgramTest`, GameTest `wp9c signal processor`.
- Data logger history (Baughn 2026-10-30, board #45): the logger records the input's mean every tick, whatever its
  period, into `LogStore`: the last 51 s tick by tick, then buckets of 0.2, 0.8, 3.2, 12.8 and 51.2 s keeping
  minimum, maximum and mean (up to about 7 h), as half-floats of the signal (0..1). The period only chooses the view,
  so changing it no longer wipes the log (0.5 s -> 5 s -> 0.5 s shows the same fine data); nor do scale and unit,
  which are per channel now and only change the drawing. The graph shows each point's range as a band, so short
  spikes survive a long period. 1.12 kept one byte per sample, shifted the whole array on every write and cleared
  on a period change. Clients get the 256 points of the view when they start tracking a logger or the period
  changes, then one point (6 bytes a channel) as each is due. Old saves and prints are converted (only the last
  ~7 h of an old log fit). Tests `LogStoreTest`, `LogAxesTest`, GameTest `wp9c data logger`.
- Loggers in the world (Baughn 2026-10-31): the graph on a logger's screen (data loggers, the tilted terminal, the
  processor's tiny screen) is a small texture painted when a point comes (`LogTexture`, 128 px wide), drawn like the
  screen's graph: newest on the right, a faint band for each point's range, a 1 px line for its mean (the 1.12 strips
  were thick and z-fought with themselves). The tilted terminal's inputs move to the next edge with shift + the wrench
  key (C) on a right-click (C alone turns the logger), saved as `pinTurn`; its terminal strip is drawn there. The
  processor's Apply is Save, red while the text differs from what the processor holds; the channel buttons are
  labelled Edit. Terminals reach 0.1 px past the block's edge so a cable knuckle in the corner (a floor cable turning
  up a wall to a wall-mounted device) lies inside them; `CableEnds` checks it when the models are made.
- Signal processor model (Baughn 2026-10-30, board #45: "DIN rails in a cupboard with arithmetic modules"): an open
  cupboard with three DIN rails; a module appears per cheap chip's worth inserted, up to 3 x 6, each face a little
  different, the logger's screen across the top. The slot takes 64 cheap chips, or Advanced Chips worth 8 cheap chips
  each, up to 8 (the same 64 cheap chips' worth; `ElnSlot` asks the container for a per-item cap). No coloured pin
  stubs: the brass terminals meet the cables. Its terminals reach the block's edge over the cable's end and are left out of its box.
- Terminals meet the cables (Baughn: "zoom in on the connectors: a gap"): the loggers' and the processor's terminals
  sit on the face and reach the block's edge, covering a signal cable's 1 x 1 px end either way round; the model build
  checks it (`CableEnds`).
- Tilted Terminal Logger (Baughn 2026-10-30, board #45): a three-channel logger (same pins and screen code) whose
  screen leans back 45 degrees (cad `TiltedTerminal`); placed on a floor or table it is a desk terminal, high on a
  wall (its front down) it leans out and down, hung from a ceiling (side YP; the model upside down, the graph turned
  half round so it reads) it faces a player about three blocks below. Recipe: a Three-Channel Data Logger, a glass pane and an iron ingot (shapeless). GameTest
  `wp9c tilted terminal logger`.
- Three-Channel Data Logger (Baughn 2026-10-30, board #45): a data logger and two cheap chips (shapeless). Its own
  model (cad `FieldLogger`: a field logger with a front screen and a handle), three inputs A, B, C: in a voxel block
  three pins on its back edge at 4, 8 and 12 voxels (`edgePins`), coloured terminals; in a plain block the back, left
  and right edges. Its screen has the graph modes and per-channel scale and unit tabs. GameTest
  `wp9c three-channel data logger` (three cables, three sources, each channel its own).
- Field bus channels (Baughn 2026-11-01, after named channels with a cap of 64 were built): a bus has exactly 16
  channels, one per Minecraft dye colour, numbered as Minecraft numbers its dyes, 0 white .. 15 black, and named as it
  names them (`BusChannel`, held to `DyeColor` by `BusChannelTest`). No names, no cap, no "bus full": every channel is
  always carried, and a device's claims only mark channels in use for Jade. EA's brushes and wire colours number the
  dyes the other way round (1.12's dye damage, 0 black .. 15 white), so a brush's colour b is channel 15 - b
  (`BusChannel.fromBrush`). Wherever a channel's colour shows, its number and name go with it (Baughn: the colours
  aren't colourblind-safe): screens show a swatch beside a button labelled `3 light_blue`, Jade on a bus cable lists
  the channels in use as `3 light_blue  12.5V` with writer clashes, Jade on a module lists `Terminal 5: 3 light_blue,
  in 30V`. The pins themselves carry no number tag yet. Tests `BusNetTest`, `BusNetPropertyTest`, `BusChannelTest`,
  GameTest `bus_cable_across_blocks`.
- Field bus packets (Baughn 2026-11-05: the bus becomes an RS485 bus). A packet is `[target][source][type][payload]`
  (`Packet`, `PacketType`): target 0 is a broadcast, 1..16 the colour channels (dye id + 1), 17..255 devices; source
  is the sender's address. Each type has a fixed payload (an index byte, a float, or both): 1 signal, 2 press,
  3 power, 4 write, 5 read, 6 reply, 7 fault, 8 arrive; `Packet.decode` drops unknown types and wrong lengths. What
  ports send in a tick is delivered at the start of the next (`Bus.swap`), ordered by source, then by order of
  sending, at most `Bus.CAP` (64) a bus a tick; the rest are dropped and counted (Jade on the cable: `Packets`). A
  packet reaches every listening port it is for except its sender: a broadcast all of them, a channel's the ports
  claiming that channel, a device's the port with that address (`BusPort.setAddress`, 17..255 only). Packets wait in
  a port's inbox (64, oldest dropped first) until polled, so a device needn't run every tick; packets still queued
  move with their sender when the bus is rebuilt. Tests `PacketTest`, `BusNetPropertyTest`.
  Colour channels are held levels set by signal packets, and the last write wins (Baughn 2026-11-05; it was the
  highest of each tick's writes, cleared every tick). Writers (Bus I/O terminals, signal processors, `MachineBus`)
  still write every tick, but `BusPort.write` sends a signal only on a change of 1e-4 (5 mV) or more, onto or off 0
  or 1, or when asked again; a port holds the channels it writes, and a tick without a write lets go: then the other
  holders send theirs again, or the channel falls to 0. A dropped signal is sent again, and every writer sends again
  on a rebuilt bus. Tests `BusNetTest`, GameTest `bus_module_last_writer_wins` (was `bus_module_highest_writer_wins`).
  Jade on a bus cable lists the device addresses on the bus (`Devices: 17, 30 (2x, DUPLICATE)`). A Bus I/O Module's
  terminal pins wear their channel's dye, so tuning one in the screen recolours the module (Baughn); unused ones stay
  signal-cable white.
- Addressable Button and Addressable Light (RS485 bus devices, Baughn 2026-11-05; `sixnode/busdevice`, ids
  `2 + (117 << 6)` and `3 + (117 << 6)`): a bus pin on the front edge in a voxel block (none in a plain block), and an
  address, 17..255, set in a small screen (`gui/bus_address.json`, NBT `addr`; anything else means none). The button
  wears the Signal Button's model: a click sends `press` (index 0) to every device from its address, and its lamp shows
  the last `power` sent to it (over half: lit, NBT `lit`); sneaking opens its screen, as the wrench turns it. The light
  wears the LED vuMeter's model and shines as bright as the last `power` sent to it, block light up to 7 (NBT `level`).
  Recipes: the Signal Button or LED vuMeter, a cheap chip and Field Bus Cable, shapeless. GameTests `bus_device_*`.
- Bus I/O Module (the field bus, `mods.eln.bus`; Baughn: 9 terminals), id `1 + (117 << 6)` beside the Modbus RTU,
  whose model it wears without the door (a proper model later). On a face, its front edge takes Field Bus Cable; its
  left, back and right edges hold three signal terminals each, numbered 1 to 9 round from the bus edge, pins 4 voxels
  apart (`edgePins`). Each terminal: unused or a colour channel, and in (its wire's level onto the bus every tick)
  or out (the bus drives its wire, a gate output). In its screen a button per terminal steps through unused, 0 white
  .. 15 black (shift-click steps back). An EA brush used on the module tunes the terminal nearest the click (each
  terminal's point is its pin's root, `terminalAt`) to the brush's colour, using paint only for a change, and says
  the terminal's channel by number and name. In a plain six-node block there is no bus cable, and each signal edge
  connects only its middle terminal (2, 5, 8), and only those are tuned by a brush. NBT `t<n>bus`, `t<n>out`. Recipe: a signal processor's with Field
  Bus Cable on top. GameTests `bus_module_*`.
- Signal processor on the field bus (Baughn: the bus is to make signal logic denser). In a voxel block's face slot its
  output edge has two terminals: the output in the middle (as before) and a bus pin 4 voxels toward `front.left()`
  (`edgePins`, `BUS_OFFSET`); a plain six-node block, or an off-centre slot, has no bus pin. Programs read a channel
  as `bus.<dye name>` anywhere a name may stand (`pump = bus.light_blue < 0.3`) and write one with a definition line
  `bus.<dye name> = expression` (`bus.red = rs(A, B)`); the 16 names are Minecraft's (`white`, `orange`, `magenta`,
  `light_blue`, `yellow`, `lime`, `pink`, `gray`, `light_gray`, `cyan`, `purple`, `blue`, `brown`, `green`, `red`,
  `black`). A read is always the bus's level as the tick began, also for a
  channel the program writes, so `bus.lime = bus.lime + 0.1` is a counter with a tick's delay, not a cycle. The last line
  may be a bus definition (only a bus definition is a definition there): the program then has no output expression
  and the output wire gives 0 V. Any other `bus.x` is a compile error that lists the 16 names, and so is a channel
  written twice. The channels read and written are claimed; writes go out once
  a tick (a slow process, after the circuit), only while the processor has its chips; a non-finite value writes 0.
  The editor colours `bus.` names, accepts them as the processor does, says a channel's number and name on hover,
  and lists the 16 in the reference's "Field bus" category. Old programs are unchanged. Tests `ProgramTest`, `SpLanguageTest`, GameTests `bus_sp_*`.
- Data loggers on the field bus: in a voxel block every data logger (one or three channels, the tilted terminal)
  has a bus pin in the middle of a free edge, the first clockwise from its inputs' edge that isn't its front
  (`busEdge`; it follows the tilted terminal's turn). A channel may record a bus channel instead of its wire: a
  "Bus channel" picker under the unit, for the selected channel, a swatch and a button that steps through unused
  (the wire, as before) and the 16 colours. A plain block has no bus pin: the picker is hidden there and every
  channel records its wire. NBT `busc<n>`. GameTest `bus_logger_channel`.
- Signal processor logger and defaults (Baughn 2026-10-30, board #45): a program may give an input a default,
  `A = 0.5` on a definition line (a constant, once per input, free), read while no cable is connected to it. The
  processor carries a four-channel logger (A, B, C as the program reads them, and the output) in its screen's left
  pane, with lines, stacked-area and lane modes, and draws the A/B/C/out graph on a tiny screen on its new model
  (cad `SignalProcessor`, no door). Loggers send their graph only to watchers: a client asks while it can see it
  (screen open, or within 32 blocks for the data logger, 6 for the processor's tiny screen) and repeats every second;
  the whole graph when watching starts or the period changes, then one point (6 bytes a channel) as each is due.
  Units are free text (10 characters, sanitized); older saves and prints keep theirs as a symbol. GameTests
  `wp9c signal processor`, `wp9c data logger`.
- Signal processor operators (Baughn 2026-10-30, board #45): `^` is power (right to left), `==` and `!=` compare
  values within 0.01, `>=` `<=` are new, `&` `|` `!` are on/off at 0.5, in the usual order (^, * /, + -, comparisons,
  !, &, |). 1.12: `=` and `^` compared as on/off, `!a` was 1 - a, and = ^ & | shared one level read left to right.
  `rs` switches at 0.5 (1.12: 0.6; old programs are not rewritten for it: the difference only shows for inputs between
  0.5 and 0.6). The 7-argument `pid` holds its integral within lo..hi (1.12: always 0..1, a bug). Programs saved at
  `programVersion` 1 or older are rewritten once on load to do the same (`ProgramUpgrade`: `a = b` to
  `(a > 0.5) == (b > 0.5)`, `a ^ b` to `!=` likewise, `!a` to `1 - a`, brackets where the order differs; a statement
  that reads the same keeps its text). A rewritten line may cost more chips (the added comparisons). Stateful
  functions' saved state is numbered in text order now (1.12: the order its reducer made them) and renumbered on load.
  The compiler is a recursive descent parser (`Equation.Parser`); 1.12's reducer stays only to tell that old order.
  Tests `ProgramUpgradeTest` (old programs read the same after the rewrite, over many inputs), `ProgramTest`.
- Signal processor functions (Baughn 2026-10-30, board #45): `rs(set, reset)` and `rc(x, tau)` (1.12: `rs(reset, set)`,
  `rc(tau, x)`). A processor saves `programVersion`; one saved without it has its program rewritten once on load
  (`ProgramUpgrade`: the two arguments of every rs and rc call swap, so it does the same). Also fixed: `derivate`
  reads 0 on its first step (1.12: x / dt, a spike); `ramp` holds at a period of 0 or below (1.12 ran backwards or to
  NaN); `batteryCharge` stops at 1 (1.12 went past it between the full voltage and 1.5). Tests `ProgramUpgradeTest`,
  GameTest `wp9c signal processor`.
- Signal processor screen (Baughn 2026-10-30, board #45): a code editor (colours for A red, B blue, C green, names,
  functions, numbers, comments; a rule above the output line; problems underlined with their message on hover; undo,
  clipboard, scrolling) instead of the one-line field, a quick reference of the functions beside it (click to insert),
  and the chips needed or the first problem shown as you type; Apply sends the program. Inputs A, B and C always
  connect, whether or not the program reads them (1.12 offered only the ones it read). GameTest `wp9c signal processor`.
