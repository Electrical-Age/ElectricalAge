"""xnor_chip: 3-input XNOR gate -> IEEE XOR + inversion bubble in the signal hue, three INK input leads
(STYLE 9 gate(); 3 inputs via families.chips.gate3); the bubble is the structural difference from xor_chip."""
from kit import *
from families.chips import gate3

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    gate3(g, 'xor', invert=True)
    return g
