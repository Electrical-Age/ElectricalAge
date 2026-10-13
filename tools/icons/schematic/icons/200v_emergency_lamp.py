"""200v_emergency_lamp: the IEC lamp in the mv hue under the INK '!' mark (STYLE 8.1, bulb(g, 'emergency'))."""
from kit import Glyph
from families.lighting_fixtures import emergency_lamp

TIER = 'mv'


def draw():
    g = Glyph(tier=TIER)
    emergency_lamp(g)
    return g
