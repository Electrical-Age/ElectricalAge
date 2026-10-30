# EA small steam turbine models (50V / 200V Steam Turbine)

> Ported to Java (board #44): the model is a class in src/cadmodels/java/mods/eln/cad/models, generated at build time
> (docs/cad.md); the Python script, its commands and its own atlas are gone (the shared palette replaces the atlas).


What: models for the 50V and 200V Steam Turbine (`mods.eln.oil.SteamGenerator*`), replacing the heat turbine placeholders
(`turbineb`, `turbinebblue`). Registered in `Wp10aContent.registerSteam` as `steamturbine50v` / `steamturbine200v`.

Design: a compact turbine-generator set on a skid, shaft running front to back.
- Front (node front, XN): the generator end. Ribbed steel stator with end bells, cooling fins (2 on 50V, 4 on 200V) and a
  tier paint band (green 50V, blue 200V, the cable colours), on cradle feet. A terminal box on its z- flank (left, seen from the front)
  feeds a bus duct along the skid rail to the back.
- Back (front.back(), XP): the turbine. A cast-iron scroll (volute) casing whose radius grows round the shaft, end covers,
  rear bearing and a governor housing with a tier-painted lid. Copper shaft coupling between turbine and generator.
- Top: the steam inlet, an iron pipe's 4 px bore the whole way (Baughn: the standard for this tier), drawn as a laid pipe:
  up out of the scroll past a tier collar, across to the middle, up to a laid pipe's flange (radius 2.75 px) on the top
  face, bolted (4 bolts 50V, 6 bolts 200V). A pressure gauge on the bend faces the front. Pipes attach on any side, so the inlet is decoration and so is the exhaust vent stack with its rain cap on the z+
  flank (right, seen from the front). It vents to air: the turbine has no steam output.
- Electric out: steel terminal plates flush with the front and back faces, from the floor up 4 px, 7 px wide, with
  three copper studs and a tier stripe, where EA's floor cables (LRDU Down) join.
- 200V is bigger: shaft 0.5 px higher, generator radius 4.5 vs 3.75 px, scroll radii 3.25-5.5 vs 2.75-4.75 px, up to
  15.75 px tall vs 14.5.

## Files
- `SteamTurbine` RESOURCES` writes `assets/eln/model/steamturbine{50v,200v}/` with `steamturbine*.obj` + `.mtl`
  (part `main`, one material `atlas`) and `tex.png` (64x32 atlas of 8 procedural 16x16 cells: steel, darksteel,
  castiron, copper, brass, paint (tier colour), glass (gauge face), rubber). 1454 / 1614 triangles (main + rotor).

## Not done
- Inventory icon: datagen (`ElnModels`, the `v_steam_turbine` line) reuses the heat turbines' flat item textures. That's
  fine for now; a flat icon rendered from this model would be better.
- Done: the shaft and coupling are a second part, `rotor` (origin on the shaft axis in `steamturbine*.txt`), which
  `SteamGeneratorRender` turns at 360 degrees/s times the shaft speed (synced, eased): a subtle sign it's running.
