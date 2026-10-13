"""50v_carbon_incandescent_light_bulb - 50 V carbon incandescent: Edison pear with tip and hairpin filament, LV collar.
Lamp-socket bulb, no world model; drawn lit.  See families/light_bulbs.py."""
from families.light_bulbs import bulb


def draw():
    return bulb('carbon', 'lv')
