"""electrical_light_sensor - lightsensor.obj: light-grey square wall plate with a dark slate-navy
recessed window.  The daylight sensor has the same plate; the window colour (navy vs royal blue)
is the only difference between the two models and so the only cue here: neither model prints a
symbol (STYLE 6).  See families/sensors.py."""
from families.sensors import light_sensor


def draw():
    return light_sensor(daylight=False)
