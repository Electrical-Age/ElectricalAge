"""on_off_regulator_10 - bang-bang regulator, 10 % hysteresis: the control module with a wide on/off loop and the printed '10' (STYLE 7b print axis; families/parts_modules)."""
from families import parts_modules as F


def draw():
    return F.regulator('onoff', '10')
