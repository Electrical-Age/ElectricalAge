"""50v_turbine: heat turbine (thermal difference -> 50V): IEC 'G' ring in the lv hue + steam input mark (STYLE 7.3); tier sibling of 200v_turbine."""
from kit import Glyph
from families.generators import driven_generator

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    driven_generator(g, 'steam')
    return g
