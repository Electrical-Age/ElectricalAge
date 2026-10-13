"""wireless_signal_receiver: antenna in the signal hue + two INK arrows in + the INK signal-antenna cue (signal-cable lead to the left edge; families/wireless: tells it from the power receivers)."""
from kit import Glyph
from families.wireless import signal_antenna

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    signal_antenna(g, 'rx')
    return g
