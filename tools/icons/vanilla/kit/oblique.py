"""Cabinet-oblique solids and the vanilla outline rule.

Projection (placeable devices): the identity face is drawn flat and front-on; depth recedes
up and to the right at 45 degrees, one pixel right and one pixel up per depth pixel.
Depth is a lookup, not a measurement: ``depth(cv, 'block')`` = 4 (8 at 32 px) for full-block
machines, ``depth(cv, 'sixnode')`` = 2 (4 at 32 px) for wall/floor parts.

Faces of every box: top = ramp[4] (lit), front = ramp[3], right side = ramp[2]; the first row
of the top face (depth 1) may take the bevel ramp[5].

Outline (copied from vanilla items, checked against 26.3 iron/copper ingot): every silhouette
pixel whose outside neighbour is on the left/top gets ramp[1]; on the right/bottom ramp[0].
Tie-break: open to the left -> light; else open right or below -> dark; else open above -> light.
At 32 px the outline is 2 px wide (two rings, same rule) so it has the same on-screen weight
as the 16 px outline.
"""
from .color import T

DEPTHS = {'block': 4, 'sixnode': 2, 'flat': 1}


def depth(cv, kind='block'):
    """Oblique depth in pixels for a kind of placeable: 'block' 4, 'sixnode' 2, 'flat' 1 (x u)."""
    return DEPTHS[kind] * cv.u


def _ring(reg):
    """Split a region's boundary pixels into (light, dark) sets by the vanilla rule."""
    light, dark = set(), set()
    for (x, y) in reg:
        L = (x - 1, y) not in reg
        R = (x + 1, y) not in reg
        U = (x, y - 1) not in reg
        D = (x, y + 1) not in reg
        if L:
            light.add((x, y))
        elif R or D:
            dark.add((x, y))
        elif U:
            light.add((x, y))
    return light, dark


def outline_region(cv, reg, dark, light, width=None):
    """Draw the vanilla outline on the boundary of pixel set ``reg``.

    ``light`` goes on the top/left arc, ``dark`` on the bottom/right arc (pass ramp[1], ramp[0]).
    ``width`` defaults to ``cv.u`` (1 at 16 px, 2 at 32 px).  Regions 1 px thick get no
    outline in practice (they are all boundary) - draw those by hand.
    Returns the set of pixels that were outlined.
    """
    width = cv.u if width is None else width
    reg = set(reg)
    done = set()
    for _ in range(width):
        lt, dk = _ring(reg)
        for p in lt:
            cv.set(p[0], p[1], light)
        for p in dk:
            cv.set(p[0], p[1], dark)
        done |= lt | dk
        reg -= lt | dk
    return done


def outline(cv, ramp, width=None):
    """Outline the whole opaque silhouette of ``cv`` with ramp[0]/ramp[1]."""
    return outline_region(cv, cv.region(), ramp[0], ramp[1], width)


def box(cv, x, y, w, h, d, ramp, outline=True, bevel=True, ow=None, region_out=None):
    """Draw one oblique box: front face x..x+w-1, y..y+h-1, extruded ``d`` px towards (+1, -1).

    ``ramp`` is a 5/6-step ramp (see module doc for the face mapping).  ``bevel`` paints the
    top face's front row in ramp[5] when the ramp has one.  ``ow`` overrides the outline
    width (default cv.u).  Returns the set of covered pixels.
    """
    reg = set()
    for k in range(d + 1):
        for yy in range(y - k, y - k + h):
            for xx in range(x + k, x + k + w):
                reg.add((xx, yy))
    xr = x + w - 1
    for (px_, py_) in reg:
        if x <= px_ <= xr and y <= py_ <= y + h - 1:
            c = ramp[3]
        else:
            dx, dy = px_ - xr, y - py_
            c = ramp[4] if (dy > 0 and (dx <= 0 or dy >= dx)) else ramp[2]
        cv.set(px_, py_, c)
    if bevel and d > 0 and len(ramp) > 5:
        for xx in range(x + 1, xr + 1):
            if (xx, y - 1) in reg:
                cv.set(xx, y - 1, ramp[5])
    if outline:
        outline_region(cv, reg, ramp[0], ramp[1], ow)
    if region_out is not None:
        region_out |= reg
    return reg


def solid(parts):
    """Z-buffered union of oblique boxes of ONE material (frames, posts, beams).

    ``parts`` is a list of ``(name, x, y, w, h, d)``.  Returns ``{(x, y): (name, face, k)}``
    where face is 'front' | 'top' | 'side' and k the depth (0 = front plane).  The nearest
    surface wins; ties go to the later part.  Paint it with ``paint_solid`` so the whole union
    gets one outline and no seams; recolour individual faces afterwards by looking them up.
    """
    zb = {}
    for (name, x, y, w, hgt, d) in parts:
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
                    old = zb.get(p)
                    if old is None or k <= old[2]:
                        zb[p] = (name, face, k)
    return zb


def paint_solid(cv, zb, ramp, bevel=True, ow=None, cut=()):
    """Paint a ``solid()`` union: faces from the ramp, bevel row, one outline round the union.

    ``cut``: pixels removed from the union *before* painting and outlining (chamfers, notches,
    gaps between feet) - so the outline follows the cut automatically.  ``zb`` is modified.
    """
    for p in cut:
        zb.pop(tuple(p), None)
    for p, (name, face, k) in zb.items():
        cv.set(p[0], p[1], {'front': ramp[3], 'top': ramp[4], 'side': ramp[2]}[face])
    if bevel and len(ramp) > 5:
        for p, (name, face, k) in zb.items():
            if face == 'top' and k == 1:
                cv.set(p[0], p[1], ramp[5])
    outline_region(cv, set(zb), ramp[0], ramp[1], ow)


def faces(zb, name=None, face=None, k=None):
    """Pixels of a ``solid()`` result filtered by part name / face / depth (any may be None)."""
    return [p for p, (n, f, kk) in zb.items()
            if (name is None or n == name) and (face is None or f == face) and (k is None or kk == k)]

