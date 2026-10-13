"""signal_cable: placeable cable -> the routed-conductor glyph, entirely in the signal tier hue."""
from kit import Glyph, cable_route

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    cable_route(g, g.hue)
    return g
