"""50v_fuel_generator - full-block machine (fuelgenerator50v.obj); body = LV tier ramp (cue 'body'). Drawing in families/generators.py."""
from families.generators import fuel_generator


def draw():
    return fuel_generator('lv')
