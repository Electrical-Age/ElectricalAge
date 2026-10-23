"""200v_active_thermal_dissipator - 200vactivethermaldissipatora.obj: full block THERMAL heat
sink with the fan on top: a '+' of STEEL blades over the fins and the hub, a d 6 standing
cylinder in the MV ramp with a lit end cap (the model's blue hub; tier_cue band).  See families/solar_thermal.py."""
from families.solar_thermal import active_dissipator


def draw():
    return active_dissipator('mv', small=False)
