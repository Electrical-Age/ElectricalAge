"""portable_battery - handheld-tool family (families/tools.py, 'cells').

One rechargeable cell standing upright, front-on: a RUBBER_BLACK can (cylinder, diameter 10
so the silhouette meets the 10 px width rule) whose top third is the orange COPPER top with
its end face showing, and a steel + terminal nub on top.  Same cell as portable_battery_pack.
"""
from kit import Canvas
from families.tools import battery_cell


def draw():
    cv = Canvas(16)
    battery_cell(cv, 3, 2, 13, d=10, top=5)
    return cv
