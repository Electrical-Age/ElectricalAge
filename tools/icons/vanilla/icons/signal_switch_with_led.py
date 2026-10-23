"""signal_switch_with_led - ledswitch.obj: the square wall plate is the signal plate cue; dark recess with a lit lip, white push-button cube with its LED lit."""
from families.power_switching import led_switch


def draw():
    return led_switch()
