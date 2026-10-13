"""portable_battery_pack - handheld-tool family (families/tools.py, 'cells').

Three portable_battery cells (black can, orange top, steel nub), staggered from the back
top-left to the front bottom-right, each drawn over the one behind it.
"""
from kit import Canvas
from families.tools import battery_cell


def draw():
    cv = Canvas(16)
    battery_cell(cv, 1, 2, 10, d=6, top=4)
    battery_cell(cv, 5, 3, 11, d=6, top=4)
    battery_cell(cv, 9, 4, 11, d=6, top=4)
    return cv
