"""cost_oriented_battery: IEC cell in the lv hue + mark '$' (cost oriented), STYLE 8.4."""
from kit import Glyph
from families.batteries import stationary_battery

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    stationary_battery(g, 'cost')
    return g
