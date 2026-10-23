"""very_high_voltage_relay - relay800.obj (cage width 12): open relay cage (white frame, full-width copper coil on its yoke, white hex armature) with the tier-tinted backplate seen through it, on the tier slab (tier_cue plate)."""
from families.power_switching import relay


def draw():
    return relay('vhv', 12)
