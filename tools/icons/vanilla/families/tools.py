"""Family helper: handheld tools (family ``tool``), 16 px, non-placeable.

Shared visual system
--------------------
Three sub-families, each with ONE shared drawing; the variants differ only in the part that
says what the tool does.

Meters (multimeter = reference, thermometer, universal_meter, wireless_analyzer)
  * The kit's handheld template: ``handheld_body`` in RUBBER_Y (the multimeter reference body),
    LCD window on top and a row of two jacks below (``meter_face``), front-on like the vanilla
    compass.  Every sibling keeps the multimeter's body and face, so the family reads as one
    instrument line.
  * Variants are told apart by their ACCESSORY only (``probe``: 2 px 45-degree strokes with a
    bright steel tip, in the free bottom-right corner, in front of the body):
      - multimeter: red + black probes, crossed (reference);
      - thermometer: one yellow probe with a long steel sensing needle (the in-game texture's
        yellow lead);
      - universal_meter: the multimeter's red + black pair, plus the thermometer's yellow
        probe leaving the top-right corner (multimeter + thermometer in one);
      - wireless_analyzer: no probes; a 2 px black rubber antenna on the top of a shorter
        body (rows 4..13, face without the jack row), the walkie-talkie silhouette, its tip in
        the signal tier ramp (manifest tier 'signal' is a colour hint only; tier_cue is null).

Diagonal tools (small/improved flashlight, portable electrical axe/drill, wrench)
  * Vanilla tool pose: the object lies on the slot diagonal from the bottom-left (grip) to the
    top-right (working end), so its top-left flank is lit and its bottom-right flank shaded.
  * ``diag_bar`` / ``diag_union`` are cylinders rolled onto that diagonal: the cross-section
    bands come from the kit's CYL_BANDS table (indexed by x + y), and one vanilla outline goes
    round the union.  Flat stamped parts (axe bar, wrench) use ``flat_fill`` instead.
  * Flashlights: a RUBBER_BLACK tube with a wider head at the top-right.  The registry icon
    is the OFF state (items/<id>.json is a condition on eln:lit; on_false = item/<id>): unlit
    glass lens with one glint.  ``lit=True`` draws the <id>_on sprite: LIT lens and one solid
    short LIT cone.  Quality (STYLE 7b ladder, QUALITY_TRIM[quality(id)]): small = 0 plain,
    improved = 2 -> a GOLD collar ring at the grip/head shoulder, clear of the lens.
  * Axe and drill share the motor: a round RUBBER_BLACK housing seen end-on (``disc``, d 8
    for the axe, d 7 for the drill) at the bottom-left with a steel spindle boss; the axe
    carries a flat steel chainsaw bar (serrated dark edge with raked teeth on both long edges,
    a CAV chain band inside it), the drill a wide dark steel bit cone (BIT) with a steel chuck,
    slanted COPPER (orange) flute bands and a point at the top-right.
  * Wrench: stamped STEEL open-end spanner, diagonal handle, round head whose jaw slot runs
    along the handle axis and opens to the top-right corner.

Cells (portable battery / capacitor and their packs)
  * Standing RUBBER_BLACK cylinders (``cylinder(axis='y', cap=...)``).  A battery has the
    orange (COPPER) top section and a steel + terminal nub; a capacitor has steel rims at both
    ends (STEEL top cap and a 1-row steel foot rim) and the light polarity stripe of an
    electrolytic can (1 px, so the black can dominates).  Packs are three of the same cells,
    staggered (back cell high-left, front cell low-right, each drawn over the one behind).

Others
  * ore_scanner: a green expansion card (PCB ramp, frozen here: no kit ramp for circuit
    board) with black chips, gold edge fingers and the steel bracket on its left edge.
  * x_ray_scanner: its 3D model (xrayscanner.obj, held as a 3D item) seen from the top
    (the reference sheet's ``top`` view): black head with the white-framed screen showing a
    grey ore picture, the button cross with the orange LED left of it, and the grip running
    out to the left with its yellow stripe.
"""
from kit import (Canvas, handheld_body, diag_stroke, outline_region, cylinder, disc, CYL_BANDS,
                 RUBBER_Y, RUBBER_BLACK, CAV, CHIP, LCD, STEEL, LEAD_RED, LEAD_BLK, GOLD, COPPER,
                 LIT, CABLE, GLASS, tier_ramp, h)

