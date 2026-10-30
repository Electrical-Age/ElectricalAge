# EA energy converter models: synchronous condenser (hand-off)

> Ported to Java (board #44): the model is a class in src/cadmodels/java/mods/eln/cad/models, generated at build time
> (docs/cad.md); the Python script, its commands and its own atlas are gone (the shared palette replaces the atlas).


What: new 3D models for eln:energy_converter_eln_to_other_{lvu,mvu,hvu}block (50/200/800V), replacing the stub cube.
Design: a small synchronous condenser on a skid. The ribbed stator runs from the EA side (front) to the export side (back).
A terminal box sits on top with tier bushings: 50V has 2 short ones with green collars, 200V 3 with one shed and blue,
800V 3 tall ones with two sheds and orange. The tier paint band matches the cable colours. EA face: terminal plate flush with the face,
down to the floor, with copper studs and a tier stripe. Export face: framed red panel with brass bolts and a copper shaft coupling.

## How it's made (code-CAD)
- `Mc`: a tiny modelling library (box, n-gon prism/frustum, tube) that writes OBJ + MTL with UVs at vanilla
  density (16 px per block, planar per face, shifted into 0..1) and one material group per texture.
  `assets/eln/models/block/energy_converter_<tier>.{obj,mtl,json}` and 9 procedural 16x16 textures in
  `assets/eln/textures/blocks/converter/` (steel, darksteel, copper, brass, porcelain, paint_{green,blue,orange}, export).
- ~930 / 1074 / 1158 triangles (lvu/mvu/hvu), static baked models via `"loader": "neoforge:obj"` (flip_v true,
  corner origin 0..1, verified with a marker texture). No block-entity renderer needed.

## Integration in EA (not done; LabAgent)
1. Assets: run `Converter` into src/main/resources (or commit its output).
2. Datagen (`ElnModels.java` l.85-112): drop the `energy_converter` cube; for `EnergyConverterElnToOtherBlock` emit, per tier,
   `{"variants": {"front=N": {"model": "eln:block/energy_converter_<tier>", ...rot}}}` with
   front 0 → x 90, 1 → x 270, 2 → none, 3 → y 180, 4 → y 270, 5 → y 90, 6 → none (verified in game: EA face = FRONT).
   Item model: `eln:block/energy_converter_<tier>` (the parent block/block gives GUI and hand transforms; the 3D icon reads OK).
3. **`noOcclusion()`** on the converter block properties (ElnRegistry.registerEnergyConverter, l.91): the model doesn't fill
   the cube, so as an occluding block it makes neighbours cull their touching faces (see-through holes under and beside it).
4. If tools/icons wants flat icons for these (CONTRACT rule 5), base them on these models; the manifest currently says block_model.
