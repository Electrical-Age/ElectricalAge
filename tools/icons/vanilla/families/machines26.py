"""Family helper: the four main machines as restyled in 2026-11 (src/cadmodels Macerator, Compressor, Magnetizer,
PlateMachine): 48V the old shapes modernised in the tier colour, 230V industrial (cast-iron grey, tier-coloured
parts). Seen from the front (the model's x = 0 face), depth receding up-right, as the old machine icons.

    macerator(tier) / compressor(tier) / magnetizer(tier) / plate_machine(tier) -> Canvas   (tier 'lv' or 'mv')
"""
from kit import Canvas, box, cylinder, disc, ring, outline_region, tier_ramp, STEEL, COPPER, GOLD, CAV, RUBBER_BLACK, h
from families.machines import zsolid, zpaint, zfaces, sub, _oct, shade_region, MINER

DARK = RUBBER_BLACK
LED_G, LED_A = h('#46e65a'), h('#faaa28')
RED = (h('#3a0e0c'), h('#6a1a16'), h('#a8322c'), h('#d4483c'), h('#ee7a6a'), h('#f8b0a4'))


def leds(cv, x, y):
    """Two status LEDs, green over amber, on a dark box face."""
    cv.set(x, y, LED_G)
    cv.set(x, y + 1, LED_A)


def industrial(tier):
    return tier == 'mv'


# ------------------------------------------------------------------------------- macerator
def macerator(tier):
    B = tier_ramp(tier)
    big = industrial(tier)
    body = MINER if big else B
    cv = Canvas(16)
    box(cv, 1, 13, 12, 1, 3, DARK, bevel=False)                                   # plinth / skid
    # the rolls and the tray seen between the side frames
    sub(cv, lambda c: (cylinder(c, 4, 8, 6, 2, DARK, axis='x'), cylinder(c, 5, 7, 6, 2, STEEL, axis='x')))
    sub(cv, lambda c: box(c, 4, 12, 6, 1, 2, DARK if not big else STEEL, bevel=False))
    # side frames: round-topped cheeks (48V), square housings (230V)
    zb = zsolid([('lp', 1, 5, 3, 9, 2, 0), ('rp', 10, 5, 3, 9, 2, 0)])
    cut = [] if big else [(1, 5), (3, 3), (10, 5), (12, 3)]
    zpaint(cv, zb, body, bevel=not big, cut=cut)
    # the hopper over the rolls: steel (48V), the tier colour and wider (230V), its mouth dark
    hop = B if big else STEEL
    hz = zsolid([('hop', 2 if big else 3, 3, 10 if big else 8, 3, 2, 0)])
    zpaint(cv, hz, hop, bevel=False)
    for p in zfaces(hz, 'hop', 'top'):
        if zfaces(hz, 'hop', 'top', 1).count(p) == 0:
            cv.set(*p, CAV[1])
    if big:                                                                        # the flywheel on the right side
        sub(cv, lambda c: ring(c, 11, 4, 4, MINER))
    leds(cv, 2, 9)
    return cv


# ------------------------------------------------------------------------------- compressor
def compressor(tier):
    B = tier_ramp(tier)
    big = industrial(tier)
    frame = MINER if big else B
    cv = Canvas(16)
    box(cv, 1, 13, 12, 1, 3, DARK, bevel=False)
    zb = zsolid([('bed', 1, 10, 10, 3, 3, 0)])
    zpaint(cv, zb, frame, bevel=False)
    # columns (two in front; the 230V's back pair shows behind)
    if big:
        sub(cv, lambda c: (cylinder(c, 4, 3, 7, 2, STEEL, axis='y'), cylinder(c, 11, 3, 7, 2, STEEL, axis='y')))
    sub(cv, lambda c: (cylinder(c, 2, 4, 6, 2, STEEL, axis='y'), cylinder(c, 8, 4, 6, 2, STEEL, axis='y')))
    sub(cv, lambda c: box(c, 4, 9, 3, 1, 2, STEEL, bevel=False))                   # the die
    sub(cv, lambda c: box(c, 3, 6, 6, 1, 3, B if big else DARK, bevel=False))       # platen / ram
    zc = zsolid([('crown', 1, 3, 10, 2, 3, 0)])
    zpaint(cv, zc, frame, bevel=False)
    if big:
        sub(cv, lambda c: box(c, 4, 1, 4, 1, 2, B, bevel=False))                   # cylinder on the crown
        sub(cv, lambda c: box(c, 11, 8, 3, 5, 1, B, bevel=False))                  # power pack tank, right
    leds(cv, 2, 11)
    return cv


