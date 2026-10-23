"""auto_miner: process machine (STYLE 7.2): a drill arrow in the HV hue going down through a broken INK surface line into INK ore grains (families.machines.auto_miner)."""
from kit import Glyph
from families.machines import auto_miner

TIER = 'hv'


def draw():
    g = Glyph(tier=TIER)
    auto_miner(g)
    return g
