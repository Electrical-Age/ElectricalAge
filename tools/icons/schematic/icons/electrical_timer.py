"""electrical_timer: clock mark + mark-layout make contact in the signal hue (STYLE 8.5)."""
from kit import Glyph
from families.signal_io import timer

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    timer(g)
    return g
