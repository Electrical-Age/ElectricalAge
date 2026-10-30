# The checked model library (cad/, board #44)

Baughn (2026-10-30): stop whack-a-moling clipping bugs. `cad/` is a small pure-Java library (package
`org.erisia.cad`, no Minecraft: `:cad` compiles alone, and the mod compiles its sources in, as Stasis does with
towergen) for geometry that is checked by tests.

- **Authoring**: px, y up. Solids: `Box`, `Frustum` (cylinder, prism, cone), `Ring` (a hollow cylinder),
  `Sweep.along` (a round section along a polyline). Each has a material, a colour, a texture mapping (`UvMap`) and
  optionally a `Part` (a movable piece with a pivot and a rotation, for entity models). Solids may overlap (the
  house style); nothing is cut.
- **Emitters** turn a `Model` into a target's convention: `VoxelGeometry` (block-local 0..16, quads with sprites and
  cull faces), `EntityLayout` (vanilla `ModelPart` cubes: y down, ground at 24, with vanilla's box unwrap).
- **Checker** (`Checker.check`), run by unit tests, never by the game, and it never moves anything:
  - degenerate: a solid thinner than 0.05 px, a face of no area;
  - z-fight: two faces of different solids facing the same way within 0.05 px of one plane, overlapping by area, that
    look different there (material, colour or texture coordinates). Opposite-facing faces are fine (back faces are
    culled); an overlap wholly inside another convex solid is hidden and not reported;
  - declared relations: `attach`/`abut` (touching, or sunk at most so far, and held over enough of itself: seen along
    the contact, the part of a inside b (grown by 0.05 px) covers at least 50 % of a's own outline, settable per
    relation with an optional minimum area; Baughn: not just touching. Declare the narrower piece as a: a stem on a
    dial, a body under a wider cap), `disjoint`, `within` (e.g. a hitbox),
    `ratio` (a proportion);
  - connected: every drawn solid hangs together with its body (`Solid.body`; else its nearest `Part.moving()` part, a
    wheel or a crank; else the model), linked to the body's main piece (the component with the most bulk) through
    joints that each hold at least 50 % of the narrower piece's outline, seen along the joint (a tube's end inside
    another: its whole section). A solid meant to float says so with `free(reason)`. VoxelGeometry makes each part a
    body of its own;
  - a decal that is meant to lie over a surface says so with `layer(n)` (n x 0.1 px out).
  `Checker.faces` also reads other engines' output (polygon soups), which is how the old geometry was surveyed.
- **OBJ targets** (`obj.ObjWriter`): `block` writes a neoforge:obj block model (0..1, usemtl per material, planar
  UVs as tools/models/mcmodel.py); `eln` writes an EA Obj3D node model (centred, parts, one atlas of 16 px cells,
  UVs inset 0.3 texel). `obj.ObjReader` reads either kind back as faces, so the checker can look at any shipped OBJ
  (`ObjSurveyTest`, report only) and `obj.ObjCompare` tells whether two models draw the same surface
  (`McmodelParityTest`: the converters and the steam oven rebuilt in Java match mcmodel's output).
- **Palette** (`tex.Palette`): every material by name (base colour, noise, a shade pattern), from the code-CAD
  models' atlases; `tex.Atlas` paints a model's cells from it.
- **Static models**: `mods.eln.cad.ModelCatalog` lists the models made with the library; `./gradlew cadModels`
  writes them into src/main/resources (committed) and `ModelCatalogTest` checks they're current and pass the checker.
  Python models move there when they are next changed; one whose look would change waits for Baughn.
- **Textures**: `tex.Sheet` paints regions (base colour, seeded noise, vertical shade, patterns) and face-aware
  features such as `ring` (a stripe round an entity box's sides that meets itself at the edges).
- **Previews**: `preview.Raster`, a small software renderer for before/after pictures (`-PcadPreviews=DIR` runs the
  preview tests).

Users: `VoxelGeometry` (pipes, fittings, valves, cables; `VoxelGeometryCadTest` checks every tier x fitting x mode x
orientation x route shape), `BikeShape`/`BikeModel`/`BikeTexture` (both bikes; `BikeModelCadTest`).

Not yet: Stasis (towers, statues) sharing the library; elncheck.py's Obj3D convention checks (bounds, floor, front
and back faces) still live in tools/models.
