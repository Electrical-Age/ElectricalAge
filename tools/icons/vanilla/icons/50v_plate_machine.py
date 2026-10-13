"""50v_plate_machine - rolling mill (platemachinea.obj), frame = LV tier ramp; see families/machines.py."""
from families.machines import plate_machine


def draw():
    return plate_machine('lv')
