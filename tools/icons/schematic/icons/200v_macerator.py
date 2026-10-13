"""200v_macerator: process machine (STYLE 7.2), the 50v_macerator reference program with TIER = 'mv':
the crushing rollers (kit.conventions.macerator_rollers) in the MV hue, feed arrowhead, grit."""
from kit import Glyph, macerator_rollers

TIER = 'mv'


def draw():
    g = Glyph(tier=TIER)
    macerator_rollers(g)
    return g
