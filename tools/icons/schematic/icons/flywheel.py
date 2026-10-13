"""flywheel: untiered shaft part: the double-rail shaft through the heavy disc seen edge-on (a tall empty rectangle, as in kinematic diagrams)."""
from kit import Glyph
from families.machines import flywheel

TIER = None


def draw():
    g = Glyph(tier=TIER)
    flywheel(g)
    return g
