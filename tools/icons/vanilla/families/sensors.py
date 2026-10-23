"""Family helper: sensors (16 px) - the signal-output sixnode sensors, the shaft tachometer and the
fire buzzer.

Shared visual system
--------------------
None of these items has a tier cue (manifest ``tier_cue = null``): no tier ramp is used anywhere.
Every icon is a likeness of its in-world model, drawn front-on in cabinet oblique (sixnode depth
2) or, for the octagonal domes and discs, flat front-on like a gauge face.  The family is built
from four shared shapes:

* ``wall_plate``  light/daylight/weather sensors: a square sixnode wall plate (front 12 x 11,
  depth 2) in STEEL (light grey models) or WHITE_PLASTIC (the white weather box), with a recessed
  window (``window``: shaded top/left jamb, lit bottom/right lip, as the LED switch recess).
* ``octagon_levels``  fire detector, fire buzzer, entity sensor: the models are octagonal discs
  seen end-on.  ``disc()``/``ring()`` are round, so concentric octagon rings (level 0 = outline,
  1 = rim, 2.. = slots/segments, inner = face) live here (kit request).
* ``probe``       electrical/voltage/thermal/temperature probes: the same black housing (RUBBER_BLACK
  for the electrical and voltage probes, THERMAL for the thermal and temperature probes, STYLE 7b)
  with a lit bevel row on its top face, the grey connector stub and its pale-brass tab, told apart
  by the model's SILHOUETTE (tall column / long thin bar / deep 3:2 box / chamfered nose) and its
  coloured marks (blue + green on the electrical and thermal probes, green only on the voltage and
  temperature probes).
* ``prism``       ``box()`` for a non-rectangular front (temperature probe nose).
* plain ``box``/``solid`` likenesses for the anemometer, scanner and tachometer (its trapezoid
  pedestal hand-banded, see ``tachometer``).

Variant cues (STYLE 7b):
* light vs daylight sensor: same plate; only the window colour differs, as in the models (dark
  slate navy vs bright royal blue).  No printed glyph: the models print none (STYLE 6) and the
  window colour already separates them, in every CVD simulation too.
* fire detector vs fire buzzer: same dome, same geometry.  The detector is drawn idle, as
  ``drawItem`` renders it (LED dark); the buzzer is drawn in alarm state: LED lit and two
  continuous sound arcs in the top-left corner over the rim (the 1.12 texture's arcs), the dome
  1 px smaller and moved 1 px right/down to make room (state axis).

Family-local ramps (reported for promotion):
* ``CUP_RED``  the anemometer cups (model texture #b50000), derive_ramp + hand tune.
* ``SKY_BLUE`` the daylight sensor's royal-blue window, ``NAVY`` the light sensor's dark window.
* ``PALE_CYAN`` the entity sensor's segments (model #adddde).
* The tachometer collar uses the signal tier ramp (TIER['signal']) as a material: see COWL.
"""
from kit import (Canvas, box, solid, paint_solid, outline_region, cylinder, h, STEEL, CAV,
                 RUBBER_BLACK, THERMAL, WHITE_PLASTIC, GOLD, LCD, MARK_BLUE, MARK_GREEN, WHITE,
                 TIER)

# ----------------------------------------------------------------------------- family ramps
#: anemometer cups: model texture #b50000 (derive_ramp(h('#b50000'), at=3), darks cooled by hand)
CUP_RED = (h('#3c0d12'), h('#6e1419'), h('#951a1c'), h('#b82a22'), h('#e0574a'), h('#f59a8a'))
#: daylight sensor window: the model's bright royal blue (#0010c0 .. #2a3ad8), lifted for GUI 2
SKY_BLUE = (h('#141c5c'), h('#1c2a8a'), h('#2438b0'), h('#3352cc'), h('#5a7ce0'), h('#93aef0'))
#: light sensor window: the model's dark navy (#0b345e)
NAVY = (h('#1a2234'), h('#1e2a42'), h('#22344f'), h('#28405f'), h('#345278'), h('#4d6c94'))

#: entity sensor segments: the model's pale cyan (#adddde)
PALE_CYAN = (h('#2c5258'), h('#4a7f86'), h('#6fa9ae'), h('#96cdd0'), h('#bfe8e8'), h('#e4f8f8'))

