"""small_active_thermal_dissipator - activethermaldissipatora.obj: the THERMAL heat sink with
the fan on top (STEEL blades, hub a d 5 cylinder in the LV ramp: the model's green hub;
tier_cue band), in SMALL_BOX: the passive's heat sink and port, front 7 x 6.
See families/solar_thermal.py."""
from families.solar_thermal import active_dissipator


def draw():
    return active_dissipator('lv', small=True)
