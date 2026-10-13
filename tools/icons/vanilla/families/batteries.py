"""Family helper: batteries (the six batterybig blocks) and battery chargers, 16 px.

Shared visual system
--------------------
Stationary batteries (batterybig.obj, one model per variant: MATCH, no tier cue)
  * Every variant stands on the SAME tray: ``tray()`` = a light STEEL slab, front cols 1..10,
    rows 11..14, depth 4 (block), with the model's dark rim as ONE solid CAV[1] band on its
    front (rows 12..13; no copied checker, STYLE 3).  Only the cell(s) above it change, so the
    family reads as one kit.
  * Cells are RUBBER_BLACK boxes standing one depth step back on the tray (front bottom row 10),
    depth 2, front rows 3..10.  The variant is told apart by the cell geometry and the one
    marking its texture carries - the drawn face is the one that shows it:
      - ``single_use`` (battery_coal): a squat stack of black plates, COPPER separator lines
        (3 of the model's 6, wrapping onto the side face), red terminal nub on top (fy view),
        and the fy face's '-' plug as a light STEEL[4] dash on the tray band;
      - ``cost`` (battery_lowcost): one wide cell, the red stripe down its left edge (lx view);
      - ``capacity`` / ``life``: the same cell with three separate drips (lengths 3, 2, 1)
        hanging from its top edge, WHITE (batterycapacity) or GOLD[3] (batterylife; not GOLD[4],
        which is the quality-trim gold and too close to WHITE for tritan viewers);
      - ``current`` (battery_current): two narrow cells with a gap, a red stripe on each (by view);
      - ``voltage`` (battery_highvoltage): two 5-wide cells touching, the right cell's outline
        forming the dark seam, one red strip beside the seam and two STEEL[4] terminal nubs on
        the top face either side of it (by view).
    The by-view variants show the tray's red '+' plug on the tray front (the item render draws
    both plugs).
Chargers (batterychargera.obj, same model for all three; tier_cue 'plate')
  * ``charger(tier, trim)``: the square wall plate IS the tier plate (as the LED switch):
    ``tier_plate`` front cols 1..12, rows 3..14, depth 2, no bevel, with the model's four
    recessed square battery slots in a 2x2 grid (floor, shaded top jamb, left jamb; no lit lip,
    which fused with the LEDs into light bars) and the four LEDs clustered in the centre
    (dark: drawItem draws them unlit).
  * Quality (STYLE 7b, ``kit.decisions.QUALITY``): weak 50 V = 0 (plain), 50 V = 2 (GOLD trim:
    a gold cap along the plate's top edge).  200 V has no quality entry: plain, told apart by
    its MV plate.
"""
from kit import (Canvas, box, tier_plate, plate_ramp, outline_region, STEEL, CAV, RUBBER_BLACK,
                 COPPER, GOLD, LEAD_RED, WHITE, QUALITY_TRIM, quality)

CELL = RUBBER_BLACK
RED = LEAD_RED          # dark, body, lit
TRAY = STEEL


# --------------------------------------------------------------------------------- batteries
def tray(cv, plug=None):
    """Light steel tray: front cols 1..10, rows 11..14, depth 4 (top face rows 7..10).

    The model's dark rim is ONE solid CAV[1] band on the front (rows 12..13) - no copied checker
    texture (STYLE 3).  ``plug``: None, '+' (by face: red plug) or '-' (fy face: a light minus
    dash on the band, in the tray's own STEEL[4] to stay within 16 colours); the item render
    draws both plugs.
    """
    box(cv, 1, 11, 10, 4, 4, TRAY, bevel=False)
    cv.rect(2, 12, 8, 2, CAV[1])
    if plug == '+':                               # the red '+' plug (by view), centred
        cv.rect(5, 12, 2, 2, RED[1])
        cv.set(5, 12, RED[2])
    elif plug == '-':                             # the '-' plug (fy view): minus dash on the band
        cv.hline(5, 6, 13, TRAY[4])               # (row 13: clear of the light tray rim)


def cell(cv, x, w, top=3, d=2):
    """One RUBBER_BLACK cell standing on the tray one step back: front cols x..x+w-1,
    rows top..10, depth ``d``."""
    return box(cv, x, top, w, 11 - top, d, CELL)


