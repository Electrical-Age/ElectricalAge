"""Family helper: lighting fixtures (lamp sockets, lamp supply, emergency lamps), 16 px.

Shared visual system
--------------------
* Every member is a placeable sixnode drawn as a likeness of its own OBJ (MATCH), cabinet oblique,
  depth 2 (``'flat'`` 1 for the flat lamp, 4 for the thick simple lamp), filling the body box
  and ending on row 14.
* **State: drawn lit** (STYLE 7b/8: lamps and bulbs are drawn lit).  Every lens, diffuser,
  glass pane and tube is the LIT ramp used as an emitting surface: LIT[4] face, LIT[5] glow
  centre / glint, LIT[3] on surfaces turned away from the viewer.  This matches the models'
  *_on textures (cream #fff0c0..#fffbe8).  The item renders of the robust/suspended/sconce
  lamps bind the off texture, but the family rule (lit) wins so all eleven sockets read as lamps.
* **Glass is framed, not outlined in LIT.**  Every glowing part sits in its housing, and the
  silhouette outline belongs to that housing's ramp (STEEL cage bars run along the robust
  lamp's edges in the model; the sconce frame is dark; the classic cap has a dark rim).  A LIT
  outline (LIT[1]/[0], brown) round a large cream body read as bread at GUI 2.
* Housings in neutral materials only, chosen from the model textures:
    - STEEL: light metal frames, cage bars, the flat lamp frame, the fluorescent trough, the
      distribution board's front bezel;
    - DARK (= RUBBER_BLACK): the near-black mounts, ceiling plates, chains, lamp post and
      lantern (#2f2f2f..#3c3c3c in the textures), the classic cap's rim, the distribution
      board's box (top, sides, back are black in distributionboard.png);
    - WHITE_PLASTIC: the simple lamp's thick housing, the emergency lamp housing;
    - CAV: interiors (the distribution board).
* Shared shapes: ``prism`` (an extruded front polygon with per-face colours, used for the
  chamfered robust-lamp glass), ``cage`` (the suspended lamps' caged glass body),
  ``ceiling_plate`` + ``chain`` (suspended vs long suspended differ in chain length: 3 vs 5
  rows; the cage gets shorter, not narrower, so both end on row 14 and stay the same cage),
  ``classic_disc``/``classic_profile`` (the classic socket's faceted cone: front-on for socket
  A; in profile for the B projector, which shares the OBJ, base at the wall on the left, the
  cone tapering right and the projector's beam leaving its tip).
* Emergency lamps (the only tiered ids): tier cue 'plate'.  The model has no plate, so they stand
  on the wave-1 tier slab (``families/power_switching.base``: front cols 1..12, rows 11..14,
  depth 2), the same slab as the cable switches.  The wall housing (coreWall + coreWallR) is a
  tall narrow white pillar whose narrow front face is the exit sign, so it is drawn as an
  upright WHITE_PLASTIC box (front 7 x 8 plus depth 2) centred on the slab.  The model's sign
  is green; green collides with the LV cue (MARK_GREEN vs LV ramp[3]: dE 8 for tritan
  viewers, same hue family for everyone) and STYLE 7 makes the cue the only hue, so the sign
  is a lit neutral panel (WHITE field, drawn lit like every lamp in the family) with the
  model's up arrow in a dark ink (CAV).  No LIT on these two (LIT is close to HV).
"""
from kit import (Canvas, box, solid, paint_solid, faces, outline_region, cylinder, disc, line,
                 line_pts, tier_plate, STEEL, CAV, RUBBER_BLACK, LIT, GLASS, COPPER, WHITE,
                 WHITE_PLASTIC)

DARK = RUBBER_BLACK
#: LIT as the face mapping of an emitting surface: [2] turned away LIT[3], [3] face LIT[4],
#: [4] lit top LIT[5].  The glass itself is outlined by its housing (see the module docstring);
#: here it is only the throwaway ramp of ``_disc_px``.  (Reported as a kit request.)
GLOW = (LIT[0], LIT[1], LIT[3], LIT[4], LIT[5], LIT[5])
#: the fluorescent tube (cylinder d3 bands 1/4/0): LIT[4] lit edge, WHITE core, LIT[3] shaded
#: edge - a pale tube, not outlined in brown LIT[0]/[1]
TUBE = (LIT[3], LIT[4], LIT[4], LIT[5], WHITE, WHITE)

