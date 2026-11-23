"""200v_oil_pump: the IEC motor (M ring at RING_M) in the mv hue with the fuel-drop mark (families/oil_chain.py)."""
from kit import Glyph
from families.oil_chain import oil_pump

TIER = 'mv'


def draw():
    g = Glyph(tier=TIER)
    oil_pump(g)
    return g
