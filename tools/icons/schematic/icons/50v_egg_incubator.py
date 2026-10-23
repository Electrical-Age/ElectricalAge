"""50v_egg_incubator: process machine (STYLE 7.2): the heater resistor in the LV hue with an INK egg (tapered, 10x11) centred above it and one INK heat wave on each side of the egg: heat rising round the egg (families.machines.egg_incubator). Not the furnace / heating-corp layout, and the egg is not in the mark slot."""
from kit import Glyph
from families.machines import egg_incubator

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    egg_incubator(g)
    return g
