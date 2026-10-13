"""Family helpers: solar panels and thermal devices (job solar_thermal).

Solar (solarpanel2x3 / smallsolarpannel[rot] models).  The cell face is the identity, and the
in-world panel LIES in (or tilts out of) the ground plane, so it is drawn as a thin STEEL-framed
panel in the top plane of cabinet oblique (``pv_panel``): front edge horizontal, cell face
receding up-right.  Face-on (as a sixnode) it read as a window or a monitor.
    frame           dark STEEL outline, then the model's light grey frame just inside it (back
                    row STEEL[4], left edge STEEL[3]) and a 2-row STEEL[3] front rim (edge=2)
    fixed panels    the panel lying on the ground (slope 1); the small one recedes 5 rows at
                    slope 0.8 so its cell field is deep enough to read inside SMALL_BOX
    rotating panels the same panel raised on its STEEL post and stand, tilted up towards the
                    viewer as the item renders it (alphaMin: 2x3 22.5 deg, small 45 deg); post
                    and beams are thin parts with no outline (lit STEEL[3], shaded [2]/[1])
    2x3 vs small    BODY_BOX vs SMALL_BOX (size axis); the 2x3 has a module grid and the
                    model's blue/red terminal box, the small one a plain cell field and its red marker
    tier            manifest tier_cue is null (LV small / MV 2x3 differ by size and model)

Thermal (passivethermaldissipatora, (200v)activethermaldissipatora, fuelheater, stonefurnace).
    heat sink       THERMAL comb seen from its fin ends (``heatsink``): fin ends and dark slots on
                    the front, lit fin edges receding over the top (the lit rim that keeps the
                    dark body visible on a dark background), the light connection square
    active          the same heat sink with the fan on top: a '+' of STEEL blades over the fins
                    (x blade and 45 deg receding blade) and the hub standing through it as a
                    cylinder in the TIER ramp (green 50 V / blue 200 V hub in-world), the tier
                    cue ('band'); 200 V d 6 with its lit cap, small d 5
    stone furnace   STONE block (family ramp, cobblestone grey) whose dark frame edges are
                    THERMAL (the model's dark frame), with the drawer (tiroir) as a thin dark
                    rectangle on the front
    fuel furnace    VCOPPER block with its carrying handle lying along the depth of the top, the
                    LED plate at its front end, and the dark round burner port (THERMAL, two
                    grille rows) centred on the front; no burner, as drawItem renders it
"""
from kit import (Canvas, outline_region, cylinder, disc, box, diag_stroke,
                 tier_ramp, THERMAL, STEEL, SOLAR, VCOPPER, MARK_RED, MARK_BLUE, h)

#: cobblestone grey for the stone heat furnace (no stone ramp in the kit; frozen here, report
#: for promotion).  Neutral, darks drift cool and lights warm; L* 21 35 45 56 66 76.
STONE = (h('#333236'), h('#525155'), h('#6c6b6d'), h('#878684'), h('#a3a19c'), h('#bdbab3'))


# ------------------------------------------------------------------------------ thermal
def heatsink(cv, x, y, w, hgt, d, fins, base_y, R=THERMAL, port=None):
    """A finned heat sink: front plane x..x+w-1, rows y..y+hgt-1, depth d.

    ``fins``: the front columns (absolute) that are fin ends.  The fins run front-to-back, so
    the front shows fin ends (R[3]) with dark slots (R[0]) between them down to ``base_y``
    (first row of the solid base), and the top shows the lit fin edges (R[5]) receding
    up-right with the slot openings (R[1]) between them; the back end of every slot is cut,
    so the far edge is castellated.  ``port``: (px, pw) light connection square at the foot.
    Returns the region (pixel set).
    """
    fins = set(fins)
    reg, col = set(), {}
    xr = x + w - 1
    for k in range(d + 1):
        for yy in range(y - k, y - k + hgt):
            for xx in range(x + k, x + k + w):
                reg.add((xx, yy))
    for (px_, py_) in reg:
        if x <= px_ <= xr and y <= py_ < y + hgt:                    # front face
            c = R[3] if (px_ in fins or py_ >= base_y) else R[0]
        else:
            dx, dy = px_ - xr, y - py_
            if dy > 0 and (dx <= 0 or dy >= dx):                      # top face
                c = R[5] if (px_ - dy) in fins else R[1]
            else:
                c = R[2]                                              # side
        col[(px_, py_)] = c
    reg -= {(s + d, y - d) for s in range(x, xr + 1) if s not in fins}
    for p in reg:
        cv.set(*p, col[p])
    outline_region(cv, reg, R[0], R[1])
    # fin edges are 1 px thin parts: they keep their lit colour over the outline (the lit rim),
    # and so do the slot openings between them, except on the back (castellated) edge
    for (px_, py_) in reg:
        dx, dy = px_ - xr, y - py_
        if not (dy > 0 and (dx <= 0 or dy >= dx)) or px_ - dy == x:
            continue
        if (px_ - dy) in fins:
            cv.set(px_, py_, R[5] if dy < d else R[4])
        elif dy < d:
            cv.set(px_, py_, R[1])
    for yy in range(y + 1, y + hgt - 1):                              # lit inner left rim
        if col.get((x + 1, yy)) == R[3]:
            cv.set(x + 1, yy, R[4])
    if port:
        px, pw = port
        for yy in range(y + hgt - 3, y + hgt - 1):
            for xx in range(px, px + pw):
                cv.set(xx, yy, STEEL[4] if xx == px else STEEL[3])
    return reg


