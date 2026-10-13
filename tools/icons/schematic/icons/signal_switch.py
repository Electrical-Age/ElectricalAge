"""signal_switch: gate-source toggle: plain make contact in the signal hue (STYLE 8.6)."""
from kit import Glyph
from families.signal_io import signal_switch

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    signal_switch(g)
    return g