#: lit green LED (scanner LED_0 #57db51): MARK_GREEN body, bright core
LED_G = (h('#2a6a2a'), MARK_GREEN, h('#a8f0a0'))
#: glint of the fire buzzer's lit LED (body CUP_RED[4])
LED_R_GLINT = h('#ffb0a0')


# ----------------------------------------------------------------------------- wall plates
PLATE = (1, 4, 12, 11, 2)          # front cols 1..12, rows 4..14, depth 2 (top rows 2..3, side 13..14)


def wall_plate(cv, ramp=STEEL, x=None, w=None):
    x0, y, w0, hh, d = PLATE
    return box(cv, x0 if x is None else x, y, w0 if w is None else w, hh, d, ramp, bevel=False)


def window(cv, x, y, w, hh, ramp, glass):
    """Recessed window: jamb on top/left (ramp[1]), lit lip bottom/right (ramp[4]); glass inside.
    ``glass`` = (fill, shade, glint): fill the pane, a shaded top row under the jamb, one glint."""
    cv.rect(x, y, w, hh, glass[0])
    cv.hline(x, x + w - 1, y, ramp[1])
    cv.vline(x, y, y + hh - 1, ramp[1])
    cv.hline(x + 1, x + w - 1, y + hh, ramp[4])
    cv.vline(x + w, y + 1, y + hh, ramp[4])
    cv.hline(x + 1, x + w - 1, y + 1, glass[1])        # shadow of the top jamb on the pane
    cv.set(x + 1, y + 2, glass[2])                     # one glint under the shadow, top-left
    cv.set(x + 2, y + 2, glass[2])


# ----------------------------------------------------------------------------- prisms
def prism(cv, mask, d, ramp, outline=True, bevel=False):
    """Extrude an arbitrary front-face pixel mask ``d`` px up-right (cabinet oblique), the
    generalisation of ``box()`` to non-rectangular fronts (the temperature probe's sloped nose).
    Faces: front ramp[3]; pixels swept by a top edge ramp[4] (a chamfer facing up-left is lit,
    as a top face); by a right edge ramp[2].  Nearer depth wins.  One outline round the union.
    ``bevel``: the depth-1 row swept by a top edge takes ramp[5] (STYLE 3 bevel / lit rim)."""
    mask = set(mask)
    zb = {}
    for k in range(d, 0, -1):
        for (px, py) in mask:
            up = (px, py - 1) not in mask
            right = (px + 1, py) not in mask
            if up or right:
                zb[(px + k, py - k)] = (5 if (bevel and k == 1 and len(ramp) > 5) else 4) if up else 2
    for p in mask:
        zb[p] = 3
    for p, f in zb.items():
        cv.set(*p, ramp[f])
    if outline:
        outline_region(cv, set(zb), ramp[0], ramp[1])
    return set(zb)


# ----------------------------------------------------------------------------- probes
#: the connector stub (model #505050 on the black housing) and its pale-brass tab (#c8b060-ish in
#: the renders): the light GOLD steps only, no orange GOLD[1]
STUB = (CAV[0], CAV[1], CAV[3], CAV[4], STEEL[3])


