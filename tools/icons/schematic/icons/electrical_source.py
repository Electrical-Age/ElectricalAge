"""electrical_source: untiered creative voltage source, all INK. Not the plain IEC source
(source(g, 'V')): signal_source (signal tier) uses that, and STYLE 5.4 requires the untiered item to
differ in structure. So it is the DC voltage source with polarity ('+' upper, '-' lower, terminals
top and bottom), the symbol printed on the in-world plate (families.generators.dc_source)."""
from kit import Glyph
from families.generators import dc_source

TIER = None


def draw():
    g = Glyph(tier=TIER)
    dc_source(g)
    return g
