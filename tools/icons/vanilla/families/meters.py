"""Family helper: meters and displays (manifest family ``meter_display``), 16 px.

Shared visual system
--------------------
* Every member is a wall (or floor) sixnode drawn as a likeness of its model, front-on, with
  the family's ONE depth rule: ``WALL_DEPTH`` = ``depth(cv, 'sixnode')`` = 2, as for every
  shipped sixnode (hub, ground_cable).  ``housing()`` draws the box with no bevel row; the
  identity face sits on its front.  One exception: the floor CRT (``data_logger``) is one
  ``solid()`` of a depth-1 bezel and a depth-3 tube stepping back behind it, so the tube shows
  real top and side faces (review ruling) rather than a plain depth-2 box.
* Untiered.  Five members carry ``tier: signal`` in the manifest, but their ``tier_cue`` is
  null, like the 50-odd other signal-level devices already shipped untiered (signal_processor,
  the timer, the analog chips); so no tier hue appears anywhere in the family.
* Housing materials follow the model textures:
    - WHITE_PLASTIC: the two energy meters (white box), the CRT data logger (beige-white), the
      digital watch (white frame);
    - STEEL: the light-grey wall plates (analog vuMeter, LED vuMeter, analog watch frame), the
      industrial panel and the modern flat-screen bezel (light grey in the model).
* Displays (``screen``): a CAV[1] field with a CAV[0] top shadow row and a lit CAV[3] glint in
  its top-left corner, and one plotted 1 px trace.  The three data loggers are told apart by
  housing shape AND trace ink, each taken from the device: CRT = LIT (the gold text colour,
  emissive, so LIT and not the GOLD quality ramp), industrial = WHITE, modern = MARK_GREEN.
* Counters (``counter``): mechanical digit wheels = a 2-row window (CAV[0] top edge, CAV[1]
  digit row) with white 1 px digits, a dark pixel between wheels; the unit wheel at the right end is MARK_RED,
  as on the model texture.  Shared by both energy meters.
* Seven-segment glyphs (``seg_text``): 3 x 5 digits (a 1 is one column), used by the digital
  watch.
"""
from kit import (Canvas, box, depth, line, STEEL, CAV, WHITE, MARK_RED, MARK_GREEN, MARK_BLUE,
                 WHITE_PLASTIC)

WALL_DEPTH = 2

#: 3x5 seven-segment digits ('1' is a single column, as a lit segment pair reads at 16 px)
SEG = {
    '0': ["###", "#.#", "#.#", "#.#", "###"],
    '1': ["#", "#", "#", "#", "#"],
    '2': ["###", "..#", "###", "#..", "###"],
    '3': ["###", "..#", "###", "..#", "###"],
    '4': ["#.#", "#.#", "###", "..#", "..#"],
    '5': ["###", "#..", "###", "..#", "###"],
    '7': ["###", "..#", "..#", "..#", "..#"],
    '8': ["###", "#.#", "###", "#.#", "###"],
    '9': ["###", "#.#", "###", "..#", "###"],
    ':': [".", "#", ".", "#", "."],
}


def housing(cv, x, y, w, h, ramp, d=WALL_DEPTH):
    """The family's box: front x..x+w-1, y..y+h-1 at depth ``d``, no bevel.  Returns the
    inner front rect (x+1, y+1, w-2, h-2) inside the outline."""
    box(cv, x, y, w, h, d, ramp, bevel=False)
    return (x + 1, y + 1, w - 2, h - 2)


def screen(cv, x, y, w, h, trace=(), ink=WHITE, field=CAV):
    """A display: ``field[1]`` fill, ``field[0]`` top shadow row, ``field[3]`` glint in the
    top-left corner; ``trace`` = a polyline of points drawn 1 px in ``ink``."""
    cv.rect(x, y, w, h, field[1])
    cv.hline(x, x + w - 1, y, field[0])
    cv.set(x, y + 1, field[3])
    for a, b in zip(trace, trace[1:]):
        line(cv, a, b, ink)


