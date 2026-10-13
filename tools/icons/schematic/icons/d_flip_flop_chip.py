"""d_flip_flop_chip: D flip-flop -> rising clock edge in the signal hue + mark D (STYLE 9)."""
from kit import *

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    mark(g, 'd')
    edge(g, 4, 27, 15, 25, g.hue)
    return g
