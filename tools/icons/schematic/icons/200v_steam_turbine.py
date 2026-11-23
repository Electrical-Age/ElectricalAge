"""200v_steam_turbine: the 230V Steam Turbine (a steam-driven generator): kit 'steam' mark + 'G' ring at RING_M in the mv hue (STYLE 7.3); tier sibling of 50v_steam_turbine."""
from kit import Glyph
from families.steam_turbines import steam_generator

TIER = 'mv'


def draw():
    g = Glyph(tier=TIER)
    steam_generator(g)
    return g
