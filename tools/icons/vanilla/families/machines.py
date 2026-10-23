"""Family helper: machines (full-block process machines, shaft parts, multiblocks), 16 px.

Shared visual system
--------------------
* Projection: cabinet oblique, depth 4 for a full block (``depth(cv, 'block')``), front plane =
  the model face that carries identity (STYLE 6).  Everything sits on baseline 14 and fills the
  body box (cols 1..14, rows 1..14).
* Parts that sit BEHIND the front plane in the model (the square posts of the press and the
  rolling mill stand in the middle of a full-depth table; the magnetizer pedestal stands in the
  middle of its base plate) are drawn at their real depth with ``zsolid`` - the kit's ``solid`` with a per-part
  depth offset ``z0`` (kit request: ``solid`` takes no z offset).  One material = one zsolid
  union = one outline, as with ``solid``.
* Tier pairs (tier_cue 'body', 50V green / 200V blue): the WHOLE frame is the tier ramp, the
  working part (press head, rollers, coil drum) is RUBBER_BLACK, so the tier is the only hue.
  The 50V and 200V icons are the same drawing with ``tier`` 'lv' / 'mv'.
* One identifying feature per machine, large:
    press (compressor)   open two-post frame, crossbar, the dark press head hanging over the table;
    rolling mill         the two stacked dark rollers filling the frame above the table;
    magnetizer           the hollow octagonal coil drum, end-on, ribbed along its depth;
    macerator            the reference drawing (``families.macerator``), not redrawn here.
* Tier 'plate' machines (egg incubator LV, defence turret HV) stand on the tier slab, as the
  switches and relays do (STYLE 7, ruling wave 1): the all-STEEL incubator cage stands set back
  on its slab (lit ledge in front), the turret's cross-shaped holder is drawn as the slab.
* Shaft system (flywheel, joint, joint hub): STEEL pedestal standing set back on a STEEL base
  plate (lit ledge in front of its foot), STEEL shaft, cowl parts in
  ``MINT`` (the model's cowl.png, #79daa2).  MINT is a family ramp (kit request: promote it; the
  generator and gas turbine use the same cowl).  These items are untiered, but MINT sits close to
  the LV hue; it is kept LIGHT (front = MINT[3], L* 80, against the LV front's L* 61).
* Untiered machines use neutral materials only: furnace (RUBBER_BLACK box, WHITE_PLASTIC lid),
  auto miner (``MINER`` rusty dark grey, family ramp), transporter (WHITE_PLASTIC pod).
"""
from kit import (Canvas, T, h, outline_region, box, cylinder, tier_ramp, plate_ramp, tier_plate,
                 STEEL, CAV, RUBBER_BLACK, WHITE_PLASTIC, GOLD, WHITE, MARK_RED)

# ------------------------------------------------------------------------------- family ramps
def _r(*hx):
    return tuple(h(x) for x in hx)


#: turbinesteam cowl.png (mint, #79daa2 = [3]); kept light so it does not read as the LV tier.
MINT = _r('#1f4136', '#33705a', '#55a483', '#79daa2', '#a8eec4', '#d8f9e4')
#: the auto miner's rusty dark grey (autominercore.png median #514f4e, lit #6e6b6c front).
MINER = _r('#232226', '#3a383c', '#535055', '#6c696a', '#8a8685', '#a6a29e')
#: the coil drum, rollers and press head: dark grey (RUBBER_BLACK) - the working parts.
DARK = RUBBER_BLACK


# ------------------------------------------------------------------------------- z-offset solid
def zsolid(parts):
    """``kit.solid`` with a depth offset per part.

    ``parts``: ``(name, x, y, w, h, d, z0)`` - x, y = the part's front face ON SCREEN (already
    shifted by its depth z0), d = its own depth.  Returns ``{(x, y): (name, face, a, k)}`` with
    a = absolute depth (z0 + k) for occlusion and k = depth within the part (k == 1 is the
    bevel row of a top face).  Nearest absolute depth wins; ties go to the later part.
    """
    zb = {}
    for (name, x, y, w, hgt, d, z0) in parts:
        xr = x + w - 1
        for k in range(d + 1):
            for yy in range(y, y + hgt):
                for xx in range(x, x + w):
                    p = (xx + k, yy - k)
                    if k == 0:
                        face = 'front'
                    else:
                        if x <= p[0] <= xr and y <= p[1] <= y + hgt - 1:
                            continue
                        dx, dy = p[0] - xr, y - p[1]
                        face = 'top' if (dy > 0 and (dx <= 0 or dy >= dx)) else 'side'
                    a = z0 + k
                    old = zb.get(p)
                    if old is None or a <= old[2]:
                        zb[p] = (name, face, a, k)
    return zb


