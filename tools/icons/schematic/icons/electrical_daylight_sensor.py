"""electrical_daylight_sensor: sun mark over the signal photodiode (STYLE 8.5, kit sensor_diode)."""
from kit import Glyph
from families.sensors import optical

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    optical(g, 'daylight')
    return g
