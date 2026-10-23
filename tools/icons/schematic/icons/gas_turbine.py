"""gas_turbine: untiered (shaft output only): IEC 'G' ring all INK + flame input mark (STYLE 7.3 / conventions)."""
from kit import Glyph
from families.generators import driven_generator

TIER = None


def draw():
    g = Glyph(tier=TIER)
    driven_generator(g, 'gas')
    return g
