"""overvoltage_protection - upgrade, crafted from the electrical probe chip: the package printed 'VP' in the electrical ink (families/parts_modules)."""
from families import parts_modules as F


def draw():
    return F.lettered('VP', F.INK_ELECTRIC)
