"""50v_farming_lamp: IEC lamp in the lv hue on RING_M + mark F (farming / grow lamp); tier sibling of 200v_farming_lamp, STYLE 8.1."""
from kit import Glyph
from families.light_bulbs import light_bulb

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    light_bulb(g, 'farming')
    return g
