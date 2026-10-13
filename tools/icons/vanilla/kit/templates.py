"""Templates for non-placeable items: materials and handheld tools.

Materials (ingots, dusts, plates) use the kit's OWN silhouettes (owner decision, kit
revision 3: no pixel copies of Mojang textures).  They are drawn in the same idiom as vanilla
material items - one object, lying diagonally or heaped, filling the slot, outlined by the
vanilla rule, shaded from the top-left - but built in this kit's cabinet oblique:

    ingot   a trapezoid bar seen END-ON: the trapezoid end faces you, the bar recedes up-right
            (front end ramp[3], long top face ramp[4] with a ramp[5] bevel along its near edge,
            sloped long side ramp[2]); runs from the bottom-left to the top-right of the slot
    dust    a low, lumpy heap of powder: lit left slopes ramp[4] with ramp[5] grains, middle
            ramp[3], shaded right slopes ramp[2] with a few darker grains
    plate   a square sheet lying flat in oblique (unchanged since revision 1; it was always ours)

The grids below hold ramp INDICES (0..5); the outline is recomputed from the silhouette with
the vanilla rule, so the digits on the boundary do not matter.  Recolour with a section 5.3
ramp; never redraw the silhouettes per family.

    ingot(ramp)     dust(ramp)     plate(ramp)
    handheld_body(cv, ramp, ...)   diag_stroke(cv, ...)
"""
from .canvas import Canvas
from .oblique import box, outline_region

#: Ingot: trapezoid end (rows 9..13, bottom-left), bar depth 7 up-right (to cols 13..14, row 2).
INGOT = [
    "................",
    "................",
    "........111110..",
    ".......1444420..",
    "......14444220..",
    ".....1444422220.",
    "....14444222220.",
    "...14444222220..",
    "..15555222220...",
    "..1333322220....",
    "..133332220.....",
    ".133333220......",
    ".13333330.......",
    ".1000000........",
    "................",
    "................",
]

#: Dust: a low heap with a shoulder on the left, cols 1..14, rows 5..13; grains hand-placed.
DUST = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "......444.......",
    ".....44543......",
    "....4454332.....",
    "...445433332....",
    "..44543333322...",
    "..43333353222...",
    ".4433533332222..",
    ".4333333232232..",
    "..333333322222..",
    "................",
    "................",
]


def _from_grid(grid, ramp, size):
    """Paint an index grid with ``ramp`` (index 5 falls back to 4), then outline it."""
    cv = Canvas(16)
    pal = {str(i): ramp[min(i, len(ramp) - 1)] for i in range(6)}
    cv.overlay(grid, pal)
    reg = cv.region()
    outline_region(cv, reg, ramp[0], ramp[1])
    if size == 16:
        return cv
    big = Canvas(size)                                   # materials are 16 px; 32 = pixel-doubled
    k = size // 16
    for (x, y) in reg:
        big.rect(x * k, y * k, k, k, cv.get(x, y))
    return big


def ingot(ramp, size=16):
    """Ingot (the kit's own end-on trapezoid bar, see module doc) in a 5/6-step ramp."""
    return _from_grid(INGOT, ramp, size)


def dust(ramp, size=16):
    """Dust heap (the kit's own silhouette, see module doc) in a 5/6-step ramp."""
    return _from_grid(DUST, ramp, size)


def plate(ramp, size=16):
    """A square metal sheet lying flat, in cabinet oblique, filling the body box (cols 1..14,
    rows 8..14 at 16 px): front edge 9 wide x 2 tall in ramp[2] (one step darker than a box
    front, so the thin edge reads as metal, not paper), top face 5 deep in ramp[4], right edge
    ramp[1], one diagonal glint (ramp[5]) across the top face.  Outline as usual."""
    cv = Canvas(size)
    u = cv.u
    edge = (ramp[0], ramp[1], ramp[1], ramp[2], ramp[4]) + ((ramp[5],) if len(ramp) > 5 else ())
    reg = box(cv, 1 * u, 13 * u, 9 * u, 2 * u, 5 * u, edge, bevel=False)
    gl = ramp[5] if len(ramp) > 5 else ramp[4]
    for i in range(2 * u, 5 * u):                    # glint: a 45-degree streak on the top face
        for j in range(u):
            p = (3 * u + i + j, 13 * u - i)
            if p in reg and cv.get(*p) == ramp[4]:
                cv.set(*p, gl)
    return cv


def handheld_body(cv, ramp, x=1, y=2, w=10, h=12):
    """Front-on body of a handheld tool/meter (like the vanilla compass): rounded corners,
    vanilla outline (ramp[1] top/left, ramp[0] bottom/right), lit inner top row and left
    column (ramp[3]), face ramp[2] (so the inner right column and bottom row read as shade).
    Default box = the handheld template (cols 1..10, rows 2..13 at 16 px; scaled by u).
    Returns the free face rectangle (x, y, w, h) inside the lit/shaded edges - (3, 4, 6, 8)
    for the default box - where screens, buttons and jacks go."""
    u = cv.u
    x, y, w, h = x * u, y * u, w * u, h * u
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            cv.set(xx, yy, ramp[2])
    for i in range(u):
        cv.hline(x + u, x + w - 1 - u, y + i, ramp[1])                 # top outline
        cv.vline(x + i, y + u, y + h - 1 - u, ramp[1])                 # left outline
        cv.hline(x + u, x + w - 1 - u, y + h - 1 - i, ramp[0])         # bottom outline
        cv.vline(x + w - 1 - i, y + u, y + h - 1 - u, ramp[0])         # right outline
    for (cx, cy) in ((x, y), (x + w - u, y), (x, y + h - u), (x + w - u, y + h - u)):
        cv.rect(cx, cy, u, u, (0, 0, 0, 0))                           # rounded corners
    for i in range(u):
        cv.hline(x + u, x + w - 1 - 2 * u, y + u + i, ramp[3])        # lit inner top row
        cv.vline(x + u + i, y + u, y + h - 1 - 2 * u, ramp[3])        # lit inner left column
    return (x + 2 * u, y + 2 * u, w - 4 * u, h - 4 * u)


def diag_stroke(cv, x, y, n, lit, body, direction=(1, 1)):
    """A 2 px thick 45-degree stroke of ``n`` steps from (x, y): each step paints (x, y) in
    ``lit`` and (x+1, y) in ``body``.  The accessory-stroke rule of handheld tools (probes,
    leads, blades in the free bottom-right corner, drawn in front of the body)."""
    dx, dy = direction
    for i in range(n):
        cv.set(x + i * dx, y + i * dy, lit)
        cv.set(x + i * dx + 1, y + i * dy, body)
