"""50v_economic_light_bulb: IEC lamp in the lv hue on RING_M + mark E (economic = fluorescent); tier sibling of 200v_economic_light_bulb, STYLE 8.1."""
from kit import Glyph
from families.light_bulbs import light_bulb

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    light_bulb(g, 'economic')
    return g
