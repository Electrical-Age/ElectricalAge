"""50v_electric_heater: the heater resistor on MID_M in the lv hue under INK heat waves at HEAT_TOP (families/steam_plant); tier sibling of the other electric heater."""
from kit import Glyph
from families.steam_plant import electric_heater

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    electric_heater(g)
    return g
