"""Family helper: parts_mechanical (manifest family machine_part) - crafting parts and machine
upgrades with no world model of their own: ferromagnetic cores, auto-miner drills, magnets,
machine blocks, motors, fuel burners, the transformer casing, the combustion chamber, the mining
pipe and thermal insulation.

Shared visual system
--------------------
* **Front-on components** (STYLE section 6, non-placeables): each object is drawn once, in cabinet
  oblique where it is box-like (depth recedes up-right) or as the kit's round primitives, filling
  the body box on baseline 14.
* **Quality ladder** (STYLE 7b, ``kit.decisions.quality``): one drawing per family, the grade is
  ONLY the trim colour of one fixed band/rim, the same place on every sibling:
      level 0 -> the body material itself (no copper or gold anywhere on the icon)
      level 1 -> VCOPPER        level 2 -> GOLD
  core: the rim round the front of the window;  drill: the 2 px band across the full width of
  the bit where the plain shank meets the flutes;  magnet: the band between the two painted
  poles;  machine block: the rim round the grille;  motor: the 1 px end-bell rim just inside
  the end face's outline.
* **Diagonal shafts**: the mining pipe lies bottom-left -> top-right (the vanilla ingot pose) with
  its bore towards you.  The drill bit lies on the same 45-degree axis but points DOWN-LEFT
  (tip at the bottom-left, shank top-right, as the 1.12 art points down) so it never sits in
  the vanilla sword pose; shank and flutes have the same width (no guard).
  ``diag_body`` sweeps a cross-section along the 45-degree axis and shades it across the axis
  (lit upper-left, shaded lower-right), then outlines it by the vanilla rule.
* **Size axis** (fuel burners): one drawing, ``burner(box)``, fitted into SMALL_BOX / MEDIUM_BOX /
  BODY_BOX (STYLE 7b).  Front face 6 / 7 / 8 wide, lit port 2x2 / 3x3 / 4x4.  All three share
  one body material (TERRACOTTA, the rust-brown of the 1.12 medium/big burner textures;
  untiered, so TERRACOTTA is allowed).
* **Magnet paint**: the kit has no red/blue paint ramp, so the poles reuse CINNABAR (red, N) and
  CAP_BLUE (blue, S) unchanged; the body between them is FERRITE (palette: "magnets").
  Reported as a kit request (paint ramps).
* **Thermal parts** (combustion chamber, insulation) are THERMAL boxes with the lit rim the
  palette asks for (ramp[4] just inside the left outline and along the front's top edge), so
  they keep an edge on a dark background.  The chamber's embers borrow COPPER (no fire ramp in
  the kit; reported as a kit request).
"""
from kit import (Canvas, box, solid, paint_solid, faces, outline_region, cylinder, disc,
                 quality, QUALITY_TRIM,
                 STEEL, CAV, FERRITE, CINNABAR, CAP_BLUE, LIT, COPPER,
                 TERRACOTTA, THERMAL, RUBBER_BLACK)


def trim_ramp(level, body):
    """The quality trim ramp: ``body`` at level 0, VCOPPER at 1, GOLD at 2."""
    r = QUALITY_TRIM.get(level)
    return body if r is None else r


def trim_of(item_id, body):
    return trim_ramp(quality(item_id), body)


def _ring_sets(reg):
    """Boundary of ``reg`` split by the vanilla rule into (light, dark) pixel sets."""
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


def outline_by_owner(cv, owner):
    """Outline the union of several materials as ONE silhouette, each outline pixel in the ramp of
    the material it belongs to (``owner``: {(x, y): ramp}).  Used where two painted materials
    meet edge to edge (magnet poles, drill trim band) without a seam between them."""
    reg = set(owner)
    light, dark = _ring_sets(reg)
    for p in light:
        cv.set(*p, owner[p][1])
    for p in dark:
        cv.set(*p, owner[p][0])


