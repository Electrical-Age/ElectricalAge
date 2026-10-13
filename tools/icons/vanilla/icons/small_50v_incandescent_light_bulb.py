"""small_50v_incandescent_light_bulb - Small 50 V incandescent: the A-globe bulb fitted into SMALL_BOX, LV collar.
Lamp-socket bulb, no world model; drawn lit.  See families/light_bulbs.py."""
from families.light_bulbs import bulb


def draw():
    return bulb('incandescent', 'lv', small=True)
