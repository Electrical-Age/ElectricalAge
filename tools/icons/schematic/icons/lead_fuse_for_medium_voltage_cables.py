"""lead_fuse_for_medium_voltage_cables: IEC fuse in the MV hue (STYLE 8.6, one icon per tier)."""
from kit import Glyph
from families.power_passive import lead_fuse

TIER = 'mv'


def draw():
    g = Glyph(tier=TIER)
    lead_fuse(g)
    return g
