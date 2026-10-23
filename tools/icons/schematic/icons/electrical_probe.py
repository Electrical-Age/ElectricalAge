"""electrical_probe: meter 'A' in a signal ring (STYLE 8.5, provisional: the probe switches V/A/W)."""
from kit import Glyph
from families.sensors import probe

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    probe(g, 'A')
    return g
