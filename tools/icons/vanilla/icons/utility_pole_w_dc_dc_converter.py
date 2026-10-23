"""utility_pole_w_dc_dc_converter - the utility pole (same POLE_WOOD post and crossarm) plus the parts its
descriptor adds: the STEEL transformer box hanging beside the post (one vent slot, yellow warning
label) hanging from the arm, and the widened POLE_WOOD foot (the feed wires and the conduit down the
post have no room at 16 px).
tier_cue is null (decided centrally), so no tier colour."""
from families.cables import pole


def draw():
    return pole(converter=True)
