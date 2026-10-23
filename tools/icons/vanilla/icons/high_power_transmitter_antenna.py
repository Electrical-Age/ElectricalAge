"""high_power_transmitter_antenna - lowpowertransmitterantenna.obj (one model shared by the LV/MV/HV transmitters):
the back block is the HV tier plate (front ramp[3] over 6 x 8 px), the light dish opens to the right
with its copper feed spot.  Transmitter vs receiver is the print axis (STYLE 7b): FONT3 'T' on the block
front, since the textures differ only by small connector letters.  See families/wireless.py."""
from families.wireless import antenna


def draw():
    return antenna('hv', 'tx')
