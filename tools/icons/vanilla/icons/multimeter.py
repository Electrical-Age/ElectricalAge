"""multimeter - handheld meter (reference icon for the handheld-tool template).

Body: kit.handheld_body in the yellow rubber ramp (cols 1..10, rows 2..13), front-on like the
vanilla compass.  Face: LCD window on top, two jacks below.  Accessories: the red and black
probe leads as 2 px 45-degree strokes (``diag_stroke``: left pixel lit, right pixel body)
leaving the jacks towards the free bottom-right corner, in front of the body, each ending in a
bright steel tip.  The black lead keeps a lit edge (LEAD_BLK[2]) so it reads on the slot grey.
"""
from kit import (Canvas, handheld_body, diag_stroke, RUBBER_Y, CAV, CHIP, LCD, STEEL, LEAD_RED,
                 LEAD_BLK)

FACE = [
    "................",
    "................",
    "................",
    "................",
    "...kkkkkk.......",
    "...kPPPPk.......",
    "...kpkpkk.......",
    "...kkkkkk.......",
    "...cccccc.......",
    "...cxccxc.......",
    "...cccccc.......",
]


def draw():
    cv = Canvas(16)
    handheld_body(cv, RUBBER_Y)                     # face rect (3, 4, 6, 8)
    cv.overlay(FACE, {'k': CAV[1], 'c': CHIP[3], 'P': LCD[3], 'p': LCD[2], 'x': STEEL[4]})
    # two probes in the crossed-tools pose in the free corner: black (behind) runs down-left,
    # red (in front) runs down-right; each = 4 steps of handle + 2 steps of steel tip
    diag_stroke(cv, 14, 8, 4, LEAD_BLK[2], LEAD_BLK[1], direction=(-1, 1))   # (14..11, 8..11)
    diag_stroke(cv, 10, 12, 2, STEEL[4], STEEL[2], direction=(-1, 1))        # tip (10..9, 12..13)
    diag_stroke(cv, 9, 8, 4, LEAD_RED[2], LEAD_RED[1])                       # (9..12, 8..11)
    diag_stroke(cv, 13, 12, 2, STEEL[4], STEEL[2])                           # tip (13..14, 12..13)
    return cv
