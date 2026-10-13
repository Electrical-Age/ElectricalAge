"""signal_processor - the PLC box (sixnode, model plc/plc.obj), 16 px, NOT a chip.

Model: ``main`` is a white wall-mounted cabinet, 10 wide x 13 tall seen from the front (taller
than wide), with a full-size hinged ``door`` on the left (hinge at z = 0.3125, i.e. the left
edge seen from the front) and eight ``led*`` parts on two inner module rails.
ElectricalMathDescriptor.drawItem renders the item with the door ~63 deg open
(``draw(0.7f, ledDefault)``) and ledDefault lit: LEDs with idx&3 == 0 red, the others green,
off ones dark grey.  That open look is the identifying feature, so it is drawn here:

 * cabinet: front 9 x 12 + sixnode depth 2 (cols 4..14, rows 1..14), white;
 * opening: CAV interior with an inner left jamb and top, two CABLE module rails, LEDs from
   ledDefault (top row R . G . -> R off G; bottom R G G . -> R G G; the 4th LED is dropped);
 * door: open on the hinge (left) side, a white panel with the grey handle.  It is drawn as a
   foreshortened trapezoid on purpose: the strict cabinet-oblique version (a parallelogram
   running down-left) was tried and read as a skewed grey slab at GUI 2.

White: families.chips.WHITE_PLASTIC (the IRON steps with the face mapping shifted one step up:
front IRON[4] = the texture's ~#e0e0e0, top IRON[5], side IRON[3], outline IRON[2]/IRON[1]),
frozen there and reported as a kit request.  No new colours.

The opening keeps a 1 px white rim on all four sides (row 4, col 5, col 12, row 13), as in
the model's front view; its inner left jamb is WHITE_PLASTIC[1]
(STYLE §3: inner left jambs use the body ramp[1]); the inner top stays CAV[2] (shadow).
"""
from kit import (Canvas, box, outline_region, STEEL, CABLE, CAV, MARK_RED, MARK_GREEN)
from families.chips import WHITE_PLASTIC

#: door panel columns 1..3 -> (first row, last row); the hinge edge (col 3) is full height
DOOR = ((5, 12), (4, 13), (3, 14))


def draw():
    cv = Canvas(16)
    W = WHITE_PLASTIC
    box(cv, 4, 3, 9, 12, 2, W, bevel=False)          # cabinet: front cols 4..12, rows 3..14

    # the opening, with a white rim all round (row 4, col 5, col 12, row 13)
    cv.rect(6, 5, 6, 8, CAV[1])                        # cols 6..11, rows 5..12
    cv.vline(6, 5, 12, W[1])                           # inner left jamb (body ramp[1])
    cv.hline(7, 11, 5, CAV[2])                         # inner top (shadow under the lintel)
    for y in (6, 10):                                  # two module rails
        cv.hline(7, 11, y, CABLE[3])
        cv.hline(7, 11, y + 1, CABLE[1])
    for x, c in ((7, MARK_RED), (9, CAV[3]), (11, MARK_GREEN)):        # LEDs, top rail
        cv.set(x, 6, c)
    for x, c in ((7, MARK_RED), (9, MARK_GREEN), (11, MARK_GREEN)):    # LEDs, bottom rail
        cv.set(x, 10, c)

    # the open door on the hinge side
    door = {(x, y) for x, (a, b) in zip(range(1, 4), DOOR) for y in range(a, b + 1)}
    for p in door:
        cv.set(*p, W[3])
    outline_region(cv, door, W[0], W[1])
    cv.set(2, 9, STEEL[2])                             # handle
    return cv
