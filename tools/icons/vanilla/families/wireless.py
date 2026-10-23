"""Family helper: wireless (power antennas, wireless signal sixnodes, wireless button/switch), 16 px.

Shared visual system (revision 2, after review)
-----------------------------------------------
No floating schematic waves: every cue is part of the model or a print on it (STYLE 6 MATCH,
7b print axis).

* Power antennas (``antenna(tier, mode)``; lowpower{receiver,transmitter}antenna.obj, shared by
  the LV/MV/HV items).  Model: a square back block (0.625 wide, 0.19 thick), a short neck and a
  large shallow 12-facet dish (1.01 across, 0.34 deep) opening away from the block.  Drawn side-on
  with the dish axis along +x, the rim leaning into a 3/4 view (``DISH`` is a raster of the OBJ,
  cleaned by hand) so the light concave inside is the identity face; the copper feed spot of the
  texture sits at its centre (COPPER[2], kept off the HV hue).  The back block is the tier cue
  (``plate``): an oblique box in ``plate_ramp(t)`` at the left, front 6 x 8 (ramp[3] over a real
  area), top and side face (ramp[2]) receding behind the dish.  rx and tx share the model; the
  texture tells them apart only by small connector letters on the block (rx S/P, tx J/P/S), so
  the block front carries the print-axis mark: FONT3 'R' (receiver) or 'T' (transmitter) in the
  block's own ramp[0].  Block colour = LV / MV / HV, letter = rx / tx.
* Wireless signal sixnodes (wirelesssignal{tx,rx,repeater}.obj): the white square wall plate
  (WHITE_PLASTIC; untiered, the manifest gives them no tier cue) front-on, the STYLE sixnode
  box: front 12 x 11 (cols 1..12, rows 4..14), depth 2.  The dark rod(s) (RUBBER_BLACK: the
  model's rods are dark grey, close to the black tip cube) stand straight out of the plate towards
  the viewer, so in cabinet oblique they run down-left at 45 deg and end in the black tip cube at
  the plate's left edge (the identity feature).  The plate print follows the model's front view:
    - transmitter: one rod + the black slot mark;
    - receiver: one rod + the black-framed LED window, LED red (drawItem: connection=false);
    - repeater: two parallel rods (upper and lower left, 2 x 2 tips with a 2 px gap) + the
      empty window (its LED is never drawn).
* Wireless button / switch (ledswitch.obj: the model of signal_button / signal_switch, drawn by
  the very same ElectricalGateSourceRenderObj, so in the world they are identical to them).  The
  geometry is ``families/signal_io.led_switch`` (copied, not imported, because of one change):
  white square plate front 12 x 12 (cols 1..12, rows 3..14), the recessed square, the STEEL
  push-button cube with its LED, the halo (red rays at rest, a lit ring when pressed).  The one
  deviation: the recess is 8 x 8 (cols 3..10, rows 5..12) instead of 9 x 9, which is closer to
  the model's proportions anyway (recess about 2/3 of the plate), and frees a 2 px frame band at
  the top and right.  That band carries the print-axis wireless mark (``WAVE``): one dark
  radio-wave arc round the button, concave towards it.  So the wireless items differ from their
  signal siblings only by the print (and wireless_switch also by its white plate).
  wireless_button = the resting state (cube out, LED and rays red, as drawItem draws it);
  wireless_switch = latched on (cube pressed flush, LED and halo ring green), as signal_switch,
  so the state difference is geometric (rays vs ring) and survives colour blindness.  Both stay
  on the white plate (tier_cue null).
"""
from kit import (Canvas, box, outline_region, plate_ramp, text, FONT3, STEEL, CAV, RUBBER_BLACK,
                 WHITE_PLASTIC, WHITE, COPPER)

WP = WHITE_PLASTIC
S = STEEL
INK = RUBBER_BLACK[0]          # printed black ink (as families/signal_io)


# ===================================================================================== antennas
#: The dish, rasterised from the OBJ (rim r 0.505 at x 0.086, ring r 0.384 at x -0.069, centre at
#: x -0.25; 11 px per block, dish axis along +x, rim leaning 0.5 for a 3/4 view into the bowl) and
#: cleaned by hand.  Keys: o outer edge (becomes the outline), w lip (the rim's near edge, lit),
#: d inside, upper-left wall in the lip's shadow, m inside mid, l inside, lower-right wall facing
#: the light, c the copper feed spot (texture) at the centre, b the cone's outside below the lip.
DISH = [
    "................",
    "................",
    "........oo......",
    ".......owdo.....",
    "......owddmo....",
    "......owdmmo....",
    ".....owdmmlo....",
    ".....owdmllo....",
    ".....owmcllo....",
    ".....owmlllo....",
    ".....owllllo....",
    ".....obwlllo....",
    ".....obwllo.....",
    "......obwlo.....",
    ".......ooo......",
    "................",
]


def dish(cv, grid=DISH, ox=0):
    """Paint the dish grid shifted right by ``ox`` and outline it (STEEL, vanilla rule)."""
    pal = {'o': S[3], 'w': WHITE, 'd': S[2], 'm': S[3], 'l': S[4], 'c': COPPER[2], 'b': S[2]}
    reg = set()
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch in '. ':
                continue
            reg.add((x + ox, y))
            cv.set(x + ox, y, pal[ch])
    outline_region(cv, reg, S[0], S[1])
    return reg


