"""very_high_voltage_cable: placeable cable -> the routed-conductor glyph, entirely in the vhv tier hue."""
from kit import Glyph, cable_route

TIER = 'vhv'


def draw():
    g = Glyph(tier=TIER)
    cable_route(g, g.hue)
    return g
