"""dc_dc_converter - full block: low light-grey base, the dark ferromagnetic core loop standing on
it, copper windings on both legs.  See families/power_passive.py."""
from families.power_passive import dc_dc_converter


def draw():
    return dc_dc_converter()
