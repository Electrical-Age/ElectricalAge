"""50v_led_bulb - 50 V LED bulb: flat frosted dome on a finned heat sink, LV collar.
Lamp-socket bulb, no world model; drawn lit.  See families/light_bulbs.py."""
from families.light_bulbs import bulb


def draw():
    return bulb('led', 'lv')
