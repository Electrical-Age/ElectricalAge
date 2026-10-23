"""robust_lamp_socket: untiered, all INK; mark R over the mark-layout lamp (STYLE 8.2, kit socket('robust'))."""
from kit import Glyph
from families.lighting_fixtures import lamp_socket

TIER = None


def draw():
    g = Glyph(tier=TIER)
    lamp_socket(g, 'robust')
    return g
