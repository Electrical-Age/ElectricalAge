"""lead_fuse_for_low_voltage_cables: IEC fuse in the LV hue (STYLE 8.6, one icon per tier)."""
from kit import Glyph
from families.power_passive import lead_fuse

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    lead_fuse(g)
    return g
