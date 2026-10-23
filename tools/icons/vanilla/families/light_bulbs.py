"""Family helper: light_bulbs - the twelve lamp-socket bulbs (``*_light_bulb``, ``*_led_bulb``,
``*_farming_lamp``; no world model of their own, never placed).

Shared visual system (STYLE.md 6 "components with no world model", 7, 7b, 8 "Lamps and bulbs")
---------------------------------------------------------------------------------------------
Every bulb is one upright drawing, front-on, lit from the top-left, built from three stacked
parts that sit at the SAME rows in every sibling of a size:

=========  ===========  ==========  =======================================================
part       normal rows  small rows  what
=========  ===========  ==========  =======================================================
glass      1..9         5..11       the lamp TYPE, drawn lit
collar     10..12       12..13      the TIER (``tier_cue`` 'band'), 8 px wide
screw      13..14       14          Edison thread crest + foot contact, STEEL (small: foot)
=========  ===========  ==========  =======================================================

* **glass** (lamps are drawn lit, STYLE 7b "state"): the envelope in ``LIT`` (the farming lamp
  in the family ramp ``GROW``), vanilla outline in its own ramp[0]/[1].  Lit glass is drawn one
  step brighter than a solid (it emits): face ramp[4], lit inner rim ramp[5] on the top-left
  arc, shaded inner rim ramp[3] on the bottom-right arc (``envelope``), plus a ramp[5] glint.
  The filament bulbs (incandescent, carbon) use face ramp[3] instead so the white-hot LIT[5]
  filament reads against it.
  The type is told apart by SHAPE (orchestrator ruling):

  ============  ===========================================================================
  incandescent  round A-globe on a narrow neck; white-hot coiled filament on two wires
  carbon        Edison "onion": pointed exhaust tip on top, widest low; hairpin loop
                filament in CARBON (material axis, 7b / 5.3), dark on the lit face - the
                material cue that separates it from the white-hot iron coil (a warmer face
                LIT[2] was tried and dropped: ΔE 6-10 to the HV plate front TIER['hv'][3])
  economic      twin-tube compact fluorescent: an upside-down U of two glowing tubes, each
                shaded as a standing tube
  led           wide, flat frosted dome on a finned STEEL heat-sink neck
  farming       reflector (R-type) grow lamp: wide lens over a cone, in GROW pink
  ============  ===========================================================================

* **collar** (tier): ``tier_ramp(t)``; outline columns ramp[1] | ramp[0], one lit ramp[4]
  column, ramp[3] across the middle, one ramp[2] column (``tier_band(vertical=True)``'s
  mapping, all rows - the glass and the screw close it top and bottom).  The cue is a real
  area of ramp[3]: 4 x 3 = 12 px normal, 4 x 2 = 8 px small.  It is the only hue besides the glass.
* **screw**: STEEL, one thread crest row shaded like a standing cylinder, then the narrower
  foot contact row.

Why the collar and screw are hand-banded, not ``cylinder()`` (STYLE 3): they are short sections
butted against another material (glass above, screw / collar below).  ``cylinder(axis='y')``
turns its first and last rows into full outline rows, which leaves a 3-row collar one row of
cue and a 1-row thread crest nothing but outline.  The bands used are the cylinder's own
left-to-right mapping (outline, lit, face, shade, outline).  Kit request: an option on
``cylinder()`` to leave the end rows open.

Size (axis 7b): ``small_`` is the same drawing fitted into ``SMALL_BOX`` (cols 3..12, rows
5..14): the glass keeps the full 10 px width on its widest rows (as every other family's small_
icon fills SMALL_BOX), the collar stays 8 px wide (2 rows), and the cap's thread crest is
dropped so the glass keeps 7 of the 10 rows; the screw is the 4 px foot contact on row 14.
"""
from kit import Canvas, outline_region, tier_ramp, h, LIT, STEEL, CARBON

#: Lit pink glass of the farming (grow) lamp: the 1.12 icon's pale rose (#ffdfdf) as a lit-glass
#: ladder parallel to LIT (similar lightness steps), hue pushed towards magenta so the large
#: glass area stays clear of the VHV salmon (#db948c).  Family-local; reported for promotion.
GROW = (h('#5e2346'), h('#99406e'), h('#d6699c'), h('#f09cc4'), h('#ffd2e6'), h('#fff3f8'))

#: the white-hot filament and the support wires / dark filament seen through lit glass
HOT = LIT[5]
WIRE = LIT[1]
#: the carbon hairpin (STYLE 7b material axis, 5.3: CARBON is the carbon-filament ramp): one
#: shade, dark against the lit LIT[3] face.  One shade only: a second CARBON step (legs [1], loop
#: [3]) takes the icon to 18 colours (> 16) and is not visible at 16 px anyway.
FIL = CARBON[2]


