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
- X-Ray Scanner recreated, not ported: 4 coloured filter slots (ore blocks or raw chunks via `c:ores` /
  `c:raw_materials`), picture on the held item; range 7.5 (1.12: 5). `item/xray/`, `XRayCase`.
- Copper Coil (`eln:copper_coil`, new): thin winding wire; windings (inductor, resistors, rheostat, thermistor,
  DC-DC, 50V copper heating corps) take coils; copper cable is 1 per ingot.
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

## Behaviour changes vs 1.12

- Meters, probes and power sockets no longer burn at their (former slot) cable's voltage: those watchdogs are off.
- Fires lag their setting (coal ~3 min, oil ~20 s), go out below 15%, take time to relight (`sim/Fire.java`, `FireTest`).
- Battery wear divided by `batteryLifeFactor` (default 25), doubled every 10 C above ambient, up to 4x at low charge.
- The FE exporter is a transparent node: it burns out in place and can be repaired instead of vanishing; old
  simple-node saves are upgraded on load (`transparentnode/energyconverter/ExporterSaveUpgrade.java`,
  `ExporterSaveUpgradeTest`). `SimpleNode` and its block/BE/item are gone.
- Nodes that cannot load are kept as orphans and written back unchanged (`node/NodeManager.java`,
  `NodeSaveLoadTest`); nodes whose block is gone leave the simulation (`node/NodeOrphans.java`, `/elnlab remove`).
- A six-node face whose device cannot load is kept as saved data and the face stays taken (placing on it is refused
  with a message; breaking the block discards it) instead of being dropped from the next save (`SixNodeLoadTest`).
- An unreadable `electricalAgeWorld<dim>.dat` or `.dat.bak` is renamed to `*.corrupt-<time>` instead of being
  overwritten by the next save (`server/ElnWorldSaves.java`, `ElnWorldSavesTest`).
- A loaded six-node connects once, not twice; a client packet for a bad or empty six-node side is ignored instead of
  throwing (`SixNodeLoadTest`). Voxel blocks invalidate capability caches when pipe adapters come and go.
- Portable batteries charge items in the offhand too. Sounds: render loops play client-side (`sound/LoopedSoundPlayer`).

## Bugs fixed

From 1.7.10/1.12 (carried in from the 1.12 branch, before the port): the WP16 sim audit (`AuditFixesTest`), fuse and
breaker trip points, generator energy accounting, a truth table per PAL chip (every PAL shared one).
In the port:
- 1.12 `ItemStackFilter.OreDict` masked damage to 8 bits (Mercury, Gold Plate passed as Coal Dust;
  `GoldenSlotsCase.KNOWN_MISSING`).
- Power sockets could never be connected in survival (1.12 container had no slot).
- `while(true)` hang traps in `SixNode` connection/disconnection and the thermal cable check now log and continue;
  disconnecting a node outside the simulation still drops its links.
- Six-node breaking takes one element and drops it (`SixNodeBreakCases`).
- MNA: a cable Line ending at ground crashed the step; a removed Monopole left its pin.
- Turret shots asked for light 25 and crashed the light block (now clamped; `TurretCases`).
- Item capabilities (hoppers, pipes) never worked in any EA version; now they do.

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
