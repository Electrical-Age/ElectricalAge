"""200v_economic_light_bulb - 200 V compact fluorescent: twin-tube U, MV collar.
Lamp-socket bulb, no world model; drawn lit.  See families/light_bulbs.py."""
from families.light_bulbs import bulb


def draw():
    return bulb('economic', 'mv')
