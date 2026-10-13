"""small_50v_carbon_incandescent_light_bulb - Small 50 V carbon incandescent: the carbon bulb fitted into SMALL_BOX, LV collar.
Lamp-socket bulb, no world model; drawn lit.  See families/light_bulbs.py."""
from families.light_bulbs import bulb


def draw():
    return bulb('carbon', 'lv', small=True)
