"""voltage_controlled_amplifier: VCA (signal + control input, AnalogChips.kt) -> amplifier triangle in the signal hue,
single INK signal input on the centre line and the INK control input as a lead from the top into the upper
slope, where the VCOs put their control arrow (STYLE 9: opamp(g, g.hue, inputs=1, control=True)). No adjust_arrow:
it fused with the input lead and read as an extra input."""
from kit import *
from families.chips import amp

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    amp(g, control=True)
    return g
