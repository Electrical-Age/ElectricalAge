# Device models with code-CAD (tools/models)

New and changed models go to the checked model library instead (docs/cad.md: `mods.eln.cad.ModelCatalog`,
`./gradlew cadModels`), which checks them for z-fights, thin parts and loose pieces; its OBJ writers follow the
conventions below. The scripts here stay until their model is next changed.

Models are Python scripts that build geometry from primitives and write OBJ files. No Blender, the output is reproducible,
and each tier is a parameter set. Examples: `converter.py` (README-converter.md), `steamturbine.py` (README-steamturbine.md).
Run everything with `nix-shell -p 'python3.withPackages(p:[p.pillow])' --run 'python3 ...'` from the repo root.

## The library: mcmodel.py
- Design in **pixels, corner origin**: the block is 0..16 on each axis, Y up. Primitives return a `Mesh` with one material
  name: `box(x0,y0,z0,x1,y1,z1,mat,faces="nsewud")`, `prism(axis,c1,c2,r,a0,a1,mat,n,r_end,caps)` (n-gon cylinder or
  frustum), `tube(...)` (annulus: fins, rings, flanges), `volute(axis,c1,c2,r0,r1,a0,a1,mat,n,start)` (scroll casing).
  `c1, c2` are the centre in the two other axes, in xyz order. Collect them with `Model().add(...)`.
- Polygons are wound CCW seen from outside; this matters (both paths cull back faces). For a new shape, build each
  polygon and pass it through `oriented(poly, outward_hint)`.
- Polygons must be convex (they are fan-triangulated from vertex 0). Non-convex outlines (the volute cap) are written
  as triangles fanned from a centre point.
- UVs are planar per face at vanilla density (16 px per block), shifted into 0..1, upright on side faces.

## Path 1: baked block model (`neoforge:obj`), e.g. the energy converters
For plain blocks without a block-entity renderer. `Model.write_obj` + `write_mtl` write one `usemtl` group per material
and `map_Kd #mat` (a model texture variable); the model JSON binds each variable to a sprite
(`eln:blocks/<dir>/<mat>`, 16x16 PNGs under `textures/blocks/`). JSON: `"loader": "neoforge:obj"`, `"flip_v": true`,
`"parent": "minecraft:block/block"`. Coordinates 0..1, corner origin. The script decides the model's facing (the
converter: EA side north) and the blockstate rotates it per `front` value (converter.py `ROT`). Give the block
`noOcclusion()` if the model doesn't fill the cube, or neighbours cull their touching faces.

## Path 2: EA 1.12 node OBJ (`Obj3D`), e.g. the steam turbines
For transparent/six nodes drawn by their `Render` class. `Model.write_eln(outdir, name, cells, grid)` writes
`assets/eln/model/<name>/<name>.obj` + `.mtl`; you write the atlas `tex.png` next to them. Conventions, all checked against
`mods/eln/misc/Obj3D.java`, `gl/GlFrame.java`, `misc/Direction.java`:
- **Coordinates**: centred on the block, -0.5..0.5 on every axis, floor at y = -0.5 (TransparentNodeRender translates by
  0.5). `write_eln` maps px to `px/16 - 0.5`.
- **Front**: model **XN** (x = 0 px). The render calls `front.glRotateXnRef()`, which rotates XN onto the node's front
  (XP: 180 deg about Y, ZP: +90, ZN: 270). So `front.back()` is XP (x = 16 px). LRDU Down on a side face = its floor edge.
- **Faces**: triangles only (other faces are logged and dropped). `vn` is ignored; normals come from the winding
  ((v1-v0)x(v2-v0)) and GlFrame culls back faces by default.
- **Parts**: `o NAME` must come before the first `v` (vertices are global across parts); the descriptor draws parts by
  name (`obj.getPart("main").draw()`). For moving parts, write several `o` blocks and a `<name>.txt` with origins
  (`o part` / `f originX 0.1` ...); `write_eln` writes one part (`part="main"`).
- **Several parts**: Obj3D adds faces to the FaceGroup of the last `usemtl`, which belongs to the part that was
  current then, so every `o` needs its own `usemtl` line or its faces land in the previous part (elncheck doesn't
  catch this). `oilkit.write_parts` writes `o NAME`, the part's `v`/`vt`, `usemtl atlas`, its faces, per part, plus
  `NAME.txt` with the origins (centred block coordinates); `.txt` part names must match the `o` names exactly.
- **UVs**: Obj3D stores `1 - v`, so write ordinary V-up OBJ UVs (0,0 = bottom left of the image). Don't flip again.
- **Textures**: `map_Kd file.png` in the MTL resolves to `eln:model/<dir>/<file>` (lower case, file name only). Each
  `usemtl` group may have its own texture (one GlFrame batch each), but the convention here is one atlas per model:
  16x16 cells, material -> (col, row), row 0 at the top of the image; `write_eln` insets UVs 0.3 texel from the cell
  borders. Lines are split on single spaces: no double or trailing spaces.
