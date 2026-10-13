"""overheating_protection - upgrade, crafted from the thermal probe chip: the package printed 'HP' in the thermal ink (families/parts_modules)."""
from families import parts_modules as F


def draw():
    return F.lettered('HP', F.INK_THERMAL)
