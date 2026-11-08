"""pole_transformer - pole-mounted distribution transformer, front-on.

A standing STEEL can (cylinder) under a darker steel lid that overhangs it by a pixel, two
porcelain bushings (WHITE_PLASTIC) on the lid and the pole-mounting bracket on the can's left
side (dark steel: a vertical strap with two arms clamping the can).  Untiered, so no tier hue."""
from kit import (Canvas, cylinder, outline_region, STEEL, RUBBER_BLACK, WHITE_PLASTIC, h)

#: darker steel for the lid (between STEEL and RUBBER_BLACK, same cool grey hue)
LID = (STEEL[0], h('#3a3945'), h('#4f4f5a'), h('#62626d'), h('#7c7c86'), h('#94949c'))
DARK = RUBBER_BLACK


def draw():
    cv = Canvas(16)
    # the can: cols 5..13 (diameter 9), rows 6..14
    cylinder(cv, 5, 6, 9, 9, STEEL, axis='y', round_ends=False)
    cv.set(5, 14, STEEL[1])
    # the lid: an overhanging cap, cols 4..14, rows 3..6 (top face row 4, front rows 5)
    lid = {(x, y) for y in (3, 4, 5, 6) for x in range(4, 15)} - {(4, 3), (14, 3)}
    for (x, y) in lid:
        if y == 4:
            c = LID[5] if x < 10 else LID[4]
        else:
            c = LID[3] if x < 12 else LID[2]
        cv.set(x, y, c)
    outline_region(cv, lid, LID[0], LID[1])
    # bushings: two porcelain insulators (2 px wide) with a dark terminal cap on top
    for bx in (6, 10):
        for y in (1, 2):
            cv.set(bx, y, WHITE_PLASTIC[4])
            cv.set(bx + 1, y, WHITE_PLASTIC[2])
        cv.set(bx, 0, STEEL[1])
        cv.set(bx + 1, 0, STEEL[0])
    # the mounting bracket on the left: vertical strap cols 1..2, arms to the can at rows 8 and 12
    strap = {(x, y) for x in (1, 2) for y in range(7, 15)}
    arms = {(3, 8), (4, 8), (3, 12), (4, 12)}
    br = strap | arms
    for (x, y) in br:
        cv.set(x, y, DARK[4])
    outline_region(cv, br, DARK[0], DARK[2])
    for y in (8, 12):                         # bolt heads where the arms meet the strap
        cv.set(2, y, STEEL[3])
    return cv
