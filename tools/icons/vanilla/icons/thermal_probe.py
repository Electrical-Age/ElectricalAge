"""thermal_probe - thermalsensor.obj: a deep THERMAL-black box, its front wider than tall (9 x 7,
depth 3: the model is near-cubic in depth, unlike the thin voltage sensor), blue mark centred on
the top edge and green on the bottom edge, the connector stub with its brass tab centred on the
side. See families/sensors.py."""
from families.sensors import thermal_probe


def draw():
    return thermal_probe()
