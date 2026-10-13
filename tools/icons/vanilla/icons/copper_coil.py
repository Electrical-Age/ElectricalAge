"""copper_coil - thin winding wire, new in 26.x (Baughn 2026-10-13): what the recipe makes, copper
wound around a stick.  A vanilla-style diagonal stick (WOOD, bottom-left to top-right) with a tight
VCOPPER winding round its middle; the turns are grooves across the stick.  Told apart from
copper_cable (an open loop of thick wire) and power_inductor (a drum between two flanges) by
silhouette: no flanges, no loop."""
from kit import Canvas, WOOD, VCOPPER, outline_region

C = 15.5          # the stick's centre line: x + y = C
HALF = 3.6        # winding half-thickness across the stick (in x + y units)
LEN = 6           # winding half-length along the stick (in x - y units)


def draw():
    cv = Canvas(16)
    stick = {(x, y) for x in range(16) for y in range(16) if abs(x + y - C) <= 1.5 and 1 <= x <= 14 and 1 <= y <= 14}
    for p in stick:
        cv.set(*p, WOOD[4] if p[0] + p[1] < C else WOOD[3])
    outline_region(cv, stick, WOOD[0], WOOD[1])

    coil = {(x, y) for x in range(16) for y in range(16)
            if abs(x + y - C) <= HALF and abs(x - y) <= LEN}
    for (x, y) in coil:
        across = x + y - C                       # < 0: top-left (lit) side
        k = 5 if across < -1.5 else 4 if across < 0 else 3 if across < 1.5 else 2
        if (x - y) % 3 == 0:                     # grooves between turns
            k = max(1, k - 2)
        cv.set(x, y, VCOPPER[k])
    outline_region(cv, coil, VCOPPER[0], VCOPPER[1])
    return cv
