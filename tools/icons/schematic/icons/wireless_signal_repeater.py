"""wireless_signal_repeater: kit repeater() in the signal hue, one INK arrow in and one out (STYLE 7.5, conventions)."""
from kit import Glyph
from families.wireless import signal_repeater

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    signal_repeater(g)
    return g