def disc_mask(x, y, d):
    """Pixels of a pixel-art circle of diameter ``d`` (same maths as the kit's ``disc``); at
    d = 4 the kit's circle is a square, so its corners are cut here to keep it round."""
    r = d / 2.0
    cx, cy = x + r, y + r
    lim = r * r + (0.6 if d <= 6 else 0.3 * r)
    m = {(xx, yy) for yy in range(y, y + d) for xx in range(x, x + d)
         if (xx + 0.5 - cx) ** 2 + (yy + 0.5 - cy) ** 2 <= lim}
    if d == 4:
        m -= {(x, y), (x + 3, y), (x, y + 3), (x + 3, y + 3)}
    return m


# ------------------------------------------------------------------------------------------------
# diagonal shafts
# ------------------------------------------------------------------------------------------------
def diag_region(profile, c=15):
    """Pixels of a section swept along the 45-degree axis x + y = ``c``: {t: half-width} with
    t = x - y along the axis and s = x + y - c across it (|s| <= hw)."""
    reg = set()
    for y in range(16):
        for x in range(16):
            t, s = x - y, x + y - c
            hw = profile.get(t)
            if hw is not None and abs(s) <= hw:
                reg.add((x, y))
    return reg


def diag_face(ramp, s, hw):
    """Face colour across the axis: upper-left (s < 0) ramp[4] (the line next to the light
    outline ramp[5] when the ramp has one), the axis ramp[3], lower-right ramp[2]."""
    if s < 0:
        return ramp[5] if (len(ramp) > 5 and s == -hw + 1 and hw >= 3) else ramp[4]
    return ramp[3] if s == 0 else ramp[2]


def diag_body(cv, profile, ramp, c=15, shade=None):
    """Sweep a round section along the 45-degree axis x + y = ``c`` (bottom-left -> top-right).

    ``profile``: {t: half-width} where t = x - y (along the axis, -13 bottom-left .. 13
    top-right) and the half-width is in units of s = x + y - c (across the axis; |s| <= hw).
    Faces across the axis by ``diag_face``, then the vanilla outline.
    ``shade(x, y, s, t)`` may return a colour to override a face pixel (grooves).
    Returns the pixel set."""
    reg = diag_region(profile, c)
    for (x, y) in reg:
        t, s = x - y, x + y - c
        col = diag_face(ramp, s, profile[t])
        if shade is not None:
            o = shade(x, y, s, t)
            if o is not None:
                col = o
        cv.set(x, y, col)
    outline_region(cv, reg, ramp[0], ramp[1])
    return reg


#: drill bit along the axis (t): conical tip at the bottom-left, flutes, trim band, plain shank
DRILL_C = 15
DRILL_TIP = {-13: 0, -12: 1, -11: 2}
DRILL_FLUTES = range(-10, 4)
DRILL_BAND = (4, 5)
DRILL_SHANK = range(6, 11)
DRILL_HW = 3


def drill_groove(t, s):
    """The helical flute: a dark band 2 steps wide every 4 steps along a line slanted across the
    axis (t + s), i.e. a helix seen on the 45-degree bit; tip and fluted body only."""
    return t <= DRILL_FLUTES[-1] and (t + s) % 4 < 2


def drill(level):
    """Auto-miner drill bit pointing down-left: a STEEL twist drill of one width from end to end
    (no guard), a short conical tip at the bottom-left, helical flutes painted as oblique dark
    bands that also notch the lit edge of the silhouette where they reach it (the serrated
    outline of a twist drill), a 2 px band in the quality trim across the full width where the flutes meet
    the plain shank (level 0: steel, i.e. no band), the shank to the top-right.  The band is
    one solid with the bit: its edge pixels take the trim ramp's own outline steps."""
    cv = Canvas(16)
    c = DRILL_C
    T = trim_ramp(level, STEEL)
    prof = dict(DRILL_TIP)
    for t in list(DRILL_FLUTES) + list(DRILL_BAND) + list(DRILL_SHANK):
        prof[t] = DRILL_HW
    reg = diag_region(prof, c)
    # notches: the flute cut into the lit (upper-left) rim, before outlining; the shaded rim
    # stays continuous so the bit does not break up into lumps at GUI 3
    reg = {(x, y) for (x, y) in reg
           if not (prof[x - y] >= 2 and x + y - c <= -prof[x - y]
                   and drill_groove(x - y, x + y - c))}
    owner = {}
    for (x, y) in reg:
        t, s = x - y, x + y - c
        R = T if t in DRILL_BAND else STEEL
        col = diag_face(R, s, prof[t])
        if drill_groove(t, s):
            col = STEEL[2] if s < 0 else STEEL[1]
        cv.set(x, y, col)
        owner[(x, y)] = R
    outline_by_owner(cv, owner)
    return cv


