"""high_voltage_switch - highvoltageswitch.obj (charcoal box, white knife blade, black handle) on the HV tier slab."""
from families.power_switching import knife_switch


def draw():
    return knife_switch('hv')
