"""small_rotating_solar_panel - smallsolarpannelrot.obj: the small panel tilted up (item pose
alphaMin, 45 deg) on its T post and small base block with the red marker, in SMALL_BOX.
Manifest tier LV, tier_cue null.  See families/solar_thermal.py."""
from families.solar_thermal import solar_rotating


def draw():
    return solar_rotating(small=True)
