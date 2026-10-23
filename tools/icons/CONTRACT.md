# ELN icon production contract

Production of two icon sets for Electrical Age, Minecraft 26.3 / NeoForge port
(this repo, branch ports/26.3). Decided by the project owner on 2026-10-11.

## The two sets
- **vanilla** — "Vanilla-native 16px".
  Default set, covers every item that has a flat icon (~290 items).
- **schematic** — "Schematic glyph 32px".
  Optional set, shipped as a bundled resource pack that players enable in Options → Resource Packs.
  Covers only items with `schematic_scope = true` in the manifest; everything else falls through to the vanilla art.

## Owner decisions (binding)
1. Players switch sets with a built-in resource pack (no config option).
2. Schematic scope = electrical devices only: everything placeable (devices, cables, switches), plus electrical
   components with a standard circuit symbol (fuses, light bulbs, heating elements, portable batteries/capacitors,
   electrical motors). Out of scope: materials (dusts, ingots, plates, resin, rubber…), machine parts (chips, cores,
   magnets, casings, machine blocks, upgrades, filters, burners), tools, brushes, armour, creative-tab icon.
3. Voltage tiers are told apart by **colour**, never by thickness alone. Items that used to share one texture across
   tiers (switches, fuses, antennas, 50V/200V lamps and chargers, …) each get their **own** colour-coded icon.
4. Vanilla icons are 16×16 by default. An icon may be 32×32 only when the manifest says `vanilla_size = 32`
   (decided centrally per family, never by an individual artist).
5. The rule from the style study still holds: a placeable device's icon either matches its in-world 3D model
   (vanilla set) or resembles no physical device at all (schematic set: flat glyph/line only, no depth, no shading).

## Tier colours (shared by both sets)
Source of truth: `VoltageLevelColor.setGLColor()` in the repo (the mod's own voltage colour code):
| tier   | RGB (0-1)        | hex      |
|--------|------------------|----------|
| signal | .80 .87 .82      | #ccded1  |
| LV     | .55 .84 .68      | #8cd6ad  |
| MV     | .55 .74 .85      | #8cbdd9  |
| HV     | .96 .80 .56      | #f5cc8f  |
| VHV    | .86 .58 .55      | #db948c  |
Each kit derives its own ramps from these hues (darker/more saturated shades are expected for small pixel areas),
but the hue family per tier must be the same in both sets. Thermal items use the colour of textures/voltages/thermal.png.

## Files and naming
- Production root: `tools/icons` in the repo. Drawing writes only here; `tools/integrate.sh` copies the result into
  the mod. Tools need the repo's dev shell (`flake.nix`; `direnv allow` or `nix develop`).
- `<set>/out/` is build output and not committed: run `python3 build.py` in each set (and
  `python3 vanilla/extra/build_extra.py`) before `tools/integrate.sh`.
- One icon per registry item id: `<set>/out/<item_id>.png`. Item ids come from `manifest/manifest.json`.
- Sources: `<set>/kit/` (shared library — **read-only for family artists**), `<set>/families/<family>.py`
  (family-local helpers), `<set>/icons/<item_id>.py` (one module per icon). `<set>/build.py` rebuilds everything
  and must tolerate a broken module (report it, keep going).
- Family artists write only their own `icons/<id>.py` and `families/<family>.py`. If the kit needs a change, they
  return it as a request in their result; they do not edit kit/ files.
- Integration (`tools/integrate.sh`): vanilla PNGs → `src/main/resources/assets/eln/textures/item/<item_id>.png`;
  schematic PNGs → a bundled pack overriding the same paths; datagen `ElnModels.itemTexture` prefers `item/<id>`.

## References
- Manifest: manifest/manifest.json (item_id, family, placeable, tier, vanilla_size, schematic_scope, model, parts,
  in-world description, current texture).
- Model reference sheets (14 views, all parts; not committed, Blender needed: `nix develop .#refs`):
  `tools/render_all.sh <modeldir>/<obj>.obj` writes refs/<modeldir>__<obj>_*.png and refs/*_parts.txt.
- Render specific parts: `blender -b -P tools/render_ref.py -- <repo>/src/main/resources/assets/eln/model/<dir>/<obj>.obj OUT_PREFIX Part1,Part2`
  (env RES=384 for size).
- Current icons: `src/main/resources/assets/eln/textures/item/` (the shipped set; the 1.12 icons are in git history before cbe98a8b)
- Model textures: `src/main/resources/assets/eln/model/<dir>/`
- Device code: `src/main/java/mods/eln/` (registry/ and registry/batch/ register items).
