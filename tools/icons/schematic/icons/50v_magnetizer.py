"""50v_magnetizer: process machine (STYLE 7.2): the coil (kit inductor humps) in the LV hue on an INK magnetic core bar (families.machines.magnetizer). 200v twin in MV."""
from kit import Glyph
from families.machines import magnetizer

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    magnetizer(g)
    return g
