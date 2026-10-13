"""fluorescent_lamp_socket: untiered, all INK; mark E (as the economic = fluorescent bulb) over the mark-layout lamp (STYLE 8.2, kit socket('fluorescent'))."""
from kit import Glyph
from families.lighting_fixtures import lamp_socket

TIER = None


def draw():
    g = Glyph(tier=TIER)
    lamp_socket(g, 'fluorescent')
    return g
