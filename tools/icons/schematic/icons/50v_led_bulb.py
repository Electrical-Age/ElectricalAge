"""50v_led_bulb: LED symbol (diode in the lv hue + INK emission arrows); tier sibling of 200v_led_bulb, STYLE 8.1."""
from kit import Glyph
from families.light_bulbs import light_bulb

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    light_bulb(g, 'led')
    return g
