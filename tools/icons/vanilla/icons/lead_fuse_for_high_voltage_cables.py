"""lead_fuse_for_high_voltage_cables - the D-type lead fuse seen end-on: knurled cap with the
FuseType disc behind the window in the HV tier ramp (tier cue: band).  See families/power_passive.py."""
from families.power_passive import fuse


def draw():
    return fuse("hv")
