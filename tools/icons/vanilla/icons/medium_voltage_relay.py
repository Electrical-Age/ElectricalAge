"""medium_voltage_relay - relaybig.obj (cage width 10): open relay cage (white frame, full-width copper coil on its yoke, white hex armature) with the tier-tinted backplate seen through it, on the tier slab (tier_cue plate)."""
from families.power_switching import relay


def draw():
    return relay('mv', 10)
