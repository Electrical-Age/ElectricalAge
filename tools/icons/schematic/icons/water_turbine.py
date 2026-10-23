"""water_turbine: IEC 'G' ring in the lv hue + water input mark (STYLE 7.3)."""
from kit import Glyph
from families.generators import driven_generator

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    driven_generator(g, 'water')
    return g
