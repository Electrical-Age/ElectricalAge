"""Family helper: the oil chain (2026-11 CAD models, src/cadmodels): the oil pump, the steam injection wellhead,
the tar sand separator, the fractionating column (base and segment) and the flare stack. Seen from the front,
depth receding up-right, as the machines26 icons.

    oil_pump(tier)            OilPump (oilpump50v/200v): the rounded tier-coloured capsule head lying over the
                              tall ribbed motor drum and the wellhead stem with its red handwheel, behind the low
                              front wall with its tier-coloured cap rail and vents. The rounded capsule and the
                              drum keep it apart from the compressor's flat slab crown.
    wellhead()                SteamInjection (in the world it borrows the 48V oil pump's model, untiered): the
                              pump model's own flanged wellhead spool, large, with its gold-bolted flanges, the
                              side valve and red handwheel, and the steam pipe turning down into its top with a
                              puff of steam (no tier colour: it takes no power, and green would read as 48V).
    tar_sand_separator(tier)  TarSandSeparator: the grey open-topped tub on its dark legs, the tier band round it,
                              the brass drive gear on its face (one of the model's pair: two overlap
                              into a blob at 16 px), the sand tray below.
    column_base()             ColumnBase: the squat vessel, dark lower shell flaring out over copper corner
                              blocks, the red band, the steel upper shell and its ladder.
    column_segment()          ColumnSegment: the straight tall steel shell with the red band at the top, a dark
                              hoop, the flanged foot and the ladder (straight sides: no flare, no feet).
    flare_stack()             FlareStack: the red/white banded stack with its flared tip and a small flame, rising
                              from the horizontal knock-out drum with red bands, on the slab.
"""
from kit import (Canvas, box, cylinder, disc, outline_region, tier_ramp,
                 STEEL, CAV, GOLD, VCOPPER, RUBBER_BLACK, LIT, h)
from families.machines import zsolid, zpaint, zfaces, sub, MINER

DARK = RUBBER_BLACK
RED = (h('#3a0e0c'), h('#6a1a16'), h('#a8322c'), h('#d4483c'), h('#ee7a6a'), h('#f8b0a4'))
#: steam (the wellhead's inlet)
STEAM = h('#f2f2ee')


# ------------------------------------------------------------------------------- oil pump
def oil_pump(tier):
    B = tier_ramp(tier)
    cv = Canvas(16)
    box(cv, 1, 13, 12, 1, 3, DARK, bevel=False)                                    # the pit floor / skid

    def drum(c):                                                                   # the tall ribbed motor drum
        cylinder(c, 8, 3, 8, 6, STEEL, axis='y')
        for y in (5, 7, 9):
            for x in range(9, 13):
                c.set(x, y, STEEL[1])
    sub(cv, drum)
    sub(cv, lambda c: box(c, 3, 5, 2, 5, 1, STEEL, bevel=False))                   # the wellhead stem
    cv.set(3, 8, GOLD[3])                                                          # its brass collar
    for x in (1, 2, 3):                                                            # its red handwheel
        cv.set(x, 6, RED[3])
    # the rounded capsule head (tier colour) lying across the top
    sub(cv, lambda c: cylinder(c, 2, 1, 11, 5, B))
    for x in range(5, 10):
        cv.set(x, 2, DARK[1])                                                      # the slot in its top
    # the low front wall: tier-coloured cap rail over the grey wall with its vents
    wz = zsolid([('wall', 1, 10, 12, 3, 1, 0)])
    zpaint(cv, wz, STEEL, bevel=False)
    for p in zfaces(wz, 'wall', 'top'):
        if p[0] < 13:
            cv.set(*p, B[4])
    for x in range(2, 12):
        cv.set(x, 10, B[3])
    cv.set(13, 10, B[2])
    for x in (3, 5, 7):
        cv.set(x, 11, RED[3])
    return cv


# ------------------------------------------------------------------------------- wellhead
def wellhead():
    cv = Canvas(16)
    box(cv, 2, 13, 10, 1, 3, DARK, bevel=False)                                    # the pad on the casing
    # the steam pipe from the right, turning down into the top of the spool, a puff of steam at its open end
    sub(cv, lambda c: (cylinder(c, 6, 1, 7, 3, STEEL), cylinder(c, 6, 1, 4, 3, STEEL, axis='y', round_ends=False)))
    for (x, y) in ((12, 1), (13, 1), (13, 0), (14, 2), (12, 2)):
        cv.set(x, y, STEAM)
    sub(cv, lambda c: cylinder(c, 5, 4, 9, 6, STEEL, axis='y'))                    # the spool
    for y in (11, 6):                                                              # its two flanges, gold bolts
        sub(cv, lambda c, y=y: box(c, 3, y, 9, 1, 1, STEEL, bevel=False))
        cv.set(4, y, GOLD[3]); cv.set(10, y, GOLD[3])
    sub(cv, lambda c: box(c, 1, 8, 4, 2, 1, STEEL, bevel=False))                   # the side valve
    for x, col in ((1, RED[3]), (2, RED[4]), (3, RED[3])):                         # its red handwheel
        cv.set(x, 6, col)
    cv.set(2, 7, RED[2])
    return cv