N4 = ((-1, 0), (1, 0), (0, -1), (0, 1))


def _disc_px(x, y, d):
    """The pixel set of a disc() of diameter d (drawn on a throwaway canvas)."""
    return disc(Canvas(16), x, y, d, GLOW, rim=False, outline=False)


def prism(cv, front, d, front_c, top_c, side_c):
    """Extrude the front pixel set ``front`` by ``d`` px towards (+1, -1) and paint it: the
    front set in ``front_c``; every other pixel takes the colour of the front edge it was swept
    from - ``top_c`` from an edge open above, else ``side_c`` (open to the right).  Returns the
    whole pixel set (outline it yourself)."""
    reg = {}
    for k in range(d, -1, -1):
        for (x, y) in front:
            reg[(x + k, y - k)] = (x, y, k)
    for p, (x, y, k) in reg.items():
        if k == 0:
            c = front_c
        elif (x, y - 1) not in front:
            c = top_c
        else:
            c = side_c
        cv.set(*p, c)
    # a pixel can be swept from several edges: the nearest (smallest k) wins
    for p in list(reg):
        for k in range(0, d + 1):
            q = (p[0] - k, p[1] + k)
            if q in front:
                if k == 0:
                    cv.set(*p, front_c)
                elif (q[0], q[1] - 1) not in front:
                    cv.set(*p, top_c)
                else:
                    cv.set(*p, side_c)
                break
    return set(reg)


def chamfered(x, y, w, h, c):
    """The front pixel set of a w x h rectangle with all four corners chamfered by ``c`` px."""
    out = set()
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            dx = min(xx - x, x + w - 1 - xx)
            dy = min(yy - y, y + h - 1 - yy)
            if dx + dy >= c:
                out.add((xx, yy))
    return out


# ------------------------------------------------------------------------------- simple lamp
def simple_lamp():
    """simplelamp.obj: a thick square lamp (front 10 x 10, depth 4) in a white housing, its
    front face glowing with the soft round centre of SimpleLamp.png (LIT[5] disc on LIT[4]).
    Depth 4 (not the sixnode 2) because the model is a thick block: it keeps it a box next
    to the thin flat panel."""
    cv = Canvas(16)
    W = WHITE_PLASTIC
    box(cv, 1, 5, 10, 10, 4, W, bevel=False)          # front cols 1..10, rows 5..14
    cv.rect(2, 6, 8, 8, LIT[4])                        # glowing face cols 2..9, rows 6..13
    cv.hline(2, 9, 13, LIT[3])
    cv.vline(9, 6, 13, LIT[3])
    for p in _disc_px(3, 7, 6):
        cv.set(*p, LIT[5])
    cv.set(4, 8, WHITE)                                # glint
    return cv


# --------------------------------------------------------------------------------- flat lamp
def flat_lamp():
    """flatlamp.obj: a thin square panel light: a grey STEEL frame (2 px) round a white diffuser,
    depth 'flat' (1).  Front cols 1..13, rows 2..14."""
    cv = Canvas(16)
    S = STEEL
    box(cv, 1, 2, 13, 13, 1, S, bevel=False)
    # diffuser cols 3..11, rows 4..12: inner top/left jamb in shade, lit lower/right lip
    cv.rect(3, 4, 9, 9, LIT[5])
    cv.hline(3, 11, 4, S[1])
    cv.vline(3, 4, 12, S[1])
    cv.hline(4, 11, 12, LIT[4])
    cv.vline(11, 5, 12, LIT[4])
    cv.pixels([(5, 6), (6, 6), (5, 7)], WHITE)
    return cv


