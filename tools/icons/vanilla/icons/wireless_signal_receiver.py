"""wireless_signal_receiver - wirelesssignalrx.obj: white wall plate (12 x 11 sixnode front), one dark
rod with the black tip cube standing out towards the viewer, black-framed LED window with the LED red
(drawItem: connection=false).  See families/wireless.py."""
from kit import MARK_RED
from families.wireless import signal_sixnode


def draw():
    return signal_sixnode('rx', MARK_RED)
