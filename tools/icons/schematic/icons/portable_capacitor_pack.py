"""portable_capacitor_pack: two IEC capacitors in series (the pack, families/tools.capacitor_pack: kit cells=2 workaround), all INK (untiered), no polar +; STYLE 8.4."""
from kit import Glyph
from families.tools import portable_store

TIER = None


def draw():
    g = Glyph(tier=TIER)
    portable_store(g, 'capacitor', pack=True)
    return g
