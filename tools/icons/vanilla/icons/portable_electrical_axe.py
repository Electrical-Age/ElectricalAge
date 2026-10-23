"""portable_electrical_axe - handheld-tool family (families/tools.py, 'diagonal tools').

The in-game texture's chainsaw-style axe in the vanilla tool pose: a flat steel bar on the slot
diagonal (rounded nose top-right) whose silhouette is serrated - raked teeth on both long
edges taken by the dark CAV edge ring - with a 1 px chain band inside the edge, leaving the
round black motor housing (disc, steel spindle boss) at the bottom-left.  Shares the motor with
portable_electrical_mining_drill; only the head differs.
"""
from families.tools import axe


def draw():
    return axe()
