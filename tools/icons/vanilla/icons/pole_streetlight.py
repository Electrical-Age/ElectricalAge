"""pole_streetlight - a pole-mounted streetlight head on its short curved arm, seen from the side.

The dark-steel arm (DARK = RUBBER_BLACK, as the lighting_fixtures lamp post) rises from a STEEL
mounting clamp at the bottom left and curves over to the right into a cobra-head lamp housing;
the warm lens on the housing's underside is drawn lit (LIT, STYLE 7b).  Arm and housing are one
dark region with one outline; the clamp gets its own.  Untiered."""
from kit import Canvas, outline_region, RUBBER_BLACK, STEEL, LIT

DARK = RUBBER_BLACK

#  a = arm, h = housing, c = steel clamp, L = lens
GRID = [
    "................",
    "................",
    "........hhhhh...",
    ".....aahhhhhhhh.",
    "....aaahhhhhhhhh",
    "...aaa.hhhhhhhh.",
    "...aa...LLLLLL..",
    "..aaa...LLLLLL..",
    "..aa.....LLLL...",
    "..aa............",
    ".cccc...........",
    ".cccc...........",
    ".cccc...........",
    "..aa............",
    "..aa............",
    "................",
]


def _cells(ch):
    return {(x, y) for y, row in enumerate(GRID) for x, c in enumerate(row) if c == ch}


def draw():
    cv = Canvas(16)
    arm, head, clamp, lens = _cells('a'), _cells('h'), _cells('c'), _cells('L')
    dark = arm | head
    for (x, y) in dark:
        lit = (x - 1, y) not in dark or (x, y - 1) not in dark
        cv.set(x, y, DARK[4] if lit else DARK[3])
    for (x, y) in head:
        if y >= 5:
            cv.set(x, y, DARK[2])
    outline_region(cv, dark, DARK[0], DARK[1])
    for (x, y) in head:                       # bevel along the housing's crown
        if y == 3 and 8 <= x <= 12:
            cv.set(x, y, DARK[5])
    for (x, y) in clamp:
        cv.set(x, y, STEEL[3] if x < 4 else STEEL[2])
    cv.hline(2, 3, 10, STEEL[4])
    outline_region(cv, clamp, STEEL[0], STEEL[1])
    # the lens, lit: face LIT[4], glow LIT[5], the far edge LIT[3]; dark rim at both ends
    for (x, y) in lens:
        cv.set(x, y, LIT[3] if y == 8 or x == 13 else LIT[4])
    cv.pixels([(9, 6), (10, 6), (9, 7)], LIT[5])
    return cv
