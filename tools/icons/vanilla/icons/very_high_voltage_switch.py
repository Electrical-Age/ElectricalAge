"""very_high_voltage_switch - highvoltageswitch.obj (same drawing as the HV switch) on the VHV tier slab."""
from families.power_switching import knife_switch


def draw():
    return knife_switch('vhv')
