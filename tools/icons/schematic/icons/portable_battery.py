"""portable_battery: the IEC cell (long + short plate), all INK (untiered), no mark; STYLE 8.4."""
from kit import Glyph
from families.tools import portable_store

TIER = None


def draw():
    g = Glyph(tier=TIER)
    portable_store(g, 'battery', pack=False)
    return g
