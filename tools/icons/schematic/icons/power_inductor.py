"""power_inductor: the IEC inductor (three humps), all INK (untiered)."""
from kit import Glyph, inductor

TIER = None


def draw():
    g = Glyph(tier=TIER)
    inductor(g)
    return g