def mining_pipe():
    """Mining pipe: a STEEL tube lying bottom-left -> top-right, its open end (the bore) facing you
    at the bottom-left, a raised coupling collar near the far end (the pipe is a string of
    segments)."""
    cv = Canvas(16)
    R = STEEL
    c = 16
    tube = {t: 4 for t in range(-7, 11)}
    tube[10] = 3                                   # rounded far end, clear of col 15
    diag_body(cv, tube, R, c=c)
    # coupling collar: one step wider than the tube, 2 px long, its own outline, darker faces
    COLLAR = (STEEL[0], STEEL[1], STEEL[2], STEEL[2], STEEL[3])
    diag_body(cv, {5: 5, 6: 5}, COLLAR, c=c)
    # open front end: a ring round the bore (its own outline), centred on the axis
    disc(cv, 1, 8, 7, R, face=3)
    cv.rect(3, 10, 3, 3, CAV[0])
    cv.set(5, 12, CAV[3])                          # lit far wall of the bore (bottom-right)
    cv.set(5, 11, CAV[2])
    cv.set(4, 12, CAV[2])
    return cv


# ------------------------------------------------------------------------------------------------
# ferromagnetic core
# ------------------------------------------------------------------------------------------------
def core(level):
    """Laminated square core frame (the in-world ``feromagneticcorea`` part): one FERRITE solid of
    four bars, see-through window, the trim on the front rim of the window."""
    cv = Canvas(16)
    R = FERRITE
    x0, y0, s, b, d = 1, 3, 12, 3, 2
    zb = solid([('top', x0, y0, s, b, d), ('bot', x0, y0 + s - b, s, b, d),
                ('l', x0, y0 + b, b, s - 2 * b, d), ('r', x0 + s - b, y0 + b, b, s - 2 * b, d)])
    paint_solid(cv, zb, R)
    # inner jamb of the left leg seen through the window: ramp[1]; inner floor: ramp[3]
    for p in faces(zb, 'l', 'side'):
        if x0 + b <= p[0] and p[1] >= y0 + b and cv.get(*p) == R[2]:
            cv.set(*p, R[1])
    for p in faces(zb, 'bot', 'top'):
        if p[0] >= x0 + b and cv.get(*p) in (R[4], R[5]):
            cv.set(*p, R[3] if zb[p][2] > 1 else R[2])
    # laminations: two seams on the front of each leg
    for yy in (y0 + 5, y0 + 7):
        cv.set(x0 + 1, yy, R[2])
        cv.set(x0 + s - 2, yy, R[2])
    # trim: the front rim round the window (1 px), lit top-left, shaded bottom-right
    T = trim_ramp(level, None)
    if T is not None:
        wx0, wy0, wx1, wy1 = x0 + b - 1, y0 + b - 1, x0 + s - b, y0 + s - b
        for xx in range(wx0, wx1 + 1):
            for yy in range(wy0, wy1 + 1):
                if xx in (wx0, wx1) or yy in (wy0, wy1):
                    lit = (yy == wy0 and xx < wx1) or (xx == wx0 and yy < wy1)
                    cv.set(xx, yy, T[4] if lit else T[2])
    return cv


# ------------------------------------------------------------------------------------------------
# magnet
# ------------------------------------------------------------------------------------------------
def magnet(level):
    """Bar magnet lying across the slot: red N pole (left), FERRITE / trim band in the middle,
    blue S pole (right); one silhouette, outline in each part's own ramp."""
    cv = Canvas(16)
    band = trim_ramp(level, FERRITE)
    x0, y0, w, hgt, d = 1, 9, 10, 6, 4
    parts = [('n', x0, y0, 4, hgt, d), ('b', x0 + 4, y0, 2, hgt, d), ('s', x0 + 6, y0, 4, hgt, d)]
    zb = solid(parts)
    ramps = {'n': CINNABAR, 'b': band, 's': CAP_BLUE}
    owner = {}
    for p, (name, face, k) in zb.items():
        R = ramps[name]
        col = {'front': R[3], 'top': R[4], 'side': R[2]}[face]
        if face == 'top' and k == 1 and len(R) > 5:
            col = R[5]
        cv.set(*p, col)
        owner[p] = R
    outline_by_owner(cv, owner)
    return cv


