"""MPPT charge controllers (mpptcontroller.obj, code CAD): a blue wall unit taller than wide, black rubber
bumpers on its corners, a dark finned heat sink across the bottom, an orange stripe, the backlit blue display.
The advanced one carries the quality ladder's GOLD trim (level 2) on its top bumper row."""
from kit import Canvas, box, STEEL, CAV
from kit.palette import _r

BLUE = _r('#162a52', '#22407a', '#2f5a9e', '#3b6fbd', '#5a8ad0')
RUBBER = _r('#121214', '#1c1c1f', '#262629', '#303034', '#3c3c40')
ORANGE = '#e2842c'
LCD = ('#24476b', '#4f8fd0', '#9fd0ff')


def mppt(advanced=False):
    from kit import GOLD
    from kit.color import h
    cv = Canvas(16)
    box(cv, 2, 3, 10, 13, 2, BLUE, bevel=False)          # the case: front cols 2..11, rows 3..15
    cv.rect(3, 12, 8, 3, STEEL[1])                        # heat sink across the bottom
    for x in (4, 6, 8, 10):
        cv.vline(x, 12, 14, STEEL[2])                     # its fins
    for (x, y) in ((2, 3), (10, 3), (2, 14), (10, 14)):   # bumpers on the corners
        cv.rect(x, y, 2, 2, RUBBER[2])
    cv.hline(4, 9, 5, GOLD[3] if advanced else h(ORANGE))   # stripe: orange, gold on the advanced one (as the model)
    cv.rect(4, 7, 6, 4, h(LCD[0]))                        # display bezel
    cv.rect(5, 8, 4, 2, h(LCD[1]))                        # backlit glass
    cv.set(5, 8, h(LCD[2]))
    if advanced:
        cv.hline(4, 9, 4, GOLD[3])
        cv.hline(4, 9, 11, GOLD[3])                        # and a gold band over the heat sink
    return cv
