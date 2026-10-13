"""Round and linear primitives: cylinders, discs, rings, lines.

Every shape here follows the same rules as ``oblique``: faces from one ramp, light from the
top-left, the vanilla outline (ramp[1] on the top/left arc, ramp[0] on the bottom/right arc),
and at 32 px everything structural is doubled (``cv.u``).

    cylinder(cv, x, y, length, diameter, ramp, axis='x')  batteries, motors, fuses, heating
                                                           elements, capacitor cans, tubes
    disc(cv, x, y, d, ramp)        a flat round face seen front-on (gauge, lamp lens, rotor hub)
    ring(cv, x, y, d, ramp, w)     an annulus (bezel, fan shroud, coil turn) - centre left alone
    knurled_disc(cv, x, y, d, ramp, teeth=8)   a disc with notches cut in its rim (knurled
                                   fuse caps, gear rims, thumbwheels), seen front-on
    oblique_ring(cv, x, y, w, dp, ramp)        a flat ring / torus LYING in a box's top face
                                   (toroidal coil, washer, bezel on a lid), cabinet oblique
    line(cv, p0, p1, c)            Bresenham line, 1 px (wires, antenna masts, blades, poles)
    line_pts(p0, p1)               the pixels of that line, for your own shading

Cylinder band table (``CYL_BANDS``): the ramp index of every row across the diameter, top to
bottom (axis 'x') or left to right (axis 'y'), INCLUDING the outline rows.  Diameters are in
16-px units; at 32 px each band is two pixels wide, so a 32 px cylinder of diameter 12 uses
the 6-row table.  The highlight (ramp[5], or [4] if the ramp has no [5]) sits one row inside
the lit edge, as on vanilla's round items.
"""
from .oblique import outline_region

#: diameter (16-px units) -> ramp index per row across the diameter, outline rows included.
#: 1 = light outline (top/left), 0 = dark outline (bottom/right).  'H' = highlight (ramp[5]).
CYL_BANDS = {
    2: (1, 0),
    3: (1, 4, 0),
    4: (1, 4, 3, 0),
    5: (1, 'H', 3, 2, 0),
    6: (1, 4, 'H', 3, 2, 0),
    7: (1, 4, 'H', 3, 3, 2, 0),
    8: (1, 4, 'H', 4, 3, 3, 2, 0),
    9: (1, 4, 'H', 4, 3, 3, 2, 2, 0),
    10: (1, 4, 'H', 4, 3, 3, 3, 2, 2, 0),
    11: (1, 4, 'H', 4, 4, 3, 3, 3, 2, 2, 0),
    12: (1, 4, 'H', 4, 4, 3, 3, 3, 3, 2, 2, 0),
}


def _c(ramp, k):
    if k == 'H':
        return ramp[5] if len(ramp) > 5 else ramp[4]
    return ramp[k]


