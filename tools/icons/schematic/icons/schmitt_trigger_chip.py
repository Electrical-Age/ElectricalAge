"""schmitt_trigger_chip: Schmitt trigger -> the hysteresis loop itself, large, in the signal hue; its tails are the
input and output (STYLE 9). No triangle: at 32px a loop inside the buf triangle closes into an INK square."""
from kit import *

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    hysteresis(g, 4, 9, 24, 14, g.hue)
    return g
