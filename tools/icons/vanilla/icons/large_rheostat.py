"""large_rheostat - full block: dark castellated heatsink (fins on every rim, notches in
the silhouette) with the tan toroidal coil sunk in its pit, seen between the fins and through the
front slots, plus the wiper.  Untiered (manifest tier null).  See families/power_passive.py."""
from families.power_passive import large_rheostat


def draw():
    return large_rheostat()