# -------------------------------------------------------------------------- fluorescent lamp
def fluorescent_lamp():
    """fluorescentlamp.obj: a long narrow grey trough (STEEL, about 2.7:1 in the model) holding
    one fluorescent tube that runs almost its full length between two dark end caps.  Laid
    horizontal (sixnode orientation is not shown): front cols 1..12, rows 9..14, depth 2.
    The steel lips are the 1 px outline rows; the tube carries the icon."""
    cv = Canvas(16)
    S = STEEL
    box(cv, 1, 9, 12, 6, 2, S, bevel=False)            # cols 1..14, rows 7..14
    cv.rect(2, 10, 10, 4, S[2])                        # reflector behind the tube
    cv.hline(2, 11, 10, S[1])                          # shade under the upper lip
    for x in (2, 11):                                  # end caps holding the tube
        cv.vline(x, 11, 13, DARK[3])
        cv.set(x, 13, DARK[1])
    cylinder(cv, 3, 11, 8, 3, TUBE)                    # the tube cols 3..10, rows 11..13
    return cv


# ---------------------------------------------------------------------- classic lamp socket
#: socket A's facet edges (start, end), from the glowing tip towards the rim
FACETS = (((6, 6), (6, 4)), ((7, 11), (7, 13)), ((4, 7), (2, 6)), ((9, 10), (11, 11)),
          ((9, 7), (11, 6)), ((4, 10), (2, 11)))


def classic_disc():
    """classiclampsocket.obj, socket A: the faceted cone seen front-on from its tip - a round
    glowing face (ClassicLampSocket.png's cream gradient disc, white centre) in its dark rim,
    with a 1 px band of the cap's back up-right (depth) and six short LIT[2] facet edges
    radiating from the glowing tip to the rim (the model's faceted cone), so it reads as a
    faceted holder, not a bulb."""
    cv = Canvas(16)
    disc(cv, 2, 2, 12, DARK, face=2, rim=False)        # the cap's back, behind (1 px up-right)
    disc(cv, 1, 3, 12, DARK, face=3, rim=False)        # rim: cols 1..12, rows 3..14
    face = _disc_px(2, 4, 10)
    for p in face:
        s = (p[0] - 6.5) + (p[1] - 8.5)
        cv.set(*p, LIT[3] if s > 4.5 else LIT[4])
    # facet edges from the tip (centre 6.5, 8.5) out to the rim, six round the cone
    for a, b in FACETS:
        for p in line_pts(a, b):
            if p in face:
                cv.set(*p, LIT[2])
    for p in _disc_px(4, 6, 5):                        # the glowing tip
        cv.set(*p, LIT[5])
    cv.set(5, 6, WHITE)
    return cv


#: socket B profile (classiclampsocket.obj fy view): the cone's extent per column, from the
#: flat base against the wall (col 4, behind it the 2 px dark rim cols 2..3) tapering, slightly
#: convex, to the tip (col 10)
_CONE = {4: (2, 13), 5: (2, 13), 6: (3, 12), 7: (3, 12), 8: (4, 11), 9: (5, 10), 10: (6, 9)}


def classic_profile():
    """classiclampsocket.obj, socket B (projector): the same faceted cone as socket A, in
    profile - a dark rim at the left (the base against the wall, 2 px) and the cream cone
    tapering right, lit upper facets (LIT[4]) over shaded lower ones (LIT[3], LIT[2] on the
    middle facet edge), outlined in the dark rim ramp - and three rays of light leaving its tip
    (the projector's aimable beam)."""
    cv = Canvas(16)
    cone = {(x, y) for x, (a, b) in _CONE.items() for y in range(a, b + 1)}
    rim = {(x, y) for x in (2, 3) for y in range(2, 14)}
    for (x, y) in cone:
        cv.set(x, y, LIT[4] if y <= 7 else LIT[3])
    for (x, y) in cone:                                 # the middle facet edge, base to tip
        if y == 8 and x < 10:
            cv.set(x, y, LIT[2])
    cv.pixels([(5, 4), (5, 5), (6, 5)], LIT[5])         # glint on the upper facet
    for p in rim:
        cv.set(*p, DARK[2])
    outline_region(cv, cone | rim, DARK[0], DARK[1])
    # rays from the tip
    for (a, b) in (((12, 7), (14, 7)), ((12, 5), (14, 2)), ((12, 9), (14, 12))):
        line(cv, a, b, LIT[4])
    cv.set(12, 7, LIT[5])
    return cv


