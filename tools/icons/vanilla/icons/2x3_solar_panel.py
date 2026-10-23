"""2x3_solar_panel - solarpanel2x3/bigsolarpanel.obj: the large panel lying flat on the ground,
drawn in the top plane of cabinet oblique (8 wide, 6 deep), SOLAR cells with a module grid,
light grey STEEL frame inside the outline, the blue/red terminal box on the front edge.  Manifest tier MV, tier_cue null.
See families/solar_thermal.py."""
from families.solar_thermal import solar_fixed


def draw():
    return solar_fixed(small=False)
