"""low_power_receiver_antenna: wireless power receiver (lv) -> kit receiver(): antenna in the lv hue + two INK arrows in (STYLE 7.5, conventions)."""
from kit import Glyph
from families.wireless import power_antenna

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    power_antenna(g, 'rx')
    return g
