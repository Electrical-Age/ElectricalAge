"""rheostat - sixnode: the power-resistor body plus the red track along its whole top face and
the small black wiper block standing across it (ResistorTrack, ResistorWiper).  See families/power_passive.py."""
from families.power_passive import resistor


def draw():
    return resistor("rheostat")
