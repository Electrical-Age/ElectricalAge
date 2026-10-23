"""signal_cable_switch: hidden in-line signal-cable switch -> signal_cable's routed step (left end up, right end down) with the make contact cut into the run, all signal hue. Accepted glyph (STYLE 8.6)."""
from kit import Glyph
from families.power_switching import cable_switch

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    cable_switch(g)
    return g
