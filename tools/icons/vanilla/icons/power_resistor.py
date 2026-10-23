"""power_resistor - sixnode: steel bracket holding the amber ceramic power resistor with its two
brass terminal tabs standing on the top face.  See families/power_passive.py."""
from families.power_passive import resistor


def draw():
    return resistor("plain")
