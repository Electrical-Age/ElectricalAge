"""stone_heat_furnace - stonefurnace.obj: a full cobblestone block whose edges carry the dark
THERMAL frame (the thermal colour), with the drawer (tiroir) on the front.
See families/solar_thermal.py."""
from families.solar_thermal import stone_furnace


def draw():
    return stone_furnace()
