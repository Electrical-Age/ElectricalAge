"""power_capacitor: placeable, untiered (its rating comes from the dielectric, not a cable tier)
-> IEC capacitor, all INK. The element has named +/- terminals, so the polarity '+' is shown; the
plates sit right of centre so the '+' keeps 3 clear px (capacitor(polar=True) does this)."""
from kit import Glyph, capacitor

TIER = None


def draw():
    g = Glyph(tier=TIER)
    capacitor(g, polar=True)
    return g
