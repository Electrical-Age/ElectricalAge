"""2x3_solar_panel: mark '2' + the diode on MID_M in the MV hue, STYLE 8.6."""
from kit import Glyph
from families.solar_thermal import solar_panel

TIER = 'mv'


def draw():
    g = Glyph(tier=TIER)
    solar_panel(g, large=True)
    return g
