# Animation (`mods.eln.anim`)

Every moving or changing thing a device draws: turning parts, levers, doors, blinking and glowing lights, particles,
levels, progress, belts, items in machines. New code uses this library rather than hand-writing another smoother or
blink counter; this page indexes the library and every animated device (Baughn 2026-11-07: one library, indexed,
referenced from CLAUDE.md).

## Rules
1. **State advances only in the render's `refresh(dt)`, never in `draw()` and never on the wall clock.** `dt` comes
   from `client/FrameTime` (once per frame, capped at 0.1 s, 0 while the game is paused), so animations stop with the
   game and don't jump after a stall. `draw()` reads the state and draws.
2. Server values (speed, on/off, progress) arrive in `networkUnserialize` only now and then; set them as targets and let
   the library ease toward them. A device seen for the first time can `snap()` to its state.
3. Draw in the model's frame (after `front.glRotateXnRef()`); parts turn about their origin from the model's .txt
   (`NodeModel.origin` in src/cadmodels).
4. Server-side smoothing that is saved (weather, sensors, turret mechanics) keeps `misc.RcInterpolator`/`SlewLimiter`;
   the library is for drawing.

## The library
| Class | What | Use |
|---|---|---|
| `Smooth` | First-order smoother, stepped exactly (`1 - e^(-dt/tau)`): no overshoot at any frame rate | the base of the others; any eased value |
| `Spin` | An angle turning at `degPerSecond x speed`, the speed eased (`Smooth`), wrapped to [0, 360) | rotors, fans, impellers, stirrers, spools |
| `Travel` | 0..1 between two poses, eased (`smooth(tau)`) or on a damped spring (`spring(...)`, misc.PhysicalInterpolator); `rotate`/`slide` a part | levers, contacts, buttons, needles, doors |
| `Blink` | On/off on frame time: fixed period and duty, or random spells | beacons, busy LEDs |
| `Emitter` | Particles per second (whole ones a frame, the rest carried), and `vent(pos, front, x, y, z)`: a model point turned to world coordinates by the facing | smoke, steam puffs, sparks, souls |
| `Led` | A status LED part: full-bright on, dimmed off | ledpower / ledrun parts |
| `Glow` | A part self-lit in a colour from a per-device `Ramp` of a 0..1 level | fire windows, peepholes (temperature-true colour: `misc.Incandescence`) |
| `Fill` | A part scaled in y about its origin | gauge glasses, tank levels |
| `Progress` | A machine's job progress run on between publishes at the published rate; `shown()` eases forward and restarts with the next piece | items in process |
| `Belt`, `BeltPath` | A belt of links wrapped round two pulleys (open belt), running with the drive (`Belt.Run`) | the 230V macerator |

Tests: `src/test/java/mods/eln/anim` (`AnimLibraryTest`, `BeltPathTest`).

## Devices (who animates what)
"lib" = on the library; "own" = still hand-written (moved over batch by batch, todo #277); "special" = kept on its own
code on purpose.

| Device(s) | What moves | How | State |
|---|---|---|---|
| Polymeriser | agitator drive | `Spin` | lib |
| Steam Cracker | peepholes, LEDs | `Glow`, `Led` | lib |
| Desulfuriser, Industrial Resin Collector, the four electrical machines, fuel heat furnace, fuel generator, carbon black burner, math chip, bus module, Modbus RTU, timeout, battery charger, wireless Rx/repeater | status LEDs, sight ports | `Led` (`draw`, tinted, `redGreen`) | lib |
| Battery charger | charging LED blink | `Blink` (was the wall clock) | lib |
| Electrical furnace | item turntable | `Spin` (was the wall clock) | lib |
| Smelter, heat furnace, fuel heat furnace | fire glow | `Glow` (each its own ramp), `Fill.drawAbout` | lib |
| 230V macerator | flywheel belt | `Belt` | lib |
| Macerator, magnetizer, plate machine, compressor | handles/rollers, ram, items | hand-written R1, items spin | own |
| Compact turbine, steam generator, inline pump, wind/water turbines, active dissipator, alarm, anemometer, auto miner pipe, wire drawer | rotors | hand-written smoothed rotor | own |
| Egg incubator | item turntable | constant spin | own |
| Switches, breaker, relay, signal sources, bus button, VU meter, timeout | levers, contacts, needles | `RcInterpolator`/`PhysicalInterpolator` | own |
| Transformer, lamp supply, teleporter | doors (player near), gyro | `PhysicalInterpolator` | own |
| Compact turbine, steam generator, steam oven, anvil, macerator, flare stack, heat furnaces, carbon black burner | particles | due-accumulators, four copies of the vent rotation | own |
| Heat furnace, fuel heat furnace | flicker | hand-written | own |
| Switch, VU meter, gate sources, fire detector, source, lamp supply, entity sensor, emergency lamp, redstone in/out, bus button/light/floor sensor, turret, data logger | LEDs with halos or mixed states | inline | own |
| Boiler, tanks, oil separator | levels | hand-written scale | own |
| Wind turbine halo, teleporter, fire detector, math chip, auto miner, battery charger | blinking | four different clocks | own |
| Shafts (all shaft parts, turbines, generator, motor) | rotors | `log(rads+1)/log(1.2)` deg-rate, unsmoothed | special: Baughn's 2015 curve (slow shafts turn visibly, fast ones don't strobe) |
| Energy meter | disk, odometer | client integration of power | special |
| Watch | hands | world time | special |
| Suspended lamp | sway | damped spring, random forcing | special |
| E-bike, elevator car, hand drill | wheels, car, bit | tick + partial tick (entities/items) | special |
| Turret | yaw, elevation | `SlewLimiter`, also simulated on the server | special |
