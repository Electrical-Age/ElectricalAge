"""joint: untiered shaft part: the double-rail shaft (families.machines shaft convention) running into a bearing collar ring from both sides."""
from kit import Glyph
from families.machines import joint

TIER = None


def draw():
    g = Glyph(tier=TIER)
    joint(g)
    return g
