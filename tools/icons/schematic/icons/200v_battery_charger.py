"""200v_battery_charger: IEC cell in the mv hue + bolt mark (charging), STYLE 8.4; tier sibling of 50v_battery_charger."""
from kit import Glyph
from families.batteries import charger

TIER = 'mv'


def draw():
    g = Glyph(tier=TIER)
    charger(g)
    return g