#: yellow probe lead (dark, body, lit) - the thermometer's lead.  Built from GOLD steps so it
#: stays a frozen palette colour; candidate for promotion next to LEAD_RED / LEAD_BLK.
LEAD_YEL = (GOLD[1], GOLD[3], GOLD[4])

#: printed-circuit-board green (expansion card).  derive_ramp(h('#3d8a3a')) hand-tuned:
#: hue drifts cool in the darks, warm in the lights.  Kept darker and more yellow than the
#: LV tier ramp so it is never read as a tier cue (the ore scanner is untiered anyway).
PCB = (h('#12301c'), h('#1d4a24'), h('#2b6a2c'), h('#3d8a35'), h('#62a846'), h('#8cc463'))

#: the meter face (multimeter reference): LCD on top, two jacks below.  Offset by ``dy``.
FACE = [
    "...kkkkkk.......",
    "...kPPPPk.......",
    "...kpkpkk.......",
    "...kkkkkk.......",
    "...cccccc.......",
    "...cxccxc.......",
    "...cccccc.......",
]
FACE_PAL = {'k': CAV[1], 'c': CHIP[3], 'P': LCD[3], 'p': LCD[2], 'x': STEEL[4]}


def meter_face(cv, y=4, rows=None):
    cv.overlay(FACE if rows is None else rows, FACE_PAL, 0, y)


def meter(y=2, h=12):
    """The shared meter body + face; returns the canvas."""
    cv = Canvas(16)
    handheld_body(cv, RUBBER_Y, y=y, h=h)
    meter_face(cv, y + 2, FACE if h >= 12 else FACE[:4] + FACE[5:])
    return cv


def probe(cv, x, y, n, lead, tip_n=2, direction=(1, 1), tip=STEEL):
    """One probe: ``n`` steps of lead (dark, body, lit triple) then ``tip_n`` steps of steel."""
    dx, dy = direction
    diag_stroke(cv, x, y, n, lead[2], lead[1], direction=direction)
    diag_stroke(cv, x + n * dx, y + n * dy, tip_n, tip[4], tip[2], direction=direction)


# ------------------------------------------------------------------------------- diagonal bars
def diag_pixels(x0, y0, n, w, size=16):
    """Pixels of a bar on the 45-degree up-right diagonal.  Its axis starts at (x0, y0) (the
    bottom-left end) and runs ``n`` steps up-right; it is ``w`` diagonal rows thick (rows are
    lines x + y = const; the first row is the top-left flank).  Returns {(x, y): row index}."""
    c = x0 + y0
    k0 = c - (w - 1) // 2
    t0, t1 = x0 - y0, x0 - y0 + 2 * n      # square ends: lines x - y = const are
    out = {}                                # perpendicular to the axis
    for y in range(size):
        for x in range(size):
            k, t = x + y, x - y
            if k0 <= k < k0 + w and t0 <= t <= t1:
                out[(x, y)] = k - k0
    return out


def band_colour(ramp, w, i):
    b = CYL_BANDS[w][i]
    if b == 'H':
        return ramp[5] if len(ramp) > 5 else ramp[4]
    return ramp[b]


def diag_bar(cv, x0, y0, n, w, ramp, outline=True, extra=None):
    """A cylinder lying on the up-right diagonal (see ``diag_pixels``), shaded across its
    width by CYL_BANDS[w] (lit top-left flank, shaded bottom-right flank), vanilla outline.
    ``extra``: more pixels {p: row index} of the same material merged before outlining.
    Returns the pixel set."""
    px = diag_pixels(x0, y0, n, w, cv.n)
    if extra:
        px.update(extra)
    for p, i in px.items():
        cv.set(*p, band_colour(ramp, w, min(max(i, 0), w - 1)))
    if outline:
        outline_region(cv, set(px), ramp[0], ramp[1])
    return set(px)


def shade_diag(cv, pts, ramp, w, k0):
    """Re-shade pixels ``pts`` (already in the silhouette) by their diagonal row x + y - k0."""
    for (x, y) in pts:
        i = min(max(x + y - k0, 0), w - 1)
        cv.set(x, y, band_colour(ramp, w, i))


