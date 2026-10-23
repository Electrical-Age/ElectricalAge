"""800v_defence_turret: function glyph (STYLE 11, turret = crosshair): a gun-sight (ring + four cross ticks) in the HV hue with an INK aim dot (families.machines.turret)."""
from kit import Glyph
from families.machines import turret

TIER = 'hv'


def draw():
    g = Glyph(tier=TIER)
    turret(g)
    return g
