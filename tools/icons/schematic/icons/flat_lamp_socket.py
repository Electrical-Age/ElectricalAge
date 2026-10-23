"""flat_lamp_socket: untiered, all INK; a ceiling bar across the top, the lamp flush under it (STYLE 8.2, kit socket('flat'))."""
from kit import Glyph
from families.lighting_fixtures import lamp_socket

TIER = None


def draw():
    g = Glyph(tier=TIER)
    lamp_socket(g, 'flat')
    return g