# ------------------------------------------------------------------------------------- cells
def battery_cell(cv, x, y, length, d=6, top=None):
    """Portable battery: black can, orange (COPPER) top section with its end face showing,
    steel + nub on top (row y-1).  The orange part is the black can recoloured step for step,
    so outline and rounding stay one silhouette."""
    cylinder(cv, x, y, length, d, RUBBER_BLACK, axis='y', cap=RUBBER_BLACK if d >= 6 else None)
    top = max(4, length // 3) if top is None else top
    m = {RUBBER_BLACK[i]: COPPER[i] for i in range(6)}
    for yy in range(y, y + top):
        for xx in range(x, x + d):
            c = cv.get(xx, yy)
            if cv.opaque(xx, yy) and c in m:
                cv.set(xx, yy, m[c])
    cx = x + (d - 2) // 2                           # + terminal nub
    cv.set(cx, y - 1, STEEL[4])
    cv.set(cx + 1, y - 1, STEEL[2])
    return cv


def capacitor_cell(cv, x, y, length, d=6, stripe=True):
    """Portable capacitor: black can with steel rims at both ends (STEEL top cap, a 1-row foot
    rim) and a 1 px CABLE[3] polarity stripe with '-' gaps, so the RUBBER_BLACK body dominates."""
    cylinder(cv, x, y, length, d, RUBBER_BLACK, axis='y', cap=STEEL if d >= 6 else None)
    bands = CYL_BANDS[d]
    # steel foot rim (1 row above the bottom outline row)
    for j, b in enumerate(bands):
        for yy in (y + length - 2,):
            if b not in (0, 1):
                cv.set(x + j, yy, band_colour(STEEL, d, j))
    if stripe:                                      # polarity stripe with '-' marks
        sx = x + 4 if d >= 10 else x + 3
        top = y + 4 if d >= 6 else y + 2
        for yy in range(top, y + length - 2):
            mark = (yy - top) % 3 == 1
            cv.set(sx, yy, RUBBER_BLACK[3] if mark else CABLE[3])
    return cv


def diag_union(cv, parts, ramp, outline=True):
    """Several ``diag_pixels`` sections of ONE material on one shared axis (a tube with a
    wider head, a tapering bit): ``parts`` = [(x0, y0, n, w), ...].  Each section is shaded
    with its own CYL_BANDS[w]; later sections win; one vanilla outline goes round the union.
    Returns {(x, y): (section index, row index, w)}."""
    out = {}
    for si, (x0, y0, n, w) in enumerate(parts):
        for p, i in diag_pixels(x0, y0, n, w, cv.n).items():
            out[p] = (si, i, w)
    for p, (si, i, w) in out.items():
        cv.set(*p, band_colour(ramp, w, i))
    if outline:
        outline_region(cv, set(out), ramp[0], ramp[1])
    return out


def recolour_ramp(cv, pts, src, dst):
    """Recolour pixels ``pts`` step for step from ramp ``src`` to ramp ``dst`` (same length
    or shorter dst: missing steps fall back to dst[-1])."""
    m = {src[i]: dst[min(i, len(dst) - 1)] for i in range(len(src))}
    for p in pts:
        c = cv.get(*p)
        if c in m:
            cv.set(*p, m[c])


def motor(cv, x=1, y=7, d=8):
    """The portable tools' motor: a round black housing seen end-on (``disc``, RUBBER_BLACK)
    at the bottom-left, its steel spindle boss in the middle.  Draw the tool head first, the
    motor over it (the head leaves the housing towards the top-right)."""
    reg = disc(cv, x, y, d, RUBBER_BLACK, face=3)
    c = d // 2
    cv.rect(x + c - 1, y + c - 1, 2, 2, STEEL[2])     # spindle boss
    cv.set(x + c - 1, y + c - 1, STEEL[4])
    return reg


# -------------------------------------------------------------------------------- flashlights
def flashlight(trim=None, lit=False):
    """The flashlight on the slot diagonal: RUBBER_BLACK grip tube (w 5) from the bottom-left,
    wider head (w 7) at the top-right, a tail cap.

    ``lit=False`` (the registry icon = the item's OFF model, items/<id>.json on_false): the lens
    is unlit glass seen edge-on (GLASS[0] inner row, GLASS[1] face, one GLASS[4] glint), no light.
    ``lit=True`` (the ``<id>_on`` sprite): the lens glows LIT and one solid short LIT cone
    (2-3 px) leaves it towards the top-right corner.
    ``trim``: quality ramp (QUALITY_TRIM[quality(id)]) for the collar ring at the grip/head
    shoulder, one RUBBER_BLACK band clear of the lens; None = plain (small flashlight)."""
    cv = Canvas(16)
    parts = [(2, 13, 7, 5), (9, 6, 3, 7)]
    px = diag_union(cv, parts, RUBBER_BLACK)
    tmax = max(x - y for (x, y) in px)
    lens = [p for p in px if p[0] - p[1] >= tmax - 1]
    if lit:
        for (x, y) in lens:
            cv.set(x, y, LIT[4] if x - y == tmax else LIT[3])
        # one solid cone of light: diagonal rows t = tmax+1 .. tmax+3, narrowing outwards
        kmid = sum(x + y for (x, y) in lens) / len(lens)
        for y in range(16):
            for x in range(16):
                t, k = x - y, x + y
                d = t - tmax
                if 1 <= d <= 3 and abs(k - kmid) <= 4 - d and not cv.opaque(x, y):
                    cv.set(x, y, LIT[4] if d == 1 else LIT[3])
    else:
        for (x, y) in lens:
            cv.set(x, y, GLASS[1] if x - y == tmax else GLASS[0])
        top = min(lens, key=lambda p: (p[0] + p[1], -(p[0] - p[1])))
        cv.set(*top, GLASS[4])
    if trim is not None:
        # collar: a ring round the grip's last steps before the head (the shoulder), three
        # diagonal lines (t = tmin-3 .. tmin-1) across the full tube width, outline included,
        # shaded 1 4 3 2 0 across the w-5 tube (the CYL_BANDS row with H -> 4, so the ring's
        # lit row stays gold, not white)
        grip = [p for p, (si, i, w) in px.items() if si == 0]
        hd = [p for p, (si, i, w) in px.items() if si == 1]
        tmin = min(x - y for (x, y) in hd)
        k0 = min(x + y for (x, y) in grip)
        for (x, y) in grip:
            if tmin - 3 <= x - y <= tmin - 1:
                cv.set(x, y, trim[(1, 4, 3, 2, 0)[min(max(x + y - k0, 0), 4)]])
    return cv


# ----------------------------------------------------------------------- axe, drill, wrench
def flat_fill(cv, reg, ramp):
    """Paint a flat (stamped/plate) part: face ramp[3], a lit bevel ramp[4] just inside the
    top-left outline, a shade ramp[2] just inside the bottom-right outline, vanilla outline."""
    reg = set(reg)
    for p in reg:
        cv.set(*p, ramp[3])
    ring = outline_region(cv, reg, ramp[0], ramp[1])
    inner = reg - ring
    for (x, y) in inner:
        if (x - 1, y) in ring or (x, y - 1) in ring:
            if cv.get(x - 1, y) == ramp[1] or cv.get(x, y - 1) == ramp[1]:
                cv.set(x, y, ramp[4])
                continue
        if (x + 1, y) in ring or (x, y + 1) in ring:
            if cv.get(x + 1, y) == ramp[0] or cv.get(x, y + 1) == ramp[0]:
                cv.set(x, y, ramp[2])
    return ring


def axe(w=8, chain=True, depth=2):
    """Portable electrical axe: a flat steel chainsaw bar on the diagonal with a rounded nose at
    the top-right (``w`` 8 diagonal rows).  Its chain: teeth stick out of both long edges on
    alternate diagonal steps (``depth`` 2 = raked towards the nose) before outlining, so the
    toothed edge is the silhouette itself and survives at GUI 2; the dark CAV[0]/CAV[1] edge
    ring takes them, and a 1 px CAV[3]/CAV[2] chain band runs inside it round the STEEL[3]
    bar face (lit STEEL[4] row along its top-left).  It comes out of the round black motor
    housing at the bottom-left.  (A RUBBER_BLACK top handle loop over the housing was tried
    and dropped: with the end-on disc it read as a kettlebell.)"""
    cv = Canvas(16)
    px = diag_pixels(5, 10, 8, w, 16)
    tmax = max(x - y for (x, y) in px)
    for p in [p for p, i in px.items() if p[0] - p[1] >= tmax - 1 and i in (0, w - 1)]:
        del px[p]                                      # rounded nose
    k0, k1 = min(x + y for (x, y) in px), max(x + y for (x, y) in px)
    tmin = min(x - y for (x, y) in px)
    teeth = set()
    for y in range(16):
        for x in range(16):
            k, t = x + y, x - y
            if (k == k0 - 1 or k == k1 + 1) and tmin + 4 <= t <= tmax - 3 \
                    and ((t - tmin) // 2) % 2 == 1:
                teeth.add((x, y))
                if depth == 2:                         # raked forward, towards the nose
                    o = -1 if k == k0 - 1 else 1
                    q = ((k + o + t + 1) // 2, (k + o - t - 1) // 2)
                    if 0 <= q[0] < 16 and 0 <= q[1] < 16:
                        teeth.add(q)
    reg = set(px) | teeth
    ring = flat_fill(cv, reg, STEEL)
    for p in ring:                                     # outer edge + teeth: dark
        cv.set(*p, CAV[1] if cv.get(*p) == STEEL[1] else CAV[0])
    if chain:                                          # the chain: a mid-dark band inside
        inner = reg - ring
        band = {p for p in inner if any(q in ring and q not in teeth for q in
                ((p[0] - 1, p[1]), (p[0] + 1, p[1]), (p[0], p[1] - 1), (p[0], p[1] + 1)))}
        for p in band:
            cv.set(*p, CAV[3] if (p[0] + p[1]) < (k0 + k1) / 2 else CAV[2])
        for (x, y) in inner - band:                    # bar face, lit row along the top-left
            cv.set(x, y, STEEL[4] if (x - 1, y) in band or (x, y - 1) in band else STEEL[3])
    motor(cv)
    return cv


#: the drill bit's dark steel: STEEL steps shifted one darker (a variant tuple, no new colour)
BIT = (STEEL[0], STEEL[1], STEEL[1], STEEL[2], STEEL[3], STEEL[3])
FLUTE = (COPPER[0], COPPER[1], COPPER[2], COPPER[3], COPPER[4], COPPER[4])


def drill(period=4, on=2, motor_d=7, helix=1):
    """Portable electrical mining drill: a substantial bit cone on the diagonal (steel chuck
    w 8, then dark steel w 7 -> 6 -> 5 -> 4 -> 3 -> a 2 px point at the top-right), at least
    as wide as the axe bar for most of its length, with full-width orange (COPPER) flute bands
    (``on`` diagonal lines of every ``period``, slanted by ``helix`` into twist-drill flutes),
    out of the round black motor housing (d 7) at the bottom-left."""
    cv = Canvas(16)
    parts = [(5, 10, 1, 8), (6, 9, 2, 7), (8, 7, 2, 6), (10, 5, 1, 5), (11, 4, 1, 4), (12, 3, 1, 3), (13, 2, 1, 2)]
    px = diag_union(cv, parts, BIT)
    chuck = [p for p, (si, i, w) in px.items() if si == 0]
    recolour_ramp(cv, chuck, BIT, STEEL)
    tc = min(x - y for (x, y) in px if px[(x, y)][0] >= 1)
    flutes = [p for p, (si, i, w) in px.items()
              if si >= 1 and (p[0] - p[1] - tc + helix * (p[0] + p[1])) % period < on]
    recolour_ramp(cv, flutes, BIT, FLUTE)
    motor(cv, 1, 8, motor_d)
    return cv


def wrench(jaw=(15, 17, 6)):
    """Open-end spanner, stamped steel: a diagonal handle (3 rows) from a rounded tail at the
    bottom-left to the round head at the top-right, whose jaw slot runs along the handle axis
    and opens to the top-right corner."""
    cv = Canvas(16)
    reg = set(diag_pixels(3, 12, 5, 3, 16))            # handle axis x + y = 15
    reg |= _disc(7, 1, 8)                              # head cols 7..14, rows 1..8
    reg |= _disc(1, 10, 5)                             # tail cols 1..5, rows 10..14
    # the jaw slot: along the handle axis (x + y = 16 through the head centre (11, 5)),
    # opening to the top-right corner
    reg = {(x, y) for (x, y) in reg if not (jaw[0] <= x + y <= jaw[1] and x - y >= jaw[2])}
    flat_fill(cv, reg, STEEL)
    return cv


def _disc(x, y, d):
    cx, cy, r = x + d / 2.0, y + d / 2.0, d / 2.0
    return {(xx, yy) for yy in range(y, y + d) for xx in range(x, x + d)
            if (xx + 0.5 - cx) ** 2 + (yy + 0.5 - cy) ** 2 <= r * r + 0.5}


# ------------------------------------------------------------------------------ scanners
def ore_scanner():
    """Expansion card front-on: green PCB with a big and a small black chip, two traces,
    gold edge fingers along the bottom edge, the steel bracket on the left edge (its top tab
    folded over to the left)."""
    cv = Canvas(16)
    from kit import box
    # bracket: one steel strip, cols 1..3 rows 2..14 (tapered foot), tab cols 4..5 at the top
    reg = {(x, y) for x in (1, 2, 3) for y in range(2, 14)} | {(2, 14)}
    reg |= {(x, y) for x in (4, 5) for y in (2, 3)}    # the top tab
    flat_fill(cv, reg, STEEL)
    cv.set(2, 8, CAV[1])                               # screw slot
    box(cv, 4, 5, 11, 7, 1, PCB, bevel=True)         # card: front cols 4..14, rows 5..11
    for x in range(6, 14):                             # gold fingers, rows 12..13, gaps between
        if x % 2 == 0:
            cv.set(x, 12, GOLD[3])
            cv.set(x, 13, GOLD[2])
    for p, c in (((5, 7), PCB[4]), ((5, 8), PCB[4]), ((5, 9), PCB[4]), ((12, 10), PCB[4]),
                 ((13, 10), PCB[4])):
        cv.set(*p, c)
    box(cv, 6, 7, 4, 4, 0, CHIP_RAMP, bevel=False)     # big chip cols 6..9, rows 7..10
    cv.set(7, 8, CHIP[5])
    box(cv, 11, 7, 3, 2, 0, CHIP_RAMP, bevel=False)    # small chip cols 11..13, rows 7..8
    return cv


#: chip packages drawn as plain boxes: CHIP outline/top, CHIP[3] face (box face mapping)
CHIP_RAMP = (CHIP[0], CHIP[1], CHIP[2], CHIP[3], CHIP[4], CHIP[5])


def xray_scanner():
    """xrayscanner.obj from above (reference sheet 'top'): black head with the white-framed
    screen (inside a black margin, the model's white dot to its right) showing a grey ore
    picture, the button cross with the orange LED left of it, and
    the narrower grip running out to the left with its yellow stripe."""
    cv = Canvas(16)
    head = {(x, y) for x in range(5, 15) for y in range(2, 14)}
    head -= {(5, 2), (14, 2), (5, 13), (14, 13)}
    grip = {(x, y) for x in range(1, 5) for y in range(6, 11)}
    flat_fill(cv, head | grip, RUBBER_BLACK)
    for y in range(7, 10):                             # yellow stripe across the grip
        cv.set(2, y, RUBBER_Y[3])
    cv.set(2, 6, RUBBER_Y[4])
    cv.set(2, 10, RUBBER_Y[1])
    # screen: white frame cols 8..11, rows 4..11 (tall, as the model's), inside a black
    # margin on the top, right and bottom; grey rock field with lighter ore spots
    for y in range(4, 12):
        for x in range(8, 12):
            edge = x in (8, 11) or y in (4, 11)
            cv.set(x, y, STEEL[4] if edge else STEEL[1])
    ORE = ["12",
           "21",
           "32",
           "22",
           "23",
           "12"]
    cv.overlay(ORE, {'1': STEEL[1], '2': STEEL[2], '3': STEEL[4]}, 9, 5)
    cv.set(13, 7, STEEL[4])                            # the model's white dot right of the screen
    # button cross with the orange LED in the middle
    cv.set(6, 7, STEEL[3]); cv.set(6, 9, STEEL[2]); cv.set(7, 8, STEEL[2])
    cv.set(6, 8, COPPER[4])
    return cv
