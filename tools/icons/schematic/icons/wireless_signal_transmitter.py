"""wireless_signal_transmitter: antenna in the signal hue + INK radiation arcs + the INK signal-antenna cue (signal-cable lead to the left edge; families/wireless: tells it from the power transmitters)."""
from kit import Glyph
from families.wireless import signal_antenna

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    signal_antenna(g, 'tx')
    return g
