"""electrical_motor: the IEC motor symbol, ring + INK 'M' at the full RING with leads both sides
(STYLE 7.3, machine(g, 'M', g.hue)). Untiered (manifest tier null), so all INK."""
from kit import Glyph
from families.parts_mechanical import motor

TIER = None


def draw():
    g = Glyph(tier=TIER)
    motor(g)
    return g
