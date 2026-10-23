"""opamp: operational amplifier -> triangle in the signal hue with INK - and + inputs (STYLE 9); the signs
sit level with their input leads and clear of the slopes (families.chips.opamp_signed)."""
from kit import *
from families.chips import opamp_signed

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    opamp_signed(g)
    return g
