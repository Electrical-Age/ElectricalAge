"""Vanilla-native icon kit for Electrical Age (read-only for family artists).

Import everything with ``from kit import *`` inside ``icons/<item_id>.py``; build.py puts the
set directory on sys.path, so ``kit`` and ``families.<family>`` are both importable.

Modules:
    color      hex/L*/LCh maths, derive_ramp, L_FLOOR, T (transparent)
    palette    every shared ramp, frozen tuples: TIER, THERMAL, neutrals (STEEL, CABLE, CAV, ...),
               vanilla metals (IRON, GOLD, VCOPPER), ELN materials (LEAD, TUNGSTEN, ...),
               GLASS, LIT, SOLAR, WOOD, POLE_WOOD, CERAMIC, TERRACOTTA, WHITE_PLASTIC,
               part ramps
    canvas     Canvas(16|32): set/get/rect/hline/vline/overlay/paste/recolor/flip_h/image
    oblique    box, solid, paint_solid, faces, outline_region, outline, depth
    shapes     cylinder, disc, ring, knurled_disc, oblique_ring, line, line_pts, CYL_BANDS
    tier       normalize_tier, tier_ramp, plate_ramp, swap_ramp, tier_plate, tier_backplate,
               tier_band, tier_stub, tier_pip
    cable      cable(tier), strip(...), s_path(...)
    font       FONT5 (5x7), FONT3 (3x5), text(), text_size(), glyph_rows(), squash()
    chip       chip_icon(item_id), CHIP_FACES, chip(face), chip_body, place_face, mark_pixels,
               face_ok, CHIP_GEOM, MARK_SHAPES
    templates  ingot, dust, plate (the kit's own silhouettes), handheld_body, diag_stroke
    decisions  vanilla_size, tier_of, tier_cue, quality/QUALITY_TRIM, is_small/SMALL_BOX, material_of
    check      lint(image) - the rules build.py enforces
    cvd        colour-vision simulation and the tier-separation report (python3 -m kit --cvd)
See STYLE.md for the rules these implement.
"""
from .color import T, L_FLOOR, h, hexs, lstar, hue, mix, derive_ramp, ramp_report, to_lch, from_lch
from .palette import *            # noqa: F401,F403
from .canvas import Canvas
from .oblique import box, solid, paint_solid, faces, outline_region, outline, depth, DEPTHS
from .shapes import cylinder, disc, ring, knurled_disc, oblique_ring, line, line_pts, CYL_BANDS
from .tier import (normalize_tier, tier_ramp, plate_ramp, swap_ramp, tier_variants, tier_plate,
                   tier_backplate, tier_band, tier_stub, tier_pip, PLATE_FACES)
from .cable import cable, strip, s_path, CABLE_SPEC
from .font import FONT5, FONT3, text, text_size, glyph_rows, squash
from .chip import (chip, chip_icon, chip_body, chip_face, place_face, text_face, mark_pixels, face_ok,
                   CHIP_FACES, CHIP_GEOM, MARK_SHAPES)
from .templates import ingot, dust, plate, handheld_body, diag_stroke, INGOT, DUST
from .decisions import (vanilla_size, tier_of, tier_cue, quality, QUALITY_TRIM, is_small, SMALL_BOX,
                        MEDIUM_BOX, BODY_BOX, material_of)
from .check import lint
