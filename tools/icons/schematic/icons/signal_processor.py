"""signal_processor: programmable math function of up to 3 signal inputs -> function ring in the signal hue
with an INK FONT f (STYLE 9, ring_letter) and three INK input leads on the left, one output on the
right (families.chips.function_block). A ring, not a box: STYLE 11 bans an outlined box round a label."""
from kit import *
from families.chips import function_block

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    function_block(g)
    return g