def counter(cv, x, y, n, red=1):
    """A row of ``n`` mechanical digit wheels: a window 2n+1 wide x 2 tall from (x, y), a CAV[0]
    top edge over a CAV[1] digit row with a dot every other pixel from x+1; the last ``red``
    are the model's red unit wheels (MARK_RED), the rest WHITE."""
    w = 2 * n + 1
    cv.hline(x, x + w - 1, y, CAV[0])
    cv.hline(x, x + w - 1, y + 1, CAV[1])
    for i in range(n):
        cv.set(x + 1 + 2 * i, y + 1, MARK_RED if i >= n - red else WHITE)


def label(cv, x, y, pattern, ink):
    """One row of printed label ink ('#' = ink), e.g. the ENERGY / TIME legends."""
    for i, ch in enumerate(pattern):
        if ch == '#':
            cv.set(x + i, y, ink)


def seg_text(cv, x, y, s, ink):
    """Seven-segment text from (x, y), 1 px between glyphs.  Returns the width."""
    cx = x
    for ch in s:
        rows = SEG[ch]
        for j, row in enumerate(rows):
            for i, c in enumerate(row):
                if c == '#':
                    cv.set(cx + i, y + j, ink)
        cx += len(rows[0]) + 1
    return cx - x - 1


def seg_width(s):
    return sum(len(SEG[ch][0]) for ch in s) + len(s) - 1


# --------------------------------------------------------------------------------- energy meters
def energy_meter(advanced=False):
    """energymeter.obj / advancedenergymeter.obj: a white box (front 12 x 12, depth 2) whose
    front is a light-grey frame (W[3], 1 px: col 2, col 12, row 3, row 13) round ONE white face
    panel (W[4], cols 3..11 x rows 4..12) on which everything sits, as in the model.

    Counters are 2 rows: a CAV[0] top edge over a CAV[1] digit row of white wheels with the red
    unit wheel at the right.  Legends (ENERGY [kJ], TIME) are short runs of dark ink (W[1]).

    plain:    one counter (rows 5-6), the centred ENERGY legend (row 7), the black power-disk
              window with its banded rim (rows 9-10), the keypad marks (row 12);
    advanced: counter (rows 5-6), right-aligned ENERGY legend (row 7), counter (rows 8-9),
              right-aligned TIME legend (row 10), white gap (row 11), the wide disk slot (row 12);
              the perforated (vented) top, and the quality ladder's GOLD trim (level 2, STYLE 7b)
              as 1 px bands over the full frame rows above and below the panel (cols 2..12).
    The two read apart by structure first (one counter + square disk window vs two counters +
    bottom slot), gold second.
    """
    from kit import GOLD
    cv = Canvas(16)
    W = WHITE_PLASTIC
    housing(cv, 1, 3, 12, 12, W)                      # front cols 1..12, rows 3..14
    cv.rect(3, 4, 9, 9, W[4])                         # face panel cols 3..11, rows 4..12
    ink = W[1]
    if not advanced:
        counter(cv, 4, 5, 3)                          # window cols 4..10; wheels 5, 7, red 9
        cv.hline(5, 9, 7, ink)                        # ENERGY [kJ] (centred)
        cv.rect(5, 9, 5, 2, CAV[1])                   # power-disk window cols 5..9, rows 9..10
        cv.hline(5, 9, 9, CAV[0])
        for xx in (6, 8):
            cv.set(xx, 10, STEEL[4])                  # the spinning disk's banded rim
        for xx in (5, 7, 9):                          # keypad marks
            cv.set(xx, 12, W[2])
    else:
        for yy in (3, 13):                            # quality 2: GOLD trim bands on the frame
            cv.hline(2, 12, yy, GOLD[3])
        counter(cv, 4, 5, 3)                          # ENERGY wheels
        cv.hline(7, 10, 7, ink)                       # ENERGY legend (right-aligned)
        counter(cv, 4, 8, 3)                          # TIME wheels
        cv.hline(8, 10, 10, ink)                      # TIME legend (right-aligned, shorter)
        cv.hline(4, 10, 12, CAV[1])                   # wide disk slot at the bottom
        for xx in (5, 7, 9):
            cv.set(xx, 12, STEEL[4])
        for xx in (5, 7, 9, 11):                      # vents in the perforated top
            cv.set(xx, 2, W[2])
    return cv


