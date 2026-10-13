"""joint_hub: untiered shaft part: double-rail shafts from all four sides meeting at the bearing collar ring; differs from the electrical hub (single wires, filled node) in structure."""
from kit import Glyph
from families.machines import joint_hub

TIER = None


def draw():
    g = Glyph(tier=TIER)
    joint_hub(g)
    return g
