"""hub: four INK wires meeting at a 6x6 contact node (STYLE section 11: hub = wires meeting at a junction)."""
from kit import *
from families.cables import hub
TIER = None


def draw():
    g = Glyph(tier=TIER)
    hub(g)
    return g
