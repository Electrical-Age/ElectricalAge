# Vanilla-native icon style: production guide

Last updated: 2026-10-12 (revision 3, after wave 1: original material silhouettes, promoted ramps
POLE_WOOD/CERAMIC/TERRACOTTA/WHITE_PLASTIC, signal plate retune and plate-face CVD check, chip face
table fixes, knurled_disc/oblique_ring/tier_backplate, wave-1 rulings in sections 7, 7b and 8).
Revision 2 (2026-10-11): CVD-safe tier ramps, frozen material ramps, chips at 32, round primitives,
process-isolated build. Every icon in `vanilla/` must follow this guide. It supersedes the
style-study `spec.md` of `01-vanilla16`. Where it disagrees with `../CONTRACT.md`, the contract wins.

Before you draw, look at:
- `out/`: the reference icons `multimeter`, `50v_macerator`, `and_chip` (32 px), `power_capacitor` and the
  five cables. Their sources in `icons/` are the worked examples.
- `examples/do_dont.png`: each rule with a "don't" next to the right version.
- `examples/primitives.png` (source `examples/primitives.py`): worked examples for families that have no
  reference icon yet, such as heating elements, fuses, batteries, gauges, fans, bulbs, signal relays, solar
  cells, the utility pole and materials. It also includes `macerator_lv_32`, which shows how a 16 px drawing scales to 32.

---

## 0. How to work

1. Write one module per item, `icons/<item_id>.py`, with a `draw()` that returns a `kit.Canvas` (or a PIL RGBA
   image). Helpers shared by your family go in `families/<family>.py`. Write nothing else.
   **Never edit `kit/`.** If you need a kit change (a ramp, a primitive, a chip face), return it as a request in your result.
2. `from kit import *` gives you everything listed in §11.
3. **Build only your own ids:** `python3 build.py <your ids...>`, or `python3 build.py --family <family>` once
   `manifest/manifest.json` exists. Never run a bare `python3 build.py` while other artists are working.
   - Each icon renders in its own process. If a module raises, calls `sys.exit` or hangs past 60 s, that icon
     FAILS and its old `out/<id>.png` is **deleted**.
   - Only registry item ids are written to `out/`. For experiments under any other name, use
     `python3 build.py --scratch NAME`, which writes `scratch/NAME.png`.
   - Lint prints `ERROR` or `WARN` (§1). An ERROR never ships.
4. Judge every icon **in inventory slots** at **GUI 2** (the hardest case), next to its tier siblings and the
   reference icons: `python3 ../tools/contact.py SHEET.png out <ids...> --cvd`, or
   `build.py <ids> --contact SHEET.png`. `--cvd` adds the GUI-2 row as protan, deutan and tritan viewers see it.
5. Run everything in the repo's dev shell (`direnv allow` or `nix develop`) from `tools/icons/vanilla/`.
6. `python3 -m kit` checks the kit itself (including the tier CVD rule). `python3 -m kit --cvd` prints the tier and material separation.

## 1. Hard rules (enforced by `build.py` through `kit.check.lint`)

| rule | level |
|---|---|
| The canvas is exactly the item's `vanilla_size`: 16, or 32 only where decided centrally (manifest, else `kit.decisions.VANILLA_32` = every chip). Never the artist's choice. | ERROR |
| Alpha is 0 or 255 only: no antialiasing, resampling or soft edges. | ERROR |
| The id is a registry item id (`manifest.json`, else `base.json`). Anything else needs `--scratch`. | ERROR |
| No pixel is darker than CIE L\* 12 (`L_FLOOR`). | WARN `floor` |
| At most 16 colours (24 at 32 px). | WARN `colours` |
| The outer ring (row/col 0 and 15) holds only protrusions, at most 12 px (24 at 32). | WARN `edge` |
| The icon covers at least 12% of the canvas. | WARN `cover` |
| The object sits on the baseline: its lowest body row is 13–14 (27–29 at 32). Row 15 doesn't count. | WARN `baseline` |
| The silhouette is at least 10 px wide (20 at 32). | WARN `width` |

A module may silence a WARN with `LINT_SKIP = {'baseline'}` (or another name from the table) **only if** its docstring says why.
Example: a suspended lamp that hangs from the top of the slot and deliberately ends above the baseline.

## 2. Canvas, margins, anchoring

- **Body box** at 16 px: cols 1..14, rows 1..14 (`BODY_BOX`). The silhouette, outline included, stays inside it.
  The **baseline is row 14**. Centre horizontally; an oblique object is centred on front face plus depth.
- Only **protrusions** may enter col/row 0 or 15: chip pins, cable ends, probe tips, stubs and antenna tips.
- **Fill the slot.** Every placeable fills the body box whatever its size in the world, as vanilla does
  (lever, hopper and comparator all fill the slot). Default boxes:
  - **Full-block machine:** front 10 wide × 10 tall plus depth 4.
  - **Sixnode** (wall or floor part): front **12 wide** × 11 tall plus depth 2.
  - **`small_` variant:** the same drawing fitted into `SMALL_BOX = (3, 5, 10, 10)` (cols 3..12, rows 5..14),
    centred and on the baseline. Size is shown by geometry, never by a different design (§7b).