def zpaint(cv, zb, ramp, bevel=True, cut=(), outline=True):
    """Paint a ``zsolid``: top ramp[4], front ramp[3], side ramp[2], bevel ramp[5] on k == 1
    of a top face, one vanilla outline round the union (cut pixels removed first)."""
    for p in cut:
        zb.pop(tuple(p), None)
    for p, (n, f, a, k) in zb.items():
        c = {'front': ramp[3], 'top': ramp[4], 'side': ramp[2]}[f]
        if bevel and f == 'top' and k == 1 and len(ramp) > 5:
            c = ramp[5]
        cv.set(p[0], p[1], c)
    if outline:
        outline_region(cv, set(zb), ramp[0], ramp[1])
    return set(zb)


def zfaces(zb, name=None, face=None, k=None):
    return [p for p, (n, f, a, kk) in zb.items()
            if (name is None or n == name) and (face is None or f == face) and (k is None or kk == k)]


def sub(cv, fn):
    """Draw ``fn(c)`` on its own canvas (own outline) and paste it over ``cv``."""
    c = Canvas(cv.n)
    fn(c)
    cv.paste(c)
    return c


def _oct(x, y, d, c=None):
    """Octagon mask in the box x..x+d-1, y..y+d-1, corners cut by ``c`` (default d//4) px."""
    c = max(1, d // 4) if c is None else c
    out = set()
    for j in range(d):
        for i in range(d):
            if min(i, d - 1 - i) + min(j, d - 1 - j) < c:
                continue
            out.add((x + i, y + j))
    return out


def shade_region(cv, reg, R, cx, cy, lo=-1.5, hi=1.5, face=3):
    """Fill ``reg`` lit top-left (R[4]) / shaded bottom-right (R[2]) about (cx, cy), then outline."""
    for p in reg:
        s = (p[0] + 0.5 - cx) + (p[1] + 0.5 - cy)
        cv.set(*p, R[4] if s < lo else (R[2] if s > hi else R[face]))
    outline_region(cv, set(reg), R[0], R[1])


# ------------------------------------------------------------------------------- press frame
#: posts: square, at mid depth (z0 1, depth 2), at both ends; table: full depth, rows 10..11.
POSTS = [('lp', 2, 3, 2, 11, 2, 1), ('rp', 10, 3, 2, 11, 2, 1)]
TABLE = ('table', 1, 10, 10, 2, 4, 0)
#: rounded post tops (model): chamfer the front-left and back-right top corner of each post
POST_CUT = [(2, 3), (5, 1), (10, 3), (13, 1)]


def compressor(tier):
    """compressora/b.obj (rx view): two square posts joined by a crossbar at the top, the
    full-depth work table across the middle and the dark press head hanging from the crossbar
    above it (the one identifying feature); open below the table."""
    B = tier_ramp(tier)
    cv = Canvas(16)
    zb = zsolid([TABLE] + POSTS + [('bar', 4, 4, 6, 1, 2, 1)])
    zpaint(cv, zb, B, bevel=False, cut=POST_CUT)
    # press head at mid depth: a dark die block on a short stem under the crossbar
    # (small, so open frame shows all round it: tells the press from the macerator's window)
    sub(cv, lambda c: box(c, 6, 7, 3, 2, 1, DARK, bevel=False))
    cv.set(8, 5, DARK[2])
    return cv


def plate_machine(tier):
    """platemachinea/b.obj (rx view): two square posts at the ends, the full-depth table, and
    the pair of large dark rollers stacked between the posts above it."""
    B = tier_ramp(tier)
    cv = Canvas(16)
    zb = zsolid([TABLE] + POSTS)
    zpaint(cv, zb, B, bevel=False, cut=POST_CUT)

    def rollers(c):
        cylinder(c, 4, 6, 6, 4, DARK)
        cylinder(c, 4, 2, 6, 4, DARK)
    sub(cv, rollers)
    return cv


# ------------------------------------------------------------------------------- magnetizer
def magnetizer(tier):
    """magnetizera/b.obj (fy/rx views): the big dark octagonal coil drum (a hollow tube, axis
    front-back, seen end-on, ribbed windings receding up-right) sitting centred on a square
    tier-coloured pedestal, which stands on a small tier base plate barely wider than itself.
    Silhouette: drum over column over a small foot.  The pedestal stands at mid depth (z0 1)
    so the plate's lit top shows round its foot; the tier cue is the pedestal front + plate.

    Layout (16 px): drum d7 front cols 3..9 rows 3..9, depth 2; pedestal front cols 5..8 rows
    9..12 (screen, z0 1); plate front cols 3..9 rows 13..14, depth 3.  The model's pedestal
    is as tall as the drum; at 16 px with the drum's depth it shows 3-4 rows - the most the
    slot allows while the drum stays readable."""
    B = tier_ramp(tier)
    cv = Canvas(16)
    zb = zsolid([('plate', 3, 13, 7, 2, 3, 0), ('ped', 5, 9, 4, 4, 1, 1)])
    zpaint(cv, zb, B, bevel=True)
    x0, y0, d, dep = 3, 3, 7, 2
    front = _oct(x0, y0, d, 2)
    sweep = {}
    for a in range(dep, 0, -1):
        for (px, py) in front:
            sweep[(px + a, py - a)] = a
    for p in front:
        sweep.pop(p, None)
    dr = Canvas(16)
    for (px, py), a in sweep.items():
        top = any((px, yy) in front for yy in range(py + 1, py + 6))
        if a == 1:
            c = DARK[4] if top else DARK[2]                  # first winding, lit on top
        else:
            c = DARK[1]                                      # groove before the next winding
        dr.set(px, py, c)
    cx, cy = x0 + d / 2.0, y0 + d / 2.0
    for p in front:
        s_ = (p[0] + 0.5 - cx) + (p[1] + 0.5 - cy)
        dr.set(*p, DARK[5] if s_ < -2 else (DARK[3] if s_ > 2 else DARK[4]))
    outline_region(dr, set(sweep) | front, DARK[0], DARK[1])
    # the bore (3x3): black, the far inner wall lit along its lower left
    for p in _oct(x0 + 2, y0 + 2, 3, 0):
        dr.set(*p, CAV[0])
    dr.set(x0 + 2, y0 + 4, CAV[2]); dr.set(x0 + 3, y0 + 4, CAV[1])
    cv.paste(dr)
    return cv


# ------------------------------------------------------------------------------- egg incubator
def egg_incubator(tier='lv'):
    """eggincubator.obj: a uniformly light-grey (STEEL) open cage box - big windows in the
    front, top and side - with the golden octagonal tray on its floor and the white
    square-wave heater on the back wall.  Tier cue 'plate': the cage STANDS ON the LV slab
    (wave-1 ruling, as power_switching.base and the turret), set back so a lit ledge of slab
    shows in front of it.

    Layout: slab front cols 1..10 rows 12..14, depth 4; cage front cols 3..11 rows 4..10,
    depth 3, standing two steps back (bottom on the slab's k=2 row)."""
    S = STEEL
    cv = Canvas(16)
    tier_plate(cv, 1, 12, 10, 3, 4, tier, bevel=False)
    cage = Canvas(16)
    box(cage, 3, 4, 9, 7, 3, S, bevel=True)
    # front window: cols 5..10, rows 5..8 - inner left jamb STEEL[1], back wall STEEL[2] with
    # the heater's white vertical strokes, STEEL floor with the golden tray on it
    for yy in range(5, 9):
        cage.set(5, yy, S[1])
        for xx in range(6, 11):
            cage.set(xx, yy, S[2])
    cage.hline(6, 10, 8, S[3])                                   # floor
    for xx in (6, 8, 10):                                        # heater: white bars of
        cage.vline(xx, 5, 6, WHITE)                              # the square wave
    for (x, y, k) in [(7, 7, 4), (8, 7, 3), (9, 7, 3), (7, 8, 2), (8, 8, 2), (9, 8, 1)]:
        cage.set(x, y, GOLD[k])                                  # the tray, lying on the floor
    # top window: a slot in the top face showing the dark interior
    cage.hline(6, 11, 2, S[1])
    # side window: through it the far (left) inner wall, in shade
    for (x, y) in [(12, 5), (12, 6), (13, 4), (13, 5)]:
        cage.set(x, y, S[1])
    cv.paste(cage)
    return cv


# ------------------------------------------------------------------------------- turret
def turret(tier='hv'):
    """turret.obj (fy view): the white bevelled head slab on the black swivel joint, two
    pointed gun barrels out of its left side; the cross-shaped holder is the HV tier slab."""
    W = WHITE_PLASTIC
    cv = Canvas(16)
    tier_plate(cv, 1, 12, 12, 3, 2, tier, bevel=False)
    sub(cv, lambda c: box(c, 6, 11, 4, 2, 2, DARK, bevel=False))

    def head(c):
        box(c, 4, 3, 8, 8, 2, W, bevel=False)
        for yy in range(4, 10):                          # bevelled edges round the raised panel
            c.set(5, yy, W[4])
            c.set(10, yy, W[2])
        for xx in range(5, 11):
            c.set(xx, 4, W[4])
            c.set(xx, 9, W[2])
    sub(cv, head)
    # two separate thin pointed gun barrels (2 rows each, no outline: STYLE 4 thin parts),
    # each tapering to one tip pixel; rows 6..7 stay open so the gap between them shows
    for y in (4, 8):
        cv.hline(1, 3, y, W[4]); cv.set(0, y, W[3])
        cv.hline(2, 3, y + 1, W[2]); cv.set(1, y + 1, W[1])
    return cv


# ------------------------------------------------------------------------------- furnace
def furnace():
    """electricfurnace.obj (lx view front): charcoal box under a light lid slab carrying two
    small buttons, the dark oven window framed in the front, vent slots on the side."""
    cv = Canvas(16)
    box(cv, 2, 7, 9, 8, 4, DARK, bevel=False)
    # vents on the side face: three slots, horizontal in the model -> diagonals here; black
    # slot with a lit lip under it so they read at GUI 2
    for yy in (8, 11):
        for k in (1, 2):
            cv.set(10 + k, yy - k, CAV[0])
            cv.set(10 + k, yy - k + 1, DARK[4])
    # the light lid slab overhangs the charcoal body by one column on the left
    sub(cv, lambda c: box(c, 1, 5, 10, 2, 4, WHITE_PLASTIC, bevel=False))
    cv.set(7, 6, DARK[2]); cv.set(9, 6, DARK[2])                       # buttons
    # oven window: dark glass in a frame, lit glint top-left, grill bars
    for yy in range(8, 13):
        for xx in range(3, 9):
            cv.set(xx, yy, CAV[0])
    for xx in range(3, 9):
        cv.set(xx, 10, CAV[2])
    cv.hline(3, 8, 8, CAV[3]); cv.set(3, 8, CAV[4])
    cv.hline(2, 9, 13, DARK[2])                                       # sill
    return cv


# ------------------------------------------------------------------------------- auto miner
def auto_miner():
    """autominer.obj, simplified (16 px multiblock ruling): the rusty dark-grey hood with a
    sloped shoulder, standing on its legs, the octagonal drill mount on top, the white control
    cabin (mesh door, screen, keys) under the hood between the legs, a red warning stripe on the
    hood's side panel (as in the model) and
    hazard-striped feet (no TERRACOTTA rust: the item is HV, STYLE 5.3)."""
    M = MINER
    cv = Canvas(16)
    zb = zsolid([
        ('hood', 1, 5, 10, 6, 4, 0),
        ('ll', 1, 11, 2, 4, 4, 0),
        ('rl', 9, 11, 2, 4, 4, 0),
        ('boss', 6, 3, 3, 2, 2, 1),
    ])
    cut = [(1, 5), (2, 5), (1, 6), (14, 1)]
    zpaint(cv, zb, M, bevel=False, cut=cut)
    # under the hood between the legs: the white control cabin
    cab = Canvas(16)
    box(cab, 3, 8, 6, 7, 0, WHITE_PLASTIC, bevel=False)
    cv.paste(cab)
    cv.set(4, 9, CAV[2]); cv.set(4, 10, CAV[1])                       # door mesh window
    cv.hline(5, 7, 9, CAV[1]); cv.set(5, 9, CAV[3])                   # screen
    cv.hline(5, 7, 11, WHITE_PLASTIC[2])                              # keyboard
    for (x, y) in [(11, 6), (12, 5), (13, 4)]:                        # warning stripe (hood side)
        cv.set(x, y, MARK_RED)
    cv.set(1, 13, GOLD[3]); cv.set(9, 13, GOLD[3])                    # hazard feet
    return cv


# ------------------------------------------------------------------------------- transporter
def transporter():
    """transporter.obj, simplified (16 px multiblock ruling): the white octagonal capsule pod
    (chamfered corners and roof edge, sliding-door seams, vent grille) with the console monitor
    on its stand in front of its right side."""
    W = WHITE_PLASTIC
    cv = Canvas(16)
    zb = zsolid([('pod', 1, 6, 9, 9, 4, 0)])
    cut = [(1, 6), (2, 6), (1, 7), (13, 2), (12, 2), (13, 3), (5, 2), (6, 2), (4, 3),
           (1, 14), (13, 10)]
    zpaint(cv, zb, W, bevel=False, cut=cut)
    for yy in range(8, 14):                                    # octagon facets on the front
        cv.set(2, yy, W[4])
        cv.set(8, yy, W[2])
    for xx in (4, 7):                                          # sliding-door seams
        cv.vline(xx, 10, 13, W[1])
    cv.hline(4, 6, 8, CAV[2]); cv.set(4, 8, CAV[3])           # vent grille

    def console(c):
        box(c, 11, 7, 4, 4, 0, W, bevel=False)
        c.set(12, 8, CAV[2]); c.set(13, 8, CAV[1]); c.set(12, 9, CAV[1]); c.set(13, 9, CAV[0])
        c.vline(12, 11, 14, STEEL[3]); c.vline(13, 11, 14, STEEL[1])
    sub(cv, console)
    return cv


# ------------------------------------------------------------------------------- shaft parts
def _pedestal(cv, cx, top, bottom=11, pd=3):
    """STEEL trapezoid pedestal centred on col cx (3 wide at ``top``, 5 at ``bottom``) on a
    STEEL base plate (front cols cx-7..cx+3, rows 13..14, depth ``pd``).  The pedestal stands
    on the plate's second depth row (row 11, centre cx at that depth), so a lit ledge of plate
    (row 12) shows in front of its foot."""
    S = STEEL
    sub(cv, lambda c: box(c, cx - 7, 13, 11, 2, pd, S, bevel=True))
    pts = set()
    for yy in range(top, bottom + 1):
        half = 1 + ((yy - top) * 2 + (bottom - top) // 2) // max(1, bottom - top)
        for xx in range(cx - half, cx + half + 1):
            pts.add((xx, yy))
    ped = Canvas(16)
    for p in pts:
        ped.set(*p, S[3])
    for yy in range(top, bottom + 1):
        xs = [x for (x, y) in pts if y == yy]
        ped.set(max(xs) - 1, yy, S[2])
    outline_region(ped, pts, S[0], S[1])
    cv.paste(ped)


def joint():
    """straightjoint.obj (rx view): the STEEL shaft across the slot through the mint bearing
    collar - a hexagonal sleeve about 1.5x wider than tall and 2.5x the shaft - on the grey
    trapezoid pedestal and its base plate."""
    cv = Canvas(16)
    _pedestal(cv, 8, 8)
    sub(cv, lambda c: cylinder(c, 0, 4, 16, 3, STEEL, round_ends=False))

    def collar(c):
        reg = {(x, y) for x in range(5, 12) for y in range(3, 8)} - {(5, 3), (11, 3), (5, 7), (11, 7)}
        for (x, y) in reg:
            c.set(x, y, MINT[4] if y <= 3 else (MINT[2] if y >= 7 else MINT[3]))
        outline_region(c, reg, MINT[0], MINT[1])
        c.set(6, 4, MINT[4])                                 # lit hex facet edge
    sub(cv, collar)
    return cv


def joint_hub():
    """jointhub.obj (fy view): the mint faceted ball hub with its round caps (left, right and
    the one facing us) on the grey trapezoid pedestal and base plate.  Proportions of the
    model: ball (7 x 7) and pedestal (rows 8..11) about equally tall, thin plate below."""
    cv = Canvas(16)
    _pedestal(cv, 8, 8, pd=2)

    def caps(c):
        # 2 visible columns each, unoutlined but for the outer edge, so they stay mint
        for (xo, xi, ko) in ((3, 4, 1), (13, 12, 0)):
            for y in range(3, 6):
                c.set(xo, y, MINT[ko])
                c.set(xi, y, MINT[4] if y == 3 else (MINT[3] if y == 4 else MINT[2]))
            c.set(xi, 2, MINT[1]); c.set(xi, 6, MINT[0])
    sub(cv, caps)

    def ball(c):
        reg = _oct(5, 1, 7, 2)
        cx, cy = 8.5, 4.5
        for (x, y) in reg:
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            if abs(dx) < 1.5 and abs(dy) < 1.5:
                k = 3                                        # front facet
            elif dx + dy < -1.5:
                k = 5 if dx + dy < -3.5 else 4               # upper-left facets
            elif dx + dy > 1.5:
                k = 2
            else:
                k = 3 if dy > 0 or dx > 0 else 4
            c.set(x, y, MINT[k])
        outline_region(c, reg, MINT[0], MINT[1])
        # the front cap: a round boss facing us
        for (x, y, k) in [(8, 3, 5), (7, 4, 4), (8, 4, 4), (9, 4, 3), (8, 5, 2)]:
            c.set(x, y, MINT[k])
    sub(cv, ball)
    return cv


def flywheel():
    """flywheel.obj (fy view): the big wheel - light concrete face ring with a thin dark-green
    inner rim, its dark tyre receding up-right, OPEN between the three green spokes (left,
    up-right, down-right) that run to the mint hub - with the light grey stand in front of it,
    from the hub down to the base plate."""
    cv = Canvas(16)
    sub(cv, lambda c: box(c, 2, 13, 11, 2, 2, STEEL, bevel=False))
    outer = _disc_set(1, 2, 12)
    hole = _disc_set(4, 5, 6)
    tyre = Canvas(16)                                                # tyre ring, 1 px back
    treg = {(x + 1, y - 1) for (x, y) in outer} - {(x + 1, y - 1) for (x, y) in hole}
    tyre.pixels(treg, DARK[2])
    outline_region(tyre, treg, DARK[0], DARK[1])
    cv.paste(tyre)
    wheel = Canvas(16)
    band = outer - hole
    shade_region(wheel, band, STEEL, 7.0, 8.0, lo=-3, hi=3)
    inner_edge = {p for p in band if any(q in hole for q in
                  ((p[0] + 1, p[1]), (p[0] - 1, p[1]), (p[0], p[1] + 1), (p[0], p[1] - 1)))}
    for p in inner_edge:                                             # green inner rim
        wheel.set(*p, MINT[1])
    cv.paste(wheel)
    # the hole stays open (the tyre's inner face shows where it is behind); green spokes
    for p in [(4, 8), (5, 8), (9, 6), (8, 7), (9, 10), (8, 9)]:      # spokes: left, up-right, down-right
        cv.set(*p, MINT[2])

    def stand(c):
        reg = set()
        for y in range(10, 13):
            o = (y - 10 + 1) // 2
            for x in range(6 - o, 9 + o):
                reg.add((x, y))
        for (x, y) in reg:
            c.set(x, y, STEEL[3])
        outline_region(c, reg, STEEL[0], STEEL[1])
    sub(cv, stand)
    # the light mint hub (3x3, too small for an outline: STYLE 4) round the shaft end
    hub = Canvas(16)
    for (x, y, k) in [(6, 7, 4), (7, 7, 4), (8, 7, 3), (6, 8, 3), (8, 8, 2), (6, 9, 3),
                      (7, 9, 2), (8, 9, 1)]:
        hub.set(x, y, MINT[k])
    hub.set(7, 8, STEEL[4])                                          # shaft end
    cv.paste(hub)
    return cv


def _disc_set(x, y, d):
    r = d / 2.0
    cx, cy = x + r, y + r
    lim = r * r + (0.6 if d <= 6 else 0.3 * r)
    return {(xx, yy) for yy in range(y, y + d) for xx in range(x, x + d)
            if (xx + 0.5 - cx) ** 2 + (yy + 0.5 - cy) ** 2 <= lim}


def _disc_px(c, x, y, d, fill, R):
    reg = _disc_set(x, y, d)
    c.pixels(reg, fill)
    outline_region(c, reg, R[0], R[1])
    return reg
