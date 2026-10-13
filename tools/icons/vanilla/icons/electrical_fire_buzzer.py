"""electrical_fire_buzzer - firedetector.obj (the fire detector's model) in alarm state: the
detector's dome 1 px smaller and moved 1 px right/down, its LED lit, two continuous sound arcs in
the top-left corner, the inner one over the dome's rim (the 1.12 texture's arcs). State axis,
STYLE 7b. See families/sensors.py."""
from families.sensors import fire_buzzer


def draw():
    return fire_buzzer()
