#!/usr/bin/env python3
"""Shaft-family device icons (vanilla set, 16 px), drawn with the vanilla kit after the new OBJ models.

Each drawing follows its model seen in the house cabinet oblique (front = the model's x- face full on, depth up-right)
- the reference is render_icons.py's oblique render (notes/ea/shaft/p5-previews/icons_ref_oblique.png) - simplified
to what reads at 16 px: casing, paint colour, the identifying part (gauge, grille, dial, wheel, ports), couplings.

  nix-shell -p 'python3.withPackages(p:[p.pillow p.numpy])' --run 'python3 tools/icons/shaft/draw_devices.py [ids]'
writes src/main/resources/assets/eln/textures/item/<id>.png and prints the kit lint for each.
"""
import os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
sys.path.insert(0, os.path.join(REPO, "tools/icons/vanilla"))
from kit import (Canvas, box, solid, paint_solid, faces, outline_region, cylinder, disc, ring, line, h,  # noqa: E402
                 STEEL, CAV, GOLD, GLASS, RUBBER_BLACK, WHITE, MARK_RED, VCOPPER)
from kit.check import lint  # noqa: E402

OUT = os.path.join(REPO, "src/main/resources/assets/eln/textures/item")


def R(*hx):
    return tuple(h(x) for x in hx)


# ---------------------------------------------------------------- ramps (0 dark outline .. 4 lit top, 5 bevel)
#: cast iron of the shaft models' casings and pedestals (atlas castiron #585450), lifted so it reads on a slot
CAST = R('#242228', '#353236', '#47433f', '#5c5752', '#79736b', '#958e84')
#: dark steel of the shaft itself (atlas darksteel)
DARK = R('#22232a', '#2c2e35', '#3d4048', '#555861', '#737680', '#90939b')
#: paints (shaftkit.PAINT)
HP_BLUE = R('#141f3d', '#1f3466', '#2c4f93', '#3a68b4', '#6d93d0', '#a3bde4')       # hp / motor / pump (52,102,176)
LP_BLUE = R('#18213d', '#1a2a55', '#24407c', '#2f5598', '#5a7dbb', '#8fa8d6')       # lp (40,84,150)
GAS_RED = R('#3a1914', '#5a2117', '#86321f', '#a8432a', '#cd6c4c', '#e59c80')       # gas (150,60,40)
GEN_YEL = R('#3d2808', '#6e4a0f', '#a37216', '#c9961f', '#e8bd4a', '#f6dc8e')       # gen (196,150,40)
TEAL = R('#0f2729', '#1b4447', '#2a6469', '#3b8188', '#62a9ae', '#98ccce')          # condenser (60,120,130)
OLIVE = R('#25220f', '#423d1b', '#625a2a', '#7d723a', '#a0955a', '#c4ba86')         # heat exchanger (120,110,60)
ALLOY = R('#2c2a33', '#4a4853', '#6f6d78', '#94929c', '#bdbbc2', '#dddce0')         # alloy tank shell
IRONT = R('#23262c', '#30333b', '#454951', '#5c6069', '#7b8089', '#9ba0a8')         # iron tank shell (69,73,79)
ORANGE = R('#3b1608', '#6b2a0e', '#a04416', '#c85e22', '#e88a4a', '#f6b483')        # thermal pads / terminal studs
BRASS = GOLD


def sub(cv, fn):
    """Draw fn(c) on its own canvas (own outline) and paste it over cv."""
    c = Canvas(16)
    fn(c)
    cv.paste(c)
    return c


def region_paint(cv, reg, colour_at, ramp):
    """Fill pixel set REG with colour_at(p) and give it the vanilla outline in RAMP."""
    for p in reg:
        cv.set(*p, colour_at(p))
    outline_region(cv, reg, ramp[0], ramp[1])


# ---------------------------------------------------------------- shared parts
def skid(cv, x0, x1, y=13, d=2, ramp=CAST):
    """The base skid: a 2 px tall slab from col x0 to x1 (front), receding d px."""
    sub(cv, lambda c: box(c, x0, y, x1 - x0 + 1, 2, d, ramp, bevel=False))