def passive_dissipator():
    """small_passive_thermal_dissipator: the THERMAL comb heat sink in SMALL_BOX
    (front 7 x 7, depth 3) with the light connection square at the foot of the front."""
    cv = Canvas(16)
    heatsink(cv, 3, 8, 7, 7, 3, [3, 4, 6, 8, 9], 12, port=(5, 2))
    return cv


def fan_blades_x(cv, x0, x1, y):
    """The blade lying along x, seen nearly edge-on: a STEEL[3] lit row over a STEEL[1] lower
    edge (a thin part: no outline, §4)."""
    for xx in range(x0, x1 + 1):
        cv.set(xx, y, STEEL[3])
        cv.set(xx, y + 1, STEEL[1])


def fan_blade_z(cv, x, y, n):
    """The blade receding along the depth: a 45 deg stroke from its front end (x, y) up-right,
    STEEL[3] lit with a STEEL[2] edge on its right (``diag_stroke``)."""
    diag_stroke(cv, x, y, n, STEEL[3], STEEL[2], direction=(1, -1))


def active_dissipator(tier, small):
    """The active dissipators: the heat sink (as the passive one) with the fan on top: a '+'
    of STEEL blades lying over the fins (one along x across nearly the whole top, one
    receding at 45 deg) and the hub standing up through it in the tier ramp (the cue, 'band').
    ``small``: SMALL_BOX geometry (front 7 x 6, depth 3, the passive's port, hub d 5, flat top),
    else the full block (front 10 x 8, depth 4, hub d 6 with its lit end cap).
    Draw order: back half of the receding blade, x blade, hub, front half of the receding blade."""
    cv = Canvas(16)
    T = tier_ramp(tier)
    if small:
        heatsink(cv, 3, 9, 7, 6, 3, [3, 4, 6, 8, 9], 12, port=(5, 2))
        bx = (4, 12, 7)                           # x blade: cols, lit row
        zb_back, zb_front = None, (5, 9, 2)       # receding blade halves (back: hidden by the hub)
        hub = (6, 5, 4, 5, None)                  # x, top row, length, diameter, cap
    else:
        heatsink(cv, 1, 7, 10, 8, 4, [1, 2, 4, 5, 7, 8, 10], 12, port=(5, 2))
        bx = (3, 13, 4)
        zb_back, zb_front = (11, 3, 2), (4, 7, 2)
        hub = (5, 1, 6, 6, T)
    if zb_back:
        fan_blade_z(cv, *zb_back)
    fan_blades_x(cv, *bx)
    hx, ht, hl, hd, cap = hub
    cylinder(cv, hx, ht, hl, hd, T, axis='y', cap=cap)
    fan_blade_z(cv, *zb_front)
    return cv


# ------------------------------------------------------------------------------ furnaces
def stone_furnace():
    """stone_heat_furnace: full block (front 10 x 10, depth 4) in STONE, its edges framed in
    THERMAL (the outline is the model's dark frame; on the front a 1 px THERMAL band inside it),
    two cobble specks, and the drawer on the front: a thin THERMAL rectangle."""
    cv = Canvas(16)
    x, y, w, hh, d = 1, 5, 10, 10, 4
    reg = box(cv, x, y, w, hh, d, STONE, outline=False, bevel=False)
    xr, yb = x + w - 1, y + hh - 1
    for (px_, py_) in reg:                                      # front frame band only
        if x <= px_ <= xr and y <= py_ <= yb and (px_ in (x + 1, xr - 1) or py_ in (y + 1, yb - 1)):
            cv.set(px_, py_, THERMAL[3])
    outline_region(cv, reg, THERMAL[0], THERMAL[1])
    # drawer (tiroir): a thin dark rectangle across the middle of the front
    for xx in range(x + 2, xr - 1):
        cv.set(xx, 8, THERMAL[2])
        cv.set(xx, 11, THERMAL[3])
    for yy in range(8, 12):
        cv.set(x + 2, yy, THERMAL[2])
        cv.set(xr - 2, yy, THERMAL[3])
    # cobble specks: one or two per face
    cv.set(4, 7, STONE[5]); cv.set(8, 13, STONE[2]); cv.set(5, 10, STONE[4])
    cv.set(7, 3, STONE[5]); cv.set(4, 2, STONE[3]); cv.set(12, 9, STONE[1])
    return cv


