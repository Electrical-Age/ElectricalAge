"""redstone_to_voltage_converter: hue staircase (quantised transfer characteristic) -> INK lead with the arrow out (family proposal, families.signal_io.converter)."""
from kit import Glyph
from families.signal_io import converter

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    converter(g, to_redstone=False)
    return g
