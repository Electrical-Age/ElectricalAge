"""Generators family (vanilla, 16 px): fuel generators, heat turbines, shaft machines, the
water and wind turbines, and the creative electrical source.

Shared visual system
--------------------
* Full-block machines (fuel generators) are cabinet-oblique blocks, front 10x10 + depth 4,
  their body in the tier ramp (cue 'body'), like the macerator.
* Round bodies lying on their side (heat turbines, gas turbine, shaft generator) are seen
  side-on, axis along x, drawn by ``revolve()`` from a per-column diameter profile: every
  column is shaded with the kit's CYL_BANDS table, so they share the light and banding of
  ``cylinder()`` while they taper or step.  The three turbinesteam machines share the COWL
  ramp and the STEEL ``pedestal()``; each shows its own identifying feature: the steam
  turbine its fan intake (seen end-on, the cowl receding up-right), the gas turbine its top
  plate and finned side rack, the generator its LED strip and the shaft through both ends.
* Multi-material solids (a steel frame round a tier cover and a dark core) go through
  ``paint_mixed()``: the faces of ONE ``solid()`` union are painted per material, and the
  silhouette gets the vanilla outline in the ramp of the material each outline pixel belongs to.
  This keeps one silhouette with no inner seams.

Family ramps (frozen here, reported for promotion):
    COWL     pale mint cowl of the turbinesteam models (cowl.png #60c090..#90d8a8)
    SOURCE   pale blue-grey plate of the voltage source (voltagesource.png #789090, lit ~#a0b2b4)
"""
from kit import (Canvas, solid, faces, outline_region, cylinder, disc, line, line_pts, box,
                 tier_ramp, tier_stub, h, STEEL, CAV, WHITE, WHITE_PLASTIC, RUBBER_Y,
                 RUBBER_BLACK, MARK_RED, MARK_BLUE, CYL_BANDS, TIER, depth)

# ---------------------------------------------------------------------------- family ramps
#: pale mint cowl (steam turbine, gas turbine, shaft generator).  Same hue family as cowl.png;
#: kept lighter and less saturated than the LV ramp on [2]/[3] (LV[3] #40a575 L*61).
COWL = (h('#1e3a34'), h('#2f6155'), h('#5a9c84'), h('#82c6a4'), h('#aee3c4'), h('#dcf6e6'))
#: pale blue-grey plate of the electrical (voltage) source.
SOURCE = (h('#262f33'), h('#46565c'), h('#6c8085'), h('#98abaf'), h('#bccbcd'), h('#dce6e6'))
#: printed symbol ink on the source plate (texture #000000, lifted above the L* floor).
INK = h('#1f2326')

_FACE = {'front': 3, 'top': 4, 'side': 2}


def _ring(reg):
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


def outline_mixed(cv, reg, ramp_at):
    """Vanilla outline round ``reg``; each outline pixel takes [1]/[0] of ``ramp_at(p)``."""
    lt, dk = _ring(set(reg))
    for p in lt:
        cv.set(*p, ramp_at(p)[1])
    for p in dk:
        cv.set(*p, ramp_at(p)[0])
    return lt | dk


def paint_mixed(cv, zb, mat, bevel=True, cut=()):
    """Paint a ``solid()`` union whose pixels belong to different materials.

    ``mat(p, name, face, k)`` -> ramp.  Faces use the usual mapping (front 3, top 4, side 2),
    top row k=1 the bevel [5] when ``bevel``; one outline round the union, per material."""
    for p in cut:
        zb.pop(tuple(p), None)
    for p, (n, f, k) in zb.items():
        r = mat(p, n, f, k)
        c = r[_FACE[f]]
        if bevel and f == 'top' and k == 1 and len(r) > 5:
            c = r[5]
        cv.set(*p, c)
    outline_mixed(cv, set(zb), lambda p: mat(p, *zb[p]))


# ---------------------------------------------------------------------------- round bodies
def revolve(cv, x, cy2, diams, ramp, region_out=None, outline=True):
    """A round body lying along x, seen side-on: column ``x + i`` has diameter ``diams[i]``,
    centred on the doubled centre row ``cy2`` (cy2 = 2 * centre; odd = between two rows).
    Each column is shaded by CYL_BANDS of its own diameter; the silhouette gets the vanilla
    outline (the end columns become ramp[1] on the left, ramp[0] on the right)."""
    pts = {}
    for i, d in enumerate(diams):
        if not d:
            continue
        top = (cy2 - d + 1) // 2
        bands = CYL_BANDS[max(2, d)] if d >= 2 else (3,)
        for j in range(d):
            k = bands[j]
            pts[(x + i, top + j)] = ramp[5] if k == 'H' else ramp[k]
    for p, c in pts.items():
        cv.set(*p, c)
    if outline:
        lt, dk = _ring(set(pts))
        # keep the band table's top/bottom outline rows; re-outline only the ends/steps
        for p in lt:
            cv.set(*p, ramp[1])
        for p in dk:
            cv.set(*p, ramp[0])
    if region_out is not None:
        region_out |= set(pts)
    return set(pts)


