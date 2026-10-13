"""signal_button: push button: make contact in the signal hue + INK actuator (STYLE 8.6)."""
from kit import Glyph
from families.signal_io import signal_button

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    signal_button(g)
    return g
