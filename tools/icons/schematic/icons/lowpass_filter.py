"""lowpass_filter: first-order low-pass -> Bode magnitude curve (flat, then rolling off) in the signal hue on
INK axes (STYLE 9, response())."""
from kit import *

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    response(g, 'lowpass', g.hue)
    return g
