# EA fractionating column and flare stack models

What: models for the Fractionating Column Base, Column Segment and Flare Stack (`mods.eln.oil.Column*`, `FlareStack*`),
replacing the placeholders. Model names (OBJ file = directory name): `columnbase`, `columnsegment`, `flarestack`; one
part `main` each, one material `atlas`. The descriptors draw all parts, so `main` is enough.

Shared look: lagged steel shell (aluminium cladding), signal-red paint bands, dark-steel straps and nuts, brass/copper
accents. Every column cylinder uses the same 12-gon (radius 5 px, flat faces towards +-x and +-z), so base and segments
line up.

## Design (px, front = XN = x 0)
- **columnbase**: reboiler. Dark steel plinth, cast-iron heat jacket (r 6.5) with a red heat band, steel shoulder up to
  the r 5 shell, lagging strap, top half-flange with nuts underneath, dark head. Thermal terminals: a copper pad flush
  with each of the 4 side faces at the floor centre (7 px wide, 3.5 px high, 4 brass bolts, a brass terminal stud at
  the README "Cable terminals" position (2.25 px up, centred), clamp bar, copper heat bus
  into the jacket), where thermal cables / heaters join (LRDU Down on the 4 horizontal sides). Front: fire porthole on
  the jacket and a level gauge (amber oil level) on the shell. Crude inlet on z+ and residue drain with a gate valve and
  red handwheel on z-, both with flanges flush with the face at face centre height (y 8), where pipes from other mods
  meet. Ladder with cage on the back from the jacket top up.
- **columnsegment**: full-height lagged shell, half-flanges at y 0..0.5 (nuts above) and 15.5..16 (nuts below), two
  straps, red band, draw-off nozzle with a gate valve and handwheel on the front (flange at face centre), temperature
  dial on z-, bolted manway on z+, ladder (rungs every 2 px, hoops at y 4 and 12) with cage on the back.
- **flarestack**: steel base plate (0.1..15.9) with anchor bolts, knockout drum on saddles with red bands, inlet
  flange at the front face centre with an elbow down into the drum, slim stack with red/white bands, guy collar with
  4 lugs and guy wires to the plate corners, flared tip with a hollow barrel (inner r 1.75, soot floor) open at the top
  centre, wind shield ring on struts, brass pilot line from an igniter box (z+) to a pilot head at the rim.

Stacking: a joint is two 0.5 px half-flanges (base/segment top + next segment bottom) that read as one 1 px bolted
flange; no coplanar overlap (the shared faces at the block boundary face opposite ways and are hidden). Ladder rails,
rung spacing and cage hoops repeat with period 2/8 px, so they continue across blocks.

## Files
- `column.py RESOURCES` writes `assets/eln/model/{columnbase,columnsegment,flarestack}/` (`<name>.obj`, `.mtl`,
  `tex.png`: 64x48 atlas of 12 procedural cells: steel, darksteel, castiron, copper, brass, paint (red), white, glass
  (gauge, amber level), lagging, soot, fire (porthole), rubber). Triangles: 1526 / 1456 / 1206.
  Run: `nix-shell -p 'python3.withPackages(p:[p.pillow])' --run 'python3 tools/models/column.py src/main/resources'`,
  then `python3 script/gen_model_index.py`.
- `column.py --stack RESOURCES OUT`: composes base + 4 segments + a flare beside it (and a variant with segments turned
  90 and 180 deg) and writes `tower_preview.png`, `tower_turned_preview.png` via `elncheck.preview`.
- Check: `python3 tools/models/elncheck.py src/main/resources/assets/eln/model/{columnbase,columnsegment,flarestack}/*.obj --preview OUT`.
  Expected: `columnsegment` reports "nothing on the front (x-) face near the floor"; it sits on the base and has no
  cable connections, so that check (meant for floor-cable nodes) doesn't apply.

## Open points
- Each segment is rotated by its own node `front`. If the player places segments facing different ways, the front
  nozzles and the back ladder don't line up (see `tower_turned_preview.png`). Making segments take the base's front
  (or the one below) when placed would fix it.
- The tower top is the top segment's half-flange and a flat head; there's no separate top cap model.
- Not animated; the flare flame is the render's particles (from y +0.55 above the tip).
