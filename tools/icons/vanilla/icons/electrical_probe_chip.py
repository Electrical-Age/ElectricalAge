"""electrical_probe_chip - crafting chip for electrical sensors: the family package printed 'V' in the electrical ink (families/parts_modules)."""
from families import parts_modules as F


def draw():
    return F.lettered('V', F.INK_ELECTRIC)
