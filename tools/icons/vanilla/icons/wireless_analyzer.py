"""wireless_analyzer - handheld-tool family (families/tools.py, 'meters').

The multimeter body and face on a shorter body (rows 4..13), with a black rubber antenna
standing on its top edge: the walkie-talkie silhouette.  The antenna tip is in the signal
tier ramp (manifest tier 'signal' is a colour hint only; tier_cue is null).
"""
from kit import RUBBER_BLACK, tier_ramp
from families.tools import meter


def draw():
    cv = meter(y=4, h=10)
    sig = tier_ramp('signal')
    cv.vline(7, 1, 3, RUBBER_BLACK[4])            # antenna, 2 px: lit column, dark column
    cv.vline(8, 1, 3, RUBBER_BLACK[1])
    cv.set(7, 0, sig[4])                          # tip
    cv.set(8, 0, sig[2])
    return cv
