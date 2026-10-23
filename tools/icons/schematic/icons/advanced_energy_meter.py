"""advanced_energy_meter: energy + elapsed-time counter -> mark 'advanced' (+) and the E meter ring at RING_M
(STYLE 8.5). Untiered (manifest tier null), all INK."""
from kit import *

TIER = None


def draw():
    g = Glyph(tier=TIER)
    mark(g, 'advanced')
    meter(g, 'E', g.hue, ring=RING_M)
    return g
