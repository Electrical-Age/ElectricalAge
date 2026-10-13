"""capacity_oriented_battery: IEC cell in the lv hue + mark 'C' (capacity oriented), STYLE 8.4."""
from kit import Glyph
from families.batteries import stationary_battery

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    stationary_battery(g, 'capacity')
    return g
