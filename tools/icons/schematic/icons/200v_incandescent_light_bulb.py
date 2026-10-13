"""200v_incandescent_light_bulb: plain IEC lamp (ring + X) in the mv hue, no mark; tier sibling of 50v_incandescent_light_bulb, STYLE 8.1."""
from kit import Glyph
from families.light_bulbs import light_bulb

TIER = 'mv'


def draw():
    g = Glyph(tier=TIER)
    light_bulb(g)
    return g
