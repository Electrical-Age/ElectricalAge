"""utility_pole_w_dc_dc_converter: the utility pole glyph (hv line + D=8 ring node) raised to the top, with the INK transformer in dc_dc_converter's kit proportions hanging directly from the node."""
from kit import *
from families.cables import utility_pole
TIER = 'hv'


def draw():
    g = Glyph(tier=TIER)
    utility_pole(g, converter=True)
    return g
