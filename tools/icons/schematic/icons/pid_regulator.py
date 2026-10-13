"""pid_regulator: PID controller -> INK FONT label PID over the feedback loop in the signal hue (STYLE 9,
regulator())."""
from kit import *

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    regulator(g, 'PID')
    return g
