"""small_solar_panel: photodiode (PV cell in the LV hue, INK incident-light arrows), STYLE 8.6."""
from kit import Glyph
from families.solar_thermal import solar_panel

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    solar_panel(g)
    return g
