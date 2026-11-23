"""addressable_light: RS485 bus device; see families.bus_devices (INK bus mark + the sibling symbol in the mark layout, signal hue)."""
from kit import Glyph
from families.bus_devices import addressable_light

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    addressable_light(g)
    return g
