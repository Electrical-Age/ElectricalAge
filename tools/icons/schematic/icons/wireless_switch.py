"""wireless_switch: mark 'wireless' (radio) + mark-layout make contact in the signal hue (STYLE 8.6)."""
from kit import Glyph
from families.wireless import wireless_contact

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    wireless_contact(g)
    return g
