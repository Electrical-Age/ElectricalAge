"""power_capacitor_block: block mark 'B' + INK capacitor on MID_M (STYLE 8.4)."""
from kit import Glyph
from families.power_passive import block_capacitor

TIER = None


def draw():
    g = Glyph(tier=TIER)
    block_capacitor(g)
    return g
