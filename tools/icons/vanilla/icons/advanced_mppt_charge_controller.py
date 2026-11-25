"""advanced_mppt_charge_controller - mpptcontroller.obj with GOLD trim (quality level 2). See families/mppt.py."""
from families.mppt import mppt


def draw():
    return mppt(advanced=True)
