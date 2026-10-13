"""small_solar_panel - smallsolarpannel.obj: the small square panel lying flat, the 2x3 panel
drawing fitted into SMALL_BOX (size axis): light grey frame, a plain 3x3 px cell field
receding 5 rows (slope 0.8, so it is not a sliver) and the red marker on its rim.
Manifest tier LV, tier_cue null.  See families/solar_thermal.py."""
from families.solar_thermal import solar_fixed


def draw():
    return solar_fixed(small=True)
