"""dc_dc_converter: IEC single-line transformer (two overlapping rings), all INK: it converts between
variable tiers, so it is untiered (STYLE 6)."""
from kit import Glyph, transformer

TIER = None


def draw():
    g = Glyph(tier=TIER)
    transformer(g)
    return g