# ------------------------------------------------------------------------------------------------
# full-block items: machine block, casing, combustion chamber
# ------------------------------------------------------------------------------------------------
BLOCK = (1, 5, 10, 10, 4)          # front x, y, w, h + depth 4: the kit's full-block box


def machine_block(level):
    """Machine casing block: STEEL cube, a recessed grille on the front framed by a rim in the
    quality trim (level 0: a steel rim = plain), a smaller grille panel on the top face."""
    cv = Canvas(16)
    x, y, w, hh, d = BLOCK
    box(cv, x, y, w, hh, d, STEEL)
    T = trim_ramp(level, None)
    gx0, gy0, gx1, gy1 = x + 2, y + 2, x + w - 3, y + hh - 3     # grille 6x6 (cols 3..8, rows 7..12)
    for yy in range(gy0, gy1 + 1):
        for xx in range(gx0, gx1 + 1):
            if xx == gx0 or yy == gy0:
                c = CAV[2]                                         # recess shadow (top/left jamb)
            else:
                c = CAV[0] if (xx - gx0) % 2 == 0 else CAV[3]      # vertical bars of the grille
            cv.set(xx, yy, c)
    # rim round the grille
    rx0, ry0, rx1, ry1 = gx0 - 1, gy0 - 1, gx1 + 1, gy1 + 1
    for xx in range(rx0, rx1 + 1):
        for yy in range(ry0, ry1 + 1):
            if xx in (rx0, rx1) or yy in (ry0, ry1):
                lit = (yy == ry0) or (xx == rx0)
                if T is None:
                    cv.set(xx, yy, STEEL[4] if (xx == rx1 or yy == ry1) else STEEL[2])
                else:
                    cv.set(xx, yy, T[4] if lit else T[2])
    # top-face panel: a recessed slot two rows deep
    for k in (2, 3):
        for xx in range(x + 2 + k, x + w - 2 + k):
            cv.set(xx, y - k, CAV[3] if k == 2 else CAV[2])
    return cv


def casing():
    """Transformer casing (the in-world ``transformatorCase`` it installs on the DC-DC converter):
    a light STEEL box whose front is a plain panel (one screw, as in the 1.12 art) above a short
    strip of vertical vent slots across its lower three rows, as on the model's louvre face (the
    model's tier-tinted label band is left out: the casing is untiered); two louvre slots on the
    side.  The latch is left out: at 16 px it merged with the slots."""
    cv = Canvas(16)
    x, y, w, hh, d = BLOCK
    box(cv, x, y, w, hh, d, STEEL)
    vy0, vy1 = y + hh - 4, y + hh - 2                 # rows 11..13
    for xx in (3, 5, 7, 9):
        cv.vline(xx, vy0, vy1, STEEL[1])
        cv.set(xx, vy0, CAV[0])
    cv.set(x + 2, y + 2, STEEL[1])                    # screw
    # louvres on the side face (2 slots)
    for k in (1, 3):
        cv.vline(x + w + k - 1, y - k + 3, y - k + hh - 4, STEEL[1])
    return cv


def lit_rim(cv, x, y, hh, R, top=True, w=None):
    """The thermal lit rim: ramp[4] in the column just inside the front's left outline (and,
    with ``top``, along the front's top row), so a dark THERMAL box keeps an edge on a dark
    background (palette note on THERMAL; cf. power_passive.large_rheostat)."""
    for yy in range(y, y + hh - 1):
        cv.set(x + 1, yy, R[4])
    if top and w is not None:
        for xx in range(x + 1, x + w - 1):
            cv.set(xx, y, R[4])


