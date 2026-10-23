"""2x3_rotating_solar_panel: sun mark (follows the sun) + the diode on MID_M in the MV hue;
differs from the small rotating panel by tier colour only (STYLE 8.6, 'adds nothing else')."""
from kit import Glyph
from families.solar_thermal import rotating_panel

TIER = 'mv'


def draw():
    g = Glyph(tier=TIER)
    rotating_panel(g)
    return g