# ------------------------------------------------------------------------------- magnetizer
def magnetizer(tier):
    B = tier_ramp(tier)
    cv = Canvas(16)
    if not industrial(tier):
        zb = zsolid([('plate', 2, 13, 10, 1, 3, 0), ('ped', 5, 9, 4, 4, 2, 1)])
        zpaint(cv, zb, B, bevel=True)
        # the coil drum end-on: steel flange ring, copper bands receding, dark bore
        dr = Canvas(16)
        x0, y0, d = 3, 2, 8
        front = _oct(x0, y0, d, 2)
        for a in (2, 1):
            for (px, py) in front:
                dr.set(px + a, py - a, COPPER[3] if a == 1 else COPPER[2])
        shade_region(dr, front, STEEL, x0 + d / 2, y0 + d / 2)
        for p in _oct(x0 + 2, y0 + 2, 4, 1):
            dr.set(*p, CAV[0])
        cv.paste(dr)
        leds(cv, 5, 10)
    else:
        box(cv, 1, 13, 12, 1, 3, DARK, bevel=False)
        # the C-yoke: back leg, arms, pole pieces with copper coils, the gap
        zb = zsolid([('leg', 9, 3, 3, 10, 2, 0), ('top', 3, 3, 6, 2, 2, 0), ('bot', 3, 11, 6, 2, 2, 0)])
        zpaint(cv, zb, MINER, bevel=False)
        sub(cv, lambda c: box(c, 4, 5, 4, 2, 2, COPPER, bevel=False))
        sub(cv, lambda c: box(c, 4, 9, 4, 2, 2, COPPER, bevel=False))
        sub(cv, lambda c: (cylinder(c, 1, 6, 7, 2, B, axis='y'), cylinder(c, 12, 7, 6, 2, B, axis='y')))   # capacitor cans
        leds(cv, 10, 6)
    return cv


# ------------------------------------------------------------------------------- plate machine
def plate_machine(tier):
    B = tier_ramp(tier)
    big = industrial(tier)
    frame = MINER if big else B
    cv = Canvas(16)
    box(cv, 1, 13, 12, 1, 3, DARK, bevel=False)
    if big:
        rolls = [(4, 3, 3, DARK), (4, 6, 2, STEEL), (4, 8, 2, STEEL), (4, 10, 3, DARK)]
    else:
        rolls = [(4, 5, 3, STEEL), (4, 8, 3, STEEL)]
    for (x, y, dia, R) in rolls:
        sub(cv, lambda c, x=x, y=y, dia=dia, R=R: cylinder(c, x, y, 6, dia, R, axis='x'))
    zb = zsolid([('lp', 1, 2 if big else 4, 3, 11 if big else 9, 2, 0), ('rp', 10, 2 if big else 4, 3, 11 if big else 9, 2, 0)])
    zpaint(cv, zb, frame, bevel=False)
    if big:
        sub(cv, lambda c: box(c, 3, 1, 8, 1, 2, MINER, bevel=False))               # screw-down box
    else:
        for x in (2, 11):
            cv.set(x + 1, 2, RED[3]); cv.set(x, 3, RED[2]); cv.set(x + 2, 3, RED[2])   # handwheels
    leds(cv, 2, 10 if not big else 9)
    return cv
