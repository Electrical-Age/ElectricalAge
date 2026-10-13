"""scanner: fill-level meter: 'L' (level, ISA 5.1 first letter) in a signal ring (family proposal, no STYLE row).

The scanner reads the block it faces like a comparator (tank / inventory fill as a 0..1 signal), so it is a
meter whose quantity is the fill level. Was '%' in review: the FONT '%' knots into a blob at GUI 2 (its dots'
halos merge with the diagonal) and its diagonal echoed analog_gauge's needle. 'L' is a plain FONT glyph, no
diagonal, unused as a ring letter elsewhere in the set. Owner question: 'L' (also IEC inductance) vs a kit
fill-level / 'looks at a block' pictogram (kit request)."""
from kit import Glyph
from families.sensors import probe

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    probe(g, 'L')
    return g
