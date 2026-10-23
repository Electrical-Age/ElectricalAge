"""Family helper: brushes, e-coal armour and the two misc placeables, 16 px.

Brushes (16 dyes)
-----------------
One drawing, ``brush(dye)``: a flat paint brush in the vanilla tool pose (handle bottom-left,
working end top-right, 45 degrees), built from three 45-degree bars on the s = x + y lattice
(``lattice``), each shaded across its width and outlined, as one silhouette, in its own ramp:
  * handle  WOOD, 3 lattice lines wide (outline 1 / WOOD[3] / outline 0), down to the baseline;
  * ferrule STEEL, 5 lines wide, darker steps (3 3 2) so it parts a white head from the handle;
  * head    the dye ramp, 7 lines wide and 10 long, flat tip (dark outline), two bristle
            streaks running along it: the only thing that changes between the 16 brushes.
Dye ramps below are hand-tuned from the 26.3 wool texture means (front face = ramp[3]); each dye
keeps its own lightness, so gray/light_gray, green/lime and blue/light_blue stay apart in
lightness as well as hue, and every dark step stays above the L* floor (black reads as the
darkest grey, as vanilla black dye does).

E-coal armour and the placeables: see the functions further down.
"""
from kit import Canvas, h, WOOD, STEEL, CARBON, LIT


def _r(*hexes):
    return tuple(h(x) for x in hexes)


# --------------------------------------------------------------------------------- dye ramps
#: ramp per dye: [0] dark outline, [1] light outline, [2] shade, [3] front, [4] lit, [5] glint.
DYE = {
    'white':      _r('#6e7173', '#9a9d9f', '#bfc3c4', '#dcdfe0', '#f2f4f4', '#ffffff'),
    'light_gray': _r('#3e3e3a', '#5b5a54', '#77766f', '#96968f', '#b5b4ad', '#d0cfca'),
    'gray':       _r('#25282b', '#3a3f43', '#4d5357', '#62696d', '#7d8488', '#979da0'),
    'black':      _r('#202024', '#26262c', '#2c2c33', '#35353d', '#45464e', '#5a5b62'),
    'brown':      _r('#331e11', '#4a2a14', '#63391d', '#7a4c2a', '#946540', '#ab8057'),
    'red':        _r('#441416', '#6a1717', '#8f2220', '#b02e26', '#cf4a39', '#e2765f'),
    'orange':     _r('#6a2a08', '#a2410a', '#cc5a0e', '#f17614', '#ff9a3e', '#ffbe78'),
    'yellow':     _r('#6e4a08', '#a0700c', '#d39c14', '#f9c628', '#ffe063', '#fff1a8'),
    'lime':       _r('#1f4a0a', '#33700e', '#4f9412', '#70b91a', '#95d43c', '#c0ea7a'),
    'green':      _r('#18260a', '#2a3d0e', '#415a14', '#5e7c16', '#7c9a2a', '#9db650'),
    'cyan':       _r('#08343a', '#0b4f57', '#0f6d74', '#158a91', '#33aaa9', '#6ccac4'),
    'light_blue': _r('#0f3c5c', '#155f88', '#2087b4', '#3aafd9', '#6cccec', '#a6e4f7'),
    'blue':       _r('#1c1e50', '#222767', '#2d3488', '#3c44aa', '#5462c8', '#7a88dc'),
    'purple':     _r('#311250', '#43176a', '#5e208d', '#7a2aad', '#9644c6', '#b46ddb'),
    'magenta':    _r('#4a1446', '#74216e', '#9b3392', '#be45b4', '#d966ce', '#ec91e0'),
    'pink':       _r('#7a3a52', '#a65172', '#cc6c8f', '#ee8dac', '#f9adc4', '#ffd0dd'),
}
#: item id prefix -> dye ('silver' is the 1.12 name of light gray)
DYE_OF = {'silver': 'light_gray'}


