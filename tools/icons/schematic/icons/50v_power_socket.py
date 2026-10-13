"""50v_power_socket: IEC socket outlet (bar + half ring, kit bell proportions flipped) in the lv hue, INK lead up (STYLE 8.6)."""
from kit import *
from families.cables import power_socket
TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    power_socket(g)
    return g
