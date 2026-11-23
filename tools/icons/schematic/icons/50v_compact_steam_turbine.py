"""50v_compact_steam_turbine: the small variant of the steam generator: kit 'steam' mark + 'G' ring at RING_M_SMALL in the lv hue (STYLE 8: small = the small symbol size)."""
from kit import Glyph
from families.steam_turbines import steam_generator

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    steam_generator(g, small=True)
    return g
