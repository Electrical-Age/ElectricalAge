"""single_use_battery: IEC cell in the lv hue + mark '1' (single use, not rechargeable), STYLE 8.4."""
from kit import Glyph
from families.batteries import stationary_battery

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    stationary_battery(g, 'single_use')
    return g
