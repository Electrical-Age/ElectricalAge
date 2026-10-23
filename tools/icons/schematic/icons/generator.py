"""generator: the shaft-driven generator (3200V, VoltageLevelColor VHV): the plain IEC 'G' machine at
the full RING, ring in the vhv hue, no mark: it is driven by the shaft network, not a fuel (STYLE 7.3)."""
from kit import Glyph
from families.generators import shaft_generator

TIER = 'vhv'


def draw():
    g = Glyph(tier=TIER)
    shaft_generator(g)
    return g
