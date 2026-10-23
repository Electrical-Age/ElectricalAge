"""sample_and_hold: analog sample-and-hold -> series sampling switch + hold capacitor in the signal hue
(STYLE 9, sample_hold())."""
from kit import *

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    sample_hold(g)
    return g
