"""voltage_probe: voltmeter: 'V' in a signal ring (STYLE 8.5)."""
from kit import Glyph
from families.sensors import probe

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    probe(g, 'V')
    return g
