"""voltage_to_redstone_converter - eletored.obj, wall sixnode.

White square wall plate (WHITE_PLASTIC) with a redstone torch standing straight out of its
centre toward the viewer.  In cabinet oblique, depth toward the viewer runs down-left, so the
torch is a short wooden stick from the plate centre, a few depth steps down-left (in
proportion with the depth-2 plate), ending in the lit red tip (drawItem draws output 15: the
'led' tip glows).  Two stick steps plus the 3 x 3 tip (4 diagonal steps in all) from the plate
centre (7, 8): any longer and the torch reads as a matchstick lying across the tile."""
from kit import Canvas, WOOD
from families.signal_io import wall_plate, RS, RS_LIT, RS_DARK


def draw():
    cv = Canvas(16)
    wall_plate(cv, 'white')
    # stick: from the plate centre (7, 8), two diagonal steps down-left into the tip
    for i in range(2):
        x, y = 7 - i, 8 + i
        cv.set(x, y, WOOD[4])
        cv.set(x + 1, y, WOOD[2])
        cv.set(x, y + 1, WOOD[1])
    # lit tip, 3 x 3, cols 3..5, rows 10..12
    cv.rect(3, 10, 3, 3, RS)
    cv.set(3, 10, RS_LIT); cv.set(4, 10, RS_LIT); cv.set(3, 11, RS_LIT)
    cv.set(5, 12, RS_DARK); cv.set(4, 12, RS_DARK); cv.set(5, 11, RS_DARK)
    return cv
