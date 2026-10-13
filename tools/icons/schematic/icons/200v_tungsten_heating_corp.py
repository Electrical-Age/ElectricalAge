"""200v_tungsten_heating_corp: IEC 16x10 resistor on MID_M in the mv hue + INK 'W' mark (STYLE 8.3); tier sibling of 50v_tungsten_heating_corp."""
from kit import Glyph
from families.heating_elements import heating_element

TIER = 'mv'


def draw():
    g = Glyph(tier=TIER)
    heating_element(g, 'tungsten')
    return g
