"""floor_sensor: RS485 bus device; see families.bus_devices (INK bus mark + the sibling symbol in the mark layout, signal hue)."""
from kit import Glyph
from families.bus_devices import floor_sensor

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    floor_sensor(g)
    return g
