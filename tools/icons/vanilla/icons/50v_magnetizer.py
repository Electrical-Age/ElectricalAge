"""50v_magnetizer - coil drum on a pedestal (magnetizera.obj), pedestal = LV tier ramp; see families/machines.py."""
from families.machines import magnetizer


def draw():
    return magnetizer('lv')
