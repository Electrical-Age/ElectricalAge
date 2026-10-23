"""power_inductor_block: block mark 'B' + INK inductor on MID_M (STYLE 8.4)."""
from kit import Glyph
from families.power_passive import block_inductor

TIER = None


def draw():
    g = Glyph(tier=TIER)
    block_inductor(g)
    return g
