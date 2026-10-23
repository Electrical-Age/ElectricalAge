"""small_50v_incandescent_light_bulb: plain IEC lamp in the lv hue on the small ring (RING_SMALL); size sibling of 50v_incandescent_light_bulb, STYLE 8.1."""
from kit import Glyph
from families.light_bulbs import light_bulb

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    light_bulb(g, small=True)
    return g
