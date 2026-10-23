"""lamp_socket_b_projector: untiered, all INK; mark B over the mark-layout lamp (STYLE 8.2, kit socket('socket_b'))."""
from kit import Glyph
from families.lighting_fixtures import lamp_socket

TIER = None


def draw():
    g = Glyph(tier=TIER)
    lamp_socket(g, 'socket_b')
    return g
