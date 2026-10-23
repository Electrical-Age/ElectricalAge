"""nuclear_alarm: '!' mark + IEC bell in the signal hue lowered by 4 (STYLE 8.6)."""
from kit import Glyph
from families.signal_io import alarm

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    alarm(g, nuclear=True)
    return g
