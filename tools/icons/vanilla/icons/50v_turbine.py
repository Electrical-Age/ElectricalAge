"""50v_turbine - heat turbine (turbineb.obj): the whole stepped housing in the LV tier ramp, neutral port blocks. Drawing in families/generators.py."""
from families.generators import heat_turbine


def draw():
    return heat_turbine('lv')
