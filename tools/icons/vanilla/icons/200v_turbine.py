"""200v_turbine - heat turbine (turbinebblue.obj): the whole stepped housing in the MV tier ramp, neutral port blocks. Drawing in families/generators.py."""
from families.generators import heat_turbine


def draw():
    return heat_turbine('mv')
