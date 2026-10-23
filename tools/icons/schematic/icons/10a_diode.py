"""10a_diode: mark '10' + INK diode on MID_M (STYLE 8.6)."""
from kit import Glyph
from families.power_passive import rated_diode

TIER = None


def draw():
    g = Glyph(tier=TIER)
    rated_diode(g, '10')
    return g
