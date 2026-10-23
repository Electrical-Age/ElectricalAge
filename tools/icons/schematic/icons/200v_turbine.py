"""200v_turbine: heat turbine (thermal difference -> 200V): IEC 'G' ring in the mv hue + steam input mark (STYLE 7.3); tier sibling of 50v_turbine."""
from kit import Glyph
from families.generators import driven_generator

TIER = 'mv'


def draw():
    g = Glyph(tier=TIER)
    driven_generator(g, 'steam')
    return g
