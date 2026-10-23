"""voltage_controlled_sine_oscillator: sine VCO -> the sine wave in the signal hue with the INK control-input
arrow from the top (STYLE 9, oscillator(controlled=True))."""
from kit import *

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    oscillator(g, 'sine', controlled=True)
    return g
