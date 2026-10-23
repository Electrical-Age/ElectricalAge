"""oscillator_chip: voltage-controlled square-wave oscillator (the input sets the frequency, LogicGate.kt) ->
the square wave in the signal hue with the INK control-input arrow from the top, like its two VCO siblings
(STYLE 9, oscillator(controlled=True))."""
from kit import *

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    oscillator(g, 'square', controlled=True)
    return g
