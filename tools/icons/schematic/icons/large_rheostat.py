"""large_rheostat: full-size INK resistor + adjust arrow (STYLE 9); manifest tier is null."""
from kit import Glyph
from families.power_passive import rheostat

TIER = None


def draw():
    g = Glyph(tier=TIER)
    rheostat(g, large=True)
    return g
