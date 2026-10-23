"""copper_thermal_cable: the routed conductor in THERMAL grey with INK heat waves above it (STYLE section 6, kit thermal_cable)."""
from kit import *

TIER = 'thermal'


def draw():
    g = Glyph(tier=TIER)
    thermal_cable(g, g.hue)
    return g