def probe(x, y, w, hh, d, marks=(), chamfer=0, stub_y=None, stub_len=2, tab=True, ramp=RUBBER_BLACK):
    """Black probe housing: an oblique box (front x..x+w-1, y..y+hh-1, depth d) with the grey
    connector stub leaving its side face to the right (rows stub_y..stub_y+2) ending in a brass tab.
    ``marks``: [(x, y, colour)] 2x2 marks on the front.  ``chamfer``: the temperature probe's sloped
    nose, cut from the front's left corners (top and bottom) before outlining.  ``ramp``:
    RUBBER_BLACK (electrical, voltage) or THERMAL (thermal, temperature; STYLE 7b).  The top face's
    depth-1 row is the ramp[5] bevel, the lit rim that keeps the dark box off a dark slot."""
    cv = Canvas(16)
    sy = (y + hh // 2 - 1 - d // 2) if stub_y is None else stub_y
    sx = x + w + d                                     # first column right of the side face
    # stub: a small box (depth 1) behind the housing's side, then the brass tab at its end
    if stub_len > 0:
        box(cv, sx - 1, sy, stub_len + 1, 3, 1, STUB, bevel=False)
        if tab:
            tx = sx + stub_len
            cv.vline(tx, sy - 1, sy + 2, GOLD[3])
            cv.set(tx, sy - 1, GOLD[4])
            cv.set(tx, sy + 2, GOLD[2])
            cv.vline(tx + 1, sy - 1, sy + 2, GOLD[2])
            cv.set(tx + 1, sy - 1, GOLD[3])
    mask = {(xx, yy) for xx in range(x, x + w) for yy in range(y, y + hh)}
    for k in range(chamfer):                       # sloped nose: cut both left corners
        for j in range(chamfer - k):
            mask -= {(x + k, y + j), (x + k, y + hh - 1 - j)}
    prism(cv, mask, d, ramp, bevel=True)
    for (mx, my, col) in marks:
        cv.rect(mx, my, 2, 2, col)
    return cv


# ----------------------------------------------------------------------------- light sensors
WIN = (3, 5, 8, 7)                 # pane cols 3..10, rows 5..11; lip col 11 / row 12


def light_sensor(daylight):
    """lightsensor.obj / daylightsensor.obj: grey plate, recessed window.  The two models differ
    only in the window colour (light: dark slate navy, daylight: bright royal blue), so that is
    the only difference here; neither model prints a symbol (STYLE 6)."""
    cv = Canvas(16)
    wall_plate(cv, STEEL)
    x, y, w, hh = WIN
    if daylight:
        window(cv, x, y, w, hh, STEEL, (SKY_BLUE[3], SKY_BLUE[2], SKY_BLUE[5]))
    else:
        window(cv, x, y, w, hh, STEEL, (NAVY[3], NAVY[1], NAVY[5]))
    return cv


# ----------------------------------------------------------------------------- weather sensor
#: the model's dark-green cells (#164700) and the lit yellow ones
CELL = LCD[0]
CELL_Y = LCD[2]


def weather_sensor():
    """electricalweathersensor.obj: white square wall box, its face a grid of dark-green cells, a
    couple of them lit yellow.  The model's 4 x 4 grid is drawn 3 x 3 with 2 px cells at pitch 3
    (1 px cells at pitch 2 read as dithering at GUI 2; 4 x 4 at 2 px needs 11 rows, the face has
    10).  The front is 11 wide (cols 2..12, interior 3..12) so the 8 px grid (cols 4..11, rows
    5..12) has a 1 px margin on every side."""
    cv = Canvas(16)
    wall_plate(cv, WHITE_PLASTIC, x=2, w=11)
    yellow = {(1, 1), (2, 0)}
    for j in range(3):
        for i in range(3):
            cv.rect(4 + 3 * i, 5 + 3 * j, 2, 2, CELL_Y if (i, j) in yellow else CELL)
    return cv


# ----------------------------------------------------------------------------- domes (octagons)
def octagon_levels(x, y, d, c=None):
    """{(px, py): level} for the octagon (x, y, d, c): level 0 is the boundary ring, 1 the ring
    inside it, ... - concentric octagons with the same 45-degree diagonals (one staircase per level)."""
    c = round(d * 0.3) if c is None else c
    lv = {}
    for j in range(d):
        for i in range(d):
            ii, jj = min(i, d - 1 - i), min(j, d - 1 - j)
            k = min(ii, jj, ii + jj - c)
            if k >= 0:
                lv[(x + i, y + j)] = k
    return lv


def _side(p, cx, cy):
    """-1 top-left half, +1 bottom-right half, 0 on the diagonal between them."""
    s = (p[0] + 0.5 - cx) + (p[1] + 0.5 - cy)
    return -1 if s < -0.5 else (1 if s > 0.5 else 0)


def fire_dome(cv, x, y, d, lit, slot_w=2):
    """firedetector.obj seen end-on: white octagonal dome - outline (level 0), lit/shaded rim (1),
    the ring of dark side slots (``slot_w`` levels; their top-left inner faces catch the light),
    the flat white face with the LED above-right of centre.  ``lit``: the LED on (alarm) or off
    (idle, as drawItem renders it: the red dimmed to half)."""
    W = WHITE_PLASTIC
    lv = octagon_levels(x, y, d)
    cx, cy = x + d / 2.0, y + d / 2.0
    for p, k in lv.items():
        sd = _side(p, cx, cy)
        if k == 1:
            c = W[4] if sd < 0 else (W[2] if sd > 0 else W[3])
        elif 2 <= k < 2 + slot_w:
            c = CAV[3] if sd < 0 else (CAV[0] if sd > 0 else CAV[2])
            if slot_w > 1 and k == 2 and sd > 0:
                c = CAV[1]
        else:
            c = W[3]
        cv.set(*p, c)
    outline_region(cv, set(lv), W[0], W[1])
    lx, ly = int(cx), int(cy) - 2
    if lit:
        cv.rect(lx, ly, 2, 2, CUP_RED[4])
        cv.set(lx, ly, LED_R_GLINT)
    else:
        cv.rect(lx, ly, 2, 2, CUP_RED[1])
        cv.set(lx, ly, CUP_RED[2])
    return cv


def fire_detector():
    cv = Canvas(16)
    return fire_dome(cv, 1, 1, 14, lit=False)


#: the buzzer's two sound arcs, hand-placed and continuous (8-connected): the outer one in the
#: corner the octagon's chamfer leaves free, the inner one painted over the dome's lit rim (the
#: 1.12 electricalfirebuzzer texture draws its arcs over the dome the same way)
ARC_OUT = [(1, 7), (1, 6), (1, 5), (2, 4), (3, 3), (4, 2), (5, 1), (6, 1), (7, 1)]
ARC_IN = [(3, 9), (3, 8), (3, 7), (4, 6), (5, 5), (6, 4), (7, 3), (8, 3), (9, 3)]


def fire_buzzer():
    """firedetector.obj in alarm state: the detector's dome 1 px smaller, moved 1 px right/down,
    its LED lit, two sound arcs in the top-left corner."""
    cv = Canvas(16)
    fire_dome(cv, 2, 2, 13, lit=True)
    for p in ARC_OUT:
        cv.set(*p, CUP_RED[3])
    for p in ARC_IN:
        cv.set(*p, CUP_RED[4])
    return cv


def entity_sensor():
    """proximitysensor.obj end-on: grey octagonal rim, a ring of eight pale-cyan segments with grey
    gaps between them, and the flat light-grey octagonal lens inset in the centre (a mid-grey
    bevel ring, light face, one white glint)."""
    import math
    cv = Canvas(16)
    lv = octagon_levels(1, 1, 14)
    lens = octagon_levels(5, 5, 6, c=2)
    cx = cy = 8.0
    for p, k in lv.items():
        sd = _side(p, cx, cy)
        if k >= 4 and p in lens:
            if lens[p] == 0:
                c = STEEL[2]
            else:
                c = STEEL[4]
        elif k == 1:
            c = STEEL[4] if sd < 0 else (STEEL[2] if sd > 0 else STEEL[3])
        else:                                   # segment ring (levels 2, 3 and the lens corners)
            c = PALE_CYAN[4] if sd < 0 else (PALE_CYAN[2] if sd > 0 else PALE_CYAN[3])
            a = math.degrees(math.atan2(p[1] + 0.5 - cy, p[0] + 0.5 - cx)) % 45.0
            if abs(a - 22.5) < 7.5:
                c = STEEL[2] if sd <= 0 else STEEL[1]
        cv.set(*p, c)
    cv.set(6, 6, WHITE)
    cv.set(7, 6, WHITE)
    outline_region(cv, set(lv), STEEL[0], STEEL[1])
    return cv


# ----------------------------------------------------------------------------- anemometer
def cup(cv, x, y):
    """One red anemometer cup (a hemisphere), 3 x 3."""
    R = CUP_RED
    cv.overlay(["ab.", "bcd", ".dd"], {'a': R[5], 'b': R[4], 'c': R[3], 'd': R[1]}, x, y)


def anemometer():
    """rx view: the light-grey wall plate front-on with the arm seen end-on (a dark socket with a
    lit lower lip), the mast up from it to the rotor above the plate: three red cups on their
    spokes across the top of the slot.  (The fy view - plate edge-on, arm, mast - read as a hook.)"""
    cv = Canvas(16)
    box(cv, 2, 8, 9, 7, 2, STEEL, bevel=False)          # wall plate, rows 8..14 (top 6..7)
    cv.rect(5, 10, 3, 2, CAV[2])                         # the arm end-on
    cv.hline(5, 7, 12, STEEL[4])
    cv.vline(6, 4, 9, STEEL[4])                          # mast: lit left, shaded right
    cv.vline(7, 4, 9, STEEL[1])
    cv.hline(2, 13, 3, STEEL[4])                         # spokes (lit top, dark under)
    cv.hline(2, 13, 4, STEEL[1])
    cup(cv, 1, 2)
    cup(cv, 12, 2)
    cup(cv, 6, 1)
    return cv


# ----------------------------------------------------------------------------- scanner
def scanner():
    """scanner.obj rx: a thin horizontal arm from the left (depth 1), widening in one 1 px step
    into the tall straight T-head bar (front cols 10..12, rows 3..14, depth 2).  One RUBBER_BLACK
    solid.  The arm/flare are kept shallow (depth 1) and the junction low enough that the head's
    left edge runs straight for 3 rows between its top chamfer and the flare: a deeper flare
    continued the head's oblique chamfer into one long diagonal that read as a megaphone.  The item
    shows ScanMode 0: only LED_0, the upper one, is drawn, lit, on the flare next to the head,
    where the model has its LEDs."""
    cv = Canvas(16)
    zb = solid([('arm', 1, 8, 7, 2, 1), ('flare', 8, 7, 2, 4, 1), ('head', 10, 3, 3, 12, 2)])
    paint_solid(cv, zb, RUBBER_BLACK, bevel=False)
    cv.rect(8, 8, 2, 2, LED_G[1])
    cv.set(8, 8, LED_G[2])
    return cv


# ----------------------------------------------------------------------------- tachometer
#: the pale-green collar: the signal tier ramp (the device outputs a signal; the cowl texture's
#: mint is closer to the LV green, which would read as an LV band on a signal device)
COWL = TIER['signal']


def tachometer():
    """turbinesteam/tachometer.obj rx: the shaft across the top through the pale-green collar, the
    solid trapezoid pedestal (narrow under the collar, wide at the foot) on the thin base slab
    (front 2 rows, top 1 row) with the black/yellow connector blocks at its ends.  The pedestal's
    top is hidden under the collar, so only its sloped right face shows: a 1 px STEEL[2] band
    parallel to the stepped right edge (a strict 45-degree sweep of a widening trapezoid hides that
    face behind its own foot and leaves a notch).  Pedestal and slab are one STEEL union with one
    outline."""
    cv = Canvas(16)
    reg = box(cv, 2, 13, 10, 2, 1, STEEL, outline=False, bevel=False)
    for yy, x0, x1 in ((8, 5, 8), (9, 5, 8), (10, 4, 9), (11, 4, 9)):
        cv.hline(x0, x1, yy, STEEL[3])
        cv.hline(x1 + 1, x1 + 2, yy, STEEL[2])
        reg |= {(xx, yy) for xx in range(x0, x1 + 3)}
    outline_region(cv, reg, STEEL[0], STEEL[1])
    for bx in (3, 10):                           # black/yellow connector blocks on the base
        cv.set(bx, 13, RUBBER_BLACK[2])
        cv.set(bx, 12, GOLD[3])
    cylinder(cv, 0, 3, 16, 3, STEEL)
    box(cv, 4, 3, 6, 5, 2, COWL, bevel=False)
    return cv


# ----------------------------------------------------------------------------- the four probes
B, G = MARK_BLUE, MARK_GREEN


def electrical_probe():
    """Tall column: front cols 3..8, rows 4..14; stub on the side, blue top, green bottom."""
    return probe(3, 4, 6, 11, 2, marks=[(5, 5, B), (5, 12, G)], stub_y=7)


def voltage_probe():
    """Long thin bar: front cols 1..9, rows 9..14, depth 2; green at the left end, stub at the
    right end."""
    return probe(1, 9, 9, 6, 2, marks=[(2, 11, G)], stub_y=9, stub_len=1)


def thermal_probe():
    """Deep box, front wider than tall: front cols 1..9, rows 8..14 (9 x 7; the model's 3:2 face,
    1 row taller so the two marks keep a 2-row gap), depth 3 (the model is nearly cubic in depth,
    the voltage sensor thin).  THERMAL housing.  Blue mark centred on the top edge, green centred
    on the bottom edge (as rx), stub centred on the side face."""
    return probe(1, 8, 9, 7, 3, marks=[(5, 8, B), (5, 12, G)], stub_len=1, ramp=THERMAL)


def temperature_probe():
    """Chamfered nose: front cols 1..9, rows 7..14; green beside the nose.  THERMAL housing."""
    return probe(1, 7, 9, 8, 2, marks=[(5, 9, G)], chamfer=3, stub_y=7, stub_len=1, ramp=THERMAL)