def fuel_furnace():
    """fuel_heat_furnace (fuelheater.obj, no burner): VCOPPER block (front 10 x 10, depth 4),
    the round THERMAL burner port centred on the front (two dark grille rows), and on the top the
    carrying handle lying along the depth with the light LED plate at its front end (LEDs off,
    as the item renders)."""
    cv = Canvas(16)
    x, y, w, hh, d = 1, 5, 10, 10, 4
    box(cv, x, y, w, hh, d, VCOPPER)
    # handle: a low copper bar receding along the top (lit edge left, shade right)
    diag_stroke(cv, 7, 3, 3, VCOPPER[5], VCOPPER[1], direction=(1, -1))
    # LED plate at its front end, facing front: light STEEL with one dark (off) LED
    for xx in range(4, 7):
        cv.set(xx, 3, STEEL[4])
        cv.set(xx, 4, STEEL[2])
    cv.set(4, 4, STEEL[1])
    # burner port
    disc(cv, 2, 6, 8, THERMAL)
    for xx in range(4, 8):
        cv.set(xx, 9, THERMAL[0])
        cv.set(xx, 11, THERMAL[0])
    return cv


# ------------------------------------------------------------------------------ solar
def pv_panel(cv, x, y, w, d, slope=1.0, xlines=(), klines=(), edge=2, frame=True,
             frame_right=False, glint=True):
    """A thin PV panel in cabinet oblique: front edge on row ``y`` (cols x..x+w-1; ``edge`` rows
    of STEEL frame including it), the cell face receding up-right for ``d`` rows, row k
    shifted right by round(k * slope) (1 = lying flat, < 1 = tilted up towards the viewer).
    One STEEL outline round the whole panel; ``frame``: the light grey rim of the model just
    inside it on the back row (STEEL[4]) and the left edge (STEEL[3]) (``frame_right``: also
    the right edge, STEEL[2]); inside that SOLAR[2] cells split by SOLAR[4] grid lines at
    offsets ``xlines`` (along the front edge, so they recede) and rows ``klines``, with a
    SOLAR[5] glint on the first cell pixel of the upper-left cell.
    Returns the region (pixel set)."""
    rows = {}
    for k in range(0, d + 1):
        sh = int(k * slope + 0.5)
        for i in range(w):
            rows[(x + sh + i, y - k)] = (i, k)
    for t in range(1, edge):
        for i in range(w):
            rows[(x + i, y + t)] = (i, -t)
    for p, (i, k) in rows.items():
        if k <= 0:
            c = STEEL[3]
        elif i in xlines or k in klines:
            c = SOLAR[4]
        else:
            c = SOLAR[2]
        cv.set(*p, c)
    reg = set(rows)
    done = outline_region(cv, reg, STEEL[0], STEEL[1])
    if frame:
        inner = [p for p in reg - done if rows[p][1] >= 1]
        for p in inner:
            i, k = rows[p]
            left, right = (p[0] - 1, p[1]), (p[0] + 1, p[1])
            if k == d - 1:
                cv.set(*p, STEEL[4])
            elif left in done:
                cv.set(*p, STEEL[3])
            elif frame_right and right in done:
                cv.set(*p, STEEL[2])
    if glint:
        cells = sorted(p for p in reg - done if cv.get(*p) == SOLAR[2])
        if cells:
            top = min(py for _, py in cells)
            g = min((p for p in cells if p[1] == top), key=lambda p: p[0])
            cv.set(*g, SOLAR[5])
    return reg


def solar_fixed(small):
    """2x3_solar_panel / small_solar_panel: the panel lying on the ground."""
    cv = Canvas(16)
    if small:
        pv_panel(cv, 3, 13, 6, 5, slope=0.8)        # 3 x 3 cell field: grid lines would be noise
        cv.set(7, 14, MARK_RED)                                    # the red marker on the rim
    else:
        pv_panel(cv, 1, 13, 8, 6, xlines=(4,), klines=(3,))
        cv.set(6, 14, MARK_BLUE); cv.set(7, 14, MARK_RED)          # terminal box
    return cv


def solar_rotating(small):
    """2x3_rotating_solar_panel / small_rotating_solar_panel: the panel tilted up on its post.
    The post and the beams are thin parts (§4): no outline, a lit STEEL[3] side over a shaded
    STEEL[2]/[1] side.  2x3: a cross-shaped ground stand (beam along x, beam along the depth)
    with the blue/red terminal box at the front end of the receding beam.  Small: the post on
    a small STEEL base block with its red marker."""
    cv = Canvas(16)
    if small:
        box(cv, 5, 13, 5, 2, 1, STEEL)                             # base block
        for yy in (11, 12):                                        # post
            cv.set(7, yy, STEEL[3]); cv.set(8, yy, STEEL[2])
        cv.set(8, 13, MARK_RED)
        pv_panel(cv, 3, 9, 8, 4, slope=0.5, xlines=(5,), frame_right=True)
    else:
        for yy in range(10, 13):                                   # post
            cv.set(7, yy, STEEL[3]); cv.set(8, yy, STEEL[2])
        fan_blades_x(cv, 2, 13, 12)                                # beam along x
        fan_blade_z(cv, 4, 14, 3)                                  # receding beam, front half
        cv.set(2, 14, MARK_BLUE); cv.set(3, 14, MARK_RED)          # terminal box
        pv_panel(cv, 1, 8, 8, 6, slope=0.8, xlines=(4,), klines=(3,))
    return cv
