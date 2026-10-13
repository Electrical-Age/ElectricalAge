"""50v_copper_heating_corp: IEC 16x10 resistor on MID_M in the lv hue + INK 'Cu' mark (STYLE 8.3); tier sibling of 200v_copper_heating_corp."""
from kit import Glyph
from families.heating_elements import heating_element

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    heating_element(g, 'copper')
    return g
