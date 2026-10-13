"""200v_compressor: process machine (STYLE 7.2): two plates in the MV hue closing on an INK workpiece, INK arrowheads pressing in from top and bottom (families.machines.compressor). 50v_compressor is the same program in LV."""
from kit import Glyph
from families.machines import compressor

TIER = 'mv'


def draw():
    g = Glyph(tier=TIER)
    compressor(g)
    return g
