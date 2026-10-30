# Shaft machine models (the shaft redo's art pass)

> Ported to Java (board #44): the model is a class in src/cadmodels/java/mods/eln/cad/models, generated at build time
> (docs/cad.md); the Python script, its commands and its own atlas are gone (the shared palette replaces the atlas).


Models for the shaft machines (Baughn 2026-10-29): HP and LP Steam Turbine, Gas Turbine, 4 kW and 8 kW Generator,
Shaft Motor, Joint, Joint Hub, Flywheel, Tachometer, Plant Steam Condenser and the stacking Counterflow Heat Exchanger.
Path 2 of docs/cad.md (EA Obj3D node OBJ), written with `ShaftKit` on top of `OilKit` (its atlas and
`write_parts`). House style: the steam turbine, boiler, condenser and tank models (skids, cast iron, bolted flanges,
brass bolts, a paint band in the device's colour, gauges).

## The frame every shaft part shares
- px, corner origin, Y up; **front XN (x = 0)**, back x = 16, **right z = 16, left z = 0** (front XN: right() = ZP).
- **The shaft runs along z at x = 8, y = 8** (`shaftkit.AX, AY`) for every shaft part, so neighbours' shafts meet.
  Shaft parts connect on their left and right faces (the Joint Hub on all four sides).
- **Half couplings** at each connecting face (`shaftkit.half_coupling`): a steel disc r 2.25, 0.5 px deep from the
  face, four brass bolts; two neighbours' halves make one bolted coupling. Keep everything else clear of the face in a
  2.5 px radius round the shaft line.
- **Part `rotor`**: everything that turns with the shaft (shaft, couplings, fans, flywheel rim, visible discs), origin
  (8, 8, 8) px in NAME.txt (`shaftkit.CENTRE_ORIGIN`). The render turns it about z through that origin by the shaft's
  angle. The Joint Hub adds `rotor_x` (its shaft along x, turning about x through the same origin).
- **Part `main`**: the static rest. Optional animated parts: `valve` (a governor/fuel valve stem: the render lifts it
  by up to 1.5 px with the valve's opening), `needle` (the tachometer's pointer: turned about x through its origin
  by up to 270 degrees with speed over 1000 rad/s).
- **Ports** (fluid): `shaftkit.pipe_flange`: a port's flange as a pipe's end is drawn (small = iron/alloy r 1.5,
  large = large alloy r 3.75, 1 px deep) at the port's spot. Spots are on the block-face lattice: CENTRE = (8, 8) on
  the face, LOW = 4 px up, HIGH = 12 px up (centred across).
- **Electric/signal floor terminals**: `oilkit.terminal_plate` (power: 3 studs) and `oilkit.signal_plate` (signal:
  one stud), on the x = 0 face; `oilkit.turned(..., 2)` puts one on the back. Keep the floor middle of those faces
  clear (docs/cad.md "Cable terminals").
- **Hitbox**: `shaftkit.write_model(..., hitbox=[...])` writes NAME.hitbox: a few boxes (px, this frame, x0 y0 z0 x1 y1
  z1) that follow the model's bulk (3-8 boxes; include the shaft and couplings on the connecting faces). The game reads
  them for the block's outline and collision and turns them with the block's front.
- Atlas: oilkit's (steel, darksteel, castiron, copper, brass, paint, glass, rubber, ceramic, coil, hotcopper, rivet,
  water, red, froth, sand, louvre, oil, lamp, firebrick); `paint` is per device (`shaftkit.PAINT`).
- ~800-1800 triangles a model; n = 8-14 for cylinders.

## Devices (ports and connections, from the code)
| model dir | device | ports / connections |
|---|---|---|
| shaftsteamhp | HP Steam Turbine | steam in: top centre, small (alloy) flange; exhaust: back centre, large flange; signal floor terminal on the front; shaft left/right; `valve` = governor valve on top by the inlet |
| shaftsteamlp | LP Steam Turbine | bigger casing (near full block); steam in: top centre, large flange; exhaust: back centre, large flange (to the condenser); signal terminal front; shaft left/right; `valve` |
| shaftgas | Gas Turbine | hot exhaust: back centre, large flange (heat-tinted); fuel: a small flange on top (any side takes fuel); air intake on the front, above the signal terminal (grille, the `rotor` fan behind it); starter coupling = the shaft; `valve` = fuel valve |
| shaftmotor | 200V Shaft Motor | power floor terminal on the front, signal terminal on the back; shaft out of both sides |
| shaftgen4kw / shaftgen8kw | Generator / 8kW Generator | power floor terminals front and back; shaft left/right; the 4 kW visibly smaller |
| shaftjoint | Joint | a bearing pedestal and the shaft |
| shafthub | Joint Hub | couplings on all four sides; a bevel gearbox; `rotor` along z, `rotor_x` along x |
| shaftflywheel | Flywheel | a big spoked wheel (in `rotor`) on pedestals |
| shafttacho | Tachometer | signal terminals front and back; a dial facing the front with a `needle` |
| plantcondenser | Plant Steam Condenser | exhaust in: large flanges top centre, back centre, right centre; condensate out: front LOW small; makeup in: left HIGH small; cooling water in: left LOW small, out: right LOW small; thermal pads; drive parts `drive_left` / `drive_front`: a half coupling at the shaft line on that face (the motor beside it drives the pumps) |
| heatexchanger | Counterflow Heat Exchanger | parts `shell` (one block; the render scales it over a 2 x 2 x N stack) and `flange` on the x = 0 face: A large at CENTRE, B small at (y 12, z 8) 0.1 px proud of A's; the render draws `flange` on each end block of a stack |

