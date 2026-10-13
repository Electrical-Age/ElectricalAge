"""wireless_button: mark 'wireless' (radio) + mark-layout make contact in the signal hue + INK push actuator on the blade (STYLE 8.6, family lead)."""
from kit import Glyph
from families.wireless import wireless_contact

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    wireless_contact(g, button=True)
    return g
