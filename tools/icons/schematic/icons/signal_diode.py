"""signal_diode: plain diode in the signal hue, no mark (STYLE 8.6)."""
from kit import Glyph
from families.power_passive import signal_diode

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    signal_diode(g)
    return g
