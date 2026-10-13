"""portable_battery_pack: two IEC cells in series (the pack, families/tools.battery_pack: kit cells=2 workaround, same geometry as portable_capacitor_pack), all INK (untiered), no mark; STYLE 8.4."""
from kit import Glyph
from families.tools import portable_store

TIER = None


def draw():
    g = Glyph(tier=TIER)
    portable_store(g, 'battery', pack=True)
    return g
