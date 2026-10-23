"""lamp_supply: untiered, all INK; the IEC voltage source at RING_M under the kit 'wireless' (radio) mark: it
feeds the lamp sockets on its channel wirelessly. Not a lamp: it holds no bulb (families.lighting_fixtures)."""
from kit import Glyph
from families.lighting_fixtures import lamp_supply

TIER = None


def draw():
    g = Glyph(tier=TIER)
    lamp_supply(g)
    return g
