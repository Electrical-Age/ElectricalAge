"""thermal_probe_chip - crafting chip for thermal probes: the family package printed 'T' in the thermal ink (families/parts_modules)."""
from families import parts_modules as F


def draw():
    return F.lettered('T', F.INK_THERMAL)
