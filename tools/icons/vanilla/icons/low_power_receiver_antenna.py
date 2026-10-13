"""low_power_receiver_antenna - lowpowerreceiverantenna.obj (one model shared by the LV/MV/HV receivers):
the back block is the LV tier plate (front ramp[3] over 6 x 8 px), the light dish opens to the right
with its copper feed spot.  Receiver vs transmitter is the print axis (STYLE 7b): FONT3 'R' on the block
front, since the textures differ only by small connector letters.  See families/wireless.py."""
from families.wireless import antenna


def draw():
    return antenna('lv', 'rx')
