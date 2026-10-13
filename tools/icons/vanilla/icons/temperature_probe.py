"""temperature_probe - temperaturesensor.obj: a THERMAL-black box with the sloped (chamfered) adapter
nose on the left, a green mark beside it and the connector stub with its brass tab. See
families/sensors.py."""
from families.sensors import temperature_probe


def draw():
    return temperature_probe()
