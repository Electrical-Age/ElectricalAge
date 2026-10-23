"""voltage_oriented_battery: IEC cell in the mv hue (200V) + mark 'V' (voltage oriented), STYLE 8.4."""
from kit import Glyph
from families.batteries import stationary_battery

TIER = 'mv'


def draw():
    g = Glyph(tier=TIER)
    stationary_battery(g, 'voltage')
    return g