def cylinder(cv, x, y, length, diameter, ramp, axis='x', round_ends=True, cap=None):
    """A cylinder seen side-on, its axis in the picture plane.

    axis 'x': lying, occupies x..x+length-1, y..y+diameter-1; bands run along it (lit on top).
    axis 'y': standing, occupies x..x+diameter-1, y..y+length-1; bands run down it (lit left).
    ``diameter`` (and ``length``) in canvas px; at 32 px the diameter must be even.
    ``round_ends``: cut the four corner pixels (x u) so the ends read as round (diameter >= 5).
    ``cap``: for a standing cylinder, a ramp for the top end face seen from above (3 rows x u
    under the top outline: cap[5] far rim, cap[4] face, cap[2] near lip; diameter >= 6); None = flat top.
    End pixels get the outline: left/top end ramp[1], right/bottom end ramp[0] (vanilla rule).
    Returns the set of pixels drawn.
    """
    u = cv.u
    if diameter % u:
        raise ValueError('at 32 px a cylinder diameter must be even')
    bands = CYL_BANDS.get(diameter // u)
    if bands is None:
        raise ValueError(f'no band table for diameter {diameter // u} (have {sorted(CYL_BANDS)})')
    rows = [b for b in bands for _ in range(u)]
    pts = {}
    for i in range(length):
        for j in range(diameter):
            k = rows[j]
            if i < u and k not in (0, 1):
                k = 1                                   # near end: light outline
            elif i >= length - u and k not in (0, 1):
                k = 0                                   # far end: dark outline
            p = (x + i, y + j) if axis == 'x' else (x + j, y + i)
            pts[p] = _c(ramp, k)
    if round_ends and diameter // u >= 5:
        corners = []
        for a in range(u):
            for b in range(u):
                for (ci, cj) in ((a, b), (a, diameter - 1 - b), (length - 1 - a, b),
                                 (length - 1 - a, diameter - 1 - b)):
                    corners.append((x + ci, y + cj) if axis == 'x' else (x + cj, y + ci))
        for p in corners:
            pts.pop(p, None)
        # the pixels diagonally inside a cut corner become outline
        for p in list(pts):
            px, py = p
            if pts[p] in (ramp[0], ramp[1]):
                continue
            n = [(px - 1, py) in pts, (px + 1, py) in pts, (px, py - 1) in pts, (px, py + 1) in pts]
            if not all(n):
                pts[p] = ramp[1] if (not n[0] or not n[2]) else ramp[0]
    if cap is not None and axis == 'y' and diameter // u >= 6:
        # top end face seen from above: rows u..3u-1 inside the outline columns; the first
        # row is the lit far rim (cap[5]), the next the face (cap[4]), then a shaded lip (cap[2])
        for j in range(u, diameter - u):
            for r in range(u, 4 * u):
                p = (x + j, y + r)
                if p in pts:
                    pts[p] = (cap[5] if len(cap) > 5 else cap[4]) if r < 2 * u else \
                             (cap[4] if r < 3 * u else cap[2])
    for (px, py), c in pts.items():
        cv.set(px, py, c)
    return set(pts)


def _disc_mask(x, y, d):
    """Pixels of a pixel-art circle of diameter d in the box x..x+d-1, y..y+d-1."""
    r = d / 2.0
    cx, cy = x + r, y + r
    lim = r * r + (0.6 if d <= 6 else 0.3 * r)
    return {(xx, yy) for yy in range(y, y + d) for xx in range(x, x + d)
            if (xx + 0.5 - cx) ** 2 + (yy + 0.5 - cy) ** 2 <= lim}


def disc(cv, x, y, d, ramp, face=3, rim=True, outline=True):
    """A flat round face seen front-on, in the box x..x+d-1, y..y+d-1 (diameter ``d`` px).

    Fill ramp[face]; with ``rim`` a lit inner arc (ramp[4]) on the top-left and a shaded arc
    (ramp[2]) on the bottom-right, ``cv.u`` px wide; outline ramp[1]/[0] around it.
    Returns the pixel set (put dial marks, needles, lamp glints on top of it).
    """
    reg = _disc_mask(x, y, d)
    for p in reg:
        cv.set(*p, ramp[face])
    u = cv.u
    inner = set(reg)
    if outline:
        ring_px = outline_region(cv, reg, ramp[0], ramp[1], u)
        inner -= ring_px
    if rim and d >= 6 * u:
        cx, cy = x + d / 2.0, y + d / 2.0
        edge = set()
        tmp = set(inner)
        for _ in range(u):
            e = {p for p in tmp if any(q not in tmp for q in
                                       ((p[0] - 1, p[1]), (p[0] + 1, p[1]), (p[0], p[1] - 1), (p[0], p[1] + 1)))}
            edge |= e
            tmp -= e
        for p in edge:
            s = (p[0] + 0.5 - cx) + (p[1] + 0.5 - cy)          # < 0: top-left half
            if s < -0.5:
                cv.set(*p, ramp[4])
            elif s > 0.5:
                cv.set(*p, ramp[2])
    return reg


def ring(cv, x, y, d, ramp, width=None, outline=True):
    """An annulus of outer diameter ``d`` and band ``width`` (default 2 px, 4 at 32): band in
    ramp[3], lit top-left / shaded bottom-right like ``disc``, outlined inside and out.
    The centre is not touched (draw the hub/face before or after).  Returns the band's pixels."""
    u = cv.u
    width = width if width is not None else 2 * u
    outer = _disc_mask(x, y, d)
    inner = _disc_mask(x + width, y + width, d - 2 * width) if d - 2 * width > 0 else set()
    band = outer - inner
    cx, cy = x + d / 2.0, y + d / 2.0
    for p in band:
        s = (p[0] + 0.5 - cx) + (p[1] + 0.5 - cy)
        cv.set(*p, ramp[4] if s < -d / 4 else (ramp[2] if s > d / 4 else ramp[3]))
    if outline:
        outline_region(cv, band, ramp[0], ramp[1], u)
    return band


def knurled_disc(cv, x, y, d, ramp, teeth=8, notch=0.4, phase=0.0, outline=True):
    """A disc with ``teeth`` notches cut into its rim (knurled cap, gear rim, thumbwheel),
    front-on, in the box x..x+d-1, y..y+d-1.

    The notches are cut from the outer ``cv.u``-px ring of the disc BEFORE outlining, so the
    outline follows the teeth.  ``notch`` = the fraction of each tooth pitch that is cut away
    (0.4 -> teeth wider than gaps); ``phase`` rotates the pattern (fraction of a pitch; 0 puts
    a tooth at 3, 6, 9 and 12 o'clock when teeth is a multiple of 4).  Faces: ramp[4] on the
    top-left third, ramp[3] across the middle, ramp[2] on the bottom-right third (like
    ``ring``); outline ramp[1]/[0].  Paint the cap's centre (a window, a disc) on top.
    Returns the pixel set.  (Reference: the lead fuse caps, ``families/power_passive.fuse``.)"""
    import math
    u = cv.u
    reg = _disc_mask(x, y, d)
    inner = _disc_mask(x + u, y + u, d - 2 * u)
    cx, cy = x + d / 2.0, y + d / 2.0
    lo, hi = 0.5 - notch / 2.0, 0.5 + notch / 2.0
    for p in reg - inner:
        a = math.atan2(p[1] + 0.5 - cy, p[0] + 0.5 - cx)
        k = (a / (2 * math.pi) * teeth + phase) % 1.0
        if lo < k < hi:
            reg.discard(p)
    for p in reg:
        s = (p[0] + 0.5 - cx) + (p[1] + 0.5 - cy)
        cv.set(*p, ramp[4] if s < -d / 4 else (ramp[2] if s > d / 4 else ramp[3]))
    if outline:
        outline_region(cv, reg, ramp[0], ramp[1], u)
    return reg


def oblique_ring(cv, x, y, w, dp, ramp, band=None, height=None, floor=None, outline=True):
    """A flat ring (washer, toroidal coil, bezel) lying in a horizontal TOP face, drawn in the
    kit's cabinet oblique.

    The ring is the ellipse inscribed in the top face of ``box(cv, x, y, w, h, dp, ...)``: that
    face spans rows y-dp..y-1 and leans right by one px per row (cols x..x+w-1+dp).  Shrink
    w/dp (and move x/y) to inset it.  ``band`` = ring width along the front edge (default
    w//3); ``height`` = how far it stands proud of the face (default 0 = flat; ``cv.u`` or more
    for a torus/coil: the near outer wall shows below the band).
    Faces: the band in ramp[4], its far-left (lit) arc ramp[5] (or [4]) and its near-right arc
    ramp[3]; the near outer wall ramp[3] (left) / ramp[2] (right); the far INNER wall seen
    through the hole ramp[2]; the hole floor is left alone unless ``floor`` (a colour) is given.
    The outline (ramp[1]/[0]) goes round the outside only and is 1 px even at 32 (the band is
    thin in depth: a 2 px outline would eat it).  Returns the ring's pixel set.
    """
    u = cv.u
    band = band if band is not None else max(u, w // 3)
    height = 0 if height is None else height
    a0, b0 = w / 2.0, dp / 2.0                        # semi-axes along the front edge / depth

    def coords(px, py):
        v = (y - 1 - py) + 0.5                        # depth coordinate (0 = front edge)
        return (px - x) + 0.5 - (v - 0.5), v          # along the front edge, shear removed

    def ell(scale):
        out = set()
        for py in range(y - dp - 1, y + 1):
            for px in range(x - 1, x + w + dp + 1):
                uu, v = coords(px, py)
                if ((uu - a0) / a0) ** 2 + ((v - b0) / b0) ** 2 <= scale * scale:
                    out.add((px, py))
        return out

    outer = ell(1.0)
    k = max(0.0, 1.0 - band / a0)
    hole = ell(k) if k > 0 else set()
    top = outer - hole
    wall = {(px, py + dy) for (px, py) in outer for dy in range(1, height + 1)} - outer
    inner_wall = (hole - {(px, py + height) for (px, py) in hole}) if height else set()
    xm = x + (w + dp) / 2.0
    for p in wall:
        cv.set(*p, ramp[3] if p[0] < xm else ramp[2])
    lit = ramp[5] if len(ramp) > 5 else ramp[4]
    for p in top:
        uu, v = coords(*p)
        t = (uu - a0) / a0 - (v - b0) / b0            # < 0: far-left half, > 0: near-right half
        cv.set(*p, lit if t < -0.9 else (ramp[3] if t > 0.9 else ramp[4]))
    for p in inner_wall:
        cv.set(*p, ramp[2])
    if floor is not None:
        for p in hole - inner_wall:
            cv.set(*p, floor)
    if not height:                                    # flat: a shaded lip along the hole's far edge
        for (px, py) in hole:
            if (px, py - 1) in top:
                cv.set(px, py, ramp[2])
    if outline:                                       # 1 px even at 32: the band is thin in depth
        outline_region(cv, top | wall | hole, ramp[0], ramp[1], 1)
    return top | wall | inner_wall


def line_pts(p0, p1):
    """Bresenham pixels from p0 to p1 inclusive (integer (x, y) tuples)."""
    (x0, y0), (x1, y1) = p0, p1
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    out = []
    while True:
        out.append((x0, y0))
        if x0 == x1 and y0 == y1:
            return out
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


def line(cv, p0, p1, c, width=1):
    """A straight line from p0 to p1 in colour ``c``.

    ``width`` > 1 thickens it downwards (shallow lines) or rightwards (steep lines) - the
    dark side - so give the extra rows a darker step yourself if it needs shading:
    ``line(cv, a, b, R[4]); line(cv, (a[0], a[1]+1), (b[0], b[1]+1), R[2])``.
    At 32 px pass width=2 for anything that is 1 px at 16 (structural weight).
    Returns the list of pixels.
    """
    pts = line_pts(p0, p1)
    steep = abs(p1[1] - p0[1]) > abs(p1[0] - p0[0])
    out = []
    for (x, y) in pts:
        for k in range(width):
            q = (x + k, y) if steep else (x, y + k)
            cv.set(*q, c)
            out.append(q)
    return out