#: back block: front cols 1..BW, rows 6..13, depth 3 (top rows 3..5, side cols BW+1..BW+3)
BW = 6


def antenna(tier, mode):
    """Power antenna; ``mode`` 'tx' or 'rx'.  See the module doc."""
    cv = Canvas(16)
    P = plate_ramp(tier)
    box(cv, 1, 6, BW, 8, 3, P, bevel=False)
    text(cv, 3, 7, 'T' if mode == 'tx' else 'R', P[0], font=FONT3)   # cols 3..5, rows 7..11
    dish(cv, ox=BW - 3)                                               # dish cols 8..14
    return cv


# ============================================================================ signal sixnodes
def wall_plate(cv):
    """White wall plate: front cols 1..12, rows 4..14, depth 2 (top rows 2..3, side cols 13..14)."""
    return box(cv, 1, 4, 12, 11, 2, WP, bevel=False)


def rod(cv, x, y, n=4, tip=3):
    """Antenna rod standing out of the plate at plate point (x, y), towards the viewer: ``n``
    steps down-left at 45 deg (a 1 px dark rod: lit upper edge, shaded underside), ending in the
    black tip cube (``tip`` px, depth 1)."""
    R = RUBBER_BLACK
    for i in range(n):
        cv.set(x - i, y + i, R[4])
        cv.set(x - i, y + i + 1, R[1])
    ex, ey = x - n + 1, y + n - 1                       # last rod pixel
    box(cv, ex - tip, ey, tip, tip, 1, RUBBER_BLACK, bevel=False)


def led_window(cv, x, y, led=None):
    """Black-framed square LED window, 4x4 at (x, y); ``led`` fills its 2x2 centre."""
    for i in range(4):
        cv.set(x + i, y, CAV[1]); cv.set(x + i, y + 3, CAV[1])
        cv.set(x, y + i, CAV[1]); cv.set(x + 3, y + i, CAV[1])
    c = led if led is not None else WP[3]
    cv.rect(x + 1, y + 1, 2, 2, c)


def signal_sixnode(kind, led=None):
    """``kind`` 'tx', 'rx' (``led`` = the LED colour) or 'repeater'.  See the module doc."""
    cv = Canvas(16)
    wall_plate(cv)
    if kind == 'tx':
        cv.hline(7, 10, 9, CAV[1])                      # the slot mark
        cv.hline(7, 10, 10, CAV[2])
        rod(cv, 6, 7)
    elif kind == 'rx':
        led_window(cv, 7, 7, led)
        rod(cv, 6, 7)
    else:
        led_window(cv, 7, 7)
        rod(cv, 6, 5, 4, tip=2)
        rod(cv, 6, 10, 4, tip=2)
    return cv


# ============================================================================== button / switch
#: the printed wireless mark: one radio-wave arc (a quarter circle, concave towards the button) in
#: the frame band round the recess's top-right corner.  It keeps off row 4 above col 9 so it does
#: not merge with the recess's dark top jamb (row 5).
WAVE = [(7, 3), (8, 3), (9, 3), (10, 4), (11, 4), (11, 5), (12, 6), (12, 7), (12, 8)]


def led_switch(led, glow=None, pressed=False, mark=True):
    """ledswitch.obj on the white plate (signal_io.led_switch geometry with an 8 x 8 recess).

    ``led``: the LED colour; ``glow``: the halo colour or None; ``pressed``: the button pushed in
    flush with the halo as a lit ring round it, otherwise the cube standing 1 px proud with short
    diagonal rays at the recess corners; ``mark``: the printed wireless mark."""
    cv = Canvas(16)
    P = WP
    box(cv, 1, 3, 12, 12, 2, P, bevel=False)          # front cols 1..12, rows 3..14
    # recess cols 3..10, rows 5..12: floor, lit lower/right lip, shaded top/left jambs
    cv.rect(3, 5, 8, 8, P[1])
    cv.hline(4, 10, 12, P[4])
    cv.vline(10, 6, 12, P[4])
    cv.hline(3, 9, 5, P[0])
    cv.vline(3, 5, 11, P[0])
    if mark:
        cv.pixels(WAVE, INK)
    # the floor is cols 4..9, rows 6..11
    if pressed:
        if glow is not None:                          # halo: a lit ring round the button
            cv.rect(4, 6, 6, 6, glow)
        box(cv, 5, 7, 4, 4, 0, STEEL, bevel=False)    # face cols 5..8, rows 7..10, flush
        cv.set(6, 8, led)
        cv.set(7, 8, led)
        return cv
    if glow is not None:
        for p in [(4, 6), (9, 6), (4, 11), (9, 11)]:  # the halo's rays at the floor corners
            cv.set(*p, glow)
    box(cv, 4, 7, 4, 4, 1, STEEL, bevel=False)        # cube: front cols 4..7, rows 7..10, out by 1
    cv.hline(5, 7, 7, WP[4])                          # lit top edge of the button face
    cv.set(5, 8, led)                                 # the LED, 2 px on the cube face
    cv.set(6, 8, led)
    return cv
