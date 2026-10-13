"""thermistor - sixnode: the power-resistor model (identical in world); to tell it apart the
ceramic body is printed with a temperature mark ("°C": a one-pixel degree and FONT3 C in near-black ink;
the mark awaits owner ratification).  See families/power_passive.py."""
from families.power_passive import resistor


def draw():
    return resistor("thermistor")
