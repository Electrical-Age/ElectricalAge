"""weak_50v_battery_charger: the 50V charger with the small cell (STYLE 8.4: 'weak' = small symbol, never a letter), lv hue + bolt mark."""
from kit import Glyph
from families.batteries import charger

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    charger(g, weak=True)
    return g
