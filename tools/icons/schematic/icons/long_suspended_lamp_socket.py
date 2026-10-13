"""long_suspended_lamp_socket: untiered, all INK; the lamp hanging lower on a longer lead from the top edge (STYLE 8.2, kit socket('long_suspended'))."""
from kit import Glyph
from families.lighting_fixtures import lamp_socket

TIER = None


def draw():
    g = Glyph(tier=TIER)
    lamp_socket(g, 'long_suspended')
    return g
