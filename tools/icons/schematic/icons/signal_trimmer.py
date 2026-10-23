"""signal_trimmer: resistor in the signal hue + INK pre-set (trimmer) mark (STYLE 9)."""
from kit import Glyph
from families.signal_io import trimmer

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    trimmer(g)
    return g
