"""ground_cable - wall sixnode (groundcable.obj 'main': a 0.375 x 0.375 x 0.125 terracotta plate),
front 12 wide x 11 tall, sixnode depth 2.

The plate is the kit's TERRACOTTA ramp, tuned on the model texture (#ab4f14) and kept as far
from HV and VHV as a terracotta can be (STYLE section 5.3).  The ground symbol
is printed on the model's texture (MATCH, like chip faces), drawn in RUBBER_BLACK[0], the
nearest frozen near-black above the L* floor, so it reads as printed black ink rather than cuts.  Untiered."""
from kit import Canvas, box, depth, TERRACOTTA, RUBBER_BLACK

SYMBOL = ["....##....",
          "....##....",
          "....##....",
          ".########.",
          "..........",
          "..######..",
          "..........",
          "...####...",
          ]


def draw():
    cv = Canvas(16)
    box(cv, 1, 4, 12, 11, depth(cv, 'sixnode'), TERRACOTTA)   # front cols 1..12, rows 4..14
    cv.overlay(SYMBOL, {'#': RUBBER_BLACK[0]}, ox=2, oy=5)
    return cv
