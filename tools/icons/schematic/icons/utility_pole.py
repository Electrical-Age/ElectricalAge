"""utility_pole: the overhead line in the hv hue with the pole as a small plan-view ring node (D=8, shared with the converter pole) threaded on it (no side-view T pole)."""
from kit import *
from families.cables import utility_pole
TIER = 'hv'


def draw():
    g = Glyph(tier=TIER)
    utility_pole(g)
    return g
