"""electrical_fuse_holder: a shortened INK IEC fuse between two open terminal rings at the lead ends (untiered device;
STYLE 8.6: the rings tell the holder apart from the fuses themselves)."""
from kit import Glyph
from families.power_passive import fuse_holder

TIER = None


def draw():
    g = Glyph(tier=TIER)
    fuse_holder(g)
    return g