def coupling(cv, x, cy, side, shaft_to=None):
    """A half coupling seen edge-on: a 2 x 5 steel disc centred on row CY at cols x, x+1, with the shaft (dark,
    2 rows) from the disc to SHAFT_TO (the body's edge). side 'l' or 'r' (which face is lit)."""
    if shaft_to is not None:
        a, b = (x + 2, shaft_to) if side == 'l' else (shaft_to, x - 1)
        for xx in range(min(a, b), max(a, b) + 1):
            cv.set(xx, cy - 1, DARK[3])
            cv.set(xx, cy, DARK[1])
    for yy in range(cy - 2, cy + 3):
        e = yy in (cy - 2, cy + 2)
        cv.set(x, yy, STEEL[1] if (side == 'l' or e) else STEEL[2])
        cv.set(x + 1, yy, STEEL[0] if (side == 'r' or e) else STEEL[3])
    if side == 'l':
        cv.set(x + 1, cy - 1, STEEL[4])
    else:
        cv.set(x, cy - 1, STEEL[4])


def terminal(cv, x, y, w=4, studs=3):
    """A floor terminal plate on the front: steel box w x 2 with copper studs."""
    sub(cv, lambda c: box(c, x, y, w, 2, 1, STEEL, bevel=False))
    for i in range(studs):
        cv.set(x + 1 + i * (w - 2) // max(1, studs - 1) if studs > 1 else x + w // 2, y, ORANGE[3])


def gauge(cv, x, y):
    """A 2x2 pressure gauge: white face, its lower-right shaded, a brass pixel of rim under it."""
    cv.set(x, y, WHITE); cv.set(x + 1, y, WHITE); cv.set(x, y + 1, WHITE); cv.set(x + 1, y + 1, GLASS[2])
    cv.set(x + 1, y + 2, BRASS[1])


def cylz(cv, x, y, length, diam, ramp, hoops=(), hoop_ramp=None, end=None):
    """A drum lying across the picture (the model's z axis) with optional hoop columns and a right end face."""
    reg = cylinder(cv, x, y, length, diam, ramp)
    if hoop_ramp:
        from kit.shapes import CYL_BANDS
        bands = CYL_BANDS[diam]
        for hx in hoops:
            for j, k in enumerate(bands):
                if (x + hx, y + j) in reg:
                    kk = {'H': 4}.get(k, k)
                    cv.set(x + hx, y + j, hoop_ramp[max(0, min(4, kk - 1 if kk > 1 else kk))])
    return reg


# ---------------------------------------------------------------- the devices
def body(c, x, y, w, hgt, d, ramp, round_top=False, cut=(), bevel=True):
    """An oblique box (one solid). ROUND_TOP: the front face banded like a drum lying across the picture (lit
    upper rows, shaded lower rows) and its top corners cut, for the shaft machines' round casings."""
    zb = solid([("b", x, y, w, hgt, d)])
    cut = list(cut)
    if round_top:
        cut += [(x, y), (x + w - 1 + d, y - d)]
    paint_solid(c, zb, ramp, bevel=bevel, cut=cut)
    if round_top:
        # a soft drum: the two upper rows lit, the middle the front shade, the lowest inner row shaded
        for (px, py), (n, f, k) in zb.items():
            if f == 'front' and x < px < x + w - 1 and y < py < y + hgt - 1:
                c.set(px, py, ramp[4] if py <= y + 2 else ramp[2] if py == y + hgt - 2 else ramp[3])
    return zb


def band_cols(c, zb, cols, ramp, x, y, hgt, d):
    """Paint hoops: front columns COLS (and their run over the top face) in RAMP, keeping each pixel's shade."""
    for (px, py), (n, f, k) in zb.items():
        for cx in cols:
            if f == 'front' and px == cx:
                c.set(px, py, ramp[2] if py >= y + hgt - 2 else ramp[3] if py > y + 1 else ramp[4])
            elif f == 'top' and px - k == cx:
                c.set(px, py, ramp[4])
    for cx in cols:
        c.set(cx, y + hgt - 1, ramp[0])


def steam_turbine():
    """shaftsteamhp: a cast-iron casing, its top round, blue paint hoops round both ends, a gauge on the front, the
    governor valve on top; on a skid; half couplings both sides; the signal terminal at the floor."""
    cv = Canvas(16)
    skid(cv, 2, 12)
    coupling(cv, 1, 9, 'l', shaft_to=4)
    coupling(cv, 13, 8, 'r', shaft_to=12)

    def casing(c):
        zb = body(c, 4, 5, 8, 8, 2, CAST, round_top=True)
        band_cols(c, zb, (5, 10), HP_BLUE, 4, 5, 8, 2)
    sub(cv, casing)
    gauge(cv, 7, 7)
    valve(cv, 7, 1)
    terminal(cv, 6, 12, w=3, studs=1)
    return cv


def valve(cv, x, y):
    """A governor/fuel valve on top: a 3-wide steel bonnet with a stem knob, rows y..y+2."""
    cv.set(x + 1, y, STEEL[1]); cv.set(x + 2, y, STEEL[0])
    cv.set(x, y + 1, STEEL[1]); cv.set(x + 1, y + 1, STEEL[4]); cv.set(x + 2, y + 1, STEEL[3]); cv.set(x + 3, y + 1, STEEL[0])
    cv.set(x, y + 2, STEEL[1]); cv.set(x + 1, y + 2, STEEL[3]); cv.set(x + 2, y + 2, STEEL[2]); cv.set(x + 3, y + 2, STEEL[0])


def lp_steam_turbine():
    """shaftsteamlp: the bigger casing (near a full block) with the large steam inlet flange on top, blue hoops,
    gauge; skid, couplings, signal terminal."""
    cv = Canvas(16)
    skid(cv, 1, 13)
    coupling(cv, 0, 9, 'l', shaft_to=2)
    coupling(cv, 14, 7, 'r', shaft_to=14)

    def casing(c):
        zb = body(c, 2, 4, 11, 9, 2, CAST, round_top=True)
        band_cols(c, zb, (3, 11), LP_BLUE, 2, 4, 9, 2)
        for xx in range(4, 11):                     # the lower front panel in paint
            c.set(xx, 11, LP_BLUE[2])
            c.set(xx, 10, LP_BLUE[3])
    sub(cv, casing)
    gauge(cv, 9, 6)

    def flange(c):                                  # the large steam inlet flange standing on the top
        reg = {(x, y) for y in (1, 2, 3) for x in range(4, 11)} - {(4, 1), (10, 1)}
        region_paint(c, reg, lambda p: STEEL[4] if p[1] == 2 else STEEL[3], STEEL)
    sub(cv, flange)
    terminal(cv, 6, 12, w=3, studs=1)
    return cv


def gas_turbine():
    """shaftgas: a cast-iron casing whose front is the red painted intake housing with its louvred grille (the fan
    behind it), the fuel valve on top; skid, couplings, signal terminal under the grille."""
    cv = Canvas(16)
    skid(cv, 1, 13)
    coupling(cv, 0, 9, 'l', shaft_to=2)
    coupling(cv, 14, 7, 'r', shaft_to=13)
    sub(cv, lambda c: body(c, 6, 4, 7, 9, 2, CAST, round_top=True))

    def housing(c):
        body(c, 2, 4, 6, 9, 2, GAS_RED, bevel=True)
        for yy in range(6, 11):                      # grille: dark slots between lit slats
            for xx in range(3, 7):
                c.set(xx, yy, CAV[0] if yy % 2 == 0 else STEEL[2])
        c.hline(3, 6, 5, GAS_RED[2])
    sub(cv, housing)
    valve(cv, 9, 1)
    terminal(cv, 3, 12, w=3, studs=1)
    return cv


def _generator(big):
    cv = Canvas(16)
    if big:
        skid(cv, 1, 13)
        x, y, w, dm = 2, 2, 11, 10
        cl, cr, yc = 0, 14, 7
    else:
        skid(cv, 3, 12)
        x, y, w, dm = 4, 4, 8, 8
        cl, cr, yc = 1, 13, 8
    coupling(cv, cl, yc, 'l', shaft_to=x)
    coupling(cv, cr, yc - 1, 'r', shaft_to=x + w)

    def drum(c):
        reg = cylinder(c, x, y, w, dm, GEN_YEL)
        cylinder_end(c, x + w, y, dm, GEN_YEL)
        for hx in range(x + 2, x + w - 1, 3):      # dark hoops round the drum
            for yy in range(y, y + dm):
                if (hx, yy) in reg:
                    c.set(hx, yy, RUBBER_BLACK[1] if yy in (y, y + dm - 1) else RUBBER_BLACK[3] if yy < y + dm // 2 else RUBBER_BLACK[2])
    sub(cv, drum)
    # power terminal box under the drum, on the front
    tx = 3 if big else 4
    tw = 7 if big else 5
    sub(cv, lambda c: box(c, tx, 11, tw, 3, 1, STEEL, bevel=False))
    for i in range(1, tw - 1, 2):
        cv.set(tx + i, 12, ORANGE[3])
    return cv


def cylinder_end(c, x, y, dm, ramp):
    """The right end face of a drum lying across the picture, seen obliquely: one column, shaded, outlined."""
    for yy in range(y + 1, y + dm - 1):
        c.set(x, yy, ramp[2] if yy > y + 1 else ramp[3])
    c.set(x, y + 1, ramp[0]); c.set(x, y + dm - 2, ramp[0])
    for yy in range(y + 2, y + dm - 2):
        c.set(x + 1, yy, ramp[0])


def generator():
    """shaftgen4kw: the yellow generator drum with dark hoops on a cast skid, couplings both ends, the power
    terminal box in front. Drawn smaller than the 8 kW."""
    return _generator(False)


def generator_8kw():
    """shaftgen8kw: the 8 kW: the same drum, longer and fatter, filling the slot."""
    return _generator(True)


RED = R('#3d1515', '#6e2020', '#a02a24', '#c83a2e', '#e2654f', '#f09a84')          # flywheel rim (atlas red)


def drum_end_on(c, x, y, dm, ramp, recede, face_ramp=None):
    """A drum whose axis is the model's x (pointing at us), seen in oblique: its body receding RECEDE px up-right
    behind a round front face (disc) of diameter DM at x, y."""
    from kit.shapes import _disc_mask
    reg = set()
    for k in range(recede, 0, -1):
        reg |= {(px + k, py - k) for (px, py) in _disc_mask(x, y, dm)}
    for p in reg:
        c.set(*p, ramp[2])
    front = _disc_mask(x, y, dm)
    # lit upper-left flank of the receding body
    for (px, py) in reg - front:
        if py < y + dm // 2 - 1 and px < x + dm // 2 + recede:
            c.set(px, py, ramp[4])
    outline_region(c, reg | front, ramp[0], ramp[1])
    sub(c, lambda cc: disc(cc, x, y, dm, face_ramp or ramp))
    return front


def motor():
    """shaftmotor: the blue ribbed motor body, its dark terminal box on the front, the steel fan cover (grille) at
    the right end, the power floor terminal; skid, couplings."""
    cv = Canvas(16)
    skid(cv, 1, 12)
    coupling(cv, 0, 8, 'l', shaft_to=2)

    def bodyf(c):
        zb = body(c, 2, 4, 10, 9, 2, HP_BLUE, round_top=True)
        for (px, py), (n, f, k) in zb.items():
            if f == 'front' and 2 < px < 11 and py in (6, 8, 10):
                c.set(px, py, HP_BLUE[1])           # cooling fins along the body
    sub(cv, bodyf)

    def fan(c):                                       # the round steel fan cover with its grille, right end
        reg = {(x, y) for x in (12, 13, 14) for y in range(3, 12)} - {(12, 3), (14, 3), (12, 11), (14, 11)}
        region_paint(c, reg, lambda p: STEEL[3] if p[0] == 12 else STEEL[2], STEEL)
        for yy in (5, 7, 9):
            c.set(13, yy, DARK[1])
    sub(cv, fan)
    cv.set(15, 7, STEEL[2]); cv.set(15, 6, STEEL[3]); cv.set(15, 8, STEEL[0])
    sub(cv, lambda c: box(c, 4, 6, 4, 4, 1, RUBBER_BLACK, bevel=False))        # terminal box
    sub(cv, lambda c: box(c, 3, 12, 6, 2, 1, STEEL, bevel=False))
    cv.set(4, 13, ORANGE[3]); cv.set(6, 13, ORANGE[3])
    for xx in range(4, 8):
        cv.set(xx, 11, HP_BLUE[4])
    return cv


def baseplate(cv, x0, x1, y=13, d=2):
    sub(cv, lambda c: box(c, x0, y, x1 - x0 + 1, 2, d, STEEL, bevel=False))


def joint():
    """shaftjoint: the cast bearing pedestal with its brass oil cup, the shaft through it, half couplings at both
    faces, on the light steel base plate."""
    cv = Canvas(16)
    baseplate(cv, 1, 12, d=2)
    coupling(cv, 0, 8, 'l', shaft_to=6)
    coupling(cv, 14, 7, 'r', shaft_to=11)

    def ped(c):
        zb = solid([("cap", 4, 5, 8, 4, 2), ("leg", 5, 9, 6, 4, 2)])
        paint_solid(c, zb, CAST, bevel=True, cut=[(4, 5), (13, 3)])
    sub(cv, ped)
    cv.hline(5, 10, 7, CAST[1])                      # the split line of the bearing cap at the shaft
    cv.set(8, 2, BRASS[3]); cv.set(9, 2, BRASS[1])   # the oil cup on the cap
    return cv


def joint_hub():
    """shafthub: the cast bevel gearbox, its brass gears showing in the open top, a coupling on the front face
    and both sides (the fourth behind), on the steel base plate."""
    cv = Canvas(16)
    baseplate(cv, 1, 12, d=2)
    coupling(cv, 0, 9, 'l', shaft_to=3)
    coupling(cv, 14, 7, 'r', shaft_to=13)

    def gbox(c):
        zb = body(c, 3, 5, 9, 8, 3, CAST, bevel=False)
        for (px, py), (n, f, k) in zb.items():
            if f == 'top' and 1 <= k <= 2 and 4 <= px - k <= 10:
                c.set(px, py, BRASS[4] if (px + py) % 3 else BRASS[2])
    sub(cv, gbox)
    sub(cv, lambda c: disc(c, 5, 7, 5, STEEL))
    cv.set(7, 9, DARK[1])
    return cv


def flywheel():
    """shaftflywheel, seen from its side (the shaft towards us): the big spoked wheel with its red rim, the hub and
    bearing on the cast pedestal and skid."""
    cv = Canvas(16)
    skid(cv, 1, 13, d=1)

    def wheel(c):
        import math
        ring(c, 1, 0, 14, RED, width=3)
        cx, cy = 7.5, 6.5
        for a in range(0, 360, 60):
            r = math.radians(a + 30)
            line(c, (round(cx + 1.6 * math.cos(r)), round(cy + 1.6 * math.sin(r))),
                 (round(cx + 4.6 * math.cos(r)), round(cy + 4.6 * math.sin(r))), CAST[4])
    sub(cv, wheel)

    def ped(c):
        reg = {(x, y) for x in range(6, 10) for y in range(9, 13)} | {(x, 12) for x in range(5, 11)}
        region_paint(c, reg, lambda p: CAST[3] if p[0] < 8 else CAST[2], CAST)
    sub(cv, ped)
    sub(cv, lambda c: disc(c, 6, 5, 4, STEEL))
    cv.set(7, 6, DARK[3]); cv.set(8, 7, DARK[1])
    return cv


def tachometer():
    """shafttacho: the cast pedestal with the big brass-rimmed white dial (red needle) on its front, the shaft and
    half couplings through it, the signal terminal at the floor."""
    cv = Canvas(16)
    skid(cv, 2, 12)
    coupling(cv, 0, 9, 'l', shaft_to=5)
    coupling(cv, 14, 8, 'r', shaft_to=12)
    sub(cv, lambda c: body(c, 5, 3, 6, 10, 2, CAST, bevel=True))

    def dial(c):
        disc(c, 1, 1, 9, GLASS, face=4, rim=False)
    sub(cv, dial)
    from kit.shapes import _disc_mask
    m = _disc_mask(1, 1, 9)
    for (x, y) in m:
        if any(q not in m for q in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1))):
            cv.set(x, y, BRASS[2] if x + y < 10 else BRASS[1])
    cv.vline(5, 2, 5, MARK_RED)                      # needle at 12 o'clock
    cv.set(5, 5, CAST[1])
    for (x, y) in ((2, 5), (8, 5), (5, 8), (3, 3), (7, 3)):
        cv.set(x, y, CAST[2])                         # ticks
    terminal(cv, 7, 12, w=3, studs=1)
    return cv


