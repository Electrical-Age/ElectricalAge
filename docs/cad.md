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
- **OBJ targets** (`obj.ObjWriter`): `block` writes a neoforge:obj block model (0..1, usemtl per material, planar UVs
  at vanilla density); `eln` writes an EA Obj3D node model (centred, parts, one atlas of 16 px cells, UVs inset 0.3
  texel). `obj.ObjReader` reads either kind back as faces, so the checker can look at any shipped OBJ
  (`ObjSurveyTest`, report only: the 1.12 Blender models have many coplanar faces) and `obj.ObjCompare` tells whether
  two models draw the same surface (how the Python models were checked when they were ported).
- **Palette** (`tex.Palette`): every material by name (base colour, noise, a shade pattern per texel of a 16 px
  cell). One shared palette (Baughn: steel is steel); a new material is added there once. `tex.Atlas` paints a
  model's cells from it.
- **Textures**: `tex.Sheet` paints regions (base colour, seeded noise, vertical shade, patterns) and face-aware
  features such as `ring` (a stripe round an entity box's sides that meets itself at the edges).
- **Previews**: `preview.Raster`, a small software renderer for before/after pictures (`-PcadPreviews=DIR` runs the
  preview tests).

Users: `VoxelGeometry` (pipes, fittings, valves, cables; `VoxelGeometryCadTest` checks every tier x fitting x mode x
orientation x route shape), `BikeShape`/`BikeModel`/`BikeTexture` (both bikes; `BikeModelCadTest`).

## Device models (src/cadmodels)

The EA device models (the oil chain, the shaft machines, the steam turbines, the column, the anvil, the capacitor
bank, the converters: 39 models) are Java classes in `src/cadmodels/java/mods/eln/cad/models`, ported from the
Python code-CAD (tools/models, retired 2026-10-30) with their z-fights and loose pieces fixed. `Catalog` lists them
(`Models`); `./gradlew generateCadModels` (run by processResources, cached on the models' and the library's code)
runs the checker over them, fails the build on any issue, and writes them into `build/generated/cad`: nothing of it is
committed. `assets/eln/model/index.txt` (`script/gen_model_index.py`) still lists every OBJ by name.

- **Kit**: `Mc` (mcmodel's primitives, corner for corner: `box(..., faces)` with faces by letter u d s n e w,
  `prism(axis, c1, c2, r, a0, a1, mat, n, rEnd, cap0, cap1, phase)`, `tube(...)` annulus, `volute(...)`), `OilKit`
  (oriented box, quarter turns about the block's vertical centre line, mirror, terminal/signal plates, heat pad, bolt
  ring, handwheel, dial), `ShaftKit` (the shaft line, half couplings, port flanges). Meshes are `org.erisia.cad.Mesh`.
- **NodeModel** (EA Obj3D): parts in order (`main` first, moving parts named as the Render draws them), the materials'
  palette entries (`paint` is the device's paint), `origin(part, x, y, z)` px for NAME.txt, extra NAME.txt lines,
  `hitbox(...)` px for NAME.hitbox. **BlockModel**: a neoforge:obj block model, its sprites painted from the palette
  (`textures/blocks/<dir>/<material>.png`) and its model JSON.
- **Checker fixes** carry a comment "(the checker: what was wrong; what changed)". A deliberate decal or floating detail
  is `.free(reason)`; a decal lifted off its surface is `.layer(1)`.

### Conventions (EA Obj3D node models, as the Render classes read them)
- **Coordinates**: px 0..16, corner origin, y up; written centred (-0.5..0.5, floor at -0.5). **Front XN (x = 0)**:
  the render calls `front.glRotateXnRef()`; the back is x = 16; right is z = 16 seen from the front.
- **Parts**: `o NAME` before the first vertex; every part has its own `usemtl` (Obj3D attaches faces to the last
  usemtl's group). Origins in NAME.txt (`o part`, `f originX ...`, centred block units) name parts exactly.
- **UVs**: V up (Obj3D flips). One atlas per model, 16 px cells, UVs inset 0.3 texel. Lines split on single spaces.
- **Hitbox**: NAME.hitbox, one box a line (x0 y0 z0 x1 y1 z1 px, front XN); `ModelHitbox.of(obj)` turns it.
- **Discovery**: models are keyed by the OBJ file name (directory and file named the same).
- **Pitfalls**: coplanar faces z-fight (the checker finds them; offset >= 0.1 px); plates on a face are recessed 0.1 px
  with studs reaching the face; stacked blocks (the column) have a half-height flange at each joint so neighbours form
  one flange; keep cylinder radius, side count and phase identical across stacked blocks; ~1000-1800 triangles.
- Per-family notes: docs/models/ (shaft machines' frame, oil chain, column, steam turbines, compact turbine, converters).

### Cable terminals (where floor cables connect)
EA floor cables are only 1-2 px tall, so a device that wants them to meet its conductors opts in, and the cable then
draws a lug at that end (`mods.eln.cable.CableTerminals`, `CableRender.drawKnuckles`):
- **Terminal plate** on each connecting face: centred across the face (4.5..11.5 px), floor to 4 px (tier stripe on
  top is fine), recessed 0.1 px (`OilKit.terminalPlate`, `signalPlate`).
- **Conductor centre 2.25 px above the floor, centred across (8 px)**, reaching the face plane. Studs are 1 px square
  (1.75..2.75 px); more studs at 8 +- 2.25 px.
- **Keep clear** of anything sticking out of the face in 6..10 px across, 0..3.5 px high: the lug lies there and reaches
  up to 2.3 px into the neighbouring block.
- **Opt in** in the descriptor's constructor: `cableTerminals = CableTerminals.FRONT;` (converters), `FRONT_AND_BACK`,
  `FLANKS`, `ALL_SIDES`, or `new CableTerminals(mask, heightPx)`.

## Other mods (Stasis)
The library's classes (`org.erisia.cad.*`) are compiled into the EA jar (the mod compiles cad/src/main/java). A mod
that already builds against EA (`compileOnly` on the EA jar, as Stasis does for EA's classes) can use them at build time
the same way: `compileOnly files(eln_jar)`, and the classes are there at runtime because EA is. For build-time
generation (its own models written by a Gradle task, as here), put the EA jar on that task's classpath. Not wired yet.

Not yet: Stasis models (towers, statues) on the library.
