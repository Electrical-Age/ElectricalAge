"""200v_plate_machine - rolling mill (platemachineb.obj), frame = MV tier ramp; see families/machines.py."""
from families.machines import plate_machine


def draw():
    return plate_machine('mv')
