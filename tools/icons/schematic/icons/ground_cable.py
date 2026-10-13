"""ground_cable: IEC earth (stem + three bars), all INK (untiered; STYLE section 6)."""
from kit import *

TIER = None


def draw():
    g = Glyph(tier=TIER)
    ground(g, top=5)
    return g
