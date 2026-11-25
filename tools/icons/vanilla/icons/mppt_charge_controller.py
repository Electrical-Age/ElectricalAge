"""mppt_charge_controller - mpptcontroller.obj: blue wall unit, bumpers, heat sink, display. See families/mppt.py."""
from families.mppt import mppt


def draw():
    return mppt(advanced=False)
