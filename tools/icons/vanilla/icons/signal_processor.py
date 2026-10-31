"""signal_processor - the DIN-rail cupboard (sixnode, cad SignalProcessor, 2026-10-30), 16 px, NOT a chip.

Model: an open grey cupboard fixed to a face, three DIN rails across it carrying up to eighteen
arithmetic modules (one per cheap chip's worth inserted; the item draws six), the logger's small
dark screen across the top with status lamps beside it, and a brass terminal on each edge.  Drawn
front-on as the family's sixnode boxes (depth 2):

 * cupboard: front cols 2..13, rows 2..13, STEEL, the open interior CAV;
 * top: the screen (CAV[0]) with a green trace and two lamps (red, green) at its right;
 * three rails (CABLE), the upper two carrying modules in the model's colours (white, the
   yellow-orange paint, dark), a red lamp or a brass screw on some faces; the lowest rail empty,
   as the item shows six modules;
 * brass terminals at the middle of the left and right edges (the top and bottom ones hide
   behind the box's own top and the bottom row).
"""
from kit import Canvas, box, STEEL, CABLE, CAV, MARK_RED, MARK_GREEN, GOLD, WHITE, line


def draw():
    cv = Canvas(16)
    box(cv, 2, 3, 11, 11, 2, STEEL, bevel=False)      # cupboard: front cols 2..12, rows 3..13
    cv.rect(3, 4, 9, 9, CAV[1])                        # the open interior, cols 3..11, rows 4..12
    cv.hline(3, 11, 4, CAV[2])
    # the screen across the top, its trace, two lamps beside it
    cv.rect(3, 5, 6, 2, CAV[0])
    line(cv, (3, 6), (5, 5), MARK_GREEN)
    line(cv, (5, 5), (8, 6), MARK_GREEN)
    cv.set(10, 5, MARK_RED)
    cv.set(11, 5, MARK_GREEN)
    # rails and modules
    body = [WHITE, GOLD[3], CAV[3], WHITE, GOLD[3], CAV[3]]
    for r, y in enumerate((8, 11)):
        cv.hline(3, 11, y, CABLE[2])
        for k in range(3):
            x = 3 + 3 * k
            c = body[r * 3 + k]
            cv.rect(x, y - 1, 2, 2, c)
            cv.set(x + 1, y - 1, MARK_RED if (r + k) % 2 == 0 else GOLD[2])
    cv.hline(3, 11, 12, CABLE[2])                      # the third rail, empty
    # brass terminals on the left and right edges
    cv.rect(1, 7, 1, 3, GOLD[2])
    cv.rect(13, 7, 1, 3, GOLD[2])
    return cv
