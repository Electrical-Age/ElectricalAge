"""standard_alarm: IEC bell in the signal hue (STYLE 8.6)."""
from kit import Glyph
from families.signal_io import alarm

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    alarm(g)
    return g
