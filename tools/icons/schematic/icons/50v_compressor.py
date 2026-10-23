"""50v_compressor: process machine (STYLE 7.2): two plates in the LV hue closing on an INK workpiece, INK arrowheads pressing in from top and bottom (families.machines.compressor). 200v_compressor is the same program in MV."""
from kit import Glyph
from families.machines import compressor

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    compressor(g)
    return g