# ------------------------------------------------------------------------------ the cage lamp
def cage(cv, x, y, w, h, d, bars_v=(), bar_h=None, chamfer_top=True):
    """The robust lamp's glass body: front x..x+w-1, y..y+h-1, depth d, glowing glass with the
    corners chamfered and the silhouette outlined in STEEL (the cage frame runs along its
    edges in the model); STEEL cage bars: ``bars_v`` = front columns of the vertical bars (they
    run back over the top face), ``bar_h`` = front row of the horizontal bar (runs back along
    the side face).  Returns the zbuffer."""
    S = STEEL
    zb = solid([('g', x, y, w, h, d)])
    cut = [(x, y + h - 1), (x + w - 1, y + h - 1)]
    if chamfer_top:
        cut += [(x, y), (x + d, y - d), (x + w - 1 + d, y - d)]
    for p in cut:
        zb.pop(p, None)
    for p, (_, f, k) in zb.items():
        cv.set(*p, {'front': LIT[4], 'top': LIT[5], 'side': LIT[3]}[f])
    for bx in bars_v:
        for k in range(0, d + 1):
            p = (bx + k, y - k)
            if p in zb:
                cv.set(*p, S[4] if k else S[3])
        for yy in range(y, y + h):
            cv.set(bx, yy, S[3])
    if bar_h is not None:
        for xx in range(x, x + w):
            cv.set(xx, bar_h, S[3])
        for k in range(1, d + 1):
            if (x + w - 1 + k, bar_h - k) in zb:
                cv.set(x + w - 1 + k, bar_h - k, S[2])
    outline_region(cv, set(zb), S[0], S[1])
    return zb


def robust_lamp():
    """robustlamp.obj: the bulkhead lamp front-on: a wide flat chamfered octagon of glowing
    glass (about 1.6:1 in the rx view; front 11 x 7, corner pixels chamfered, depth 2) with two vertical
    cage bars at about 1/4 and 3/4 and one horizontal through the middle, on its dark mount
    (the same octagon one step further back, a dark sliver up-right).  Front cols 1..11,
    rows 8..14.  The chamfer is 1 px: 2 px chamfers run parallel to the 45 deg depth, so the
    top-left and bottom-right edges merged into one long diagonal and the lamp read as a
    skewed lozenge at GUI 2 (tried)."""
    cv = Canvas(16)
    S = STEEL
    x, y, w, h, d = 1, 8, 11, 7, 2
    F = chamfered(x, y, w, h, 1)
    # the dark mount: the same octagon, behind the glass (one more step of depth)
    mreg = prism(cv, {(px + d, py - d) for (px, py) in F}, 1, DARK[3], DARK[4], DARK[2])
    glass = prism(cv, F, d, LIT[4], LIT[5], LIT[3])
    outline_region(cv, mreg - glass, DARK[0], DARK[1])
    # bars: verticals run back over the top face, the horizontal back along the side face
    for bx in (x + 3, x + w - 4):
        top = min(py for (px, py) in F if px == bx)
        for k in range(1, d + 1):
            if (bx + k, top - k) in glass:
                cv.set(bx + k, top - k, S[4])
        for yy in range(y, y + h):
            if (bx, yy) in F:
                cv.set(bx, yy, S[3])
    by = y + h // 2
    for xx in range(x, x + w):
        if (xx, by) in F:
            cv.set(xx, by, S[3])
    right = max(px for (px, py) in F if py == by)
    for k in range(1, d + 1):
        if (right + k, by - k) in glass:
            cv.set(right + k, by - k, S[2])
    outline_region(cv, glass, S[0], S[1])
    cv.set(x + 2, y + 1, WHITE)                           # glint
    return cv


def ceiling_plate(cv):
    """The dark square base plate on the ceiling: front row 2, cols 4..11, depth 1 (row 1)."""
    box(cv, 4, 2, 8, 1, 1, DARK, bevel=False)


def chain(cv, x, y0, y1):
    """A hanging chain from row y0 to y1: links alternately edge-on (1 px, STEEL[2]) and face-on
    (3 px: lit left, dark hole, shaded right).  1 px parts get no outline (STYLE 4)."""
    for i, y in enumerate(range(y0, y1 + 1)):
        if i % 2 == 0:
            cv.set(x, y, STEEL[2])
        else:
            cv.set(x - 1, y, STEEL[3])
            cv.set(x, y, DARK[1])
            cv.set(x + 1, y, STEEL[1])


