"""wireless_signal_repeater - wirelesssignalrepeater.obj: white wall plate (12 x 11 sixnode front), two
parallel dark rods with black tip cubes (2 x 2, 2 px apart), empty black-framed LED window (its LED is
not drawn).  See families/wireless.py."""
from families.wireless import signal_sixnode


def draw():
    return signal_sixnode('repeater')
