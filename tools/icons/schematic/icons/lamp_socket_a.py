"""lamp_socket_a: untiered, all INK; mark A over the mark-layout lamp (STYLE 8.2, kit socket('socket_a'))."""
from kit import Glyph
from families.lighting_fixtures import lamp_socket

TIER = None


def draw():
    g = Glyph(tier=TIER)
    lamp_socket(g, 'socket_a')
    return g