def pedestal(cv, x=3, y=12, w=9, hgt=3, d=2):
    """The STEEL stand of the turbinesteam machines (base.png): a flat plinth."""
    return box(cv, x, y, w, hgt, d, STEEL)


# ---------------------------------------------------------------------------- fuel generator
def fuel_generator(tier, disp=1):
    """fuelgeneratorXXv.obj: light-grey corner frame, a tier-coloured bevelled cover on top,
    a dark ribbed engine core seen through the open sides, and a tier control panel with a
    white display and a switch on one face (drawn as the front)."""
    B = tier_ramp(tier)
    cv = Canvas(16)
    d, x0, y0, fw, fh = depth(cv, 'block'), 1, 5, 10, 10
    xr, yb = x0 + fw - 1, y0 + fh - 1
    cover_rows = 3                                   # front rows 5..7
    zb = solid([('body', x0, y0, fw, fh, d)])

    def mat(p, n, f, k):
        px, py = p
        if f == 'top':
            return B
        if f == 'front':
            if py < y0 + cover_rows:
                return B
            if px <= x0 + 1 or px >= xr - 1 or py == yb:
                return STEEL
            return B
        # side face: local front-plane row and depth
        ly = py + k
        if ly < y0 + cover_rows:
            return B
        if k in (1, d) or ly == yb:
            return STEEL
        return CAV

    cut = [(x0, y0), (xr + d, y0 - d)]                # bevelled cover: chamfered corners
    paint_mixed(cv, zb, mat, bevel=True, cut=cut)
    # the engine core seen through the side opening: flat CAV[2], two ribs (CAV[3]) running
    # with the depth, 3 rows apart (no per-row alternation: that was a checkerboard)
    ribs = (y0 + cover_rows + 1, y0 + cover_rows + 4)
    for p in faces(zb, 'body', 'side'):
        if p in cut:
            continue
        k = zb[p][2]
        ly = p[1] + k
        if y0 + cover_rows <= ly < yb and 1 < k < d:
            cv.set(*p, CAV[3] if ly in ribs else CAV[2])
    # shadow under the cover's lip, front and side
    for xx in range(x0 + 2, xr - 1):
        cv.set(xx, y0 + cover_rows, B[1])
    # control panel: white display (with a dark trace) and the switch
    if disp == 1:                                     # display: a lit row over its dark frame
        cv.hline(x0 + 2, x0 + 4, y0 + 5, WHITE)
        cv.hline(x0 + 2, x0 + 4, y0 + 6, CAV[1])
    else:                                             # display: white, one dark text pixel
        cv.rect(x0 + 2, y0 + 5, 3, 2, WHITE)
        cv.set(x0 + 3, y0 + 5, CAV[3])
    cv.set(x0 + 6, y0 + 4, WHITE)                     # switch: lit toggle over its slot
    cv.set(x0 + 6, y0 + 5, CAV[1])
    cv.hline(x0 + 2, x0 + 6, y0 + 8, B[2])            # vent line under the panel
    return cv


# ---------------------------------------------------------------------------- heat turbine
def heat_turbine(tier):
    """turbineb / turbinebblue: a heptagonal housing lying on its side, stepped in ribs and
    tapering from the wide intake (right) to the narrow end (left), on grey feet; the grey
    thermal port blocks sit under both ends.  The housing is tier-coloured in-world, so the
    whole housing is the tier ramp (MATCH; the manifest says cue 'band', reported)."""
    B = tier_ramp(tier)
    cv = Canvas(16)
    # feet / port blocks first (the housing sits on them)
    box(cv, 1, 11, 3, 3, 1, STEEL, bevel=False)
    box(cv, 10, 11, 3, 3, 1, STEEL, bevel=False)
    # stepped housing, centre row 6.5: ribs of growing diameter, narrow end left
    diams = [6, 6, 8, 8, 8, 10, 10, 10, 10, 10, 10, 10, 10]
    reg = revolve(cv, 1, 13, diams, B)
    # rib seams: a shaded column at each step and one inside the wide section
    for sx in (3, 6, 9):
        top = (13 - diams[sx - 1] + 1) // 2
        for yy in range(top + 1, top + diams[sx - 1] - 1):
            c = cv.get(sx, yy)
            cv.set(sx, yy, B[2] if c in (B[4], B[5], B[3]) else B[1])
    # the thermal ports: a dark opening in each port block's front face.  The model frames
    # them blue (cold) and red (hot); those hues are left out because the tier is the only
    # hue on the device (STYLE section 7) and MARK_BLUE beside the MV body read as MV.
    cv.set(2, 12, CAV[1])
    cv.set(11, 12, CAV[1])
    return cv


