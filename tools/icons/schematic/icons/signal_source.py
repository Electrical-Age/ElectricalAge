"""signal_source: IEC voltage source in the signal hue (STYLE 7.3)."""
from kit import Glyph
from families.signal_io import signal_source

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    signal_source(g)
    return g
