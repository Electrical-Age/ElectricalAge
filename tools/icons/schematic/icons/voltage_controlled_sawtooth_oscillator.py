"""voltage_controlled_sawtooth_oscillator: sawtooth VCO -> the sawtooth in the signal hue with the INK
control-input arrow from the top (STYLE 9, oscillator(controlled=True))."""
from kit import *

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    oscillator(g, 'sawtooth', controlled=True)
    return g