def combustion_chamber():
    """Firebox: a THERMAL (charcoal, heat-system) box with a square opening on the front and the
    fire inside (LIT flames over embers); a flue slot in the top; the thermal lit rim."""
    cv = Canvas(16)
    R = THERMAL
    x, y, w, hh, d = BLOCK
    box(cv, x, y, w, hh, d, R)
    lit_rim(cv, x, y, hh, R)
    ox0, oy0, ox1, oy1 = x + 2, y + 2, x + w - 3, y + hh - 2     # opening cols 3..8, rows 7..13
    for yy in range(oy0, oy1 + 1):
        for xx in range(ox0, ox1 + 1):
            cv.set(xx, yy, R[1] if xx == ox0 else CAV[0])
    flame = [
        "...4..",
        "..43..",
        ".4532.",
        ".5532.",
        "455322",
        "C33C3C",
    ]
    pal = {'5': LIT[5], '4': LIT[3], '3': LIT[2], '2': COPPER[3], 'C': COPPER[2]}
    cv.overlay(flame, pal, ox0, oy1 - 5)
    # lintel lip over the opening
    cv.hline(ox0, ox1, oy0, R[1])
    # flue slot on the top face
    for xx in range(x + 4, x + 8):
        cv.set(xx + 2, y - 2, CAV[0])
    return cv


# ------------------------------------------------------------------------------------------------
# fuel burner (size axis)
# ------------------------------------------------------------------------------------------------
#: box width -> (visible pipe length, depth, pipe diameter, port diameter); the front face is
#: bw - pl - d = 6 / 7 / 8 wide, the lit port (port diameter - 2) 2x2 / 3x3 / 4x4
BURNER_GEOM = {10: (2, 2, 3, 4), 12: (2, 3, 4, 5), 14: (3, 3, 4, 6)}


def burner(bbox):
    """Fuel burner fitted into ``bbox`` = (x, y, w, h): a TERRACOTTA burner block, a STEEL fuel
    pipe entering its left side, a round glowing burner port (black rubber nozzle rim) on the
    front and cooling fins (dark grooves) on its side face.  Same drawing at every size: only the
    geometry scales (``BURNER_GEOM``)."""
    bx, by, bw, bh = bbox
    cv = Canvas(16)
    pl, d, pd, pdm = BURNER_GEOM[bw]
    fx, fw = bx + pl, bw - pl - d              # block front
    fy, fh = by + d, bh - d
    R = TERRACOTTA
    box(cv, fx, fy, fw, fh, d, R)
    # cooling fins: dark grooves across the side face, every other row
    for k in range(1, d + 1):
        for yy in range(fy - k + 2, fy - k + fh - 1, 2):
            cv.set(fx + fw - 1 + k, yy, R[1])
    # fuel pipe into the left face (lying cylinder, its own outline)
    py = fy + (fh - pd) // 2 + (1 if pd >= 4 else 0)
    cylinder(cv, bx, py, pl + 1, pd, STEEL, round_ends=False)
    cv.set(bx + pl, py, STEEL[1])
    # burner port: a round black nozzle rim round a glowing window, on the front face
    px = fx + (fw - pdm) // 2 + (1 if fw - pdm >= 3 else 0)
    pyy = fy + (fh - pdm) // 2
    reg = disc_mask(px, pyy, pdm)
    for p in reg:
        cv.set(*p, RUBBER_BLACK[2])
    rim = outline_region(cv, reg, RUBBER_BLACK[0], RUBBER_BLACK[1])
    win = reg - rim
    s0 = min(xx + yy for (xx, yy) in win)
    s1 = max(xx + yy for (xx, yy) in win)
    for (xx, yy) in win:
        s = xx + yy - s0
        if s == 0:
            col = LIT[4]
        elif s == 1:
            col = LIT[3]
        elif xx + yy == s1 and s1 - s0 >= 3:
            col = LIT[1]
        else:
            col = LIT[2]
        cv.set(xx, yy, col)
    return cv


# ------------------------------------------------------------------------------------------------
# motor
# ------------------------------------------------------------------------------------------------
#: rib positions on the barrel: angles (degrees, 0 = right, -90 = up) of the front rim points
#: the cooling ribs run back from
MOTOR_RIBS = (-150, -110, -70, -30, 10, 50)


