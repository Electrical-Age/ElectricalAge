"""Steam plant family (vanilla, 16 px): the boilers, the turbines' add-on condensers and the electric
heaters (src/cadmodels Boiler, LargeBoiler, Condenser, ElectricHeater).

Shared visual system
--------------------
* The CAD steam machines stand on a dark iron skid with riveted copper pads on its faces; the
  shipped siblings (plant_steam_condenser, 200v_inline_pump, lp_steam_turbine) draw that skid in
  the warm dark iron ramp IRON_D and the pads in PAD.  The boiler and the condensers reuse both,
  so the family reads as one plant.
* boiler: the riveted drum lying on its side (``revolve``-style CYL_BANDS shading per column), its
  steam dome on top, the dark firebox hatch, a pressure gauge and the feed pipe, on the skid.
* large_boiler: the big drum (d 10, chamfered heads, seam bands) filling the slot, copper heat
  plates along its flank, two domes and the dial on its stalk breaking the top silhouette, the
  copper feed line up the front, on the skid: fuller and rounder than the boiler (d 8, one dome,
  firebox hatch), and round where the electrical furnace is a dark box under a flat white slab.
* condensers: the grey shell lying on its side, wound with copper cooling fins, the exhaust stack
  on top, the thin painted ring by its end (model paint PAINT_50V / PAINT_200V: the turbine it
  belongs to; the item is untiered: the ring is drawn in the lv/mv ramp colours as model paint only,
  like water_turbine's housing - ``ring=False`` drops it), on the
  skid.  small_ = the same drawing smaller (SMALL_BOX).
* electric heaters: a full block, steel front with the glowing element behind a grille, the tier
  lid (top face ramp[4] with dark vent slots, a 2-row front lip in ramp[3], side in ramp[2]) and
  the copper heat pad on its right side (VCOPPER; TERRACOTTA is barred on tiered devices).

Family ramps (frozen here, reported for promotion):
    IRON_D  warm dark iron of the CAD skids (sampled from the shipped lp_steam_turbine)
    PAD     riveted copper pads (#6b2a0e #a04416 #c85e22 sampled from the shipped siblings)
    DRUM    the boilers' dark riveted steel (families.machines.MINER)
"""
from kit import (Canvas, box, cylinder, outline_region, tier_ramp, h, CYL_BANDS,
                 STEEL, CAV, VCOPPER, LIT, WHITE, MARK_RED, GOLD)
from families.machines import MINER, sub
from families.generators import revolve

IRON_D = (h('#242228'), h('#353236'), h('#47433f'), h('#5c5752'), h('#79736b'), h('#958e84'))
PAD = (h('#3d190a'), h('#6b2a0e'), h('#a04416'), h('#c85e22'), h('#e08a48'), h('#f2b47c'))
DRUM = MINER


def _pads(cv, y, xs, w=3, hgt=2):
    """Riveted copper pads on a skid's front face: PAD[3] with a lit top row."""
    for x in xs:
        cv.rect(x, y, w, hgt, PAD[3])
        cv.hline(x, x + w - 1, y, PAD[4])


