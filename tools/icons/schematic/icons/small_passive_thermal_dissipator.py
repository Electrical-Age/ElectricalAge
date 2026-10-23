"""small_passive_thermal_dissipator: three THERMAL heat-sink fins on a base rail, each under one of the
three INK heat waves rising off it (families/solar_thermal). The manifest names 'heat-sink fins' as the
symbol; whether a flat comb is too close to the in-world finned block (decision 5) is with the owner."""
from kit import Glyph
from families.solar_thermal import passive_dissipator

TIER = 'thermal'


def draw():
    g = Glyph(tier=TIER)
    passive_dissipator(g)
    return g
