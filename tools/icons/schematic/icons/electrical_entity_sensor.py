"""electrical_entity_sensor: person mark over the signal photodiode, an IR presence detector (STYLE 8.5)."""
from kit import Glyph
from families.sensors import optical

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    optical(g, 'entity')
    return g
