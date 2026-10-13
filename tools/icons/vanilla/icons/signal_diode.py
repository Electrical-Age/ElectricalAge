"""signal_diode - sixnode: the same diode model (black can, white cathode band, no rating mark).
Tier signal, shown on the mounting base (signal plate ramp: the kit's plate cue; the manifest gives no
tier_cue).  See families/power_passive.py."""
from families.power_passive import diode


def draw():
    return diode(None, "signal")
