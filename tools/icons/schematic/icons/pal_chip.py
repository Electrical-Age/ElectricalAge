"""pal_chip: programmable array logic -> crossing grid in the signal hue with INK programmed junction
dots (STYLE 9, matrix())."""
from kit import *

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    matrix(g, g.hue)
    return g
