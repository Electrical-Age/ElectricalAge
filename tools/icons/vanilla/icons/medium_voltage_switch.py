"""medium_voltage_switch - lowvoltageswitch.obj (same drawing as the LV switch) on the MV tier slab (tier_cue plate)."""
from families.power_switching import lever_switch


def draw():
    return lever_switch('mv')
