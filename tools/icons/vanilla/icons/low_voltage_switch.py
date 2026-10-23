"""low_voltage_switch - lowvoltageswitch.obj (charcoal box, contact plate, rotary hub) on the LV tier slab (tier_cue plate)."""
from families.power_switching import lever_switch


def draw():
    return lever_switch('lv')
