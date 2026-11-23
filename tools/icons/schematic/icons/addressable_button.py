"""addressable_button: RS485 bus device; see families.bus_devices (INK bus mark + the sibling symbol in the mark layout, signal hue)."""
from kit import Glyph
from families.bus_devices import addressable_button

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    addressable_button(g)
    return g
