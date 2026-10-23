"""50v_farming_lamp - 50 V grow lamp: pink reflector (R-type) lamp, LV collar.
Lamp-socket bulb, no world model; drawn lit.  See families/light_bulbs.py."""
from families.light_bulbs import bulb


def draw():
    return bulb('farming', 'lv')
