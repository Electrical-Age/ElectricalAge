"""Family helper: signal I/O sixnodes (wall-mounted signal sources, sinks and converters), 16 px.

Shared visual system
--------------------
* Every member is a wall sixnode drawn front-on in cabinet oblique, sixnode depth 2, in the
  colours of its own model (MATCH).  Two body shapes:
    - ``wall_plate``: the square module (ledswitch, signalsourcepot, signalsource, redtoele,
      eletored): front 12 x 12 (cols 1..12, rows 3..14), top rows 1..2, side cols 13..14,
      the same plate as ``families/power_switching.led_switch`` so the ledswitch siblings line up;
    - wall boxes (drawn per icon with ``box``): the deeper boxes (electricaltimer 12 x 10 wide, alarmmedium 10 x 12 tall,
      rtu 11 x 12), same depth, centred on the body box.
* Body material: WHITE_PLASTIC for the white models (texture ~#f0f0f0); STEEL for the light grey
  speckled RTU (texture ~#c6c6c6), with two speckle pixels; the signal plate
  (``plate_ramp('signal')``) where the manifest gives the plate cue (signal_switch), and for the
  green signal source (see ``icons/signal_source.py``).
* The identifying feature is printed or mounted on the front face and is drawn large: dial and
  needle, lamp and beacon, recessed button, knob, redstone glyph, torch, square wave.
* Device inks (no tier hue): MARK_RED / MARK_GREEN / MARK_BLUE (LEDs, marks), LEAD_RED (redstone,
  needle, alarm lamp), LIT (the alarm beacon), RUBBER_BLACK (knob, printed glyphs), WOOD (torch).
* Parts that stand out of the face toward the viewer recede DOWN-LEFT in cabinet oblique
  (depth goes up-right): the torch, the alarm beacon.
"""
from kit import (Canvas, box, outline_region, plate_ramp, WHITE_PLASTIC, STEEL, CAV,
                 RUBBER_BLACK, LEAD_RED, LIT, WOOD, MARK_RED, MARK_GREEN, MARK_BLUE)

W = WHITE_PLASTIC
INK = RUBBER_BLACK[0]          # printed black ink (nearest frozen near-black above the L* floor)

#: redstone red, light to dark: lit glint, body, shade/outline (LEAD_RED is the frozen red ramp)
RS_LIT, RS, RS_DARK = LEAD_RED[2], LEAD_RED[1], LEAD_RED[0]


def plate_colours(kind):
    """'white' -> WHITE_PLASTIC, 'signal' -> the signal plate ramp, 'steel' -> STEEL."""
    if kind == 'white':
        return W
    if kind == 'signal':
        return plate_ramp('signal')
    if kind == 'steel':
        return STEEL
    return kind


def wall_plate(cv, kind='white'):
    """The square wall module: front cols 1..12, rows 3..14, depth 2.  Returns its ramp."""
    r = plate_colours(kind)
    box(cv, 1, 3, 12, 12, 2, r, bevel=False)
    return r


def blob(cv, pixels, ramp, lit=None, shade=None):
    """Fill an arbitrary pixel set with ramp[3], lit top-left edge ramp[4] (or ``lit``), vanilla
    outline in ramp[0]/[1].  For knobs and glyph-shaped parts that are not boxes or discs."""
    pixels = set(pixels)
    for p in pixels:
        cv.set(*p, ramp[3])
    edge = outline_region(cv, pixels, ramp[0], ramp[1])
    inner = pixels - edge
    for (x, y) in inner:
        if (x - 1, y) in edge and (x, y - 1) in edge or (x, y - 1) in edge and (x - 1, y) not in inner:
            cv.set(x, y, lit if lit is not None else ramp[4])
    return pixels


