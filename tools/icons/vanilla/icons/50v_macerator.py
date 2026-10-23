"""50v_macerator - full-block machine, 16 px reference (cabinet oblique, depth 4).

Body ramp = the LV tier ramp: the maceratora texture is green, which is the LV tier hue, so
the machine's own colour is the tier cue (STYLE.md "Tiers", rule 1).  200v_macerator is the
same drawing with tier 'mv'.  Drawing code lives in families/macerator.py.
"""
from families.macerator import macerator


def draw():
    return macerator('lv', 16)
