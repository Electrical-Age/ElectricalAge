"""experimental_transporter: function glyph: two pods (plan-view D=8 rings) in the HV hue and an INK dotted transfer arrow (dashed() dot spacing, L route) from one to the other (families.machines.transporter)."""
from kit import Glyph
from families.machines import transporter

TIER = 'hv'


def draw():
    g = Glyph(tier=TIER)
    transporter(g)
    return g
