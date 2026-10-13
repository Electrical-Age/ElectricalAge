"""thermal_probe: heat-side mark 'H' + temperature meter 'T' at RING_M (STYLE 8.5)."""
from kit import Glyph
from families.sensors import probe

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    probe(g, 'T', 'H')
    return g
