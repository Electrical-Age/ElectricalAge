# EA compact steam turbine model (50V Compact Steam Turbine)

> Ported to Java (board #44): the model is a class in src/cadmodels/java/mods/eln/cad/models, generated at build time
> (docs/cad.md); the Python script, its commands and its own atlas are gone (the shared palette replaces the atlas).


What: the model for the 50V Compact Steam Turbine, the early-game bootstrap generator (300 W, 50 V): one block with
everything packaged on a skid, visibly smaller and simpler than the 50V Steam Turbine (`steamturbine.md`).
Written as `compactturbine50v`.

Design (front = XN, x = 0; left seen from the front = z-, right = z+):
- Left flank (z = 0): the heat plate, where a heat furnace touches it. Vertical heat-tinted copper ribs reach the face
  over a recessed cast-iron plate in a heat-red rim, floor to 7 px, on a cast-iron firebox. A small riveted drum boiler
  lies on top of it (axis along x), with a sight glass between brass cocks on its front head, facing the front.
- Middle: a green-banded generator (50V tier paint) at the front and a small cast-iron scroll turbine at the back, shaft
  along x at y = 5.5, z = 8.5 px (off centre to make room for the drum and the condenser). Steam pipe from the drum's
  top to the scroll, with a tier collar. The shaft and copper coupling are part `rotor`, so the render can spin it.
- Right flank (z = 16): a tiny air-cooled condenser: a header with 11 thin copper fins out to the face, a cap plate,
  the exhaust duct into it from the scroll, and a condensate return to the drum along the back of the skid. On top: a
  short vent stack with a rain cap on three posts. Its outlet is `ventX/ventY/ventZ` in part `main` of the `.txt`.
- Top: water inlet. A laid pipe's flange (r 2.75 px, 4 bolts) is centred on the top face (y 15..16), and a reducer and
  a small pipe with a check valve lead down into the drum. The body is about 13 px tall and the flange reaches 16.
- Electric out: steel terminal plates flush with the front and back faces, floor to 4 px, 7 px wide, three copper
  studs and a green tier stripe (`oilkit.terminal_plate`, as the steam turbines).

## Files
- `CompactTurbine` RESOURCES` writes `assets/eln/model/compactturbine50v/`: `compactturbine50v.obj` + `.mtl`
  (parts `main`, `rotor`; one material `atlas`), `tex.png` (the shared oilkit atlas, paint = 50V green) and
  `compactturbine50v.txt`:
  `o rotor` / `f originY -0.15625` / `f originZ 0.03125`, `o main` / `f ventX 0.17188` / `f ventY 0.18750` / `f ventZ 0.37500`.
  1554 triangles (main 1462, rotor 92).

## Not done
- Java wiring (descriptor, render spinning `rotor`, steam puffs at the vent, `CableTerminals.FRONT_AND_BACK`).
- Inventory icon.
