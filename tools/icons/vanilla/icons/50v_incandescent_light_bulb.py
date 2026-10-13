"""50v_incandescent_light_bulb - 50 V incandescent (iron filament): A-globe with a coiled filament, LV collar.
Lamp-socket bulb, no world model; drawn lit.  See families/light_bulbs.py."""
from families.light_bulbs import bulb


def draw():
    return bulb('incandescent', 'lv')
