"""signal_trimmer - signalsourcepot.obj (ElectricalGateSourceDescriptor 'trimmer'), wall sixnode.

White square wall plate (the model's own white; tier_cue none).  The black rotary knob ('rot')
is a rounded body with a pointer, drawn at the model's rest pose (pointer to the left), with
the red end-of-travel mark top right and the blue one bottom right (texture #ff0000 / #0000ff
squares at the plate's right edge)."""
from kit import Canvas, RUBBER_BLACK, MARK_RED, MARK_BLUE
from families.signal_io import wall_plate, blob

KNOB = [
    "....####..",
    "..#######.",
    ".#########",
    "##########",
    ".#########",
    "..#######.",
    "....####..",
]


def draw():
    cv = Canvas(16)
    wall_plate(cv, 'white')
    pts = [(1 + x, 6 + y) for y, row in enumerate(KNOB) for x, c in enumerate(row) if c == '#']
    blob(cv, pts, RUBBER_BLACK)
    for x, y in ((6, 8), (7, 8), (5, 9)):                    # lit upper face of the knob
        cv.set(x, y, RUBBER_BLACK[5])
    cv.rect(10, 4, 2, 2, MARK_RED)                           # end-of-travel marks
    cv.rect(10, 12, 2, 2, MARK_BLUE)
    return cv
