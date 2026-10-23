"""signal_switch_with_led: in-line signal switch with LED -> mark 'led' + mark-layout switch in the signal hue (STYLE 8.6)."""
from kit import Glyph
from families.power_switching import led_switch

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    led_switch(g)
    return g