def spans(d):
    """{row: [(x0, x1), ...]} (inclusive) -> pixel set."""
    return {(x, y) for y, ss in d.items() for (a, b) in ss for x in range(a, b + 1)}


def _tube2(a, b):
    return [(a, a + 3), (b, b + 3)]


#: per size: collar (x, y, w, h), screw spans, glass spans per type, LED heat sink spans
GEOM = {
    False: {
        'collar': (4, 10, 8, 3),
        'screw': {13: [(5, 10)], 14: [(6, 9)]},
        'glass': {
            'incandescent': {1: [(5, 10)], 2: [(4, 11)], 3: [(3, 12)], 4: [(3, 12)],
                             5: [(3, 12)], 6: [(3, 12)], 7: [(4, 11)], 8: [(5, 10)],
                             9: [(5, 10)]},
            'carbon': {1: [(7, 8)], 2: [(6, 9)], 3: [(5, 10)], 4: [(4, 11)], 5: [(4, 11)],
                       6: [(3, 12)], 7: [(3, 12)], 8: [(4, 11)], 9: [(5, 10)]},
            'economic': {1: [(4, 11)], 2: [(3, 12)], **{y: _tube2(3, 9) for y in range(3, 10)}},
            'led': {1: [(5, 10)], 2: [(3, 12)], 3: [(2, 13)], 4: [(2, 13)], 5: [(2, 13)]},
            'farming': {1: [(3, 12)], 2: [(2, 13)], 3: [(2, 13)], 4: [(3, 12)], 5: [(3, 12)],
                        6: [(4, 11)], 7: [(4, 11)], 8: [(5, 10)], 9: [(5, 10)]},
        },
        'sink': {6: [(3, 12)], 7: [(3, 12)], 8: [(4, 11)], 9: [(4, 11)]},
    },
    True: {
        'collar': (4, 12, 8, 2),
        'screw': {14: [(6, 9)]},
        'glass': {
            'incandescent': {5: [(5, 10)], 6: [(4, 11)], 7: [(3, 12)], 8: [(3, 12)],
                             9: [(3, 12)], 10: [(4, 11)], 11: [(5, 10)]},
            'carbon': {5: [(7, 8)], 6: [(6, 9)], 7: [(5, 10)], 8: [(4, 11)], 9: [(3, 12)],
                       10: [(3, 12)], 11: [(5, 10)]},
            'economic': {5: [(4, 11)], 6: [(3, 12)], **{y: _tube2(3, 9) for y in range(7, 12)}},
        },
    },
}

#: hand-placed details per (type, small): [(colour, [(x, y), ...]), ...]
DETAILS = {
    ('incandescent', False): [(WIRE, [(6, 6), (6, 7), (6, 8), (9, 6), (9, 7), (9, 8)]),
                              (HOT, [(6, 5), (7, 5), (8, 5), (9, 5), (7, 4)]),
                              (LIT[5], [(5, 3), (4, 4)])],
    ('incandescent', True): [(WIRE, [(6, 9), (9, 9), (6, 10), (9, 10)]),
                             (HOT, [(6, 8), (7, 8), (8, 8), (9, 8), (7, 7)]),
                             (LIT[5], [(5, 6), (4, 7)])],
    ('carbon', False): [(FIL, [(6, 6), (6, 7), (6, 8), (9, 6), (9, 7), (9, 8), (7, 5), (8, 5)]),
                        (LIT[4], [(6, 3), (5, 4)])],
    ('carbon', True): [(FIL, [(6, 9), (6, 10), (9, 9), (9, 10), (7, 8), (8, 8)])],
    ('led', False): [(LIT[5], [(5, 2), (6, 2), (4, 3)])],
    ('farming', False): [(GROW[5], [(4, 2), (5, 2), (3, 3), (4, 3)])],
    ('economic', False): [],
    ('economic', True): [],
}


#: glass face step per type: the filament bulbs one step down so the white-hot filament reads
FACE = {'incandescent': 3, 'carbon': 3, 'led': 4, 'farming': 4}


