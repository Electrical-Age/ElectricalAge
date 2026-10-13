"""thermistor: mark 'temperature' (t°) + INK resistor on MID_M + IEC non-linear slash with foot (NTC thermistor,
IEC 60617 -t°; STYLE 8.6)."""
from kit import Glyph
from families.power_passive import thermistor

TIER = None


def draw():
    g = Glyph(tier=TIER)
    thermistor(g)
    return g