def motor(level):
    """Electric motor seen end-on, as in the 1.12 art, with cabinet-oblique depth: the round
    STEEL end face (d = 10) on two mounting feet, the housing barrel receding up-right with
    cooling ribs running back along it, a 1 px end-bell rim just inside the face's outline in
    the quality trim (level 0: steel, i.e. a plain lit/shaded bevel), and the dark hub with the
    bright shaft end in the centre.  The round silhouette keeps the motors apart from the
    family's boxes (casing, machine block)."""
    import math
    cv = Canvas(16)
    R = STEEL
    d, depth = 10, 3
    fx, fy = 1, 14 - d                              # end face: cols 1..10, rows 4..13
    # feet (each its own outline), mostly hidden behind the end face
    for (a, b) in ((2, 4), (d - 3, d - 1)):
        reg = {(xx, yy) for xx in range(a, b + 1) for yy in range(fy + d - 4, 15)}
        for p in reg:
            cv.set(*p, R[2])
        outline_region(cv, reg, R[0], R[1])
    front = disc_mask(fx, fy, d)
    barrel = set()
    for k in range(1, depth + 1):
        barrel |= {(x + k, y - k) for (x, y) in front}
    barrel -= front
    r = d / 2.0
    cx, cy = fx + r, fy + r
    for (x, y) in barrel:
        q = (x + 0.5 - cx) + (y + 0.5 - cy)        # across the axis (< 0: upper-left, lit)
        cv.set(x, y, R[4] if q < -1 else (R[2] if q > 1 else R[3]))
    for ang in MOTOR_RIBS:
        a = math.radians(ang)
        px = int(cx + (r - 0.5) * math.cos(a))
        py = int(cy + (r - 0.5) * math.sin(a))
        for k in range(1, depth + 1):
            p = (px + k, py - k)
            if p in barrel:
                q = (p[0] + 0.5 - cx) + (p[1] + 0.5 - cy)
                cv.set(*p, R[2] if q < -1 else R[1])
    outline_region(cv, barrel, R[0], R[1])
    # end face, its outline, then the end-bell rim in the trim
    for p in front:
        cv.set(*p, R[3])
    inner = front - outline_region(cv, front, R[0], R[1])
    T = trim_ramp(level, R)
    rim = {p for p in inner if any(q not in inner for q in
           ((p[0] - 1, p[1]), (p[0] + 1, p[1]), (p[0], p[1] - 1), (p[0], p[1] + 1)))}
    for p in rim:
        s = (p[0] + 0.5 - cx) + (p[1] + 0.5 - cy)
        cv.set(*p, T[4] if s < -r / 2 else (T[2] if s > r / 2 else T[3]))
    # hub recess and shaft end
    m = d // 2
    for p in disc_mask(fx + m - 2, fy + m - 2, 4):
        cv.set(*p, CAV[1])
    cv.set(fx + m - 1, fy + m - 1, R[4]); cv.set(fx + m, fy + m - 1, R[3])
    cv.set(fx + m - 1, fy + m, R[3]); cv.set(fx + m, fy + m, R[2])
    return cv


# ------------------------------------------------------------------------------------------------
# thermal insulation
# ------------------------------------------------------------------------------------------------
#: the front's inner area, cols 2..10 x rows 6..13: lit rim column (4), facings (3) top and
#: bottom, the wool field (f = THERMAL[2]) with two rows of lit wavy fibre (5), two periods each
INSULATION_FACE = [
    "433333333",
    "4f5fff5ff",
    "45f5f5f5f",
    "4fff5fff5",
    "4f5fff5ff",
    "45f5f5f5f",
    "4fff5fff5",
    "433333333",
]


def thermal_insulation():
    """A slab of insulation: THERMAL (charcoal, the heat-system colour) box whose front shows the
    wool as lit wavy fibres (two rows of the old texture's zig-zag, two periods each, over the
    whole field) between the two facings, with the thermal lit rim round the front."""
    cv = Canvas(16)
    R = THERMAL
    x, y, w, hh, d = 1, 5, 11, 10, 3
    box(cv, x, y, w, hh, d, R)
    lit_rim(cv, x, y, hh, R, top=True, w=w)
    pal = {'5': R[5], '4': R[4], '3': R[3], 'f': R[2]}
    cv.overlay(INSULATION_FACE, pal, x + 1, y + 1)
    return cv
