"""sconce_lamp_socket: untiered, all INK; a wall bar on the left, an arm to the lamp (STYLE 8.2, kit socket('sconce'))."""
from kit import Glyph
from families.lighting_fixtures import lamp_socket

TIER = None


def draw():
    g = Glyph(tier=TIER)
    lamp_socket(g, 'sconce')
    return g