- **Hitbox** (optional): `NAME.hitbox` beside the OBJ, one box a line (`x0 y0 z0 x1 y1 z1`, px, this frame); a
  descriptor returning `ModelHitbox.of(obj)` from `modelHitbox()` gets it as outline and collision, turned with its
  front. See README-shaft.md (the shaft machines all have one).
- **Discovery**: `assets/eln/model/index.txt` lists every OBJ (the jar can't be scanned); regenerate it with
  `python3 script/gen_model_index.py`. Models are keyed by the OBJ **file name** without extension
  (`Eln.obj.getObj("steamturbine50v")`), so name the directory and the file the same.
- **Wiring**: pass that name to the descriptor (e.g. `Wp10aContent.registerSteam`); the descriptor's draw picks parts,
  the render applies `front.glRotateXnRef()` first. Obj3D also loads on the server (descriptors read geometry), so a
  broken OBJ shows up in `build26.sh runGameTestServer` logs as "failed to load obj".

## Verify and preview
`python3 tools/models/elncheck.py MODEL.obj [...] --preview OUTDIR` (path 2) parses like Obj3D and checks: part
present, triangles only, MTL and texture exist, bounds in -0.5..0.5, touches the floor, geometry on the front and back
faces near the floor, UVs in 0..1, no degenerate triangles or UVs. `--preview` writes a z-buffered sheet of 7 views
(front = looking from XN, back, side_z+ / side_z- = the flank facing +z / -z, top with the front up, two isometric) with back faces culled as in game, so a
wrong winding shows as a hole. For path 1, regenerate into a scratch dir and diff, or load it as a resource pack
(`converter.py OUT --pack`). The game is the final check: orientation, lighting and cable joints.

## Pitfalls
- Coplanar faces of different parts z-fight (speckle in the preview): offset by >= 0.1 px.
- Plates on the block face: recess 0.1 px and let studs reach the face, rather than poking outside the block.
- Floor cables join a node's face at the floor centre: keep the bottom middle of connecting faces filled (a plate down
  to y = 0, about 7 px wide) and clear of overhangs. See "Cable terminals" below.
- Keep it ~1000-1500 triangles: 8-12 sides for big cylinders, 6-8 for bolts and pipes.
- `mcmodel.write_obj`/`write_mtl` must stay byte-identical for the converters: after editing the library, regenerate
  converter.py into a scratch dir and `cmp` with the committed OBJs.
- Stacked blocks (e.g. `column.py`): give each block a half-height flange at the joint (0.5 px at its bottom and top)
  so two neighbours form one flange without coplanar overlap; keep cylinder radius, side count and phase identical and
  periodic details (rungs, hoops) at a period dividing 16 px. Check with a composed preview (`column.py --stack`).
- elncheck's "front/back face near the floor" check is for nodes with floor cables; nodes without (stacked segments)
  fail it legitimately.

## Cable terminals (where floor cables connect)
EA floor cables are only 1-2 px tall, so a device that wants them to meet its conductors opts in, and the cable then
draws a lug at that end: the cable turns up against the face, a copper crimp barrel and a copper palm cover the middle
conductor (`mods.eln.cable.CableTerminals`, `CableRender.drawKnuckles`). The convention, as in converter.py and
steamturbine.py:
- **Terminal plate** on each connecting face: centred across the face (4.5..11.5 px), floor to 4 px (tier stripe on
  top is fine), flush with the face (recessed <= 0.1 px).
- **Conductor centre 2.25 px above the floor, centred across (8 px, the cable's axis)**, reaching the face plane
  (within 0.1 px). Studs are 1 px square (1.75..2.75 px); more studs go at 8 +- 2.25 px.
- **Keep clear** of anything sticking out of the face in 6..10 px across, 0..3.5 px high: the lug lies on the face
  there (palm up to 3.1 px high, barrel up to 2.85 px and +-2.1 px wide for the HV cable) and reaches 0.35 + cable
  thickness px (at most 2.3 px) into the neighbouring block.
- **Opt in** with one line in the descriptor's constructor, for the faces that carry plates (relative to the node's
  front): `cableTerminals = CableTerminals.FRONT;` (converters), `FRONT_AND_BACK` (steam turbines), `ALL_SIDES`, or
  `new CableTerminals(mask, heightPx)` for another conductor height. `TransparentNodeDescriptor` and, for simple
  nodes, `DescriptorBase` have the field; a transparent node render can override `cableKnuckleHeight(face)`. Only
  floor cables (LRDU Down onto a side face) get lugs; cable-to-cable joints are unchanged.
