"""200v_magnetizer: process machine (STYLE 7.2): the coil (kit inductor humps) in the MV hue on an INK magnetic core bar (families.machines.magnetizer). 50v twin in LV."""
from kit import Glyph
from families.machines import magnetizer

TIER = 'mv'


def draw():
    g = Glyph(tier=TIER)
    magnetizer(g)
    return g
