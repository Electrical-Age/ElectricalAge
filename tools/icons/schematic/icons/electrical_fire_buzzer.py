"""electrical_fire_buzzer: flame mark + IEC bell, all INK (untiered, battery powered; STYLE 8.5)."""
from kit import Glyph
from families.sensors import fire_buzzer

TIER = None


def draw():
    g = Glyph(tier=TIER)
    fire_buzzer(g)
    return g
