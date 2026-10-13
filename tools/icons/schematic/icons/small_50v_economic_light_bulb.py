"""small_50v_economic_light_bulb: IEC lamp in the lv hue on the small ring (RING_M_SMALL) + mark E; size sibling of 50v_economic_light_bulb, STYLE 8.1."""
from kit import Glyph
from families.light_bulbs import light_bulb

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    light_bulb(g, 'economic', small=True)
    return g
