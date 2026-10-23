"""power_resistor: the plain IEC resistor, all INK (untiered; a hued plain resistor is reserved for heating corps)."""
from kit import Glyph, resistor

TIER = None


def draw():
    g = Glyph(tier=TIER)
    resistor(g)
    return g
