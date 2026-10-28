# The electric bike

Baughn's brief (2026-10-28): a ridden electric bike, "Not a motorcycle" (its tooltip). Code in
`src/main/java/mods/eln/bike/`, client in `client/entity/Bike*` and `client/gui/BikeTrunkScreen`.

## Rules (`bike/BikePhysics`, unit-tested in `BikePhysicsTest`)

| Rule | Value |
|---|---|
| Speed | the rider's input (W = 1, S = -1, none = 0) averaged over 50 ticks (2.5 s), times the top speed; nothing else (Baughn: "bicycle-y"). Holding W reaches full speed in 2.5 s; letting go coasts down over 2.5 s; S brakes through the same average. |
| Top speed | on battery 0.475 b/t (9.5 b/s, an average horse); pedalling (no battery or a flat one) 0.33 b/t (6.6 b/s; sprinting is 5.6) |
| Backing up | S held past a standstill: 1 b/s backwards |
| Steering | the bike turns toward where the rider looks, at most 12° a tick |
| Steps | step height 0.6: slabs, stairs, snow layers ride; a full block doesn't |
| Crash | running into anything (a full-block step or a wall) faster than 3 b/s throws the rider off and the bike over |
| Jump | the horse's charge bar; full charge 0.43 b/t up, peak 1.31 blocks (one block with room, never 1.5) |
| Fall gravity | a living entity's: 0.08 b/t², drag 0.98 |
| Motor | 100 W at full speed, falling with the square of speed, capped by the battery's discharge power: a Portable Battery (40 kJ) lasts ~7 min at full speed, a Pack ~20 min |

## Config (`[bike]` in `config/eln-synced.toml`, `ElnConfig`)

Baughn (2026-10-28): "Might adjust values in playtesting, and the obvious config options can be config options." EA's
config is SYNCED, so the server's values reach the rider's client, which does the moving. `ElnConfig.refresh` copies
them into the statics of `BikePhysics` and `ElectricBike`; the defaults are the values above.

| Key | Default | Becomes |
|---|---|---|
| `poweredSpeed` | 9.5 b/s | `BikePhysics.VMAX_POWERED` (/20, b/t) |
| `pedallingSpeed` | 6.6 b/s | `VMAX_PEDALLING` |
| `inputAverageSeconds` | 2.5 | `AVERAGE_TICKS` (x20; the input window resizes on the next tick) |
| `motorWatts` | 100 | `MOTOR_WATTS` |
| `tipOverDelaySeconds` | 5 | `ElectricBike.STILL_TICKS` (x20) |
| `tipOverChancePerSecond` | 0.1 | `ElectricBike.TIP_CHANCE` |
| `crashSpeed` | 3 b/s | `CRASH_SPEED` (/20) |
| `jumpHeight` | 1.31 blocks | `JUMP_VY` = `velocityForPeak(jumpHeight)` (0.43 b/t) |
| `stepHeight` | 0.6 blocks | `STEP_HEIGHT` (1 or more climbs full blocks) |

## Recipe (Baughn 2026-10-28)

Shaped, in `datagen/ElnRecipes`: `CL ` / `ImI` / `R R`: C chest (the trunk), L leather (the saddle), I iron ingot (the
frame), m Electrical Motor, R rubber (`c:rubbers`, EA's Rubber). New in 26.x, so not in the 1.12 golden recipes;
GameTest `bike_recipe` crafts it.

## The entity (`bike/ElectricBike`, `eln:electric_bike`, 0.8 x 1.0)

- A `VehicleEntity` ridden the boat's way: the controlling player's client simulates it (`isLocalInstanceAuthoritative`),
  the server takes its vehicle-move packets, other clients interpolate. Input is the rider's `zza` (common code can't
  read `LocalPlayer`'s input).
- Synced: `mode` (RIDDEN, PARKED, FALLEN) and `powered` (the server: the trunk's battery has charge).
- **Parked** (on its kickstand): when the rider gets off. Not pushable; it stands where it was left (gravity still
  applies).
- **Ridden, stopped**: below 0.05 b/s for 5 s, then each second a 10% chance it goes over with its rider (server,
  `serverTick`).
- **Crash**: the controlling client sees a horizontal collision above 3 b/s and sends `BikeCrash(entityId)`
  (`bike/BikeCrash`, C2S payload `eln:bike_crash` on EA's channel); the server checks the sender rides it, then `fall()`.
- **Fallen**: lies on its side; nobody can get on; a right-click stands it up (PARKED).
- **Trunk**: `SimpleContainer(4)`: slot 0 the battery (what `ElectricBike.accepts`: EA's portable batteries, packs,
  condensators, a battery belt), 1-3 anything. Shift-right-click opens it, or the inventory key while riding
  (`HasCustomInventoryScreen`). `BikeTrunkMenu` sends the charge (per mille) as menu data. Saved as `Items`; `mode` is
  saved (RIDDEN loads as PARKED).
- Broken up (hit, like a boat) it drops `eln:electric_bike` and spills its trunk (`remove` with a destroying reason,
  creative too).
- **Energy** (server): the speed it moved this tick (from the client's moves) gives the motor's draw, taken from the
  battery item.

## Client

- `client/entity/BikeModel`: cube model, regions of one material each in `textures/entity/electric_bike.png` (drawn by
  `tools/bike/texture.py`, which also draws the item icon): diamond frame in EA blue, battery on the down tube, hub
  motor in the back wheel, octagon-rim wheels with four spokes across that turn with the distance rolled
  (`ElectricBike.wheel`), saddle, bars with grips, a lamp, a rack with the trunk, a kickstand that folds up when ridden.
- `BikeRenderer`: parked it leans 8° onto the kickstand; fallen it lies on its side; hit, it rocks like a boat.
- `BikeSound`: EA's `rotor_tone`, quiet, pitch with speed, only while the motor runs; it takes a sound source only then
  (started when it runs, stopped after 2 s quiet).
- `client/BikeHud` (GUI layer `eln:bike_speedometer`, above the hotbar; Baughn: tell players it needs a battery): while
  the local player rides a bike, a round dial left of the hotbar (past the offhand slot): needle 0 to `poweredSpeed` in
  km/h (b/s x 3.6) over 270 degrees, ticks every tenth (the last red), a digital readout; the speed is the bike's
  horizontal speed, smoothed. Beside it the battery, filled by charge (green, yellow under 50%, red under 20%); with no
  battery or a flat one, a red outlined battery with a slash, blinking (1 s on, 0.5 s off). Drawn with fills.
- The charge comes from the synced `charge` (0..1, `NO_BATTERY` = -1 when the slot holds no battery), sent by the server
  when it moves by half a percent, reaches 0 or 1, or the battery comes or goes (`syncCharge`). Getting on with no
  battery, or a flat one, also says so once over the hotbar ("No battery: pedalling only ...").
- `BikeTrunkScreen`: EA's panel, the battery slot with a charge bar and %, three slots, the inventory.

## Tests

`BikePhysicsTest` (rules), GameTests `gametest/cases/BikeCases`: trunk slots, save, break drops; kickstand, tip-over
timing, rider off, stand up; half slab rides, full block stops; motor draw (~1 kJ in 10 s at full speed) and
powered/pedalling; the recipe; the synced charge (missing, flat, partial, half-percent threshold). Riding itself is client-side and needs a client to test.
