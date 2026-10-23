"""electrical_weather_sensor: cloud (rain) mark + rain resistor in the signal hue on MID_M (STYLE 8.5)."""
from kit import Glyph
from families.sensors import weather

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    weather(g)
    return g
