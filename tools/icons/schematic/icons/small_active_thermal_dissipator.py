"""small_active_thermal_dissipator: short THERMAL heat-sink comb on top, kept apart (3 clear rows) from the motor ring 'M' in the
LV hue (50V fan), INK leads; STYLE 5.1 two-hue case (families/solar_thermal)."""
from kit import Glyph
from families.solar_thermal import active_dissipator

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    active_dissipator(g)
    return g
