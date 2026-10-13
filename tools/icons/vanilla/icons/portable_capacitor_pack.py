"""portable_capacitor_pack - handheld-tool family (families/tools.py, 'cells').

Three portable_capacitor cans (black, steel rims at both ends), staggered from the back
top-left to the front bottom-right as the battery pack.
"""
from kit import Canvas
from families.tools import capacitor_cell


def draw():
    cv = Canvas(16)
    capacitor_cell(cv, 1, 1, 11, d=6)
    capacitor_cell(cv, 5, 2, 12, d=6)
    capacitor_cell(cv, 9, 3, 12, d=6)
    return cv
