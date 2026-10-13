"""advanced_electrical_motor: the IEC motor symbol with the kit mark 'advanced' (+) top right and the 'M'
ring at RING_M (STYLE 7.3). Untiered (manifest tier null), so all INK; the '+' mark is what tells it from
electrical_motor, as with energy_meter / advanced_energy_meter."""
from kit import Glyph
from families.parts_mechanical import motor

TIER = None


def draw():
    g = Glyph(tier=TIER)
    motor(g, advanced=True)
    return g
