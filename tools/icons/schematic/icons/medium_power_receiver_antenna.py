"""medium_power_receiver_antenna: wireless power receiver (mv) -> kit receiver(): antenna in the mv hue + two INK arrows in (STYLE 7.5, conventions)."""
from kit import Glyph
from families.wireless import power_antenna

TIER = 'mv'


def draw():
    g = Glyph(tier=TIER)
    power_antenna(g, 'rx')
    return g