- Material templates (`ingot`, `dust`, `plate`) are the kit's own silhouettes (section 6) and end on row 13, which
  passes the baseline lint.
- At 32 px every number doubles (body box cols/rows 2..29, baseline 29). See §9.

## 3. Light, shading and round shapes

- Light comes from the **top-left**. No cast shadows, no ambient-occlusion blobs, no dithering, no gradients.
- Faces of every box/solid: **top = ramp[4]**, **front = ramp[3]**, **right side = ramp[2]**. The first row
  of a top face (depth 1) may take the bevel ramp[5].
- Cavities (windows, troughs, sockets, slots) use `CAV`. Inner left jambs (the inside faces you see through an
  opening) use the body's ramp[1].
- Texture: at most one or two hand-placed detail pixels per surface. Never copy a model texture's noise.
- **Cylinders: always `cylinder()`**, never hand-drawn rows. The band table `CYL_BANDS` fixes the ramp index of
  every row across the diameter (outline rows included; H = ramp[5]):

  | diameter | rows (top → bottom, or left → right when standing) |
  |---|---|
  | 3 | 1 4 0 |
  | 4 | 1 4 3 0 |
  | 5 | 1 H 3 2 0 |
  | 6 | 1 4 H 3 2 0 |
  | 7 | 1 4 H 3 3 2 0 |
  | 8 | 1 4 H 4 3 3 2 0 |
  | 9 | 1 4 H 4 3 3 2 2 0 |
  | 10 | 1 4 H 4 3 3 3 2 2 0 |
  | 11/12 | same pattern, the extra rows go to 4 and 3 |

  `axis='x'` lies (lit on top). `axis='y'` stands (lit on the left), and `cap=RAMP` shows its top end.
  Corners are rounded automatically from diameter 5 up. At 32 px each band is 2 px and the diameter must be even.
- **Round faces seen front-on** (gauges, lenses, hubs, the daylight sensor): `disc()`. **Annuli** (bezels, fan shrouds): `ring()`.
  **Knurled or toothed rims** (fuse caps, gear rims, thumbwheels): `knurled_disc(..., teeth=8)`: the notches are cut
  before outlining, so the outline follows the teeth.
- **A ring or torus lying in a top face** (toroidal coils, washers, a bezel on a lid): `oblique_ring()`. It is the
  ellipse inscribed in the top face of `box(x, y, w, h, dp)`; `height` > 0 stands it proud (a coil), 0 paints it flat.
  Its outline is 1 px even at 32, because the band is thin in depth. It reads at 32 px, or at 16 px with dp >= 4. Below that,
  hand-place it (as `families/power_passive.RHEO_TOP` does).
  **Lines** at any angle (antenna masts, wires, blades, needles): `line()` (Bresenham, 1 px; `width=2` at 32 px).
  A 45° accessory stroke with a lit edge is `diag_stroke()`.

## 4. Outline (the vanilla rule)

Checked against the 26.3 `iron_ingot` and `copper_ingot` textures.
- A silhouette pixel whose outside neighbour is on the **left or top** gets **ramp[1]**. One whose outside neighbour is on the
  **right or bottom** gets **ramp[0]**. Tie-break: open left gives light; otherwise open right or below gives dark;
  otherwise open above gives light. `outline_region`, `box`, `paint_solid`, `cylinder`, `disc`, `ring`, `strip` and
  `tier_stub` all apply this rule.
- The outline uses **the object's own ramp**: its darkest shade of its own hue, never black and never a shared grey.
- A part in another material gets its own outline in its own ramp (steel pins on a chip, tier caps on a fuse).
- **One material = one `solid()` union**, so it has one outline and no inner seams (`do_one_solid` vs
  `dont_seamed_boxes`; reference: the `power_capacitor` bracket). Occlusion between materials is handled by repainting
  the front part's pixels from a separate render (see `icons/power_capacitor.py`), not by drawing separate boxes.
- Chamfers, notches and gaps between feet are cut **before** outlining (`paint_solid(..., cut=[...])`).
- Parts only 1 px wide get no outline. Draw them in mid or dark ramp steps.

## 5. Palette

All shared ramps are frozen tuples in `kit/palette.py`. `python3 -m kit` prints them with L\* and hue. Index 0 is the darkest
(bottom/right outline) and 4 the lit top; 5 is the optional bevel/specular.
- **Hue shift:** darks drift cool, lights drift warm, about 4–8° per step. The near-neutral materials keep a single hue.
- **Floor:** L\* ≥ 12.
- **Don't invent a ramp for a material that is in the table.** For a genuinely new material, use `derive_ramp(h('#rrggbb'))`,
  hand-tune it, freeze it in `families/<family>.py`, and report it so it can be promoted. Ramps are tuples; to make a
  variant, build a new tuple (`STEEL[:3] + (h('#..'),) + STEEL[4:]`). Never assign into a kit ramp, which fails anyway.

### 5.1 Tier ramps: a lightness ladder, so tiers survive colour blindness

