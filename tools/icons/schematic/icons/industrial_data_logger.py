"""industrial_data_logger: panel signal recorder -> mark I + the recorder ring at RING_M in the signal hue
(STYLE 8.6). 'I' is a literal FONT mark (mark() falls through to FONT for unregistered keys)."""
from kit import *

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    mark(g, 'I')
    trace(g, g.hue, ring=RING_M)
    return g
