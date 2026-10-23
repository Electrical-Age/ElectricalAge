"""voltage_to_redstone_converter: INK lead with the arrow in -> hue staircase (quantised transfer characteristic) (family proposal, families.signal_io.converter)."""
from kit import Glyph
from families.signal_io import converter

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    converter(g, to_redstone=True)
    return g
