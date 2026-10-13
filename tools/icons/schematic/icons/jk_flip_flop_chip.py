"""jk_flip_flop_chip: JK flip-flop -> rising clock edge in the signal hue + mark JK (STYLE 9)."""
from kit import *

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    mark(g, 'jk')
    edge(g, 4, 27, 15, 25, g.hue)
    return g
