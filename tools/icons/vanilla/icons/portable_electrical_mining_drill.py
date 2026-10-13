"""portable_electrical_mining_drill - handheld-tool family (families/tools.py, 'diagonal tools').

The in-game texture's drill in the vanilla tool pose: a wide dark steel bit cone (as wide as
the axe bar for most of its length) tapering along the slot diagonal to a point at the
top-right, with slanted orange (COPPER) flute bands two lines wide, a steel chuck at its root,
leaving the round black motor housing (d 7) at the bottom-left (shared with the axe).
"""
from families.tools import drill


def draw():
    return drill()