| ramp | 0 | 1 | 2 | 3 | 4 (contract) | 5 |
|---|---|---|---|---|---|---|
| TIER signal | `#212f2d` | `#3e554f` | `#739686` | `#9fc4ae` | **`#ccded1`** | `#edf6ec` |
| TIER lv | `#063029` | `#02513f` | `#1a7656` | `#40a575` | **`#8cd6ad`** | `#cbeed2` |
| TIER mv | `#18203e` | `#18355e` | `#0c4c78` | `#2473a0` | **`#8cbdd9`** | `#c6e1ed` |
| TIER hv | `#442413` | `#77431f` | `#b06c28` | `#e5a44c` | **`#f5cc8f`** | `#feecc9` |
| TIER vhv | `#391a20` | `#582028` | `#7b272d` | `#a63836` | **`#db948c`** | `#edc3b5` |
| THERMAL | `#232226` | `#2e2d30` | `#363537` | **`#424242`** | `#5c5a58` | `#7a7672` |

ramp[4] is exactly the mod's `VoltageLevelColor` colour, so it cannot move. **The owner keeps the mod's tier palette:
no tier hue or ramp[4] is retuned for colour blindness** (Minecraft has no colour-blind mode); separation is only
worked on in ramp[0..3] and in which step a face uses. The tiers are separated in ramp[0..3] by
**lightness**: on ramp[3] signal is L\* 76, HV 72, LV 61, MV 46 and VHV 40; ramp[2] follows the same order. Every pair of tiers is
**ΔE ≥ 15 on ramp[2] and ramp[3] for normal, protan, deutan and tritan vision** (`python3 -m kit --cvd`).
For example, LV and VHV, which used to collapse to the same khaki for red-green viewers, are now 23–30 apart.
The consequence: a tier cue must always show **ramp[3] (or [2]) over a real area**, never ramp[4] alone. Every
kit tier helper already does this: a plate front is ramp[3] (every tier, signal included since revision 3) and a
jacket's shaded half is ramp[3].

**Plate faces as painted.** The check also compares the faces `plate_ramp` actually paints (`kit.cvd.plate_pairs`):
every pair of tiers' plate **fronts** must be ΔE ≥ 15 in all four modes (`python3 -m kit` fails otherwise; `--cvd`
also lists tops and sides for information). This caught the revision-2 signal plate, whose front was ramp[2] and
came within ΔE 9.8 of the LV front for deuteranopes. Now the closest pair is signal and LV: 17.7 (deutan) and 18.8 (protan).

### 5.2 Neutral device materials (which one is which)

| ramp | 0 | 1 | 2 | 3 | 4 | 5 | use for |
|---|---|---|---|---|---|---|---|
| STEEL | `#24232e` | `#4c4b58` | `#7b7c86` | `#aeafb0` | `#dcdcd0` | | **every housing, frame, bracket, pin, screw**: the default metal |
| CABLE | `#25242b` | `#4e4d56` | `#83848c` | `#a9aaad` | `#c6c7c2` | | plastic housings and knobs, untiered jackets |
| CAV | `#202129` | `#262730` | `#383a43` | `#5a5c66` | `#8a8b94` | | interiors: windows, slots, sockets, screens that are off. Never an outline |
| CHIP | `#212229` | `#4a4c58` | `#2a2b33` | `#34363f` | `#454752` | `#6a6c78` | **only through `chip()`** (its [2] < [3] < [4] is not a normal ladder) |
| ACHIP | `#1a2133` | `#36465e` | `#1a2638` | `#223249` | `#30435e` | `#557090` | only through `chip()` |

### 5.3 Materials (frozen; use these and nothing else)

