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
- `BikeTrunkScreen`: EA's panel, the battery slot with a charge bar and %, three slots, the inventory.

## Tests

`BikePhysicsTest` (rules), GameTests `gametest/cases/BikeCases`: trunk slots, save, break drops; kickstand, tip-over
timing, rider off, stand up; half slab rides, full block stops; motor draw (~1 kJ in 10 s at full speed) and
powered/pedalling. Riding itself is client-side and needs a client to test.
