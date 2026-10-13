"""suspended_lamp_socket: untiered, all INK; the lamp hanging from a short lead from the top edge (STYLE 8.2, kit socket('suspended'))."""
from kit import Glyph
from families.lighting_fixtures import lamp_socket

TIER = None


def draw():
    g = Glyph(tier=TIER)
    lamp_socket(g, 'suspended')
    return g
