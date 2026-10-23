"""scanner - scanner.obj side-on (rx): thin black arm from the left, a short two-step flare into the
tall straight T-head bar; the upper indicator LED (LED_0, ScanMode 0) lit green on the flare next
to the head; the other LED is not drawn in that mode. The grey base plate is not drawn in world,
so not here. See families/sensors.py."""
from families.sensors import scanner


def draw():
    return scanner()
