"""Cable template: a flat jacketed strip with one S-bend on the bottom-left/top-right diagonal.

The in-world cable is a flat strip (sprites/cable.png, player-dyeable).  The icon keeps that
shape - flat, two-tone along its length (lit half towards the top-left, shaded half), tinned
(STEEL) conductor at both cut ends - and puts the TIER COLOUR on the jacket, because the owner
decided tiers are told apart by colour.  The ends are steel, not copper, so the tier colour is the
only hue on the icon (copper ends sat next to the HV amber and, for red-green viewers, VHV).
Width varies a little with the tier (as in the world) but is never the cue: signal and LV are
both 3 px, MV/HV/VHV 4 px.  (``ribbed=True`` reproduces cableVHV's ribbing; not used by default.)

    cable('lv')                      -> Canvas, the low_voltage_cable icon
    cable('thermal', ribbed=False)   -> charcoal thermal cable
    strip(cv, ramp, width=3)         -> paint any jacket on any canvas (ground cable, wires...)
"""
import math

from .canvas import Canvas
from .palette import STEEL
from .tier import tier_ramp, normalize_tier

#: tier -> (jacket width in px at 16, ribbed)
#: VHV is not ribbed by default: at GUI 2 the ribs read as blotches, and the tier must not need them.
CABLE_SPEC = {'signal': (3, False), 'lv': (3, False), 'mv': (4, False), 'hv': (4, False),
              'vhv': (4, False), 'thermal': (4, False)}


def s_path(cv, p0=(2.6, 13.4), p1=(13.4, 2.6), amp=1.4):
    """The standard cable centre-line (16-px coordinates, scaled by cv.u): a diagonal with one
    sine S-bend.  Returns f(t) -> (x, y) for t in 0..1."""
    u = cv.u
    P0 = (p0[0] * u, p0[1] * u)
    P1 = (p1[0] * u, p1[1] * u)
    A = amp * u

    def f(t):
        o = A * math.sin(2 * math.pi * t)
        return (P0[0] + (P1[0] - P0[0]) * t - o * 0.7071,
                P0[1] + (P1[1] - P0[1]) * t - o * 0.7071)
    return f


def strip(cv, ramp, width=3, path=None, ends=STEEL, end_len=0.08, ribbed=False, rib_n=7,
          lit=4, shade=3, bounds=None):
    """Paint a flat strip of ``width`` px (x u) along ``path`` (default ``s_path``) with an
    outside vanilla outline (ramp[1] top/left, ramp[0] bottom/right).

    Jacket: ramp[lit] on the half facing the top-left, ramp[shade] on the other half.
    ``ends``: ramp for the bare conductor over the first/last ``end_len`` of the length, lit
    half ends[4], shaded half ends[2] (default tinned STEEL; None = no bare ends).  A bare
    material wire (copper_cable, iron_cable...) passes its material ramp as both ``ramp`` and
    ``ends=None``.  ``ribbed``: bands across the shaded half darken one step (VHV;
    supplementary only, never counted as the tier cue).
    ``bounds``: (x0, y0, x1, y1) the outline must stay inside (default 1..n-2).
    Returns the set of jacket pixels.
    """
    u = cv.u
    path = path or s_path(cv)
    w = width * u
    samples = []
    for i in range(800):
        t = i / 799
        x, y = path(t)
        xa, ya = path(max(0, t - 1e-3))
        xb, yb = path(min(1, t + 1e-3))
        L = math.hypot(xb - xa, yb - ya)
        nx, ny = -(yb - ya) / L, (xb - xa) / L
        if nx + ny > 0:
            nx, ny = -nx, -ny                 # normal points towards the top-left
        samples.append((x, y, t, nx, ny))
    x0, y0, x1, y1 = bounds or (1, 1, cv.n - 2, cv.n - 2)
    pts = {}
    for yy in range(y0, y1 + 1):
        for xx in range(x0, x1 + 1):
            cx, cy = xx + 0.5, yy + 0.5
            b = min(samples, key=lambda q: (q[0] - cx) ** 2 + (q[1] - cy) ** 2)
            if math.hypot(b[0] - cx, b[1] - cy) <= w / 2.0:
                pts[(xx, yy)] = (b[2], (cx - b[0]) * b[3] + (cy - b[1]) * b[4])
    # outline: outside the strip, u rings
    reg = set(pts)
    for ring in range(u):
        new = {}
        for (x, y) in reg:
            for ax, ay in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                if (ax, ay) not in reg and (ax, ay) not in new and x0 <= ax <= x1 and y0 <= ay <= y1:
                    light = (ax + 1, ay) in reg or (ax, ay + 1) in reg
                    new[(ax, ay)] = ramp[1] if light else ramp[0]
        for p, c in new.items():
            cv.set(p[0], p[1], c)
        reg |= set(new)
    for (x, y), (t, sg) in pts.items():
        c = ramp[lit] if sg > 0 else ramp[shade]
        if ribbed and sg <= 0 and (t * rib_n) % 1.0 < 0.3:
            c = ramp[shade - 1]                   # ribs on the shaded half only: a texture, not a cue
        if ends is not None and (t < end_len or t > 1 - end_len):
            c = ends[4] if sg > 0 else ends[2]
        cv.set(x, y, c)
    return set(pts)


def cable(tier, size=16, width=None, ribbed=None):
    """The cable item icon for a tier ('signal'|'lv'|'mv'|'hv'|'vhv'|'thermal' or alias)."""
    t = normalize_tier(tier)
    w0, rb = CABLE_SPEC[t]
    cv = Canvas(size)
    strip(cv, tier_ramp(t), width=width or w0, ribbed=rb if ribbed is None else ribbed)
    return cv
