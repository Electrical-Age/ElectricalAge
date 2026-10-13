"""200v_magnetizer - coil drum on a pedestal (magnetizerb.obj), pedestal = MV tier ramp; see families/machines.py."""
from families.machines import magnetizer


def draw():
    return magnetizer('mv')