# ------------------------------------------------------------------------------ ledswitch family
def led_switch(kind, led, glow=None, pressed=False):
    """ledswitch.obj (signal_button, signal_switch): square wall plate with a recessed square,
    the white push-button cube in it, its LED on the cube face.

    ``kind``: plate material ('white' or 'signal').  ``led``: the LED colour.  ``glow``: the
    'halo' part (a starburst of light round the button, drawn by drawItem in the LED colour) as
    rays on the recess floor, or None.  ``pressed``: the button pushed in (the model's lever
    translation): it sits one depth step back, flush with the recess (no top or side face), and
    the halo spreads round it.  Out: a cube standing 1 px proud, short diagonal rays.
    """
    cv = Canvas(16)
    P = wall_plate(cv, kind)
    # recess cols 3..11, rows 4..12 (as led_switch in power_switching): floor, lit lower/right
    # lip, shaded top/left jambs
    cv.rect(3, 4, 9, 9, P[1])
    cv.hline(4, 11, 12, P[4])
    cv.vline(11, 5, 12, P[4])
    cv.hline(3, 10, 4, P[0])
    cv.vline(3, 4, 11, P[0])
    if pressed:
        if glow is not None:                                  # halo: a lit ring round the button
            cv.rect(5, 5, 6, 6, glow)                         # cols 5..10, rows 5..10
        box(cv, 6, 6, 4, 4, 0, STEEL, bevel=False)            # face cols 6..9, rows 6..9
        cv.set(7, 7, led)
        cv.set(8, 7, led)
        return cv
    if glow is not None:
        # halo: short diagonal rays starting at the cube's four corners (2 px each, the outer
        # pixel on the recess floor corner / lip), so they read as light spreading from the
        # button rather than as corner rivets
        rays = [(5, 6), (4, 5),            # up-left
                (10, 5), (11, 4),          # up-right (beyond the cube's side face)
                (4, 11), (3, 12),          # down-left
                (9, 11), (10, 12)]         # down-right
        for p in rays:
            cv.set(*p, glow)
    # push button: front cols 5..8, rows 7..10, depth 1
    box(cv, 5, 7, 4, 4, 1, STEEL, bevel=False)
    cv.hline(6, 8, 7, W[4])                 # lit top edge of the button face
    cv.rect(6, 8, 2, 2, led)                # the LED, 2 x 2 on the cube face
    return cv


# ------------------------------------------------------------------------------------- alarms
#: the lamp face ('light'): nearly the whole front width (cols 3..11, rows 4..10), as in the
#: model's front view, with 1 px rounded corners
LAMP = [
    ".#######.",
    "#########",
    "#########",
    "#########",
    "#########",
    "#########",
    ".#######.",
]

#: the lit lamp (AlarmMedium_ON is a light salmon red): face LEAD_RED[2], light top/left outline
#: LEAD_RED[1], dark bottom/right outline LEAD_RED[0] (STYLE section 4)
LAMP_RAMP = (RS_DARK, RS, RS_LIT, RS_LIT, RS_LIT)


#: nuclear_alarm's radiation trefoil, printed in black INSIDE the lamp outline (interior cols
#: 4..10, rows 5..9), hub and blades all INK as on the real symbol.  Drawn with one blade up and
#: two down (the 3-fold mark turned 60 deg): at 7 px the standard two-up/one-down layout reads
#: as a face (two eyes, a mouth); this one reads as a trefoil / Y star at GUI 2.
TREFOIL = [
    "..###..",
    "..###..",
    "...#...",
    ".##.##.",
    "##...##",
]


def lamp(cv):
    """The alarm's round lamp, cols 3..11, rows 4..10.  Returns its pixel set."""
    pts = {(3 + x, 4 + y) for y, row in enumerate(LAMP) for x, c in enumerate(row) if c == '#'}
    for p in pts:
        cv.set(*p, LAMP_RAMP[3])
    outline_region(cv, pts, LAMP_RAMP[0], LAMP_RAMP[1])
    return pts


def alarm(mark=None):
    """alarmmedium.obj, drawn in the ON texture (drawItem binds AlarmMedium_ON): white wall box
    taller than wide (front 10 x 12, depth 2), the big salmon-red lamp face ('light') spanning
    nearly the whole front, and the two small orange marks below it.

    ``mark`` None (standard_alarm): the beacon reflector ('rot') stands out of the lamp centre
    toward the viewer, drawn as a lit LIT blade anchored at the centre and receding down-left
    (cabinet oblique), its near end passing over the lamp rim onto the white face.
    ``mark`` 'trefoil' (nuclear_alarm): the two alarms share model and texture, so the nuclear
    one carries a radiation trefoil printed on the lamp, inside its outline (print axis); the
    beacon blade is left off so it does not cross the print."""
    cv = Canvas(16)
    box(cv, 2, 3, 10, 12, 2, W, bevel=False)          # front cols 2..11, rows 3..14
    lamp(cv)
    cv.set(5, 12, RS)                                 # the two orange marks
    cv.set(8, 12, RS)
    if mark == 'trefoil':
        cv.overlay(TREFOIL, {'#': INK}, ox=4, oy=5)
        return cv
    # blade: 3 px per row, one row per depth step, from the centre (7, 7) down-left
    for (y, x0, c) in ((7, 6, LIT[4]), (8, 5, LIT[3]), (9, 4, LIT[3]), (10, 3, LIT[2])):
        cv.hline(x0, x0 + 2, y, c)
    cv.set(7, 7, LIT[5])                              # glint at the mount
    cv.set(3, 10, LIT[1])                             # near end, past the lamp rim
    return cv
