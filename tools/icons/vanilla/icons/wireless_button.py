"""wireless_button - ledswitch.obj, the same model and renderer as signal_button, untiered (white plate).

The push-button at rest as drawItem draws it: the cube standing out of the recess, LED and halo
rays red (ledOnOffColor(false)).  Since the world model is identical to signal_button's, the
wireless mark is a print (STYLE 7b): one dark wave arc on the plate frame round the recess's
top-right corner (the recess is 8 x 8, one step smaller than signal_io's, to make room for it).
See families/wireless.py."""
from kit import MARK_RED
from families.wireless import led_switch


def draw():
    return led_switch(MARK_RED, glow=MARK_RED)
