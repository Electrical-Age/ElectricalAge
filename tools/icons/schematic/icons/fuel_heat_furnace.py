"""fuel_heat_furnace: two INK heat waves + drop mark (liquid fuel) + THERMAL heater on MID_M, STYLE 8.6."""
from kit import Glyph
from families.solar_thermal import heat_furnace

TIER = 'thermal'


def draw():
    g = Glyph(tier=TIER)
    heat_furnace(g, 'fuel')
    return g
