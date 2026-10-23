"""medium_power_transmitter_antenna: wireless power transmitter (mv) -> kit transmitter(): antenna in the mv hue + INK radiation arcs (STYLE 7.5, conventions)."""
from kit import Glyph
from families.wireless import power_antenna

TIER = 'mv'


def draw():
    g = Glyph(tier=TIER)
    power_antenna(g, 'tx')
    return g