# ---------------------------------------------------------------------------- shaft machines
def _shaft(cv, y, x0, x1):
    """A 2 px steel shaft from x0 to x1 (inclusive) on rows y, y+1."""
    cv.hline(x0, x1, y, STEEL[4])
    cv.hline(x0, x1, y + 1, STEEL[2])


def _disc_mask(x, y, d):
    r = d / 2.0
    cx, cy = x + r, y + r
    lim = r * r + (0.6 if d <= 6 else 0.3 * r)
    return {(xx, yy) for yy in range(y, y + d) for xx in range(x, x + d)
            if (xx + 0.5 - cx) ** 2 + (yy + 0.5 - cy) ** 2 <= lim}


def _edge(mask):
    return {p for p in mask if any(q not in mask for q in
                                   ((p[0] - 1, p[1]), (p[0] + 1, p[1]), (p[0], p[1] - 1), (p[0], p[1] + 1)))}


def _fan(cv, mouth, cx, cy, n=4, twist=0.5, gap=0.25, hub_r=1.2, phase=0.3):
    """A pinwheel fan filling ``mouth`` (pixel set) round the centre (cx, cy): steel blades,
    lit on the top-left half, with ``n`` dark gaps on a spiral that starts off the radius
    (so no gap runs straight through the hub and no 'X' forms)."""
    import math
    step = 2 * math.pi / n
    for (px, py) in mouth:
        dx, dy = px + 0.5 - cx, py + 0.5 - cy
        r = math.hypot(dx, dy)
        if r <= hub_r:
            continue
        a = (math.atan2(dy, dx) - twist * r - phase) % step
        if a < gap * step:
            c = CAV[2]
        else:
            c = STEEL[3] if dx + dy < 0 else STEEL[2]
        cv.set(px, py, c)


