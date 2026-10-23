"""signal_relay: signal relay -> kit relay: make contact in the signal hue, INK coil + dashed link (STYLE 8.6)."""
from kit import Glyph
from families.power_switching import power_relay

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    power_relay(g)
    return g
