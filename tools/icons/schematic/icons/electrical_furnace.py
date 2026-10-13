"""electrical_furnace: untiered (all INK) furnace convention, STYLE 8.6: heat_waves at HEAT_TOP over a resistor on MID_M (families.machines.furnace)."""
from kit import Glyph
from families.machines import furnace

TIER = None


def draw():
    g = Glyph(tier=TIER)
    furnace(g)
    return g
