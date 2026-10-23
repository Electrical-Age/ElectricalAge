"""200v_macerator - full-block machine, 16 px (cabinet oblique, depth 4).

The reference macerator drawing (families/macerator.py, maceratorb.obj) with tier 'mv': the
maceratorb texture is blue, the MV tier hue, so the body is the tier cue (STYLE 7, cue 'body').
"""
from families.macerator import macerator


def draw():
    return macerator('mv', 16)
