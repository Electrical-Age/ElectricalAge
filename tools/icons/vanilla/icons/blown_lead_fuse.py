"""blown_lead_fuse - the burnt-out lead fuse: the same end-on knurled cap as the four tiered lead
fuses, but untiered (no tier colour) with a dark, scorched window.  See families/power_passive.py."""
from families.power_passive import fuse


def draw():
    return fuse(None)