# ------------------------------------------------------------------------------- tar sand separator
BRASS = (GOLD[0], GOLD[1], GOLD[1], GOLD[2], GOLD[3], GOLD[3])
SAND = (h('#5a4a2a'), h('#7c6a40'), h('#a8925c'), h('#cdb47a'), h('#e6d39c'), h('#f4e8c4'))


def _gear(cv, x, y, d=5):
    """A small brass gear front-on: a ``d`` px disc, a dark hub, four teeth standing out of its rim."""
    m = d // 2

    def g(c):
        disc(c, x, y, d, BRASS, face=3, rim=False)
        for (tx, ty) in ((x + m, y - 1), (x + d, y + m), (x + m, y + d), (x - 1, y + m)):
            c.set(tx, ty, BRASS[2])
    sub(cv, g)
    cv.set(x + m, y + m, DARK[2])


def tar_sand_separator(tier):
    B = tier_ramp(tier)
    cv = Canvas(16)
    # the dark legs, the sand tray between them
    sub(cv, lambda c: (box(c, 1, 11, 2, 3, 1, DARK, bevel=False), box(c, 9, 11, 2, 3, 1, DARK, bevel=False)))
    for x in range(3, 9):
        cv.set(x, 12, SAND[3]); cv.set(x, 13, SAND[3])
    # the tub: grey, open at the top (dark inside), depth 4, the tier band round its foot
    tz = zsolid([('tub', 1, 4, 10, 7, 4, 0)])
    zpaint(cv, tz, STEEL, bevel=False, outline=False)
    for p in zfaces(tz, 'tub', 'top'):
        cv.set(*p, CAV[2])
    for p in zfaces(tz, 'tub', 'top', 1):
        cv.set(*p, STEEL[4])
    for x in range(1, 11):
        cv.set(x, 8, B[3]); cv.set(x, 9, B[3])
    for k in range(1, 5):
        cv.set(10 + k, 8 - k, B[2]); cv.set(10 + k, 9 - k, B[2])
    outline_region(cv, set(tz), STEEL[0], STEEL[1])
    _gear(cv, 2, 3)                                                                # the drive gear (one of the model's pair)
    return cv


# ------------------------------------------------------------------------------- fractionating column
def _ladder(cv, x, y0, y1, R=DARK):
    for y in range(y0, y1 + 1):
        cv.set(x, y, R[2])
        if (y - y0) % 2 == 1:
            cv.set(x + 1, y, R[3])


def column_base():
    cv = Canvas(16)
    sub(cv, lambda c: cylinder(c, 2, 8, 6, 11, MINER, axis='y', round_ends=False))  # the dark lower shell
    sub(cv, lambda c: cylinder(c, 3, 2, 6, 9, STEEL, axis='y', cap=STEEL))          # the steel upper shell
    for x in range(2, 13):                                                          # the red band
        cv.set(x, 8, RED[3] if x < 10 else RED[2])
    cv.set(2, 8, RED[2]); cv.set(12, 8, RED[2])
    for x0 in (1, 11):                                                              # the copper corner blocks
        sub(cv, lambda c, x0=x0: box(c, x0, 11, 2, 3, 1, VCOPPER, bevel=False))
    _ladder(cv, 12, 2, 7, MINER)
    return cv


def column_segment():
    cv = Canvas(16)
    sub(cv, lambda c: cylinder(c, 3, 1, 13, 9, STEEL, axis='y', cap=STEEL))        # the shell
    for x in range(4, 11):
        cv.set(x, 4, RED[3] if x < 9 else RED[2])                                  # the red band at the top
        cv.set(x, 5, RED[2] if x < 9 else RED[1])
        cv.set(x, 9, STEEL[1])                                                     # a hoop
    sub(cv, lambda c: box(c, 2, 13, 11, 1, 1, STEEL, bevel=False))                 # the flanged foot
    _ladder(cv, 12, 2, 12)
    return cv


# ------------------------------------------------------------------------------- flare stack
def flare_stack():
    cv = Canvas(16)
    box(cv, 1, 13, 13, 1, 2, STEEL, bevel=False)                                   # the slab
    sub(cv, lambda c: cylinder(c, 2, 8, 11, 5, STEEL))                             # the knock-out drum
    for x in (4, 10):
        for y in range(9, 12):
            cv.set(x, y, RED[3] if y < 11 else RED[2])                             # its red straps

    def stack(c):                                                                  # banded stack, flared tip
        cylinder(c, 6, 3, 6, 5, STEEL, axis='y')
        for y in (4, 6):
            c.set(7, y, RED[4]); c.set(8, y, RED[3]); c.set(9, y, RED[2])
        box(c, 5, 2, 7, 1, 0, STEEL, bevel=False)
        c.set(7, 2, DARK[1]); c.set(8, 2, DARK[1]); c.set(9, 2, DARK[1])
    sub(cv, stack)
    for (x, y, col) in ((8, 0, LIT[3]), (7, 1, LIT[3]), (8, 1, LIT[4]), (9, 1, LIT[2])):
        cv.set(x, y, col)                                                          # the flame
    return cv
