"""2x3_rotating_solar_panel - solarpanel2x3/bigsolarpanelrot.obj: the large panel tilted up
(item pose alphaMin, 22.5 deg) on its central STEEL post and cross-shaped ground stand, with the
blue/red terminal box at the foot.  Manifest tier MV, tier_cue null.  See families/solar_thermal.py."""
from families.solar_thermal import solar_rotating


def draw():
    return solar_rotating(small=False)
