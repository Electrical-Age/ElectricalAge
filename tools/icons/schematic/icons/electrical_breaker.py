"""electrical_breaker: circuit breaker (untiered, works on any tier) -> IEC make contact with the breaker cross on the fixed contact, all INK (STYLE 8.6). families.power_switching.power_breaker = kit breaker(g, g.hue): the cross replaces the fixed-contact dot."""
from kit import Glyph
from families.power_switching import power_breaker

TIER = None


def draw():
    g = Glyph(tier=TIER)
    power_breaker(g)
    return g
