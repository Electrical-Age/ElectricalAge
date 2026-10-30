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
  - declared relations: `attach`/`abut` (touching, or sunk at most so far), `disjoint`, `within` (e.g. a hitbox),
    `ratio` (a proportion);
  - a decal that is meant to lie over a surface says so with `layer(n)` (n x 0.1 px out).
  `Checker.faces` also reads other engines' output (polygon soups), which is how the old geometry was surveyed.
- **Textures**: `tex.Sheet` paints regions (base colour, seeded noise, vertical shade, patterns) and face-aware
  features such as `ring` (a stripe round an entity box's sides that meets itself at the edges).
- **Previews**: `preview.Raster`, a small software renderer for before/after pictures (`-PcadPreviews=DIR` runs the
  preview tests).

Users: `VoxelGeometry` (pipes, fittings, valves, cables; `VoxelGeometryCadTest` checks every tier x fitting x mode x
orientation x route shape), `BikeShape`/`BikeModel`/`BikeTexture` (both bikes; `BikeModelCadTest`).

Not yet: OBJ emitters and a Gradle task for static models (the Python scripts in tools/models stay until a model is
touched), and Stasis (towers, statues) sharing the library.
