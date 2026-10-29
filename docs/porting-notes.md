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
  turbine's shaft out of its front flange (15 bar), makeup in on top; the models join (turbine parts vent / exhaust,
  condenser ducts). Over half full it floods the turbine. `oil_steam_turbine_condenser`, `oil_condenser_floods`.
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

## Behaviour changes vs 1.12

- Meters, probes and power sockets no longer burn at their (former slot) cable's voltage: those watchdogs are off.
- Fires lag their setting (coal ~3 min, oil ~20 s), go out below 15%, take time to relight (`sim/Fire.java`, `FireTest`).
- Battery wear divided by `batteryLifeFactor` (default 25), doubled every 10 C above ambient, up to 4x at low charge.
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
  that heat; hoppers: top/sides in, bottom out. Model `tools/models/smelter.py`. Test `wp10a_smelter`.

- New: the Steam Oven (`oil/SteamOven*`, Baughn 2026-10-23): steam into a port on top (as the turbine's), a 10 W fan
  on 50 V; cooks food only, at up to 280 W (twice the electrical oven's best food speed), condensing 280 W / 25% of the
  steam's heat (~500 mB/s at full speed; the rest vents as a plume); hoppers: sides in, bottom out. Model
  `tools/models/steamoven.py`. Test `steam_oven_cooks_with_steam`.
- Devices (transparent and six-node) can be placed against any full face, not only opaque cubes (Baughn): a top
  slab, the underside of a bottom slab or stair, a stair's back, glass (`TransparentSupportCases`).
- Egg incubator: its windows are glass (the 1.7.10 texture left them fully transparent, so the model looked like a bare
  frame; Baughn), and the egg sits higher and larger in the nest (it was half sunk in the bowl).
- Boiler: a feedwater pipe flange at the centre of its right flank (`front.right()`), into the vessel through a check
  valve (Baughn). Device links can be check valves now (`FluidDevice.Link.kind`); a shut one is left out of the
  network's pressure limiter (`boiler_feedwater_flange`, `FluidCoreTest.checkValvePassesOneWayOnly`).
- Boiler: a phase signal on its front (Baughn: where the dials are): high while the water is above half the gauge
  glass (half the 8 B tank), with a deadband; heat now goes in on the back and flanks only. The gauge glass shows the
  fill and the dial the water temperature (0..300 C) (`boiler_phase_signal`).
- Submersible pumps, 50V (100 W) and 200V (400 W) (Baughn 2026-10-25): placed in a water source (transparent-node
  blocks are waterloggable now, as six nodes were), they push the water they stand in up a pipe from the flange on
  top: 3.5 turbines' worth of feedwater (57 / 230 mB/s), falling to nothing at 3 bar against the pipe. Power on the
  flanks, a signal on the front sets the speed (no cable there: full speed). Model `tools/models/submersiblepump.py`
  (`submersible_pump_feeds_a_boiler`, `submersible_pump_water_and_signal`).

- Logic and analog chips are drawn at 0.75 (a case 6 voxels across; Baughn) and, in voxel blocks, go anywhere on a
  face; pins that touch connect (docs/voxel-cables.md, devices off centre).

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

- Two devices on adjacent faces of one block shared heat only on some corners: `SixNode.tryConnectTwoInternalElement`
  paired the wrong thermal loads (1.12). Test: `six_node_inner_corner_a_temperature_probe_reads_a_wall_cable_s_heat`.

## Deferred

- Sound redesign: synthesize machine sound client-side from device state; one-shot sounds are still server packets.
- Modbus RTU: element ported, render and GUI not; registered without a creative entry while `Eln.modbusEnable` is false.
- Other languages: only the generated `en_us`; 1.12's 40+ `.lang` files to convert.
- Config: the rest of the 1.12 balancing values.
- Chunk baking of static device geometry (voxel nodes are already baked into the chunk mesh); only if playtests need it.
- In-game wiki guide (`wiki/Data.java` only collects descriptor groups), ore regeneration.

## Open questions

- Breaker: optional cable slot (empty = voltage trip only) instead of the required one?
- Should open, unloaded cables still burn from overvoltage (`BurnChainCases`' 1000 V case burns two)?
- Voxel cable conversion on load (`[cables] convertOnLoad`) is off until approved.
- `LampSupplyElement` (`sixnode/lampsupply`): `setWirelessName` registers the wireless name as a power channel
  (1.7.10 behaviour, listed as a bug to fix, unchanged).

## History and next steps

The 1.12 tree is `upstream/ports/1.12` (local `port-1.12`); this port is pushed as `ports/26.3`. Phase Q (after
parity and Baughn's sign-off): port the age-series fork's features (github.com/age-series/ElectricalAge, ~654 commits
on 1.7.10) onto 26.x.

- Power Capacitor (six-node) leaks with RC 30 min, not 1.12's 33 h (Baughn 2026-10-28; config
  `capacitorBank.powerCapacitorLeakTauSeconds`, `GoldenCase.CHANGED`). With the compressor's four Dielectrics a sand (was
  one) a row of them would have been a cheap, non-wearing bulk store; batteries stay the only sensible one. Saved
  capacitors keep their charge and take the new leak on load. GameTest `power_capacitor_leaks_like_the_bank`.
