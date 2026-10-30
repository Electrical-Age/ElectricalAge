# Oil-chain device models (electric heater, boiler, oil pump, tar sand separator)

> Ported to Java (board #44): the model is a class in src/cadmodels/java/mods/eln/cad/models, generated at build time
> (docs/cad.md); the Python script, its commands and its own atlas are gone (the shared palette replaces the atlas).


EA 1.12 node OBJs (path 2 in docs/cad.md), front XN, block -0.5..0.5, floor y = -0.5. Shared pieces live in `OilKit`
(atlas, multi-part writer with `.txt` origins, `turned(meshes, k)` to put a fitting designed for the x = 0 face on
z = 16 (k=1), x = 16 (k=2) or z = 0 (k=3), terminal plate, heat pad,
dial, handwheel, bolt ring, composed preview); `Mc` is unchanged. Each script writes
`assets/eln/model/<name>/<name>.obj + .mtl + tex.png` (64x80 atlas, 20 procedural cells; `paint` = tier colour:
50V green, 200V blue, boiler oxide red). Run from the repo root:



| model | parts | triangles | descriptor | replaces |
|---|---|---|---|---|
| electricheater50v / 200v | main | 708 / 814 | ElectricHeaterDescriptor (draws main/dissipator/body if present) | largerheostat |
| boiler | main | 1236 | BoilerDescriptor (draws main/body/furnace if present) | electricfurnace |
| oilpump50v / 200v | main | 1426 / 1462 | OilPumpDescriptor (draws main) | compressora / compressorb |
| tarsandseparator50v / 200v | main, rot1, rot2 (+ .txt origins) | 1576 / 1684 | OilSeparatorDescriptor = MaceratorDescriptor draw, unchanged | maceratora / maceratorb |

## Electric heater (`ElectricHeater`)
Power on the two side faces (model z = 0 / 16), heat out of the back (XP), nothing on the front. Front: framed window,
grille over glowing coiled elements on ceramic end insulators (3 on 50V, 4 on 200V), dial + amber pilot lamp + brass
nameplate on the top rail (static decoration; the regulator lives in the GUI). Tier-painted cap with a louvred vent.
Sides: floor terminal plates with a terminal box behind each. Back: copper heat pipes into a cast-iron backing plate
and a full-height heat-tinted copper contact flange (down to the floor) that presses against the boiler/column.

## Boiler (`Boiler`)
Thermal on all four faces: a heat-tinted copper pad (8 px wide, floor to 4 px) on every face of a cast-iron
heat-collector base. Riveted horizontal drum (axis front-back) on red saddles, dished heads, seam bands. Front head:
gauge glass between brass cocks (water level), pressure gauge. Top: steam dome with the outlet flange, lever safety
valve with its weight. z+ flank: feedwater inlet with a check valve and handwheel. Back head: manhole. The fittings
face the boiler's front; when a heater is placed against that face they are hidden behind it.

## Oil pump (`OilPump`)
Power on all four faces (floor terminal plate + junction box each). Wellhead at the bottom centre over the casing:
casing-head flange with bolts, tubing head, flanged flow tee (pressure gauge to the front, discharge line with gate
valve + red handwheel to z+), brass stuffing box, polished rod, drive-head frame, cast-iron gearbox; vertical finned
motor at the back on a pedestal, tier band, terminal box + conduit, tier-painted belt guard on top. A progressing-
cavity style drive head, not a pumpjack: with the well at the block centre a beam pump does not fit one block.

## Tar sand separator (`TarSandSeparator`)
A heated twin-shaft paddle trough (conditioning mixer). Designed for MaceratorDescriptor.draw as it is:
`main.draw()`, `rot1.draw(counter, 0,0,-1)`, `rot2.draw(counter, 0,0,1)`, input item at (0, 0.4, 0) and output item at
(0, -0.5, 0) after a 0.7 scale (about 12.5 px and 2.4 px up, block centre).
- rot1 / rot2: the two paddle shafts along z (paddles staggered, helical) with equal spur gears on the z- end. Origins
  (`.txt`): rot1 x -0.140625 (50V) / -0.15625 (200V), rot2 the mirror, y 0.09375 / 0.109375. The gears are phased by
  half a tooth, so they stay meshed at every angle (checked over a full turn, no overlap); everything that turns stays
  inside the block and clear of `main`.
- The trough top is open (froth surface below the shafts' tops, paddles break through it): the input item shows
  above the froth. Under the trough a sand spout and a sand tray with a pile, between the legs: the output item sits on it.
- Power on the z faces (powerLrdu: not front/back): floor terminal plates there. Tier-painted heating jacket with
  element terminals on the front, feed chute on the front rim, motor standing on a gearbox at the z+ end, froth
  overflow launder + oil outlet pipe with valve on the back (x+).
