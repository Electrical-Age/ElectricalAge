"""modern_data_logger: flat-screen signal recorder -> mark M + the recorder ring at RING_M in the signal hue
(STYLE 8.6). 'M' is a literal FONT mark (mark() falls through to FONT for unregistered keys)."""
from kit import *

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    mark(g, 'M')
    trace(g, g.hue, ring=RING_M)
    return g
