"""configurable_summing_unit: weighted sum of inputs -> summing point: ring in the signal hue with an INK +,
inputs left and top, output right (STYLE 9, summing())."""
from kit import *

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    summing(g, g.hue)
    return g
