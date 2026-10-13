"""electrical_fire_detector - firedetector.obj end-on, idle as drawItem renders it (LED dark): white octagonal dome,
ring of dark side slots, dim red LED. See families/sensors.py."""
from families.sensors import fire_detector


def draw():
    return fire_detector()