def plant_condenser():
    """plantcondenser (main): the teal shell, its round end towards us with the drive opening, gauge, the large
    exhaust flange on top and on the right, the orange thermal pads on the cast skid."""
    cv = Canvas(16)
    skid(cv, 1, 13)
    for x in (1, 2, 12, 13):
        cv.set(x, 12, ORANGE[3] if x in (1, 12) else ORANGE[2])
        cv.set(x, 13, ORANGE[1])
    sub(cv, lambda c: drum_end_on(c, 1, 3, 10, TEAL, 2))
    sub(cv, lambda c: disc(c, 4, 6, 4, CAV, face=0, rim=False))
    cv.set(5, 7, DARK[3])
    gauge(cv, 8, 4)

    def flanges(c):
        top = {(x, y) for x in range(4, 10) for y in (1, 2)} | {(x, 0) for x in range(5, 9)}
        region_paint(c, top, lambda p: STEEL[4] if p[1] < 2 else STEEL[3], STEEL)
        side = {(x, y) for x in (12, 13, 14) for y in range(4, 9)} - {(14, 4), (14, 8)}
        region_paint(c, side, lambda p: STEEL[2] if p[0] > 12 else STEEL[3], STEEL)
    sub(cv, flanges)
    return cv


def heat_exchanger():
    """heatexchanger (shell + flange): the square cast tube sheet in front with the large (brass) and small (copper)
    port rings, the olive shell with its dark hoops receding behind it. The back end's flange (the flange part
    turned 180 degrees) is hidden behind the shell in this view, so it is not drawn."""
    cv = Canvas(16)

    def shell(c):
        from kit.shapes import _disc_mask
        reg, col = {}, {}
        for k in range(5, 0, -1):
            for (px, py) in _disc_mask(1 + k, 5 - k, 10):
                reg[(px + 0, py)] = k
        for (px, py), k in reg.items():
            hoop = k in (2, 4)
            lit = (py - (4 - k)) < 4
            c.set(px, py, (RUBBER_BLACK[3] if lit else RUBBER_BLACK[2]) if hoop else (OLIVE[4] if lit else OLIVE[2]))
        outline_region(c, set(reg), OLIVE[0], OLIVE[1])
    sub(cv, shell)

    def plate(c):
        box(c, 1, 4, 11, 11, 0, CAST, bevel=False)
    sub(cv, plate)
    sub(cv, lambda c: ring(c, 3, 8, 6, BRASS, width=2))
    cv.set(5, 10, CAV[0]); cv.set(6, 10, CAV[0]); cv.set(5, 11, CAV[0]); cv.set(6, 11, CAV[1])
    cv.set(5, 6, VCOPPER[3]); cv.set(6, 6, VCOPPER[2]); cv.set(5, 5, VCOPPER[4]); cv.set(6, 5, VCOPPER[3])
    return cv


