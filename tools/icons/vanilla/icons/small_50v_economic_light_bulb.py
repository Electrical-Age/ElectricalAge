"""small_50v_economic_light_bulb - Small 50 V compact fluorescent: the twin-tube bulb fitted into SMALL_BOX, LV collar.
Lamp-socket bulb, no world model; drawn lit.  See families/light_bulbs.py."""
from families.light_bulbs import bulb


def draw():
    return bulb('economic', 'lv', small=True)