# ------------------------------------------------------------------------------- boiler
def boiler():
    """Boiler (cadmodels Boiler): the riveted drum on its side with the steam dome, the firebox
    hatch, the pressure gauge and the feed pipe, on the copper-padded skid."""
    cv = Canvas(16)
    box(cv, 1, 12, 12, 3, 2, IRON_D, bevel=False)                      # skid
    _pads(cv, 12, (2, 8), w=4)
    # the saddles under the drum (red in the model): two dark-red stubs on the skid
    # -> left out (another hue; the skid already carries the drum)
    diams = [4, 6, 8, 8, 8, 8, 8, 8, 8, 8, 6, 4]               # capsule: rounded heads
    revolve(cv, 1, 15, diams, DRUM)                                      # rows 4..11
    for sx in (4, 9):                                                  # rivet seams round the drum
        for yy in range(5, 11):
            c = cv.get(sx, yy)
            cv.set(sx, yy, DRUM[2] if c in (DRUM[3], DRUM[4], DRUM[5]) else DRUM[1])
    sub(cv, lambda c: cylinder(c, 5, 1, 4, 4, DRUM, axis='y'))          # steam dome
    cv.hline(5, 8, 1, DRUM[4])                                         # dome's flat lid
    cv.hline(6, 7, 1, DRUM[5])
    # the firebox hatch: a dark door between the seams, lit rim top-left
    cv.rect(6, 7, 2, 3, CAV[1])
    cv.set(6, 7, DRUM[5])
    # pressure gauge: a white face with a dark needle, right of the rear seam
    cv.set(10, 6, WHITE); cv.set(11, 6, WHITE)
    cv.set(11, 7, DRUM[1])
    # feed pipe up the right end, elbow over the drum (the same dark steel)
    cv.vline(13, 3, 11, DRUM[3])
    cv.vline(14, 3, 11, DRUM[0])
    cv.set(12, 3, DRUM[4]); cv.set(13, 3, DRUM[4]); cv.set(14, 3, DRUM[0])
    cv.set(11, 3, MARK_RED)                                            # the safety valve
    return cv


# ------------------------------------------------------------------------------- large boiler
def large_boiler():
    """Large Boiler (cadmodels LargeBoiler, 2x2x2): the big riveted drum lying along the slot (front
    head on the left, chamfered heads, seam bands), copper heat plates on bosses along its flank, the
    safety-valve dome and the steam dome on its crest, the pressure dial on its stalk over the front
    and the copper feed line up the front, on the low copper-padded skid. Taller and fuller than the
    Boiler (whose drum is d 8 with one dome), and no firebox: it is heated from outside."""
    cv = Canvas(16)
    box(cv, 1, 13, 13, 2, 1, IRON_D, bevel=False)                     # the skid, rows 12..14
    _pads(cv, 13, (4, 9), w=3)
    diams = [6, 8, 10, 10, 10, 10, 10, 10, 10, 10, 10, 8, 6]         # chamfered heads
    revolve(cv, 2, 15, diams, DRUM)                                    # cols 2..14, rows 3..12
    for sx in (4, 8, 12):                                              # seam bands round the drum
        for yy in range(4, 12):
            c = cv.get(sx, yy)
            cv.set(sx, yy, DRUM[2] if c in (DRUM[3], DRUM[4], DRUM[5]) else DRUM[1])
    for x in (5, 9):                                                   # the upper heat plates on their bosses
        cv.rect(x, 8, 3, 2, PAD[3])
        cv.hline(x, x + 2, 8, PAD[4])
        cv.hline(x, x + 2, 10, DRUM[0])
    # the safety-valve dome (its brass valve) and the steam dome with its flange
    sub(cv, lambda c: cylinder(c, 5, 1, 3, 3, DRUM, axis='y'))
    cv.set(6, 0, GOLD[3])
    sub(cv, lambda c: cylinder(c, 10, 1, 3, 4, DRUM, axis='y'))
    cv.hline(10, 13, 1, DRUM[4])
    # the pressure dial on its stalk over the front head, the copper feed line up the front
    cv.set(2, 2, WHITE); cv.set(3, 2, WHITE); cv.set(3, 3, GOLD[3])
    cv.vline(1, 7, 12, VCOPPER[2]); cv.set(1, 6, VCOPPER[3]); cv.set(2, 6, VCOPPER[3])
    cv.set(1, 10, MARK_RED)
    return cv


