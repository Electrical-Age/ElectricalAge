"""medium_voltage_relay: mv relay -> kit relay: make contact in the mv hue, INK coil + dashed link (STYLE 8.6)."""
from kit import Glyph
from families.power_switching import power_relay

TIER = 'mv'


def draw():
    g = Glyph(tier=TIER)
    power_relay(g)
    return g
