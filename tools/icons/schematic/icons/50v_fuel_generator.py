"""50v_fuel_generator: IEC 'G' ring in the lv hue + fuel-drop input mark (STYLE 7.3); tier sibling of 200v_fuel_generator."""
from kit import Glyph
from families.generators import driven_generator

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    driven_generator(g, 'fuel')
    return g
