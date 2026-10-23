"""low_power_transmitter_antenna: wireless power transmitter (lv) -> kit transmitter(): antenna in the lv hue + INK radiation arcs (STYLE 7.5, conventions)."""
from kit import Glyph
from families.wireless import power_antenna

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    power_antenna(g, 'tx')
    return g
