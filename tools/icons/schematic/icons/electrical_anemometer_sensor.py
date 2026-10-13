"""electrical_anemometer_sensor: wind mark + speed meter 'n' at RING_M (STYLE 8.5)."""
from kit import Glyph
from families.sensors import probe

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    probe(g, 'n', 'wind')
    return g
