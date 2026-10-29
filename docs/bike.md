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
| `turnRate` | 12 °/tick | `TURN_PER_TICK`: the steering at a standstill |
| `turnRateAtTopSpeed` | 5 °/tick | `TURN_AT_TOP`: at the powered top speed and above, linear in between (Baughn: steering slows with speed) |

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
- **Deep water**: water more than 0.5 deep at the bike (over the hubs) throws the rider off and it goes down (server).
  Shallower water just slows it to 30%.
- **Fallen**: lies on its side; nobody can get on; a right-click stands it up (PARKED).
- **Trunk**: `SimpleContainer(4)`: slot 0 the battery (what `ElectricBike.accepts`: EA's portable batteries, packs,
  condensators, a battery belt), 1-3 anything. Shift-right-click opens it, or the inventory key while riding
  (`HasCustomInventoryScreen`). `BikeTrunkMenu` sends the charge (per mille) as menu data. Saved as `Items`; `mode` is
  saved (RIDDEN loads as PARKED).
- Broken up (hit, like a boat) it drops `eln:electric_bike` and spills its trunk (`remove` with a destroying reason,
  creative too).
- **Container items in the trunk** (Baughn 2026-10-28, Discord 1565048425525944382): an item in slots 1-3 with NeoForge's
  item capability (`Capabilities.Item.ITEM`: shulker boxes, bundles, other mods' bags that register it), one item, not a
  stack, gets a tab under its slot; clicked (menu button 1-3), its insides open in a panel right of the trunk, clicked
  again it closes. `BikeTrunkMenu` has a fixed panel of 27 slots (`PANEL`; a bigger handler shows its first 27; menu data:
  charge, open tab, the handler's size, so the client hides the rest). On the server each panel slot is an `InsideSlot`:
  - every access finds the handler again: `ItemAccess.forHandlerIndexStrict(VanillaContainerWrapper.of(trunk), tab)`, so
    edits replace the trunk slot's stack and nothing outlives the item: taken out or swapped, the panel reads empty and
    refuses everything (a stale `set` gives the stack back to the player);
  - changes go through the handler's insert/extract in a transaction (`remove`, `safeInsert`, `tryRemove` and the delta
    `set` for swaps and drags), so its rules hold (a bundle's weight); vanilla's rule that a shulker box goes in no
    container item (`canFitInsideContainerItems`) is checked here, the handlers don't; what a handler won't take goes back
    to the player, never lost;
  - vanilla's in-place edits (`getItem` copy changed, then `setChanged`) are written back only if the trunk still holds the
    same stack object they were read from (another player's change in between voids them);
  - shift-click out of the panel takes the items out first, then places them (what doesn't fit goes back); from the
    inventory, the open item takes what it can before the trunk does.
  Tabs and sizes come from the server (menu data: a mask of the trunk slots whose item has a handler with slots, the
  open handler's size), since a bag's contents may live only there; an item whose handler has no slots gets no tab.
  Sophisticated Backpacks (checked in its jar, 3.26.13): its `Capabilities.Item.ITEM` provider ignores the ItemAccess,
  builds a `BackpackWrapper` from the stack, and returns an empty handler until the backpack has a contents id, which it
  gets the first time it's opened. `bike/BagStorage` does that for a backpack in the trunk (server, each menu tick, by
  reflection, no compile dependency: `BackpackWrapper.fromStack(stack).getInventoryHandler()`), so a fresh one opens too;
  its contents then live in the backpack's world storage, and the panel shows its first 27 slots.
  JEI: `compat/jei/ElnJeiPlugin.registerGuiHandlers` gives JEI the open panel's rectangle (`BikeTrunkScreen.extraAreas`),
  so its ingredient list keeps clear of it.
  On the client the panel is a plain mirror the server fills; `BikeTrunkScreen` draws the tabs and the panel, and a click
  on the panel isn't "outside" (which would drop the held stack). Sophisticated Backpacks registers `Capabilities.Item.ITEM`
  for its backpacks; see above.
- **Energy** (server): the speed it moved this tick (from the client's moves) gives the motor's draw, taken from the
  battery item.

## Paint (Baughn 2026-10-29: dyeable, per frame element)

- Four groups (`BikeColors`, `BikeModel.Group`): the frame (tubes and stays), the fork, the trunk (box and lid), the
  saddle. Right-click a part with a vanilla dye: the group of the part the player looks at takes the dye's colour
  (`DyeColor.getTextureDiffuseColor`), one dye used unless creative, none if it's that colour already.
- Which part: the player's eye ray (server and client alike, up to 4.5 blocks), turned into the bike's frame, against
  boxes from the model's geometry (`BikeColors.BOXES`: saddle, trunk, fork, and the frame as three boxes), nearest hit
  wins. The boxes are computed from `bike/BikeGeometry`, the shapes `BikeModel` draws, plus 0.03 all round, with the
  saddle's back and the trunk's front meeting halfway between them (0.334 and 0.39 behind the middle), so they can't
  drift from the model; never the entity's bounding box (AdventurAgent: side-on frame clicks missed, a saddle click painted the trunk).
  With a dye in hand a click only paints: nothing hit (or the same colour) uses no dye and doesn't mount. The pick
  radius is 0.45, so the fork and the trunk, outside the 0.8-wide box, can be clicked at all. `BikeColorsTest` aims at
  every part from each side it can be seen from, at four headings.
- Synced (`DATA_COLOR`, one int per group; -1 = default), saved as `colors`, and on the item when broken up
  (`CUSTOM_DATA` `eln_bike_colors`; a plain bike's item has none) and back on the bike when placed.
- Drawing: the dyeable parts are neutral grey in the texture; `BikeRenderer` submits the model once per group, each with
  its tint (the defaults reproduce the old blue frame, slate trunk, dark saddle).

## Client

- `client/entity/BikeModel`: cube model, regions of one material each, each sized for its largest box's UV layout
  (2(d+w) x (d+h); the trunk's once overran into the battery's, an odd patch on its back: Baughn) in `textures/entity/electric_bike.png` (drawn by
  `tools/bike/texture.py`, which also draws the item icon): diamond frame in EA blue, battery on the down tube, hub
  motor in the back wheel, octagon-rim wheels with four spokes across that turn with the distance rolled
  (`ElectricBike.wheel`), saddle, bars with grips, a lamp, a rack with the trunk, a kickstand that folds up when ridden.
- `BikeRenderer`: parked it leans 8° onto the kickstand; fallen it lies on its side; hit, it rocks like a boat.
- **The rider pedals** (Baughn 2026-10-28: the legs should sit on the pedals): `ElectricBike.crank` turns with the wheel
  (`GEAR` 2 wheel turns per crank turn) while the bike is driven (rolling forward and not slowing); coasting it
  freewheels, so the legs rest. Computed from motion on every client, so other players' riders pedal too. The bike's
  cranks turn with it (`BikeModel`). A NeoForge render-state modifier on `AvatarRenderer` (`client/entity/RiderPose`)
  puts the crank angle in a riding player's render state; `mixin/client/HumanoidModelMixin` (EA's first mixin,
  `eln.client.mixins.json`, client, not required) calls `RiderPose.apply` after vanilla's `setupAnim`: each leg points from the
  hip at its pedal (the bike's left crank under the right leg), and with no knee in the player model the hip rises up
  to 4 px (or drops 1.5) so the foot meets the pedal within 2 px (`RiderPoseTest`); the arms reach forward to the
  grips: the upper body leans 0.35 rad forward about the hips (body, head and shoulders move with it) and each arm points
  at its grip (Baughn: hands on the handles; `RiderPoseTest`). `SEAT_Y` 0.87, `SEAT_Z` -0.22: the hips a pixel into the
  saddle, over it.
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
- `BikeTrunkScreen` (its container panel's top level with the trunk window's, Baughn): EA's panel, the battery slot with a charge bar and %, three slots, the inventory.

## Tests

`BikePhysicsTest` (rules), GameTests `gametest/cases/BikeCases`: trunk slots, save, break drops; kickstand, tip-over
timing, rider off, stand up; half slab rides, full block stops; motor draw (~1 kJ in 10 s at full speed) and
powered/pedalling; the recipe; container insides (shulker read/write lands in its CONTAINER, inert when taken out, stale edits void,
shift-click both ways conserves items, a bundle's weight limit); the synced charge (missing, flat, partial, half-percent threshold). Riding itself is client-side and needs a client to test.
