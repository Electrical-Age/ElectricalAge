"""portable_capacitor - handheld-tool family (families/tools.py, 'cells').

One electrolytic can standing upright, front-on: RUBBER_BLACK cylinder (diameter 10, as the
portable battery) with steel rims at both ends (STEEL end cap on top, a 1-row steel foot
rim) and a 1 px light polarity stripe down its lit side, so the black can dominates.  Told
from the battery by the steel ends (no orange top, no + nub).
"""
from kit import Canvas
from families.tools import capacitor_cell


def draw():
    cv = Canvas(16)
    capacitor_cell(cv, 3, 2, 13, d=10)
    return cv