def steam_turbine(x=2, y=4, D=10, recede=3, lip=False, **fan):
    """turbinesteam/turbine.obj seen from its intake end (the model's 'by' view, as the iso
    views show it): the pale mint capsule cowl recedes up-right behind a round mouth with the
    steel fan inside (a pinwheel: four offset spiral gaps round a 2x2 hub, so no 'X' through
    the hub), on the light-grey stand seen from its short end (6 wide).  The cowl recedes
    3 px so the capsule body shows; the model's thin mint lip is left out (with it the fan
    shrank to 6 px and the pinwheel no longer read), so the fan meets the cowl outline."""
    cv = Canvas(16)
    pedestal(cv, x + D // 2 - 3, 12, 6, 3, 2)
    # capsule: the front ring, more full sections and a rounded rear dome, receding (+1, -1)
    layers = [(k, D) for k in range(recede)] + [(recede, D - 2)]
    reg = set()
    for k, dk in layers:
        o = (D - dk) // 2
        reg |= {(px + k, py - k) for (px, py) in _disc_mask(x + o, y + o, dk)}
    front = _disc_mask(x, y, D)
    cx, cy = x + D / 2.0, y + D / 2.0
    for (px, py) in reg - front:                      # cowl body seen behind the mouth
        dx, dy = px + 0.5 - (cx + 1), (cy - 1) - (py + 0.5)
        cv.set(px, py, COWL[4] if dy > dx * 0.8 else COWL[2])
    for p in front:
        cv.set(*p, COWL[3])
    outline_region(cv, reg, COWL[0], COWL[1])
    inner = front - _edge(front)
    if lip:                                           # thin mint lip, lit top-left
        lipp = _edge(inner)
        for (px, py) in lipp:
            s = (px + 0.5 - cx) + (py + 0.5 - cy)
            cv.set(px, py, COWL[4] if s < -1 else (COWL[2] if s > 1 else COWL[3]))
        inner = inner - lipp
    _fan(cv, inner, cx, cy, **fan)
    mx, my = x + D // 2, y + D // 2                   # hub: the 2x2 at mx-1..mx, my-1..my
    cv.rect(mx - 1, my - 1, 2, 2, STEEL[4])
    cv.set(mx, my, STEEL[2])
    return cv


def gas_turbine(rack=(2, 7, 11, 3), slots=(4, 7, 10), plate=None, diams=None):
    """turbinesteam/gasturbine.obj seen side-on (the model's rx view): an angular mint cowl
    (straight chamfers at both ends) with the finned rack (four fins) standing proud on its side, the
    shaft knob at the rear, on the light-grey pedestal.

    The rack is lighter than the cowl behind it: a COWL[5] top lip, a COWL[4] front with
    dark CAV slots between the fins, a COWL[2] side and a COWL[1] shadow row under it, so it
    reads as raised at GUI 2.  It is the same material as the cowl, so it has no outline
    ring of its own (STYLE section 4: one material, one outline).  The small plate on top of
    the model is left out: drawn at 16 px it read as a lid knob (a cooking pot)."""
    cv = Canvas(16)
    pedestal(cv)
    _shaft(cv, 8, 13, 14)
    if plate:
        px_, py_, pw, ph = plate
        box(cv, px_, py_, pw, ph, 1, COWL, bevel=False)
    diams = diams or [5, 7, 9, 9, 9, 9, 9, 9, 9, 9, 9, 7, 5]     # straight chamfers: angular
    revolve(cv, 1, 16, diams, COWL)
    rx, ry, rw, rh = rack
    cv.hline(rx + 1, rx + rw, ry - 1, COWL[5])          # top lip (depth 1)
    cv.rect(rx, ry, rw, rh, COWL[4])                    # front
    cv.vline(rx + rw, ry, ry + rh - 2, COWL[2])         # right side
    cv.hline(rx, rx + rw, ry + rh, COWL[1])             # shadow under the rack
    for sx in slots:                                    # dark slots between the fins
        cv.vline(sx, ry, ry + rh - 1, CAV[2])
        cv.set(sx + 1, ry - 1, COWL[3])                 # the slot seen in the top lip
    return cv


def generator():
    """turbinesteam/generator.obj: a straight mint cylinder with flat ends, a row of white LEDs
    along its top and the shaft leaving its open (fan) end, a small knob on the closed end, on
    the pedestal."""
    cv = Canvas(16)
    pedestal(cv)
    cylinder(cv, 2, 2, 12, 10, COWL)
    cv.hline(3, 12, 5, COWL[2])                       # the LED strip along the top
    for xx in range(4, 12, 2):
        cv.set(xx, 5, WHITE)
    _shaft(cv, 6, 0, 1)                               # the shaft leaves the open (fan) end only
    cv.set(13, 6, STEEL[3])                           # the closed end's small knob
    cv.set(13, 7, STEEL[1])
    return cv


# ---------------------------------------------------------------------------- water turbine
def _paddle_px(i, n, r0, R):
    """Pixels of paddle ``i`` of ``n`` round the canvas centre (8.0, 8.0), seen edge-on: a
    2 px radial stroke.  Returns {(x, y): lit?}: the half facing the light is lit, the other
    half is the stick-style dark edge (vanilla tie-break: open left = light, open right = dark)."""
    import math
    a = 2 * math.pi * i / n
    ux, uy = math.cos(a), math.sin(a)
    nx, ny = -uy, ux                                   # a normal; make it the shaded side
    if nx + ny < -1e-6 or (abs(nx + ny) < 1e-6 and nx < 0):
        nx, ny = -nx, -ny
    out = {}
    for py in range(16):
        for px in range(16):
            vx, vy = px + 0.5 - 8, py + 0.5 - 8
            along = vx * ux + vy * uy
            s = vx * nx + vy * ny
            if r0 <= along <= R and -0.6 <= s <= 0.8:
                out[(px, py)] = s < 0.25
    return out


def water_turbine(hub=6, r0=2.0, R=7.4, n=8):
    """smallwaterwheel.obj seen along its axle (the model's rx/lx view): eight flat grey
    paddles seen edge-on, radiating from the small green generator housing on the axle.

    Each paddle is a 2 px radial stroke split like a vanilla stick: the half facing the light
    in STEEL[3] (STEEL[4] on the paddles pointing up/left), the other half its STEEL[1] dark
    edge.  A 2 px part has no room for a separate outline ring (``strip()`` would make it
    4 px wide and the eight paddles would merge into a disc).  The housing is the model's
    green (tex.png #306048), drawn as an LV ``disc`` (face 3) over the paddle roots."""
    cv = Canvas(16)
    for i in range(n):
        import math
        a = 2 * math.pi * i / n
        lit_c = STEEL[4] if math.cos(a) + math.sin(a) < -0.1 else STEEL[3]
        for p, lit in _paddle_px(i, n, r0, R).items():
            cv.set(*p, lit_c if lit else STEEL[1])
    o = (16 - hub) // 2
    disc(cv, o, o, hub, TIER['lv'], face=3)            # generator housing on the axle
    cv.set(o + hub // 2 - 1, o + hub // 2 - 1, STEEL[3])  # axle cap
    cv.set(o + hub // 2, o + hub // 2, STEEL[1])
    return cv


# ---------------------------------------------------------------------------- wind turbine
def wind_turbine(hx=8, hy=5, L=6.0, angles=(180, 60, -60), foot_rows=2):
    """windturbinemini.obj, rotor facing the viewer (the model's fy view): a slim white tower
    over most of the height ending in a short yellow/black hazard band and a black foot, a
    small white nacelle with the red halo light, and three thin white blades.  No blade lies
    along the tower: with three blades 120 deg apart that leaves the rotor at 0/120/240 or
    60/180/300 (this one: one blade level to the left, two to the right)."""
    import math
    W = WHITE_PLASTIC
    cv = Canvas(16)
    yb = 14                                                            # black foot row
    y_band = yb - foot_rows                                            # first hazard row
    cylinder(cv, hx - 1, hy + 1, y_band - hy - 1, 3, W, axis='y', round_ends=False)   # tower
    # hazard band: the tower's own foot, yellow with one black diagonal stripe, black bottom
    for yy in range(y_band, yb):
        for xx in range(hx - 1, hx + 2):
            black = (xx + yy) % 3 == 0
            if black:
                c = RUBBER_BLACK[2]
            else:
                c = RUBBER_Y[2] if xx == hx - 1 else (RUBBER_Y[1] if xx == hx + 1 else RUBBER_Y[3])
            cv.set(xx, yy, c)
    cv.hline(hx - 1, hx + 1, yb, RUBBER_BLACK[1])
    # nacelle (behind the rotor)
    box(cv, hx - 1, hy - 1, 3, 2, 1, W, bevel=False)
    cv.set(hx + 1, hy - 3, MARK_RED)                                   # halo light on top
    # blades: 1 px W[3] lines, a W[1] shade pixel beside the root, W[2] tip
    for ang in angles:
        a = math.radians(ang)
        tip = (round(hx + L * math.cos(a)), round(hy + L * math.sin(a)))
        pts = line_pts((hx, hy), tip)[1:]
        for j, (px, py) in enumerate(pts):
            cv.set(px, py, W[2] if j == len(pts) - 1 else W[3])
        # root shade: the side away from the light
        px, py = pts[0]
        sx, sy = (0, 1) if abs(math.cos(a)) > 0.7 else (1, 0)
        cv.set(px + sx, py + sy, W[1])
    cv.set(hx, hy, W[4])                                               # hub (spinner)
    return cv

# ---------------------------------------------------------------------------- electrical source
#: the printed voltage-source symbol (voltagesource.png): a ring with '+' in the upper half and
#: '-' in the lower.  9 x 9 so that '+' and '-' each keep a clear pixel from the ring and from
#: each other (in a 7 x 7 ring they touched and, with the leads, read as a tree in a circle).
#: The leads are left out: on the 11 x 11 front they would run into the plate's edge rows.
SOURCE_SYMBOL = [
    '...###...',
    '.##...##.',
    '.#..#..#.',
    '#..###..#',
    '#...#...#',
    '#.......#',
    '.#.###.#.',
    '.##...##.',
    '...###...',
]


def electrical_source(sym=None, at=(3, 5)):
    """voltagesource.obj: a pale blue-grey sixnode plate (front 11 x 11, depth 2) printed with
    the voltage-source symbol.  The front is 11 wide (not 12) so the symbol has a centre
    column."""
    cv = Canvas(16)
    box(cv, 2, 4, 11, 11, 2, SOURCE, bevel=True)
    cv.overlay(sym or SOURCE_SYMBOL, {'#': INK}, *at)
    return cv
