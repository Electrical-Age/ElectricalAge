"""rheostat: small INK resistor + adjust arrow (STYLE 9)."""
from kit import Glyph
from families.power_passive import rheostat

TIER = None


def draw():
    g = Glyph(tier=TIER)
    rheostat(g)
    return g
