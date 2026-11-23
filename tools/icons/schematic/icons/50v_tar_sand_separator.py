"""50v_tar_sand_separator: process machine, feed arrow into a separating funnel in the lv hue, crude drop out (families/oil_chain.py)."""
from kit import Glyph
from families.oil_chain import tar_sand_separator

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    tar_sand_separator(g)
    return g
