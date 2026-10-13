"""amplifier: gain amplifier (one input, AnalogChips.kt) -> amplifier triangle in the signal hue with a single
INK input on the centre line, no signs (families.chips.amp). One input vs opamp's two signed inputs is the
structural difference. STYLE 9: opamp(g, g.hue, inputs=1)."""
from kit import *
from families.chips import amp

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    amp(g)
    return g