# ------------------------------------------------------------------------------------------------
# parts
# ------------------------------------------------------------------------------------------------
def envelope(cv, reg, ramp, face=4):
    """Lit glass of any shape: fill ramp[face], vanilla outline ramp[0]/[1], then a lit inner
    rim (ramp[face+1]) on the top-left arc and a shaded one (ramp[face-1]) on the bottom-right
    arc, split on the diagonal through the shape's centre as ``disc()`` does.  Returns the
    inner (non-outline) pixels."""
    reg = set(reg)
    for p in reg:
        cv.set(*p, ramp[face])
    inner = reg - outline_region(cv, reg, ramp[0], ramp[1])
    xs = [p[0] for p in reg]
    ys = [p[1] for p in reg]
    cx, cy = (min(xs) + max(xs) + 1) / 2.0, (min(ys) + max(ys) + 1) / 2.0
    for p in inner:
        if all(q in inner for q in ((p[0] - 1, p[1]), (p[0] + 1, p[1]),
                                    (p[0], p[1] - 1), (p[0], p[1] + 1))):
            continue
        s = (p[0] + 0.5 - cx) + (p[1] + 0.5 - cy)
        if s < -0.5:
            cv.set(*p, ramp[min(face + 1, len(ramp) - 1)])
        elif s > 0.5:
            cv.set(*p, ramp[face - 1])
    return inner


def tubes(cv, reg, ramp):
    """Twin-tube CFL: one outline for the whole U (one material); inside, each tube is shaded
    as a standing tube (lit column ramp[5], face ramp[4]); the bridge over the top keeps the
    face with a lit top row."""
    reg = set(reg)
    for p in reg:
        cv.set(*p, ramp[4])
    inner = reg - outline_region(cv, reg, ramp[0], ramp[1])
    for (x, y) in inner:
        if (x - 1, y) not in inner and (x, y + 1) in inner or (x, y - 1) not in inner:
            cv.set(x, y, ramp[5])          # lit left column of each tube, lit top of the bridge
    return inner


def collar(cv, x, y, w, hgt, tier):
    """The tier band of the cap (see module doc)."""
    r = tier_ramp(tier)
    for yy in range(y, y + hgt):
        for i in range(w):
            if i == 0:
                k = 1
            elif i == w - 1:
                k = 0
            elif i == w - 2:
                k = 2
            elif i == 1 and w >= 8:
                k = 4
            else:
                k = 3
            cv.set(x + i, yy, r[k])


def screw(cv, sp):
    """Edison cap in STEEL: thread crest row(s) shaded like a standing cylinder; the last row is
    the narrower foot contact."""
    rows = sorted(sp)
    for y in rows:
        for (a, b) in sp[y]:
            w = b - a + 1
            foot = y == rows[-1]
            for i in range(w):
                if i == 0:
                    c = STEEL[1]
                elif i == w - 1:
                    c = STEEL[0]
                elif foot:
                    c = STEEL[2]
                elif i == 1:
                    c = STEEL[4]
                elif i == w - 2:
                    c = STEEL[2]
                else:
                    c = STEEL[3]
                cv.set(a + i, y, c)


def heat_sink(cv, sp):
    """LED neck: STEEL, banded like a standing cylinder (lit left), outlined, with fin grooves
    (single STEEL[1] / STEEL[0] columns, every other column) below the lip row."""
    reg = spans(sp)
    for (x, y) in reg:
        c = STEEL[4] if x <= 4 else STEEL[3] if x <= 9 else STEEL[2]
        cv.set(x, y, c)
    inner = reg - outline_region(cv, reg, STEEL[0], STEEL[1])
    top, bot = min(sp), max(sp)
    for (x, y) in inner:
        if y > top and x in (5, 7, 9):
            cv.set(x, y, STEEL[2] if x < 8 else STEEL[1])
    # the top row sits under the dome's own dark outline, and the bottom row on the collar's
    # own colour: no second (steel) outline on either; the bottom row reads as fin ends
    for yy in (top, bot):
        xs = sorted(x for (x, y) in reg if y == yy)
        for x in xs[1:-1]:
            cv.set(x, yy, STEEL[4] if x <= 4 else STEEL[3] if x <= 9 else STEEL[2])
    return reg


# ------------------------------------------------------------------------------------------------
# the bulb
# ------------------------------------------------------------------------------------------------
def bulb(kind, tier, small=False):
    """``kind``: 'incandescent' | 'carbon' | 'economic' | 'led' | 'farming';
    ``tier``: 'lv' | 'mv'; ``small``: the same drawing fitted into SMALL_BOX (7b size axis)."""
    g = GEOM[bool(small)]
    cv = Canvas(16)
    screw(cv, g['screw'])
    collar(cv, *g['collar'], tier)
    if kind == 'led':
        heat_sink(cv, g['sink'])
    glass = spans(g['glass'][kind])
    if kind == 'economic':
        tubes(cv, glass, LIT)
    else:
        envelope(cv, glass, GROW if kind == 'farming' else LIT, face=FACE[kind])
    for c, pts in DETAILS.get((kind, bool(small)), []):
        cv.pixels(pts, c)
    return cv
