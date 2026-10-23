"""low_voltage_relay: lv relay -> kit relay: make contact in the lv hue, INK coil + dashed link (STYLE 8.6)."""
from kit import Glyph
from families.power_switching import power_relay

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    power_relay(g)
    return g
