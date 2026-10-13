"""streetlight: untiered, all INK; the lamp at the top on a pole down to a full-width floor bar (the floor
counterpart of the flat socket's ceiling bar; STYLE 8.2 family-lead proposal, families.lighting_fixtures)."""
from kit import Glyph
from families.lighting_fixtures import streetlight

TIER = None


def draw():
    g = Glyph(tier=TIER)
    streetlight(g)
    return g