| ramp | 0 | 1 | 2 | 3 | 4 | 5 | use for |
|---|---|---|---|---|---|---|---|
| IRON | `#353535` | `#5e5e5e` | `#828282` | `#a8a8a8` | `#d8d8d8` | `#ffffff` | = vanilla `iron_ingot`: iron dust/plate/cable, iron heating elements |
| GOLD | `#752802` | `#b26411` | `#dc9613` | `#e9b115` | `#fdf55f` | `#fffde0` | = vanilla `gold_ingot`: gold dust/plate, quality trim level 2 |
| VCOPPER | `#6d3421` | `#9c4529` | `#c15a36` | `#e77c56` | `#fc9982` | `#fbc3b6` | = vanilla `copper_ingot` (salmon): copper dust/plate/cable, copper heating elements, trim level 1 |
| COPPER | `#3d1510` | `#76291a` | `#ad4a24` | `#d9773a` | `#f6b46a` | `#ffe2ac` | orange wire copper: coil windings and bare wire *inside* devices only |
| LEAD | `#2a3137` | `#444c54` | `#5f6973` | `#7f8a94` | `#a3acb5` | `#c6cdd3` | lead ingot/dust/plate, the fuse wire |
| TUNGSTEN | `#2e2620` | `#473b33` | `#62554a` | `#807267` | `#a19389` | `#c5b9b1` | tungsten ingot/dust/plate/cable, tungsten heating elements and filaments |
| SILICON | `#1f2334` | `#2b3249` | `#3e4662` | `#595f7d` | `#7d829e` | `#b2b5c7` | silicon ingot/dust/plate, wafers |
| ALLOY | `#302f3b` | `#4b4a5b` | `#6a687e` | `#8e8ca3` | `#b3b1c4` | `#dad9e4` | alloy ingot/dust/plate |
| FERRITE | `#1e2528` | `#293236` | `#364248` | `#49545a` | `#626c72` | `#858d91` | ferrite ingot, ferromagnetic cores, magnets |
| CARBON | `#1f2021` | `#272829` | `#313335` | `#414245` | `#55575a` | `#75777a` | coal dust/plate, carbon filaments, graphite |
| CINNABAR | `#451a1e` | `#6c262d` | `#993340` | `#c14e5a` | `#dd787e` | `#edabac` | cinnabar dust (mercury ore) |
| RUBBER_BLACK | `#1e2022` | `#282a2e` | `#35373b` | `#47494e` | `#5e6166` | `#7f8184` | rubber, black plastic, battery cans, tyres |
| WOOD | `#2e220e` | `#493615` | `#684e1e` | `#896727` | `#a98638` | `#c9a65a` | wooden handles and other light wood (vanilla `stick` hues) |
| POLE_WOOD | `#39190c` | `#502003` | `#5a2900` | `#6a3e10` | `#7e5622` | `#93693a` | the utility poles (the model's dark reddish-brown creosoted wood, #62380b / #472c0f) |
| CERAMIC | `#451c00` | `#713700` | `#995800` | `#d4922c` | `#ebb445` | `#f4d99a` | amber resistor ceramic: power resistor, rheostat and thermistor bodies |
| TERRACOTTA | `#4b1611` | `#6e1705` | `#872800` | `#9c4913` | `#a67448` | `#bb9067` | untiered terracotta/copper-coloured housings: the ground cable plate (texture #ab4f14) |
| WHITE_PLASTIC | `#5e5e5e` | `#828282` | `#a8a8a8` | `#d8d8d8` | `#ffffff` | | white plastic cabinets and modules (plc / signal_processor); = IRON[1..5], so 5 steps and no bevel |
| GLASS | `#587176` | `#809ba0` | `#a7c0c5` | `#cadee1` | `#e8f3f5` | `#f8fdfe` | clear glass: fuse bodies, windows, unlit envelopes |
| LIT | `#7a4a1c` | `#b8792e` | `#eeb54a` | `#ffda70` | `#fff1ad` | `#fffbe8` | emissive light: lit bulbs, lamp lenses, LEDs that are on |
| SOLAR | `#221f35` | `#2f2a4a` | `#3f3860` | `#524b74` | `#6d668a` | `#9893ab` | photovoltaic cells (violet-navy, kept clear of MV); grid lines in [4] |

Part ramps: `RUBBER_Y` (multimeter body), `CAP_BLUE` (capacitor can), `LCD`, `LEAD_RED`/`LEAD_BLK` (probe leads).
Single colours: `MARK_RED #c45a52`, `MARK_BLUE #5a7cc4`, `MARK_GREEN #5ab85a`, `WHITE #ececec`, `GLYPH #c8c9cc`, `GLYPH_A #a9c0d6`,
and the chips' second ink `GLYPH_DIM #87888e` / `GLYPH_A_DIM #71869c` (face key `a`).

**Colour collisions to respect** (`python3 -m kit --cvd`, normal vision ΔE < 15): COPPER and LIT are close to HV;
CINNABAR is close to VHV; GLASS and IRON are close to signal; SILICON is close to MV; CERAMIC is close to HV (ΔE 9 on [3]).
TERRACOTTA lies between HV and VHV in lightness: it is tuned as far from both as a terracotta can be (ΔE ≥ 10.5 on
the faces a box shows, in all four modes; COPPER was 5.5), but it cannot reach 15. These materials may appear on a
tiered device only as small parts, and TERRACOTTA and CERAMIC only on untiered devices. Never put them next to the tier cue of the tier they resemble. CAP_BLUE next to an MV cue is ambiguous
in the same way: move the cue.

## 6. Projection

**Placeables (devices, machines, sixnodes): cabinet oblique.** The face that carries identity is drawn flat and
front-on. Depth recedes **up-right at 45°** (+1 x, −1 y per depth px). Depth is a lookup, not a measurement:
`depth(cv, 'block')` = 4, `'sixnode'` = 2, `'flat'` = 1 (doubled at 32).
- **MATCH rule (contract 5):** the icon is a likeness of the in-world model. Use the OBJ parts and the texture
  colours (`refs/<dir>__<obj>_sheet.png`, `refs/*_parts.txt`, `tools/render_ref.py`). Never draw a schematic symbol
  unless the model's texture prints one (chip faces do).
- Underlay: take the ortho front, top and side renders, multiply the box sizes by 10/16 (or by your body box) and pass them
  to `solid()`. Then add a hand-placed detail grid.
- Keep **one identifying feature** (rollers, dial, window, lamp, blades) and make it large. Drop the rest.

**Non-placeables: front-on templates.**
- Materials: `ingot(ramp)`, `dust(ramp)` and `plate(ramp)`. **Owner decision (revision 3): these are the kit's own
  silhouettes, not copies of Mojang's 26.3 alpha masks.** They follow the vanilla idiom (one object lying diagonally
  or heaped, filling the slot, vanilla outline, lit from the top-left), built in our cabinet oblique:
  `ingot` is a trapezoid bar seen end-on. The trapezoid end is at the bottom-left (ramp[3]), the long top face (ramp[4], ramp[5]
  bevel along its near edge) and the sloped long side (ramp[2]) recede up-right. `dust` is a low lumpy heap: lit
  left slopes with ramp[5] grains, shaded right slopes with darker grains. `plate` is a square sheet lying flat in oblique,
  cols 1..14, with a darker front edge. Recolour them with the §5.3 ramp of the material; never redraw the
  silhouettes per family. Sheet: `sheets/vanilla/material_templates.png`.
- Handheld tools and meters: `handheld_body(cv, ramp)` draws the front-on body (cols 1..10, rows 2..13) and returns the free face
  rect. Accessories (leads, probes, blades) are **2 px 45° strokes** (`diag_stroke`) in the free bottom-right corner, in front of
  the body, with a bright STEEL tip. Two accessories use the crossed-tools pose. Reference: `icons/multimeter.py`.
- Components with no world model (bulbs, batteries, cores, motors, resistors, heating elements): draw the object front-on
  with `cylinder`/`disc`/`box`, following every rule above (`examples/primitives.py`).

## 7. Tiers (owner decision: COLOUR, never thickness alone)

Every tiered item has its **own** icon, and the tier is read from colour. **Where the colour goes is decided centrally,
not by you:** `tier_cue(item_id)` (`kit.decisions`; the manifest's `tier_cue` column wins once it exists):

| cue | what it means | helper | families (from `kit.decisions`) |
|---|---|---|---|
| `body` | the machine body *is* the tier ramp (its in-world texture is green at 50 V and blue at 200 V) | `tier_ramp(t)` as the body ramp | 50V/200V macerator, compressor, magnetizer, plate machine, fuel generator |
| `plate` | the mounting base or backplate | `tier_plate(...)`, or `plate_ramp(t)` in a `solid()` | switches, relays, cable switch, antennas, power sockets, battery chargers, emergency lamps, egg incubator, turret |
| `stub` | the terminals or cable stubs leaving the device | `tier_stub(...)`, jacket ≥ 4 px long | heating elements (`_heating_corp`) |
| `band` | end caps, a collar, or a stripe on a cartridge or accent | `cylinder(..., tier_ramp(t))` caps, or `tier_band` | lead fuses, bulb collars, 50V/200V turbines, active dissipators, energy converters |
| `jacket` | cables | `cable(t)` | the five tier cables and the thermal cables |

Rules:
- The cue is the **only hue** on the device. Everything else is STEEL, CABLE, CAV or the device's own material.
- **Signal** plates use the normal face mapping (top ramp[4], front ramp[3], side ramp[2]) **without the ramp[5]
  bevel**: their near-white bevel made the top read as steel (`PLATE_FACES['signal'] = (4, 3, 2, 4)`). Revision 2
  shifted the front to ramp[2]. That kept it off steel, but it collided with the LV front for deuteranopes (§5.1).
  The top stays close to STEEL[4] (ramp[4] is the contract colour); the front carries the cue. `tier_plate`
  and `plate_ramp` do this for you. Don't paint signal plates by hand.
- **Seen-through backplates** (an open cage or frame in front of the tier plate, e.g. the relays): `tier_backplate(cv,
  rect_or_pixels, tier)` fills the field with the plate front colour. Keep materials that collide with that tier
  (§5.3) off the field: put them on a STEEL or CAV strip (the relay coil sits on a steel yoke).
- **Devices whose model has no plate stand on the tier slab** when their cue is `plate` (ruling, wave 1): the cable
  switches (`lowvoltageswitch` / `highvoltageswitch` models) stand on the same 12 × 4 slab, depth 2, as the relays
  (`families/power_switching.base`), so every sibling carries the tier in the same place.
- `tier_band` paints exactly the rectangle you give it. Keep it **inside** the body outline (one pixel in, `cv.u` at 32).
  End caps that form the silhouette are their own `cylinder`/`box` in `tier_ramp(t)`, so they get their own outline.
- `tier_stub` is 4 px thick (8 at 32), with its own outline and a tinned STEEL tip. Show at least 3 px of jacket.
- `tier_pip` is a **supplement only**, never the only cue (`dont_tier_as_pip`): it disappears at GUI 2.
- Tier names: `normalize_tier` accepts `signal`, `lv`/`50v`/`low`/`low_power`, `mv`/`200v`/`medium`/`medium_power`,
  `hv`/`800v`/`high`/`high_power`/`grid`, `vhv`/`veryhigh`/`3.2kv`/`highgrid`, and `thermal`. It also accepts
  `none`/`neutral`, which mean untiered (`None`; `tier_ramp` then raises: draw in STEEL). `grid` and `highgrid` are the
  utility pole's VoltageLevelColors; their textures are the HV and VHV colours.
- Tier twins: draw once with a parameter (`def relay(tier)` in `families/`), then write one module per id that calls it.
  `swap_ramp(cv, src, dst)` is safe **only between tier ramps** (`python3 -m kit` checks that they share no step with any other ramp).
- Check every tiered family with `contact.py --cvd`: siblings must stay distinct in all four rows.

**Cables** (`cable(tier)`, reference `icons/*_cable.py`): a flat strip with one S-bend from bottom-left to top-right. The lit half
faces top-left (ramp[4]), the shaded half is ramp[3], the outline is ramp[1]/[0], and both cut ends are **tinned (STEEL)**, never
copper (`dont_cable_copper_ends`). Width is 3 px for signal and LV and 4 px for MV/HV/VHV. Width is **never** the cue
(`dont_cable_grey_width`). There is no VHV ribbing by default: at GUI 2 it read as blotches. Thermal cable: `cable('thermal')`. Bare
material wires (copper, iron and tungsten cable) use `strip(cv, MATERIAL, width=2..3, ends=None)`.
**`ground_cable` is not a wire:** in the world it is a placeable plate (groundcable.obj) with the ground symbol printed on
it, so the MATCH rule wins. Draw it as a sixnode TERRACOTTA plate with the symbol (`icons/ground_cable.py`).

## 7b. Variant axes other than tier

Each axis has one fixed cue, used the same way in every family:

| axis | cue | examples |
|---|---|---|
| **material** | the element's §5.3 ramp (`material_of(id)`) | copper/iron/tungsten heating elements (VCOPPER/IRON/TUNGSTEN), carbon vs tungsten filament, iron/tungsten/copper cable |
| **tier** | `tier_cue(id)` (§7) | everything with a 50V/200V or LV..VHV sibling |
| **size** | geometry only: `small_` = the same drawing fitted into `SMALL_BOX` | small heating elements, small bulbs, small solar panel, small dissipators, small/medium/big fuel burner (3 box sizes: SMALL_BOX, MEDIUM_BOX, BODY_BOX) |
| **quality** | one ladder everywhere (`quality(id)`, `QUALITY_TRIM`): 0 = plain, 1 = VCOPPER trim, 2 = GOLD trim (a band, rim or cap of 1–2 px). Two-level families use 0 and 2 | cheap/average/optimal core, cheap/average/fast drill, basic/advanced magnet, cheap/advanced chip, machine block/advanced, motor/advanced, energy meter/advanced, small/improved flashlight, weak/normal 50V charger |
| **model** | its own in-world model (MATCH) | the five `*_oriented_battery` blocks and `single_use_battery`: each has its own model and label colour, so draw that |
| **state** | lamps and bulbs are drawn **lit** (`LIT`); blown items show the break | `blown_lead_fuse` = the same end-on knurled cap as the tiered fuses, with a neutral, scorched window in place of the tier disc (§8) |
| **print** | where two items share one model and nothing else tells them apart, a mark printed on the component body (FONT3, near-black or a dim ink) | the thermistor's printed °C (same ResistorCore as `power_resistor`; ratified); 10 A / 25 A diode ratings |

Two axes on one icon combine: for example, a `small_200v_tungsten_heating_corp` is a TUNGSTEN cylinder in SMALL_BOX with MV stubs.
Thermal: passive dissipators and thermal probes use THERMAL (with a lit rim on dark backgrounds). `200v_active_thermal_dissipator`
and `small_active_thermal_dissipator` keep THERMAL fins plus an MV or LV `band` (their in-world fans are tier-tinted).

## 8. Family conventions

- **Chips: `vanilla_size = 32`, decided centrally.** Every chip icon is a single call, `chip_icon('<id>')`.
  The faces live in the frozen `CHIP_FACES` table (12 logic, 9 analog). Each is transcribed from `logicgates.png`/`analogchips.png`
  as a half-resolution bitmap drawn at 2× (**one stroke weight, 2 px, for the whole family**) and placed so that no pixel is on,
  or 4-adjacent to, a mark or the output square. Don't draw chip faces yourself. If a face looks wrong, request a table change.
  `chip(face, ...)` and `chip_body(...)` exist for chip-shaped devices outside the table, with the same placement check.
- **Chip faces (revision 3).** The free face is cols 8..25 × rows 10..26 at 32 px (placement still aims at the
  revision-2 centre, so the old faces did not move). A face that needs 1 px letter spacing to fit is a **full-res**
  bitmap (`scale=1`) drawn with 2 px strokes, so the stroke weight is still the same: VCA, PID and LPF. Key `a` is the
  dimmer second ink (PAL's A, the S&H hold tick). Mark shapes follow the textures: `mark_style={'b': 'caret'}` is the
  clock caret (D flip-flop, sample & hold), `{'l': 'gt'}` the clock '>' (JK flip-flop), and `mark_at` moves a mark (VCA's
  input mark sits high-left). Amplifier and S&H share the texture's shallow (1:2) buffer triangle.
- **Machines (full block):** front 10×10 plus depth 4, body = tier ramp when `tier_cue` is `body`, otherwise STEEL. One
  identifying feature. Reference: `families/macerator.py`.
- **Multiblocks** (`auto_miner`, `experimental_transporter`): **16 px** like every other machine (no 32 px exception).
  Draw a simplified likeness of the multiblock's most recognisable part (ruling, wave 1).
- **Hidden blocks with no model** (`power_capacitor_block`, `power_inductor_block` and other `*_block` variants): the
  component inside a full-block STEEL cabinet with an open front, **front 10 × 11** (rows 4..14) plus depth 4, so the
  component fits the opening (accepted fallback; `families/power_passive.cabinet`).
- **Sixnodes** (wall and floor parts): front 12 wide, depth 2. Draw the face that carries identity (dial, screen, lamp) front-on.
  Wall or floor orientation is not shown. Where the item render poses the model (an open door, a lit LED), draw that
  pose: `signal_processor` is the plc cabinet with its door open as `drawItem` renders it. The door is drawn as a
  foreshortened trapezoid, a ratified deviation from strict cabinet oblique, because the parallelogram read as a grey slab.
- **Handheld tools and meters:** §6, the handheld template.
- **Materials:** `ingot`, `dust` and `plate` (the kit's own silhouettes, §6) with the §5.3 ramp. `copper_dust`, `copper_plate` and `copper_cable` are vanilla copper
  (VCOPPER), since ELN's own copper ingot no longer exists. Ores (`lead_ore`, `tungsten_ore`, `cinnabar_ore`) are block-model
  items with no flat icon and are **out of scope**.
- **Lamps and bulbs:** drawn lit (`LIT` glass with a LIT[5] glint). Tier on the base collar (`band`). Lamp sockets show the socket,
  with the bulb lit where the model has one.
- **Heating elements:** a `cylinder` in the element material, coil grooves as single darker columns, and tier stubs at both ends
  (`examples/primitives.py: heating_element`).
- **Lead fuses (ratified, wave 1):** the model is a D-type fuse cap seen end-on, so the icon is that: a STEEL
  `knurled_disc` (8 teeth) with the FuseType disc behind the window in the tier ramp (`disc(..., tier_ramp(t))`, the
  `band` cue as a large ramp[3] area). `blown_lead_fuse` is the same cap with a neutral, scorched CAV window. Reference:
  `families/power_passive.fuse`. The glass-cartridge drawing (`examples/primitives.fuse`) is only for cartridge fuses
  that have no such model.
- **Brushes** (16 dyes): one shared drawing with the bristle colour from the vanilla dye hues (a family ramp table).
- **Utility poles:** POLE_WOOD (not WOOD), the model's dark creosoted brown.
- **Not redrawn:** `item_creative_tab` keeps the 1.12 `elncreativetab` texture (central decision); it has no module.

## 9. 32 px icons (only where `vanilla_size = 32`)

A 32 px icon is the **same drawing at 2× density**, so it sits next to 16 px icons without looking like another style
(compare `out/50v_macerator.png` with `examples/png/macerator_lv_32.png` at GUI 2):
- Lay out **geometry on the 16 px plan ×2**: body box, depth (8 for a block), chamfers as 2-step diagonals, pins.
- **Outline 2 px** (two rings, same rule). The kit does this through `cv.u`. A 1 px outline reads as a finer "HD" style
  (`dont_32_outline_1px`).
- **Same ramps, same light, same face mapping**, with a colour budget of 24.
- Spend the extra density **only on interior detail that stays readable at GUI 2**: seams, bolts, inner faces seen through
  openings. 1 px features that repeat (teeth, grilles) read as dithering, so draw them 2 px or leave them out.
- Strokes of text and symbols are 2 px (`dont_32_glyph_1x`).

## 10. Do / don't (`examples/do_dont.png`)

| do | don't |
|---|---|
| Outline in the object's own ramp[0]/[1]. | Black or shared-grey outlines (`dont_outline_black`). |
| One material = one `solid()` (`do_one_solid`). | Separate boxes with seams (`dont_seamed_boxes`). |
| Hand-placed pixels or kit primitives; `cylinder()` for anything round. | A downscaled render, soft edges or partial alpha (`dont_resampled`), or cylinders shaded row by row by hand. |
| The tier on the decided cue, over a real area of ramp[3]. | A pip as the only cue (`dont_tier_as_pip`), cable tier by width (`dont_cable_grey_width`). |
| Tinned STEEL cable ends. | Copper ends beside the tier colour (`dont_cable_copper_ends`). |
| 32 px: 2 px outline and 2 px strokes. | A 1 px outline or glyph at 32 (`dont_32_outline_1px`, `dont_32_glyph_1x`). |
| One identifying feature, big and clear; fill the body box on baseline 14. | Every model part shrunk to a pixel; a small device drawn small. |
| Frozen §5.3 ramps for materials. | Your own "lead" or "tungsten" ramp. |
| A likeness of the world model (MATCH). | Schematic symbols on placeables (that is the other set). |
| Check at GUI 2 with `--cvd`, next to your siblings. | Judge only the magnified view. |

## 11. Kit API (artist-facing)

```python
from kit import *
# canvas -----------------------------------------------------------------------------------------
Canvas(size=16)            # .n, .u (1|2); set/get/opaque/clear/rect/hline/vline/pixels(pts, c)
                           # overlay(grid, pal, ox=0, oy=0, scale=1)  ('.'/' ' skip, '_' clears)
                           # paste(other, ox, oy)/recolor({old: new})/region()/colours()/copy()
                           # flip_h() (re-outline afterwards: light must stay top-left)/image()/save(p)
                           # Canvas.from_image(im)
# colour -----------------------------------------------------------------------------------------
h('#rrggbb'); hexs(c); lstar(c); hue(c); to_lch(c); from_lch(L, C, H); mix(a, b, t); T (transparent)
derive_ramp(anchor, at=4) -> list (hand-tune, then freeze as a tuple in families/)
# ramps (tuples): TIER[t] (read-only mapping), TIERS, TIER_HEX, THERMAL, STEEL, CABLE, CAV, CHIP, ACHIP,
#   IRON, GOLD, VCOPPER, COPPER, LEAD, TUNGSTEN, SILICON, ALLOY, FERRITE, CARBON, CINNABAR, RUBBER_BLACK,
#   WOOD, POLE_WOOD, CERAMIC, TERRACOTTA, WHITE_PLASTIC, GLASS, LIT, SOLAR, RUBBER_Y, CAP_BLUE, LCD,
#   LEAD_RED, LEAD_BLK
# colours: MARK_RED, MARK_BLUE, MARK_GREEN, WHITE, GLYPH, GLYPH_A, GLYPH_DIM, GLYPH_A_DIM
# solids -----------------------------------------------------------------------------------------
box(cv, x, y, w, h, d, ramp, outline=True, bevel=True, ow=None, region_out=None) -> pixel set
solid([(name, x, y, w, h, d), ...]) -> zbuf {(x, y): (name, face, k)}
paint_solid(cv, zb, ramp, bevel=True, ow=None, cut=())
faces(zb, name=None, face=None, k=None) -> pixels;  depth(cv, 'block'|'sixnode'|'flat')
outline_region(cv, pixels, dark, light, width=None);  outline(cv, ramp, width=None)
# round and linear -------------------------------------------------------------------------------
cylinder(cv, x, y, length, diameter, ramp, axis='x'|'y', round_ends=True, cap=None) -> pixels; CYL_BANDS
disc(cv, x, y, d, ramp, face=3, rim=True, outline=True) -> pixels
ring(cv, x, y, d, ramp, width=None, outline=True) -> pixels
knurled_disc(cv, x, y, d, ramp, teeth=8, notch=0.4, phase=0.0, outline=True) -> pixels
oblique_ring(cv, x, y, w, dp, ramp, band=None, height=None, floor=None, outline=True) -> pixels
line(cv, p0, p1, c, width=1) -> pixels;  line_pts(p0, p1)
diag_stroke(cv, x, y, n, lit, body, direction=(1, 1))
# tiers ------------------------------------------------------------------------------------------
normalize_tier(t) -> 'signal'|'lv'|'mv'|'hv'|'vhv'|'thermal'|None;  tier_ramp(t);  plate_ramp(t);  PLATE_FACES
tier_backplate(cv, (x, y, w, h) | pixels, tier) -> pixels     # seen-through plate field, plate_ramp(t)[3]
tier_plate(cv, x, y, w, h, d, tier, **box_kw);  tier_band(cv, x, y, w, h, tier, vertical=False)
tier_stub(cv, x, y, length, tier, direction='right'|'left'|'down'|'up', width=None, tip=STEEL)
tier_pip(cv, x, y, tier, size=None);  swap_ramp(cv, src, dst);  tier_variants(fn, tiers=TIERS)
# decisions (manifest fallback) ------------------------------------------------------------------
vanilla_size(id); tier_of(id); tier_cue(id); quality(id); QUALITY_TRIM; is_small(id); SMALL_BOX; MEDIUM_BOX; BODY_BOX
material_of(id) -> palette name
# templates --------------------------------------------------------------------------------------
cable(tier, size=16, width=None, ribbed=None)
strip(cv, ramp, width=3, path=None, ends=STEEL, end_len=0.08, ribbed=False, lit=4, shade=3, bounds=None)
s_path(cv, p0=(2.6, 13.4), p1=(13.4, 2.6), amp=1.4)
ingot(ramp); dust(ramp); plate(ramp);  handheld_body(cv, ramp, x=1, y=2, w=10, h=12) -> face rect
# chips ------------------------------------------------------------------------------------------
chip_icon(item_id)                       # the production chip icon (32 px), from CHIP_FACES
chip(face, size=32, kind='logic'|'analog', marks='tlbo', pins='lbr', pal=None, at=None, ramp=None,
     scale=None, mark_style=None, mark_at=None)     # scale=1: full-res face; mark_style {'b':'caret','l':'gt'}
chip_body(size=32, kind, marks, pins, ramp=None, mark_style=None, mark_at=None) -> (cv, geom)
chip_face(size) -> geom (CHIP_GEOM);  MARK_SHAPES;  mark_pixels(size, marks, mark_style, mark_at)
place_face(face, size, marks, scale=None, mark_style=None, mark_at=None) -> (x, y) | None
face_ok(face, at, size, marks, scale, mark_style, mark_at) -> bool;  text_face('JK') -> half-res rows (FONT3)
# face keys: '#' glyph, 'a' second ink, 'r'/'g'/'b' mark colours, 'W' white
# text -------------------------------------------------------------------------------------------
text(cv, x, y, s, colour, font=FONT5|FONT3, gap=1, scale=1, colours=None, overbar=False) -> width
text_size(s, font, gap, scale); glyph_rows(tok, font); squash(rows, height)
# FONT5 symbols: {GE} {SIGMA} {INT} {SQUARE} {SINE} {SAW} {HYST} {TRI} {CLK}
lint(image, expected=16, skip=())        # what build.py runs; LINT_SKIP = {...} in a module
```

## 12. Open items (owner / orchestrator)

- `manifest/manifest.json` exists and wins over `kit.decisions` (build.py warns when they disagree).
- Resolved in revision 3: the material silhouettes are now the kit's own (owner veto on copying Mojang's masks).
