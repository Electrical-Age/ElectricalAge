"""energy_meter: counts the energy passed through -> meter ring with the INK quantity letter E (STYLE 8.5,
meter(g, 'E', ...)). Untiered (manifest tier null, Neutral colour), so the whole glyph is INK; no signal
meter uses E, so it differs from the signal meters in structure (STYLE 5.4)."""
from kit import *

TIER = None


def draw():
    g = Glyph(tier=TIER)
    meter(g, 'E', g.hue)
    return g
