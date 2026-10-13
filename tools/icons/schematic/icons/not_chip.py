"""not_chip: inverter -> IEEE buffer triangle + inversion bubble in the signal hue, INK leads (STYLE 9)."""
from kit import *

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    gate(g, 'buf', g.hue, invert=True)
    return g
