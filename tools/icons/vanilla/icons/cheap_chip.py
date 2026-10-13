"""cheap_chip - crafting chip, quality 0: the family package with STEEL pins and a plain silicon die (families/parts_modules)."""
from families import parts_modules as F


def draw():
    return F.chip_quality('cheap_chip')
