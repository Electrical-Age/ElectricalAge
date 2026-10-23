"""50v_incandescent_light_bulb: plain IEC lamp (ring + X) in the lv hue, no mark; tier sibling of 200v_incandescent_light_bulb, size sibling of small_50v_incandescent_light_bulb, STYLE 8.1."""
from kit import Glyph
from families.light_bulbs import light_bulb

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    light_bulb(g)
    return g