# ------------------------------------------------------------------------------- condenser
def condenser(paint, small=False, ring=True):
    """Steam condenser (cadmodels Condenser): grey shell on its side wound with copper cooling fins,
    the exhaust stack on top, the thin ring in the model's paint by its end, on the skid.
    small: the same drawing in SMALL_BOX (cols 3..12, rows 5..14)."""
    cv = Canvas(16)
    P = tier_ramp(paint) if ring else None
    if small:
        sx, sw, sy, sd, fins, stack = 3, 10, 7, 6, (5, 7, 9), (6, 5, 3)
        skid = (3, 13, 9, 2, 1)
        pads = (4, 8)
    else:
        sx, sw, sy, sd, fins, stack = 1, 12, 4, 8, (3, 5, 7, 9, 11), (5, 1, 4)
        skid = (1, 12, 12, 3, 2)
        pads = (2, 8)
    box(cv, *skid, IRON_D, bevel=False)
    _pads(cv, skid[1], pads, w=3 if small else 4, hgt=1 if small else 2)
    sub(cv, lambda c: cylinder(c, stack[0], stack[1], stack[2] + 2, 3, STEEL, axis='y'))
    sub(cv, lambda c: cylinder(c, sx, sy, sw, sd, STEEL, axis='x'))
    top, bot = sy + 1, sy + sd - 2
    for fx in fins:                                                    # copper fins round the shell
        cv.set(fx, top, PAD[4])
        for yy in range(top + 1, bot + 1):
            cv.set(fx, yy, PAD[3] if yy < sy + sd // 2 else PAD[2])
    if ring:                                                           # the painted ring (2 cols, 1 small)
        rx = sx + sw - 3
        for xx in ((rx,) if small else (rx, rx + 1)):
            for yy in range(sy + 1, sy + sd - 1):                      # inside the shell outline
                cv.set(xx, yy, P[3] if yy < sy + sd // 2 else P[2])
    return cv


# ------------------------------------------------------------------------------- electric heater
def electric_heater(tier):
    """Electric heater (cadmodels ElectricHeater): a full block, steel front with the glowing
    element behind its grille, the tier lid with dark vent slots (front lip ramp[3]), and the copper
    heat pad covering its right side."""
    T = tier_ramp(tier)
    cv = Canvas(16)
    x0, y0, w, hh, d = 1, 5, 10, 10, 4
    box(cv, x0, y0, w, hh, d, STEEL, bevel=False)
    # heat pad: the whole right side in VCOPPER, two darker courses
    for k in range(1, d + 1):
        for yy in range(y0 - k, y0 - k + hh):
            cv.set(x0 + w - 1 + k, yy, VCOPPER[2])
    outline_region(cv, {(x0 + w - 1 + k, yy) for k in range(1, d + 1)
                        for yy in range(y0 - k, y0 - k + hh)}, VCOPPER[0], VCOPPER[1])
    for k in (1, 2, 3):
        cv.set(x0 + w - 1 + k, y0 + 4 - k, VCOPPER[1])
    # the tier lid: top face ramp[4] with two dark vent slots, a 2-row lip on the front in ramp[3]
    lid = set()
    for k in range(1, d + 1):
        for xx in range(x0 + k, x0 + k + w):
            lid.add((xx, y0 - k))
    lid |= {(xx, yy) for xx in range(x0, x0 + w) for yy in (y0, y0 + 1)}
    for p in lid:
        cv.set(*p, T[4] if p[1] < y0 else T[3])
    outline_region(cv, lid, T[0], T[1])
    for k in (2, 3):                                                   # vent slots in the lid
        cv.hline(x0 + k + 2, x0 + k + 6, y0 - k, CAV[1])
    # the grille: glowing element behind dark bars
    gx, gy, gw, gh = x0 + 2, y0 + 3, 6, 5
    cv.rect(gx, gy, gw, gh, LIT[2])
    cv.hline(gx, gx + gw - 1, gy + 2, LIT[3])
    for xx in range(gx + 1, gx + gw, 2):
        cv.vline(xx, gy, gy + gh - 1, CAV[1])
    # recessed frame: shadowed top/left jamb, lit bottom/right lip
    cv.hline(gx - 1, gx + gw, gy - 1, STEEL[1])
    cv.vline(gx - 1, gy - 1, gy + gh, STEEL[1])
    cv.hline(gx, gx + gw, gy + gh, STEEL[4])
    cv.vline(gx + gw, gy, gy + gh, STEEL[4])
    return cv
