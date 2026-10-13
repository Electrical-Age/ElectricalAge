"""very_high_voltage_switch: in-line vhv switch -> IEC make contact (switch), whole contact in the vhv hue (STYLE 8.6)."""
from kit import Glyph
from families.power_switching import power_switch

TIER = 'vhv'


def draw():
    g = Glyph(tier=TIER)
    power_switch(g)
    return g
