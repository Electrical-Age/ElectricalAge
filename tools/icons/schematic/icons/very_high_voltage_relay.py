"""very_high_voltage_relay: vhv relay -> kit relay: make contact in the vhv hue, INK coil + dashed link (STYLE 8.6)."""
from kit import Glyph
from families.power_switching import power_relay

TIER = 'vhv'


def draw():
    g = Glyph(tier=TIER)
    power_relay(g)
    return g
