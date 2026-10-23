"""nand_chip: 3-input NAND gate -> IEEE AND body + inversion bubble in the signal hue, three INK input
leads (STYLE 9 gate(); 3 inputs via families.chips.gate3)."""
from kit import *
from families.chips import gate3

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    gate3(g, 'and', invert=True)
    return g
