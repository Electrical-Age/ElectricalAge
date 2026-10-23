"""200v_fuel_generator - full-block machine (fuelgenerator200v.obj); body = MV tier ramp (cue 'body'). Drawing in families/generators.py."""
from families.generators import fuel_generator


def draw():
    return fuel_generator('mv')
