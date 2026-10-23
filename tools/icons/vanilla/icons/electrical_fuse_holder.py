"""electrical_fuse_holder - sixnode, Case part only (the item draws no fuse): a tall narrow
light-grey box with a faceted face and the empty round seat of the fuse cap.  See families/power_passive.py."""
from families.power_passive import fuse_holder


def draw():
    return fuse_holder()