# --------------------------------------------------------------------------------- wall plates
#: the analog vuMeter scale inks (vumeter.png: blue at the low end, navy / purple across the
#: top, red down the right, an orange end mark), frozen: MARK_BLUE, DIAL_PURPLE (= the midpoint
#: of MARK_BLUE and MARK_RED, #8f6b8b), MARK_RED, LIT[1].  DIAL_PURPLE is reported for promotion.
from kit import MARK_BLUE as _MB, LIT as _LIT, h as _h
DIAL_PURPLE = _h('#8f6b8b')
DIAL_INKS = {'b': _MB, 'p': DIAL_PURPLE, 'r': MARK_RED, 'o': _LIT[1]}


# 9 x 8 face grid for the vuMeter at (3, 4): a 270-degree scale (radius 4) centred on the pivot
# 'k' at grid (4, 4) = canvas (7, 8), the centre of the 11 x 11 front; 'n' = the Pointer,
# standing straight up from the pivot (mid-scale) and reaching the scale, as in the OBJ.
DIAL = ["...ppp...",
        ".bb.n.rr.",
        ".b..n..r.",
        "b...n...r",
        "b...k...r",
        "b.......r",
        ".b.....r.",
        ".b.....o."]


def analog_gauge():
    """vumeter.obj (analog vuMeter): the thin light-grey square wall plate (STEEL, front 12 x 12,
    depth 2) whose whole face is the dial printed on vumeter.png: a 270-degree scale from blue
    (low, bottom-left) over purple to red with an orange end mark, the dark pivot in the
    centre and the red Pointer standing straight up from it to the scale (mid-scale)."""
    from kit import LEAD_RED
    cv = Canvas(16)
    housing(cv, 1, 3, 12, 12, STEEL)
    inks = dict(DIAL_INKS, k=CAV[1], n=LEAD_RED[0])
    cv.overlay(DIAL, inks, ox=3, oy=4)
    return cv


def led_vumeter():
    """led.obj (LED vuMeter): the light-grey square plate (STEEL, front 12 x 12, depth 2) with
    its darker square recess (cols 3..11 x rows 4..12, a 1 px plate frame on every side) holding
    the single square LED, centred on the recess floor.  The item draw tints the Led part with
    factor 0 = red (0.7, 0, 0) and draws no halo, so the LED is red: LEAD_RED with a lit
    top-left edge and a white glint."""
    from kit import LEAD_RED
    cv = Canvas(16)
    housing(cv, 1, 3, 12, 12, STEEL)
    # recess cols 3..11, rows 4..12: shadowed top/left inner faces, darker floor cols 4..11 x 5..12
    cv.rect(3, 4, 9, 9, STEEL[2])
    cv.hline(3, 11, 4, STEEL[1])
    cv.vline(3, 4, 12, STEEL[1])
    # LED cols 6..9, rows 7..10 (2 px of floor on every side)
    cv.rect(6, 7, 4, 4, LEAD_RED[1])
    cv.hline(6, 9, 7, LEAD_RED[2])
    cv.vline(6, 7, 10, LEAD_RED[2])
    cv.hline(7, 9, 10, LEAD_RED[0])
    cv.vline(9, 8, 10, LEAD_RED[0])
    cv.set(7, 8, WHITE)
    return cv


