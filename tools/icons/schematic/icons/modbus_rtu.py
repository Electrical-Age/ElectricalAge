"""modbus_rtu: signal bus in the hue with three tapped channel terminals + INK two-way link arrow (family proposal, families.signal_io.rtu)."""
from kit import Glyph
from families.signal_io import rtu

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    rtu(g)
    return g
