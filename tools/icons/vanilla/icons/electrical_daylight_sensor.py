"""electrical_daylight_sensor - daylightsensor.obj: light-grey square wall plate with a bright
royal-blue recessed window.  The light sensor has the same plate; the window colour (royal blue vs
dark slate navy) is the only difference between the two models and so the only cue here: neither
model prints a symbol (STYLE 6).  See families/sensors.py."""
from families.sensors import light_sensor


def draw():
    return light_sensor(daylight=True)
