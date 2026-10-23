"""electrical_timer - electricaltimer.obj (ElectricalTimeoutDescriptor), wall sixnode.

White wall box, wider than tall (front 12 x 10, depth 2): the grey round dial on the left with
the red needle ('rot', pointing left at rest), and the round LED ('led') on the right, drawn
green because drawItem draws draw(1f) -> ledOnOffColor(true).  The green connector patch on the
model's right side shows on the side face.

The dial is printed flat on the white face (model rx view: a mid-grey disc, no rim, no
relief), so it is a flat WHITE_PLASTIC[2] disc with no outline or bevel, 7 px across."""
from kit import Canvas, box, disc, MARK_GREEN
from families.signal_io import W, RS, RS_LIT, RS_DARK


def draw():
    cv = Canvas(16)
    box(cv, 1, 5, 12, 10, 2, W, bevel=False)          # front cols 1..12, rows 5..14
    disc(cv, 2, 6, 7, W, face=2, rim=False, outline=False)   # dial cols 2..8, rows 6..12
    cv.hline(2, 5, 9, RS)                             # needle, pivot at (5, 9), tip to the left
    cv.set(2, 9, RS_LIT)
    cv.set(5, 9, RS_DARK)
    cv.hline(10, 11, 8, MARK_GREEN)                   # LED (round, 2 x 3)
    cv.hline(10, 11, 9, MARK_GREEN)
    cv.hline(10, 11, 10, MARK_GREEN)
    cv.vline(13, 6, 8, MARK_GREEN)                    # side patch
    return cv
