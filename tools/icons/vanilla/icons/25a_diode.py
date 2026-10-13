"""25a_diode - sixnode: as 10a_diode, rating "25" printed on the can.  See families/power_passive.py."""
from families.power_passive import diode


def draw():
    return diode("25")
