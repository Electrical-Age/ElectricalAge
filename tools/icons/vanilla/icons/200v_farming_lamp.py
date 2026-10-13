"""200v_farming_lamp - 200 V grow lamp: pink reflector (R-type) lamp, MV collar.
Lamp-socket bulb, no world model; drawn lit.  See families/light_bulbs.py."""
from families.light_bulbs import bulb


def draw():
    return bulb('farming', 'mv')
