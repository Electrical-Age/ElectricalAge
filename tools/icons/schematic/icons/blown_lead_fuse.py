"""blown_lead_fuse: the lead fuse, untiered INK, element cut (STYLE 8.6 fuse(blown=True))."""
from kit import Glyph
from families.power_passive import blown_fuse

TIER = None


def draw():
    g = Glyph(tier=TIER)
    blown_fuse(g)
    return g
