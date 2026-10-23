"""portable_capacitor: the IEC capacitor (two equal plates), all INK (untiered), centred, no polar '+' (that is power_capacitor); STYLE 8.4."""
from kit import Glyph
from families.tools import portable_store

TIER = None


def draw():
    g = Glyph(tier=TIER)
    portable_store(g, 'capacitor', pack=False)
    return g
