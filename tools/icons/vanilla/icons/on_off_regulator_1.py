"""on_off_regulator_1 - bang-bang regulator, 1 % hysteresis: the control module with the on/off step and the printed '1' (STYLE 7b print axis; families/parts_modules)."""
from families import parts_modules as F


def draw():
    return F.regulator('onoff', '1')