#: (chain rows, cage front (x, y, w, h), mount front (x, y, w), vertical bar columns) per kind
SUSPENDED = {
    'short': ((3, 5), (2, 8, 10, 7), (5, 7, 6), (4, 9)),
    'long': ((3, 7), (2, 10, 10, 5), (5, 9, 6), (6,)),
}


def suspended(kind):
    """robustlampsuspended.obj: the dark ceiling plate, the chain, and the caged glass body (the
    robust lamp's ``cage``, depth 2) hanging below under its dark mount, ending on row 14.
    ``kind`` 'short' (chain of 3 in the renderer) or 'long' (chain of 7): the chain is longer
    and the cage shorter (same width, one bar), geometry only."""
    (c0, c1), (x, y, w, h), (mx, my, mw), bars = SUSPENDED[kind]
    cv = Canvas(16)
    ceiling_plate(cv)
    chain(cv, 8, c0, c1)
    cage(cv, x, y, w, h, 2, bars_v=bars, bar_h=y + h // 2, chamfer_top=False)
    box(cv, mx, my, mw, 1, 1, DARK, bevel=False)        # the dark mount on the cage's top face
    cv.set(x + 1, y + 1, WHITE)
    return cv


# ------------------------------------------------------------------------------------ sconce
def _sconce_rows():
    """sconce trapezoid rows 2..14: (first col, last col) per row, widening upward along one
    straight diagonal from (1, 2) to (4, 14) (mirrored on the right)."""
    left = {}
    for (x, y) in line_pts((1, 2), (4, 14)):
        left.setdefault(y, x)
    return {y: (a, 15 - a) for y, a in left.items()}


def sconce():
    """sconcelamp.obj: an upward-widening trapezoid of glass panes (drawn lit) in a thin dark
    frame: four panes, the frame lines converging downwards.  The front panes are LIT[4] with
    one LIT[5] glint on the left pane; only the far-right pane, turned away, is LIT[3]."""
    cv = Canvas(16)
    rows = _sconce_rows()
    reg = {(x, y) for y, (a, b) in rows.items() for x in range(a, b + 1)}
    for (x, y) in reg:
        cv.set(x, y, LIT[4])
    frames = ((4, 6), (8, 8), (11, 9))                  # frame lines between the panes (top, bottom col)
    for (x, y) in reg:                                   # the far-right pane, turned away
        xt, xb = frames[2]
        if x > xt + (xb - xt) * (y - 3) / 10:
            cv.set(x, y, LIT[3])
    for (xt, xb) in frames:
        line(cv, (xt, 3), (xb, 13), DARK[3])
    cv.pixels([(3, 4), (3, 5), (4, 6)], LIT[5])          # glint on the left pane
    cv.hline(2, 13, 2, DARK[3])
    outline_region(cv, reg, DARK[0], DARK[1])
    return cv


# ------------------------------------------------------------------------------- streetlight
#: lantern per front row: (first col, last col).  A stubby frustum (the model's narrow end is
#: about 2/3 of the wide end), wider at the top; row 9 is the dark rim where it meets the pole.
_LANTERN = {4: (3, 11), 5: (3, 11), 6: (4, 10), 7: (4, 10), 8: (5, 9), 9: (6, 8)}


def streetlight():
    """streetlight.obj stood upright: a thick base section, a thin square pole and the lantern
    head, a stubby inverted frustum whose sides are almost all cream glass (drawn lit) under a
    thin dark lid (its top face only) with a 1 px dark rim where it joins the pole.  All the
    dark metal is ONE solid (one outline).  Depth 2."""
    cv = Canvas(16)
    parts = [('base', 6, 12, 3, 3, 1),                  # cols 6..8, rows 12..14
             ('pole', 7, 10, 1, 2, 1)]                  # col 7, rows 10..11
    for y, (a, b) in _LANTERN.items():
        parts.append((f'l{y}', a, y, b - a + 1, 1, 2))
    zb = solid(parts)
    paint_solid(cv, zb, DARK, bevel=False)
    # the lens: the front face inside the dark corner edges, and the side face
    for y, (a, b) in _LANTERN.items():
        if y == 9:
            continue
        for xx in range(a + 1, b):
            cv.set(xx, y, LIT[4])
    for p in faces(zb, face='side'):
        if 3 <= p[1] <= 8 and zb[p][0].startswith('l') and zb[p][0] != 'l9' \
                and (p[0] - 1, p[1]) in zb and (p[0] + 1, p[1]) in zb:
            cv.set(*p, LIT[3])
    cv.pixels([(4, 4), (5, 4), (4, 5)], LIT[5])          # glint top-left
    return cv


# ------------------------------------------------------------------------------- lamp supply
def lamp_supply():
    """distributionboard.obj (the lamp supply): a black wall box (top, sides and outline in
    DARK, as in distributionboard.png) with a light STEEL bezel on its front face round the
    dark interior, two copper bus bars carrying white breaker blocks, and the glass door open,
    folded forward and down on its bottom hinge (the OBJ rest pose that drawItem renders),
    drawn as a foreshortened trapezoid in a thin dark frame (the ratified signal_processor
    door), so it cannot be taken for a tier slab."""
    cv = Canvas(16)
    S = STEEL
    box(cv, 2, 3, 11, 9, 2, DARK, bevel=False)         # front cols 2..12, rows 3..11
    cv.rect(3, 4, 9, 7, S[3])                          # bezel (front face inside the outline)
    cv.hline(3, 11, 4, S[4])
    cv.vline(3, 4, 10, S[4])
    cv.rect(5, 5, 6, 5, CAV[1])                        # interior cols 5..10, rows 5..9
    cv.vline(5, 5, 9, S[1])                            # inner left jamb
    for bx in (6, 9):
        cv.vline(bx, 5, 9, COPPER[3])
        cv.vline(bx + 1, 5, 9, COPPER[1])
    cv.hline(6, 8, 6, WHITE)                           # breaker blocks across the bars
    cv.hline(9, 10, 8, WHITE)
    cv.set(8, 6, S[3])
    cv.set(10, 8, S[3])
    # door: hinged under the box, folded toward the viewer: a trapezoid widening downwards
    rows = {12: (3, 11), 13: (2, 12), 14: (1, 13)}
    door = {(x, y) for y, (a, b) in rows.items() for x in range(a, b + 1)}
    for p in door:                                     # GLASS[4], not [3]: [3] is next to
        cv.set(*p, GLASS[4])                           # the signal plate colour (STYLE 5.3)
    cv.pixels([(4, 13), (5, 13)], GLASS[5])            # glint
    outline_region(cv, door, DARK[0], DARK[1])
    return cv


# ---------------------------------------------------------------------------- emergency lamp
#: the exit pictogram on the 5 x 6 sign field (cols 5..9, rows 4..9): the model's up arrow.
#: The door mark above it and the running man below do not survive at this size (tried: the
#: door mark made figure and ground ambiguous at GUI 2).  The field is 5 wide because a 4 px
#: wide arrow is a cross (and on a white pillar read as a tombstone).
EXIT = [
    "..#..",
    ".###.",
    "#####",
    "..#..",
    "..#..",
    ".....",
]


def emergency_lamp(tier, glyph=EXIT):
    """emergencyexitlighting.obj (wall mount: coreWall + coreWallR) on the tier slab: the tall
    narrow white housing (WHITE_PLASTIC, front 7 x 8 plus depth 2) standing upright, centred
    on the slab, its front the exit sign - a lit neutral panel (WHITE) with the up arrow in
    dark ink.  Tier cue: the slab (plate), the only hue."""
    cv = Canvas(16)
    tier_plate(cv, 1, 11, 12, 4, 2, tier, bevel=False)
    W = WHITE_PLASTIC
    box(cv, 4, 3, 7, 8, 2, W, bevel=False)             # front cols 4..10, rows 3..10
    cv.rect(5, 4, 5, 6, WHITE)                         # sign field cols 5..9, rows 4..9
    for j, row in enumerate(glyph):
        for i, ch in enumerate(row):
            if ch == '#':
                cv.set(5 + i, 4 + j, CAV[1])
    return cv
