"""high_power_receiver_antenna: wireless power receiver (hv) -> kit receiver(): antenna in the hv hue + two INK arrows in (STYLE 7.5, conventions)."""
from kit import Glyph
from families.wireless import power_antenna

TIER = 'hv'


def draw():
    g = Glyph(tier=TIER)
    power_antenna(g, 'rx')
    return g
