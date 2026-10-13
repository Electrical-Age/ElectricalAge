"""electrical_light_sensor: photodiode with two incident-light arrows (STYLE 8.5)."""
from kit import Glyph
from families.sensors import optical

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    optical(g)
    return g