def vring(pts):
    """Split the boundary of pixel set ``pts`` by the vanilla outline rule: returns (light, dark).
    Open left -> light; otherwise open right or below -> dark; otherwise open above -> light."""
    lt, dk = set(), set()
    for (x, y) in pts:
        L, R, U, D = ((x - 1, y) not in pts, (x + 1, y) not in pts, (x, y - 1) not in pts,
                      (x, y + 1) not in pts)
        if L:
            lt.add((x, y))
        elif R or D:
            dk.add((x, y))
        elif U:
            lt.add((x, y))
    return lt, dk


def lattice(s_of_t, t0, t1):
    """The pixels of a 45-degree bar: lattice lines s = x + y in ``s_of_t(t)`` (a range) for each
    t = x - y in t0..t1.  Returns {(x, y): (s, t)}."""
    out = {}
    for t in range(t0, t1 + 1):
        for s in s_of_t(t):
            if (s + t) % 2 == 0:
                out[((s + t) // 2, (s - t) // 2)] = (s, t)
    return out


def shade_bar(cv, part, ramp, bands, union):
    """Paint a lattice bar: across its width (by s within the part's s range at that t) the
    band table ``bands`` (ramp indices, 'H' = ramp[5]); the silhouette of ``union`` gets the
    vanilla outline in this part's own ramp."""
    lt, dk = vring(union)
    by_t = {}
    for (p, (s, t)) in part.items():
        by_t.setdefault(t, []).append(s)
    for (p, (s, t)) in part.items():
        ss = by_t[t]
        lo, hi = min(ss), max(ss)
        f = 0.0 if hi == lo else (s - lo) / (hi - lo)
        k = bands[min(len(bands) - 1, int(f * len(bands)))]
        c = ramp[5] if k == 'H' else ramp[k]
        if p in lt:
            c = ramp[1]
        elif p in dk:
            c = ramp[0]
        cv.set(p[0], p[1], c)


def _in_box(part):
    return {p: v for p, v in part.items() if 1 <= p[0] <= 14 and 1 <= p[1] <= 14}


def brush(dye):
    dye = DYE_OF.get(dye, dye)
    R = DYE[dye]
    cv = Canvas(16)
    handle = _in_box(lattice(lambda t: range(14, 17), -14, -4))
    ferrule = _in_box(lattice(lambda t: range(13, 18), -3, 0))
    head = _in_box(lattice(lambda t: range(12, 19), 1, 10))
    union = set(handle) | set(ferrule) | set(head)
    shade_bar(cv, handle, WOOD, (3,), union)
    # light heads (light_gray, white) get the darker ferrule so it always parts head from handle
    fb = (2, 2, 1) if dye in ('light_gray', 'white') else (3, 3, 2)
    shade_bar(cv, ferrule, STEEL, fb, union)
    shade_bar(cv, head, R, (4, 'H', 4, 3, 3, 2, 2), union)
    # bristle streaks: two lattice lines of the shade step running along the head
    for (x, y), (s_, t) in head.items():
        if s_ in (14, 16) and 3 <= t <= 8 and cv.get(x, y) not in (R[0], R[1]):
            cv.set(x, y, R[2] if s_ == 16 else R[3])
    return cv


# ---------------------------------------------------------------------------- e-coal armour
# Four original front-on silhouettes in the vanilla armour-item idiom (helmet, chestplate,
# leggings, boots), filling the body box.  Body = CARBON (coal; the worn texture eln:ecoal is a
# flat #2c2c2c).  Shading is the same 'pillow' on every piece: vanilla outline in CARBON[1]/[0],
# the inner rim next to a top/left edge ramp[4] (ramp[5] at top-left corners), next to a
# right/bottom edge ramp[2], the rest ramp[3].  The 'electric' part is the charge bolt: a small
# LIT lightning bolt (LIT[3] with a LIT[5] core pixel) set into the plate at the same structural
# spot on every piece (brow, chest, hips; a 3-pixel vein on each boot shaft, which is too narrow
# for the full bolt).


def mask_of(grid):
    return {(x, y) for y, row in enumerate(grid) for x, ch in enumerate(row) if ch not in '. '}


def pillow(cv, pts, ramp):
    """Paint pixel set ``pts`` front-on with the pillow shading described above."""
    lt, dk = vring(pts)
    edge = lt | dk
    for (x, y) in pts:
        if (x, y) in lt:
            c = ramp[1]
        elif (x, y) in dk:
            c = ramp[0]
        else:
            nl = (x - 1, y) in lt or (x, y - 1) in lt
            nd = (x + 1, y) in dk or (x, y + 1) in dk
            if nl and not nd:
                c = ramp[4]
                if (x - 1, y) in edge and (x, y - 1) in edge and len(ramp) > 5:
                    c = ramp[5]
            elif nd:
                c = ramp[2]
            else:
                c = ramp[3]
        cv.set(x, y, c)


def armour(grid, bolts=(), extra=None):
    """Draw one armour piece from a 16-row ``grid`` ('#' = plate, '.' = empty).  ``bolts`` are
    the LIT vein pixel lists; their first pixel gets the bright core LIT[5]."""
    cv = Canvas(16)
    pts = mask_of(grid)
    pillow(cv, pts, CARBON)
    if extra:
        extra(cv, pts)
    for bolt in bolts:
        for i, (x, y) in enumerate(bolt):
            cv.set(x, y, LIT[5] if i == 0 else LIT[3])
    return cv


#: the family's charge bolt, 4 x 5 ('o' = the bright core pixel, LIT[5]; '#' = LIT[3])
BOLT_ROWS = ("..##",
             ".##.",
             "#o##",
             ".##.",
             "##..")


def pattern(rows, x, y):
    """Pixel list of a small LIT pattern at (x, y), core ('o') pixels first."""
    core = [(x + i, y + j) for j, r in enumerate(rows) for i, ch in enumerate(r) if ch == 'o']
    rest = [(x + i, y + j) for j, r in enumerate(rows) for i, ch in enumerate(r) if ch == '#']
    return core + rest


def BOLT(x, y):
    """The 4 x 5 charge bolt with its top-left corner at (x, y)."""
    return pattern(BOLT_ROWS, x, y)


# ------------------------------------------------------------------------ misc placeables
#: tree resin, the model's fill colour (treeresincolector.png #f1800f / #f0990f): derive_ramp(
#: '#f08a10', at=3), dark steps warmed by hand.  Proposed for promotion (the tree_resin item).
RESIN = _r('#4c1a04', '#7d3000', '#a75000', '#f08a10', '#ffb14a', '#ffdaa6')
#: the tutorial sign's light, drawn with glColor(0, 1, 0) at factor 1 (drawItem): an emissive
#: saturated green, kept away from the LV tier's mint (#8cd6ad) by hue and chroma.
SIGN_GLOW = _r('#0f4a14', '#1f7a1f', '#2fa82a', '#4ad83a', '#8df546', '#e7ffbb')
#: the halo over the steel plate: STEEL[3] mixed 40 % towards SIGN_GLOW[3]
HALO = h('#86bf81')


def octagon(x, y, d, cut):
    """Pixel set of a d x d octagon with corners cut by ``cut`` steps, top-left at (x, y)."""
    pts = set()
    for j in range(d):
        k = max(0, cut - j, j - (d - 1 - cut))
        for i in range(k, d - k):
            pts.add((x + i, y + j))
    return pts


def extrude(front, dp):
    """Cabinet-oblique extrusion of a front pixel set by ``dp`` (up-right).  Returns (top, side):
    the receding pixels above the front (top face) and the rest (side face)."""
    rec = set()
    for k in range(1, dp + 1):
        rec |= {(x + k, y - k) for (x, y) in front}
    rec -= front
    top = set()
    for (x, y) in rec:
        # a receding pixel is 'top' if the front lies straight below it within dp rows
        if any((x, y + i) in front for i in range(1, dp + 1)) and not any(
                (x - i, y) in front for i in range(1, dp + 1)):
            top.add((x, y))
    return top, rec - top


def paint_faces(cv, front, top, side, ramp):
    """Front ramp[3] (pillow rim: ramp[4] inside the lit edge, ramp[2] inside the dark edge is
    left to the caller), top ramp[4], side ramp[2], then the vanilla outline of the union."""
    for p in front:
        cv.set(*p, ramp[3])
    for p in top:
        cv.set(*p, ramp[4])
    for p in side:
        cv.set(*p, ramp[2])
    lt, dk = vring(front | top | side)
    for p in lt:
        cv.set(*p, ramp[1])
    for p in dk:
        cv.set(*p, ramp[0])
    return lt | dk


def resin_collector():
    """tree_resin_collector (treeresincolector.obj 'main' + 'fill'): a small wooden cup hung on
    a log, open at the top (LRDU.Down turns the OBJ's +z opening to world up), tapering from
    0.5 wide at the rim to 0.375 at the bottom.  Cabinet oblique, front 9 wide at the rim
    (cols 1..9, rows 7..14; cols 2..8 below row 10), depth 5 so that looking into the opening
    shows, near to far: the lit front rim, the RESIN surface (three rows, with a glint; one
    shaded wall pixel at the back-left corner) and the back rim.  The model texture's dark seam line is
    one WOOD[2] row on the front."""
    cv = Canvas(16)
    front = set()
    for y in range(7, 15):
        a, b = (1, 9) if y <= 10 else (2, 8)
        front |= {(x, y) for x in range(a, b + 1)}
    dp = 5
    top = {(x + k, 7 - k) for k in range(1, dp + 1) for x in range(1, 10)}
    side = set()
    for k in range(1, dp + 1):
        side |= {(x + k, y - k) for (x, y) in front} - front - top
    paint_faces(cv, front, top, side, WOOD)
    for x in range(3, 9):
        cv.set(x, 12, WOOD[2])                   # the texture's seam line, edge to edge
    # the opening, inside a 1 px rim: k = 1 front rim, 2..4 the resin surface filling it
    # (far rows lit, the row against the front rim darker); one shaded pixel at the back-left
    # corner is all that shows of the inner wall
    for k in range(1, dp):
        y = 7 - k
        for x in range(2 + k, 9 + k):
            if k == 1:
                c = WOOD[4]
            elif k == 2:
                c = RESIN[3]
            else:
                c = RESIN[4]
            cv.set(x, y, c)
    cv.set(6, 3, WOOD[2])                        # inner back-left corner, in shadow
    cv.set(8, 3, RESIN[5])                       # glint on the resin
    return cv


def tutorial_sign():
    """tutorial_sign (tutoplate.obj): a thin octagonal plate (0.876 across, 1/16 thick: depth
    'flat' = 1) with an octagonal light ring (0.625 / 0.5 across) that drawItem renders green
    (factor 1).  The 'halo' plane is an additive 40 % glow: drawn as a green-tinted plate rim
    either side of the ring, since this style has no partial alpha."""
    cv = Canvas(16)
    front = octagon(1, 3, 12, 3)                 # cols 1..12, rows 3..14
    top, side = extrude(front, 1)
    edge = paint_faces(cv, front, top, side, STEEL)
    inner = front - edge
    lt, dk = vring(inner)                        # bevel: the front face is smaller than the back
    for p in lt:
        cv.set(*p, STEEL[4])
    for p in dk:
        cv.set(*p, STEEL[2])
    ring = octagon(3, 5, 8, 2)
    ring -= octagon(4, 6, 6, 1)
    halo = set()
    for (x, y) in ring:
        for q in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if q in inner and q not in ring and q not in lt and q not in dk:
                halo.add(q)
    for p in halo:
        cv.set(*p, HALO)
    for p in ring:
        cv.set(*p, SIGN_GLOW[3])
    for p in ((5, 5), (4, 6), (3, 7)):           # lit top-left arc of the ring
        cv.set(*p, SIGN_GLOW[4])
    return cv
