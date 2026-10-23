"""analog_gauge (Analog vuMeter): analog signal voltmeter with a needle -> meter ring in the signal hue with
an INK pointer inside instead of a letter (families.meters.needle_meter). Not meter(g, 'V'): that is
voltage_probe's glyph (also signal), and the two items must not share one icon."""
from kit import *
from families.meters import needle_meter

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    needle_meter(g)
    return g
