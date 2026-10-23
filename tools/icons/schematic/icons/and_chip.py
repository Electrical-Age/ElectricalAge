"""and_chip: placeable 3-input AND gate (signal tier) -> IEEE distinctive-shape AND, body in the signal
hue, three INK input leads (STYLE 9 gate(); 3 inputs as in LogicGate.kt, via families.chips.gate3)."""
from kit import *
from families.chips import gate3

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    gate3(g, 'and')
    return g
