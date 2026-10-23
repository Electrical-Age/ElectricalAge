"""voltage_probe - voltagesensor.obj: a long thin black bar lying along the cable, green mark at its
left end, the connector stub with its brass tab at the right end. See families/sensors.py."""
from families.sensors import voltage_probe


def draw():
    return voltage_probe()