def analog_watch():
    """wallclock.obj (Analog Watch): the thin square wall plate (STEEL, front 12 x 12, depth 2)
    with the raised, lighter clock face of wallclock.png (lit top/left edge, shaded bottom/right
    edge) and the dark pointed hour/minute hands from the centre (3 o'clock: the minute hand up,
    the shorter hour hand right).  The second hand (cSec) is not drawn by the item."""
    cv = Canvas(16)
    housing(cv, 1, 3, 12, 12, STEEL)
    W = WHITE_PLASTIC
    # face cols 3..11, rows 5..13 (centre (7, 9)): raised pillow
    cv.rect(3, 5, 9, 9, W[3])
    cv.hline(3, 11, 5, W[4])
    cv.vline(3, 5, 13, W[4])
    cv.hline(4, 11, 13, STEEL[2])
    cv.vline(11, 6, 13, STEEL[2])
    for p in ((5, 9), (7, 11)):               # hour marks at 9 and 6 o'clock
        cv.set(*p, STEEL[2])
    hand = CAV[0]
    cv.vline(7, 6, 9, hand)                   # minute hand
    cv.hline(8, 9, 9, hand)                   # hour hand
    return cv


def digital_watch(time='2:11'):
    """digitalwallclock.obj (Digital Watch): the thin wide white panel (WHITE_PLASTIC, front 12
    wide x 9 tall, depth 2) with an even 1 px white frame on all four sides (col 2, col 12,
    row 6, row 13) round the dark glass (cols 3..11 x rows 7..12), showing red seven-segment
    digits.  Four 3 x 5 digits need 15 px and the glass is 9, so the icon shows a 3-digit time
    (9 px, its 1s single columns): the read at GUI 2 is red segments on dark glass in a white
    frame, not the time."""
    cv = Canvas(16)
    W = WHITE_PLASTIC
    housing(cv, 1, 6, 12, 9, W)                # front cols 1..12, rows 6..14
    screen(cv, 3, 7, 9, 6)                      # glass cols 3..11, rows 7..12
    w = seg_width(time)
    assert w <= 9, f'{time!r} is {w} px; the glass is 9'
    seg_text(cv, 3 + (9 - w) // 2, 8, time, MARK_RED)
    return cv


def data_logger():
    """dataloggercrtfloor.obj (Data Logger): the floor-standing beige-white CRT monitor
    (WHITE_PLASTIC) as ONE solid: the bezel box (front 11 x 9, depth 1) and, stepping back
    behind it, the narrower and lower tube housing (inset 1 on every side, depth 3 more), so
    the tube shows a real lit top (ramp[4]) and shaded side (ramp[2]) above and right of the
    bezel.  The big dark screen in a thin bezel, the plotted trace in the logger's gold text
    colour (LIT: emissive)."""
    from kit import LIT, solid, paint_solid
    cv = Canvas(16)
    W = WHITE_PLASTIC
    # tube first: its front plane (true depth 1) lies wholly behind the bezel, which wins ties
    zb = solid([('tube', 3, 6, 9, 7, 3),          # true front cols 2..10 x rows 7..13 at depth 1
                ('bezel', 1, 6, 11, 9, 1)])       # front cols 1..11, rows 6..14
    paint_solid(cv, zb, W, bevel=False)
    screen(cv, 3, 7, 8, 6, trace=[(3, 10), (4, 10), (4, 9), (5, 9), (6, 9), (6, 11), (7, 11),
                                  (7, 8), (8, 8), (9, 8), (9, 10), (10, 10)], ink=LIT[3])
    return cv


def industrial_data_logger():
    """industrialpanel.obj (Industrial Data Logger): the thin grey wall panel (STEEL, front
    12 x 12, depth 2) with the large black screen and the keypad column on the screen's LEFT
    (the 'reflection' glass quad sits inside the +x face, so +x is the front and the rx render
    is the front view: keypad strip at z 0.215..0.356 = screen-left).  The keypad (the
    texture's pad cross, key rows and sliders) is a column of dark key dots; the trace is the
    logger's white text colour, one plotted value per column."""
    cv = Canvas(16)
    housing(cv, 1, 3, 12, 12, STEEL)
    screen(cv, 5, 4, 7, 9, ink=WHITE,             # screen cols 5..11, rows 4..12
           trace=[(5, 9), (6, 9), (6, 8), (7, 8), (8, 8), (8, 9), (9, 9), (9, 10), (10, 10),
                  (11, 10)])
    for y in (5, 7, 9, 11):                       # keypad column (frame col 2, gap col 4)
        cv.set(3, y, CAV[2])
    return cv


def modern_data_logger():
    """flatscreenmonitor.obj (Modern Data Logger): the thin light-grey flat screen (STEEL bezel,
    the model's value; front 11 x 10, depth 2) with a 1 px bezel on all four sides round the
    dark screen and the in-world green step plot.  It is held off the wall on an arm; in
    cabinet oblique the arm runs straight back behind the screen and is hidden, so it (and its
    wall plate) is left out.  Told from the industrial panel by shape (no keypad, smaller) and
    trace ink (green vs white)."""
    cv = Canvas(16)
    housing(cv, 1, 5, 11, 10, STEEL)                     # screen: front cols 1..11, rows 5..14
    screen(cv, 3, 6, 8, 7, trace=[(3, 11), (4, 11), (4, 10), (5, 10), (6, 10), (6, 9), (7, 9),
                                  (7, 8), (8, 8), (9, 8), (9, 7), (10, 7)], ink=MARK_GREEN)
    return cv


#: the field logger's teal paint (cad palette "paint_condenser", 60 120 130)
TEAL = None


def _teal():
    from kit.palette import _r
    return _r('#1c3238', '#2d5258', '#3c7882', '#5a9aa3', '#8cc2c8')


def three_channel_data_logger():
    """fieldlogger (cad FieldLogger: Three-Channel Data Logger): a small teal field logger standing
    on the floor, a carrying handle on top, the dark screen on its front with three traces in the
    channels' colours (A red, B blue, C green, as the in-world graph and the terminals)."""
    from kit import line as _line
    cv = Canvas(16)
    housing(cv, 1, 5, 12, 10, _teal())                  # case: front cols 1..12, rows 5..14
    for x in (4, 9):                                    # handle posts on the top face
        cv.set(x + 1, 3, CAV[1])
    cv.hline(5, 10, 2, CAV[0])                          # the grip
    screen(cv, 3, 7, 8, 6)                              # screen cols 3..10, rows 7..12
    _line(cv, (3, 9), (6, 8), MARK_RED)
    _line(cv, (6, 8), (10, 9), MARK_RED)
    _line(cv, (3, 11), (6, 11), MARK_BLUE)
    _line(cv, (7, 10), (10, 10), MARK_BLUE)
    _line(cv, (3, 12), (10, 11), MARK_GREEN)
    return cv


def tilted_terminal_logger():
    """tiltedterminal (cad TiltedTerminal: Tilted Terminal Logger): a low grey body on a plinth and
    the head leaning back above it, its screen up and toward the viewer with three traces (A red,
    B blue, C green)."""
    from kit import line as _line
    cv = Canvas(16)
    housing(cv, 2, 11, 10, 4, STEEL)                    # body: front cols 2..11, rows 11..14
    box(cv, 2, 4, 10, 7, 3, STEEL, bevel=False)         # head, leaning back (deeper box above)
    screen(cv, 3, 5, 8, 5)                              # screen cols 3..10, rows 5..9
    _line(cv, (3, 7), (6, 6), MARK_RED)
    _line(cv, (6, 6), (10, 7), MARK_RED)
    _line(cv, (3, 8), (10, 8), MARK_BLUE)
    _line(cv, (3, 9), (7, 9), MARK_GREEN)
    _line(cv, (7, 9), (10, 8), MARK_GREEN)
    return cv
