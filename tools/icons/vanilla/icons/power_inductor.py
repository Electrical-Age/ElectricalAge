"""power_inductor - sixnode: steel bracket with the standing drum inductor (copper winding
between two black octagonal flanges).  See families/power_passive.py."""
from families.power_passive import inductor


def draw():
    return inductor()
