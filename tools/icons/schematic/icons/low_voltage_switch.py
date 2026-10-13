"""low_voltage_switch: in-line lv switch -> IEC make contact (switch), whole contact in the lv hue (STYLE 8.6)."""
from kit import Glyph
from families.power_switching import power_switch

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    power_switch(g)
    return g