def _tank(shell, alloy):
    cv = Canvas(16)
    skid(cv, 2, 12)

    def ports(c, xs, ys):
        for (x, y) in zip(xs, ys):
            reg = {(xx, yy) for xx in (x, x + 1, x + 2) for yy in range(y, y + 4)}
            region_paint(c, reg, lambda p: STEEL[4] if p[1] == y + 1 else STEEL[3], STEEL)
    sub(cv, lambda c: ports(c, (11, 11), (2, 7)))     # right side ports (further back: higher)
    sub(cv, lambda c: cylinder(c, 4, 1, 13, 8, shell, axis='y', cap=shell))
    sub(cv, lambda c: ports(c, (2, 2), (4, 9)))
    for yy in range(4, 13):                           # the sight glass between brass bars
        cv.set(6, yy, BRASS[3])
        cv.set(7, yy, CAV[0] if not alloy else CAV[1])
        cv.set(8, yy, BRASS[2])
    hoop = BRASS if alloy else RUBBER_BLACK
    for yy in (5, 10):
        for xx in range(5, 11):
            if xx not in (6, 7, 8):
                cv.set(xx, yy, hoop[3] if xx < 8 else hoop[2])
    return cv


def iron_tank():
    """tankiron (shell, ports, glass): the dark iron drum standing on its skid, the sight glass between brass bars,
    dark hoops, two steel port flanges each side."""
    return _tank(IRONT, False)


