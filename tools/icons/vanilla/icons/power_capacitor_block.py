"""power_capacitor_block - hidden full-block capacitor (invisible in world, no model).  Drawn as
the power capacitor's blue can in a full-block steel cabinet: the size axis shown by geometry only.
See families/power_passive.py."""
from families.power_passive import capacitor_block


def draw():
    return capacitor_block()
