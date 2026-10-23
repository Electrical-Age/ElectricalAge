"""Schematic glyph 32px kit (read-only for family artists; request changes instead).

    from kit import *            # Glyph, palette, symbols, conventions, constants
    TIER = 'lv'                  # the manifest's tier, canonical spelling; None if untiered
    def draw():
        g = Glyph(tier=TIER)     # g.hue = the tier colour
        lamp(g, g.hue)           # defining element in g.hue, everything else INK
        return g

Modules
    palette      colours, tier ramps, tier_key / hue_for, CVD self-check (python3 -m kit.palette --cvd)
    canvas       Glyph (drawing + check()), RuleBreak, layout constants N/CORE/MID/S/MIN_HUE_PX/MIN_GAP
    font         FONT (2px, cap height 7) and MICRO (1px, 3x5)
    symbols      IEC / IEEE primitives, layouts (RING, RING_M, MID_M, MARK_BOX), marks and pictograms
    conventions  the binding drawing of each multi-variant family (bulb, heating_corp, battery_block ...)
    catalog      python3 -m kit.catalog ref/catalog.png [conventions]   (reference sheets, all checked)
    dodont       python3 -m kit.dodont ref/dodont.png
"""
from .palette import (HALO, INK, THERMAL, THERMAL_RAMP, TIER_RAMPS, TIERS, STROKE_COLOURS,
                      tier_key, tier_color, tier_ramp, hue_for, hexof, normalize_tier)
from .canvas import Glyph, RuleBreak, N, CORE, MID, S, MIN_HUE_PX, MIN_GAP, bezier, arc_points
from .font import FONT, MICRO, LOWER_DROP, text_width
from .symbols import *          # noqa: F401,F403
from .conventions import *      # noqa: F401,F403
from .palette import TIER as TIER_MAIN   # {tier: main colour}; NOT exported as TIER (icons define TIER)
from . import symbols, conventions