def alloy_tank():
    """tankalloy: the same tank in pale alloy with brass hoops."""
    return _tank(ALLOY, True)


def inline_pump():
    """inlinepump200v: the blue motor drum (its end towards us) on the pump body, the pipe through below it with
    flanges at both ends, the power terminal box (blue lid) in front."""
    cv = Canvas(16)
    skid(cv, 2, 12)
    sub(cv, lambda c: cylinder(c, 1, 8, 14, 3, STEEL, round_ends=False))
    for (x, y) in ((0, 8), (14, 7)):
        reg = {(xx, yy) for xx in (x, x + 1) for yy in range(y, y + 4)}
        sub(cv, lambda c, reg=reg: region_paint(c, reg, lambda p: STEEL[3] if p[0] % 2 == 0 else STEEL[2], STEEL))
    sub(cv, lambda c: drum_end_on(c, 3, 3, 8, HP_BLUE, 3))
    sub(cv, lambda c: box(c, 4, 11, 6, 3, 1, STEEL, bevel=False))
    cv.hline(5, 10, 10, HP_BLUE[3]); cv.hline(4, 9, 11, HP_BLUE[2])
    cv.set(5, 12, ORANGE[3]); cv.set(7, 12, ORANGE[3])
    return cv


DRAW = {
    "steam_turbine": steam_turbine,
    "lp_steam_turbine": lp_steam_turbine,
    "gas_turbine": gas_turbine,
    "generator": generator,
    "8kw_generator": generator_8kw,
    "200v_shaft_motor": motor,
    "joint": joint,
    "joint_hub": joint_hub,
    "flywheel": flywheel,
    "tachometer": tachometer,
    "plant_steam_condenser": plant_condenser,
    "counterflow_heat_exchanger": heat_exchanger,
    "iron_tank": iron_tank,
    "alloy_tank": alloy_tank,
    "200v_inline_pump": inline_pump,
}


def limit_colours(im, n=16):
    """Merge the rarest colour into its nearest neighbour until at most N colours remain (lint: <= 16)."""
    px = im.load()
    while True:
        cnt = {}
        for y in range(16):
            for x in range(16):
                p = px[x, y]
                if p[3]:
                    cnt[p] = cnt.get(p, 0) + 1
        if len(cnt) <= n:
            return im
        best = None
        for a in cnt:
            for b in cnt:
                if a != b:
                    d = sum((i - j) ** 2 for i, j in zip(a[:3], b[:3])) * min(cnt[a], cnt[b])
                    if best is None or d < best[0]:
                        best = (d, a, b) if cnt[a] <= cnt[b] else (d, b, a)
        _, a, b = best
        for y in range(16):
            for x in range(16):
                if px[x, y] == a:
                    px[x, y] = b


def write(item):
    cv = DRAW[item]()
    im = limit_colours(cv.image())
    im.save(os.path.join(OUT, item + ".png"))
    print(item, lint(im))


if __name__ == "__main__":
    for it in sys.argv[1:] or list(DRAW):
        write(it)