def single_use():
    cv = Canvas(16)
    tray(cv, plug='-')
    cell(cv, 3, 8)                                # front cols 3..10, rows 3..10; side cols 11..12
    for y in (5, 7, 9):                           # copper separators, wrapping onto the side face
        cv.hline(4, 9, y, COPPER[3])
        cv.set(10, y, COPPER[2])
        cv.set(11, y - 1, COPPER[2])              # (col 12 is the cell's outline: kept)
    cv.set(7, 1, RED[2])                          # red terminal nub on the top face
    cv.set(8, 1, RED[1])
    return cv


def _wide_cell(cv):
    cell(cv, 3, 8)                                # lx face: front cols 3..10
    cv.vline(4, 3, 9, RED[1])                     # red stripe down the left edge
    cv.set(4, 3, RED[2])


def cost():
    cv = Canvas(16)
    tray(cv)
    _wide_cell(cv)
    return cv


def drips(cv, ink, tip, hi=None):
    """Three separate drip marks hanging from the cell's top edge, lengths 3, 2, 1
    (batterycapacity / batterylife lx view).  ``tip`` shades the end of the long drip;
    ``hi`` (optional) lights its head."""
    for x, n in ((6, 3), (8, 2), (10, 1)):        # col 5 kept clear of the red stripe
        cv.vline(x, 3, 3 + n - 1, ink)
    cv.set(6, 5, tip)
    if hi is not None:
        cv.set(6, 3, hi)


def capacity():
    cv = Canvas(16)
    tray(cv)
    _wide_cell(cv)
    drips(cv, WHITE, STEEL[4])
    return cv


def life():
    cv = Canvas(16)
    tray(cv)
    _wide_cell(cv)
    drips(cv, GOLD[3], GOLD[2])
    return cv


def current():
    cv = Canvas(16)
    tray(cv, plug='+')
    cell(cv, 2, 4)                                # left cell cols 2..5 (+ side 6..7)
    cell(cv, 8, 4)                                # right cell cols 8..11 (+ side 12..13)
    for x in (4, 10):
        cv.vline(x, 3, 9, RED[1])
        cv.set(x, 3, RED[2])
    return cv


def voltage():
    cv = Canvas(16)
    tray(cv, plug='+')
    cell(cv, 2, 5)                                # left cell: front cols 2..6
    cell(cv, 7, 5)                                # right cell: front cols 7..11 (+ side 12..13);
                                                  # its light outline (col 7) meets the seam
    cv.vline(8, 4, 9, RED[1])                     # red strip beside the seam (by face)
    cv.set(8, 3, RED[2])
    cv.set(5, 2, STEEL[4])                        # two terminal nubs either side of the seam,
    cv.set(9, 2, STEEL[4])                        # on the top face
    return cv


# ---------------------------------------------------------------------------------- chargers
SLOTS = ((3, 5), (8, 5), (3, 10), (8, 10))       # top-left of each 3x3 recess


def charger(tier, trim=None):
    cv = Canvas(16)
    P = plate_ramp(tier)
    tier_plate(cv, 1, 3, 12, 12, 2, tier, bevel=False)    # front cols 1..12, rows 3..14
    for (x, y) in SLOTS:
        cv.rect(x, y, 3, 3, CAV[1])                        # slot floor
        cv.hline(x, x + 2, y, CAV[0])                      # shaded top jamb
        cv.vline(x, y, y + 2, P[1])                        # inner left jamb (body ramp[1])
    cv.rect(6, 8, 2, 2, CAV[3])                            # four unlit LEDs in the centre
    cv.set(6, 8, CAV[4])
    if trim is not None:                                   # gold-plated top edge (quality trim)
        cv.hline(3, 13, 1, trim[1])                        # top face, far row (light outline)
        cv.set(14, 1, trim[0])
        cv.set(2, 2, trim[1])
        cv.hline(3, 12, 2, trim[4])                        # top face, near row
        cv.set(13, 2, trim[2])
    return cv


def charger_for(item_id, tier):
    return charger(tier, QUALITY_TRIM.get(quality(item_id)))
