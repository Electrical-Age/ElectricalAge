"""signal_cable_switch - lowvoltageswitch.obj (same drawing as the LV/MV switch) on the signal tier slab (tier_cue plate)."""
from families.power_switching import lever_switch


def draw():
    return lever_switch('signal')
