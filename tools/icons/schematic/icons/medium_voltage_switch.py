"""medium_voltage_switch: in-line mv switch -> IEC make contact (switch), whole contact in the mv hue (STYLE 8.6)."""
from kit import Glyph
from families.power_switching import power_switch

TIER = 'mv'


def draw():
    g = Glyph(tier=TIER)
    power_switch(g)
    return g
