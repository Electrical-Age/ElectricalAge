"""data_logger: logs and plots a signal over time -> recorder ring in the signal hue with the INK
recorded trace (STYLE 8.6, trace(g, g.hue))."""
from kit import *

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    trace(g, g.hue)
    return g
