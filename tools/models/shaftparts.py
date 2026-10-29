#!/usr/bin/env python3
"""Shaft parts (README-shaft.md): 8 kW and 4 kW Generator, Joint, Joint Hub, Flywheel, Tachometer.

Frame: px, corner origin, Y up, front XN (x = 0), right z = 16; the shaft runs along z at x = 8, y = 8 with a half
coupling on each shaft face (the Joint Hub also along x, part `rotor_x`). Parts: `main` (static), `rotor` (turns about z
through (8, 8, 8)), hub `rotor_x` (about x through (8, 8, 8)), tachometer `needle` (modelled pointing up = 0, turned about
x through the dial's centre; as the boiler's: draw(-135 + 270 * frac, 1, 0, 0) sweeps from 7:30 clockwise seen from the
front to 4:30).

Usage: shaftparts.py RESOURCES_DIR   (e.g. src/main/resources) -> assets/eln/model/shaft{gen8kw,gen4kw,joint,hub,flywheel,tacho}/
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Model, box, prism, tube
from oilkit import turned, obox, terminal_plate, signal_plate, bolt_ring
import shaftkit as sk
from shaftkit import AX, AY, shaft, half_coupling, write_model, PAINT, CENTRE_ORIGIN

SHAFT_BOX = (5.75, 5.75, 0, 10.25, 10.25, 16)

def couplings():
    return [shaft(0.5, 15.5)] + half_coupling(0) + half_coupling(16)

def rails_x(z_pairs, y1=1.25, x0=0.0, x1=16.0, mat="darksteel"):
    """Skid rails running front to back (x), touching the front/back faces at the floor."""
    return [box(x0, 0, za, x1, y1, zb, mat) for za, zb in z_pairs]

# ---------------------------------------------------------------- generators
GENS = {
    # rg: stator radius; zs: stator z span; fins: cooling fin z starts (0.4 px thick); rails: skid rail z spans
    "shaftgen8kw": dict(rg=5.5, zs=(3.0, 13.0), fins=(3.3, 6.3, 9.3, 12.3), rails=((4.0, 5.75), (10.25, 12.0))),
    "shaftgen4kw": dict(rg=4.1, zs=(4.0, 12.0), fins=(4.3, 6.4, 9.2, 11.3), rails=((4.8, 6.3), (9.7, 11.2))),
}

def gen_terminal(rg):
    """Power terminal for the front (x = 0): oilkit plate, a junction box behind it, a conduit up into the stator."""
    ms = terminal_plate()
    ms += [box(0.95, 0, 5.0, 2.75, 4.25, 11.0, "darksteel"),
           box(0.95, 4.25, 4.8, 2.95, 4.6, 11.2, "steel")]
    yc = 6.0
    xs = AX - math.sqrt(rg * rg - (AY - yc) ** 2) + 0.5
    ms.append(prism("y", 1.85, 8.0, 0.75, 4.6, 5.0, "brass", n=6, caps=(False, True)))      # gland
    ms.append(prism("y", 1.85, 8.0, 0.45, 5.0, yc + 0.45, "steel", n=8, caps=(False, True)))
    ms.append(prism("x", yc, 8.0, 0.45, 1.85, xs, "steel", n=8, caps=(False, False)))
    return ms

def generator(name):
    g = GENS[name]
    rg, (zs0, zs1) = g["rg"], g["zs"]
    m = Model()
    ap = rg * math.cos(math.pi / 12)
    # stator (painted), cooling fins
    m.add(prism("z", AX, AY, rg, zs0, zs1, "paint", n=12))
    for z in g["fins"]:
        m.add(tube("z", AX, AY, rg - 0.1, rg + 0.55, z, z + 0.4, "darksteel", n=12, inner=False))
    # nameplate on the top flat
    hw = 0.9 if rg > 5 else 0.7
    m.add(box(AX - hw, AY + ap - 0.2, 7.0, AX + hw, AY + ap + 0.12, 9.0, "steel"))
    # left end: a fan cage (ring, bars, three-arm spider with the bearing hub); the fan in `rotor` shows through it
    cz0 = zs0 - 1.5
    m.add(tube("z", AX, AY, rg - 1.0, rg - 0.4, cz0, cz0 + 0.5, "castiron", n=12))
    for k in range(6):
        a = 2 * math.pi * k / 6
        m.add(prism("z", AX + (rg - 0.7) * math.cos(a), AY + (rg - 0.7) * math.sin(a), 0.3, cz0 + 0.5, zs0, "castiron", n=4,
                    caps=(False, False)))
    for k in range(3):
        a = math.pi / 2 + 2 * math.pi * k / 3
        u = (math.cos(a), math.sin(a), 0.0)
        rr = (rg - 0.7 + 1.3) / 2
        m.add(obox((AX + rr * u[0], AY + rr * u[1], cz0 + 0.25), u, (-u[1], u[0], 0.0), (0, 0, 1),
                   (rg - 0.7 - 1.3) / 2, 0.3, 0.22, "castiron"))
    m.add(prism("z", AX, AY, 1.5, cz0 - 0.25, cz0 + 0.6, "castiron", n=10))
    m.add(prism("z", AX, AY, 1.25, 1.0, cz0 - 0.25, "darksteel", n=10, caps=(True, False)))
    # right end: closed end bell, bearing cap
    m.add(prism("z", AX, AY, rg, zs1, zs1 + 1.2, "castiron", n=12, r_end=rg - 1.6, caps=(False, True)))
    m.add(prism("z", AX, AY, 1.4, zs1 + 1.2, zs1 + 1.8, "darksteel", n=10, caps=(False, True)))
    m.add(*bolt_ring("z", AX, AY, rg - 2.2, zs1 + 1.2, zs1 + 1.4, 4, phase=0.0))
    # skid rails (front-back) with cradle saddles, cross ties
    for za, zb in g["rails"]:
        m.add(box(2.75, 0, za, 13.25, 1.5, zb, "darksteel"))
        m.add(box(AX - 0.75 * rg, 1.5, za + 0.25, AX + 0.75 * rg, AY - 0.62 * rg, zb - 0.25, "castiron"))
        for x in (AX - 0.75 * rg + 0.6, AX + 0.75 * rg - 0.6):   # hold-down bolts
            m.add(prism("y", x, (za + zb) / 2, 0.3, AY - 0.62 * rg, AY - 0.62 * rg + 0.35, "brass", n=6, caps=(False, True)))
    z0, z1 = g["rails"][0][1], g["rails"][1][0]
    for x in (3.25, 11.5):
        m.add(box(x, 0.25, z0, x + 1.25, 1.25, z1, "darksteel"))
    # power terminals front and back
    t = gen_terminal(rg)
    m.add(*t, *turned(t, 2))
    # rotor: shaft, couplings, the fan in the cage
    r = Model()
    r.add(*couplings())
    fz = zs0 - 0.5
    r.add(prism("z", AX, AY, 1.1, cz0 + 0.6, zs0, "steel", n=8, caps=(True, False)))
    pitch = math.radians(35)
    R = rg - 1.15
    for k in range(6):
        a = 2 * math.pi * k / 6
        u = (math.cos(a), math.sin(a), 0.0)
        t_ = (-u[1], u[0], 0.0)
        w = (t_[0] * math.cos(pitch), t_[1] * math.cos(pitch), math.sin(pitch))
        n_ = (u[1] * w[2] - u[2] * w[1], u[2] * w[0] - u[0] * w[2], u[0] * w[1] - u[1] * w[0])
        rr = (R + 1.0) / 2
        r.add(obox((AX + rr * u[0], AY + rr * u[1], fz), u, w, n_, (R - 1.0) / 2, 0.55, 0.06, "steel"))
    hit = [(AX - rg - 0.55, AY - rg - 0.55, 1.0, AX + rg + 0.55, AY + rg + 0.55, zs1 + 1.8),
           (2.75, 0, g["rails"][0][0], 13.25, AY - 0.62 * rg, g["rails"][1][1]),
           (0, 0, 4.25, 2.95, 4.6, 11.75), (13.05, 0, 4.25, 16, 4.6, 11.75), SHAFT_BOX]
    return [("main", m), ("rotor", r)], {"rotor": CENTRE_ORIGIN}, PAINT["gen"], hit

# ---------------------------------------------------------------- joint (plummer block)
def plummer(m, z0, z1, rh=3.0, base_y=2.0, flange_x=(2.5, 13.5), bolts=True):
    """A split plain bearing pedestal round the shaft line between z0 and z1, standing on base_y."""
    zc = (z0 + z1) / 2
    m.add(box(flange_x[0], base_y, z0, flange_x[1], base_y + 1.0, z1, "castiron"))                # sole flange
    m.add(box(AX - rh + 0.6, base_y + 1.0, z0 + 0.25, AX + rh - 0.6, AY - 1.0, z1 - 0.25, "castiron"))  # web
    m.add(prism("z", AX, AY, rh, z0 + 0.25, z1 - 0.25, "castiron", n=12))                           # housing
    m.add(box(AX - rh - 0.6, AY - 0.2, z0 + 0.1, AX + rh + 0.6, AY + 0.3, z1 - 0.1, "paint"))           # split line
    if bolts:
        for x in (flange_x[0] + 0.75, flange_x[1] - 0.75):                                           # hold-down bolts
            m.add(prism("y", x, zc, 0.35, base_y + 1.0, base_y + 1.5, "brass", n=6, caps=(False, True)))
        for x in (AX - rh - 0.15, AX + rh + 0.15):                                                    # cap bolts
            m.add(prism("y", x, zc, 0.3, AY + 0.3, AY + 1.3, "brass", n=6, caps=(False, True)))
    m.add(prism("y", AX, zc, 0.35, AY + rh - 0.3, AY + rh + 0.6, "brass", n=6, caps=(False, False)))   # grease cup
    m.add(prism("y", AX, zc, 0.6, AY + rh + 0.6, AY + rh + 1.2, "brass", n=8))
    for za, zb in ((z0 - 0.15, z0 + 0.25), (z1 - 0.25, z1 + 0.15)):                                      # seals
        m.add(tube("z", AX, AY, 1.05, 1.6, za, zb, "darksteel", n=10))

def joint():
    m = Model()
    m.add(*rails_x(((2.0, 4.0), (12.0, 14.0))))
    m.add(box(1.5, 1.25, 1.5, 14.5, 2.0, 14.5, "steel"))
    plummer(m, 5.5, 10.5)
    r = Model()
    r.add(*couplings())
    for za in (4.6, 10.65):                                                     # collars with set screws
        r.add(prism("z", AX, AY, 1.45, za, za + 0.75, "steel", n=10))
        r.add(prism("y", AX, za + 0.375, 0.2, AY + 1.3, AY + 1.6, "darksteel", n=6, caps=(False, True)))
    hit = [(0, 0, 1.5, 16, 2.0, 14.5), (1.5 + 1.0, 2.0, 5.5, 13.5, 12.2, 10.5), SHAFT_BOX]
    return [("main", m), ("rotor", r)], {"rotor": CENTRE_ORIGIN}, PAINT["plain"], hit

# ---------------------------------------------------------------- joint hub (bevel gearbox, open top)
def bevel_gear_zp(teeth=10):
    """Bevel gear on the z shaft, z+ side of the centre: pitch cone apex at (8, 8, 8), r = z - 8 from 2.5 to 3.5."""
    ms = [prism("z", AX, AY, 2.5, 10.5, 11.5, "brass", n=teeth, r_end=3.5),
          prism("z", AX, AY, 3.5, 11.5, 11.9, "brass", n=teeth, caps=(False, True)),
          prism("z", AX, AY, 1.5, 11.9, 12.6, "steel", n=8, caps=(False, True))]
    s2 = math.sqrt(0.5)
    for k in range(teeth):
        a = 2 * math.pi * (k + 0.5) / teeth
        u = (math.cos(a), math.sin(a), 0.0)
        sl = (u[0] * s2, u[1] * s2, s2)              # along the slant
        nn = (u[0] * s2, u[1] * s2, -s2)             # out of the cone face
        tg = (-u[1], u[0], 0.0)
        c = (AX + 3.0 * u[0] + 0.12 * nn[0], AY + 3.0 * u[1] + 0.12 * nn[1], 11.0 + 0.12 * nn[2])
        ms.append(obox(c, sl, tg, nn, 0.65, 0.28, 0.25, "brass"))
    return ms

def hub():
    m = Model()
    m.add(*rails_x(((1.5, 3.5), (12.5, 14.5))))
    m.add(box(2.0, 1.25, 2.0, 14.0, 2.0, 14.0, "steel"))
    m.add(box(3.5, 2.0, 3.5, 12.5, 3.0, 12.5, "castiron"))
    lo, hi, top = 3.25, 12.75, 10.5
    m.add(box(lo, 3.0, lo, hi, 3.75, hi, "castiron"))                                     # floor of the case
    walls = [box(lo, 3.75, lo, hi, top, 4.0, "paint"), box(lo, 3.75, 12.0, hi, top, hi, "paint"),
             box(lo, 3.75, 4.0, 4.0, top, 12.0, "paint"), box(12.0, 3.75, 4.0, hi, top, 12.0, "paint")]
    m.add(*walls)
    # coping on the walls
    m.add(box(lo - 0.15, top, lo - 0.15, hi + 0.15, top + 0.35, 4.15, "castiron"),
          box(lo - 0.15, top, 11.85, hi + 0.15, top + 0.35, hi + 0.15, "castiron"),
          box(lo - 0.15, top, 4.15, 4.15, top + 0.35, 11.85, "castiron"),
          box(11.85, top, 4.15, hi + 0.15, top + 0.35, 11.85, "castiron"))
    # bearing bosses on the four faces of the case, with seals
    boss = [prism("z", AX, AY, 2.0, 1.75, lo, "castiron", n=10, caps=(True, False))]
    bolts = bolt_ring("z", AX, AY, 2.4, hi - 0.05, hi + 0.3, 4, phase=0.0)   # (heads +z; turned for the z- face)
    boss2 = [prism("z", AX, AY, 2.0, hi, 14.25, "castiron", n=10, caps=(False, True))] + bolts
    boss += turned(bolts, 2)
    for k in (0, 1):                                    # z faces, then x faces
        m.add(*turned(boss + boss2, k))
    r = Model()
    r.add(*couplings())
    r.add(*bevel_gear_zp())
    rx = Model()
    xs = [shaft(0.5, 5.0)] + half_coupling(0) + [shaft(11.0, 15.5)] + half_coupling(16)
    rx.add(*turned(xs, 1))
    rx.add(*turned(bevel_gear_zp(), 1), *turned(bevel_gear_zp(), 3))
    hit = [(0, 0, 1.5, 16, 3.0, 14.5), (lo - 0.4, 2.0, lo - 0.4, hi + 0.4, 12.0, hi + 0.4), SHAFT_BOX,
           (0, 5.75, 5.75, 16, 10.25, 10.25)]
    return [("main", m), ("rotor", r), ("rotor_x", rx)], {"rotor": CENTRE_ORIGIN, "rotor_x": CENTRE_ORIGIN}, PAINT["plain"], hit

# ---------------------------------------------------------------- flywheel
def flywheel():
    m = Model()
    m.add(*rails_x(((1.25, 4.75), (11.25, 14.75))))
    for z0, z1 in ((1.75, 4.25), (11.75, 14.25)):
        zc = (z0 + z1) / 2
        m.add(box(3.5, 1.25, z0, 12.5, 2.0, z1, "castiron"))
        m.add(box(AX - 1.6, 2.0, z0 + 0.25, AX + 1.6, AY - 1.0, z1 - 0.25, "castiron"))
        m.add(prism("z", AX, AY, 2.25, z0 + 0.25, z1 - 0.25, "castiron", n=10))
        m.add(box(AX - 2.75, AY - 0.15, z0 + 0.35, AX + 2.75, AY + 0.3, z1 - 0.35, "paint"))
        for x in (AX - 2.45, AX + 2.45):
            m.add(prism("y", x, zc, 0.25, AY + 0.3, AY + 1.0, "brass", n=6, caps=(False, True)))
        for x in (4.2, 11.8):
            m.add(prism("y", x, zc, 0.35, 2.0, 2.5, "brass", n=6, caps=(False, True)))
        m.add(prism("y", AX, zc, 0.5, AY + 2.0, AY + 2.9, "brass", n=8))
    r = Model()
    r.add(*couplings())
    r.add(tube("z", AX, AY, 6.1, 7.45, 6.5, 9.5, "castiron", n=16))                       # rim
    for z0, z1 in ((6.4, 6.5), (9.5, 9.6)):
        r.add(tube("z", AX, AY, 6.4, 7.15, z0, z1, "red", n=16))                          # painted bands on the rim's faces
    r.add(prism("z", AX, AY, 1.9, 6.6, 9.4, "castiron", n=10))                             # hub
    r.add(prism("z", AX, AY, 1.3, 5.8, 10.2, "steel", n=8))
    r.add(*bolt_ring("z", AX, AY, 1.6, 9.4, 9.65, 4), *turned(bolt_ring("z", AX, AY, 1.6, 9.4, 9.65, 4), 2))
    for k in range(6):
        a = 2 * math.pi * k / 6 + math.pi / 6
        u = (math.cos(a), math.sin(a), 0.0)
        rr = (1.8 + 6.2) / 2
        r.add(obox((AX + rr * u[0], AY + rr * u[1], 8.0), u, (-u[1], u[0], 0.0), (0, 0, 1), (6.2 - 1.8) / 2, 0.5, 0.6,
                   "castiron"))
    hit = [(0.5, 0.5, 6.4, 15.5, 15.5, 9.6), (0, 0, 1.25, 16, 2.0, 14.75), (5.75, 2.0, 1.75, 10.25, 11.0, 4.25),
           (5.75, 2.0, 11.75, 10.25, 11.0, 14.25), SHAFT_BOX]
    return [("main", m), ("rotor", r)], {"rotor": CENTRE_ORIGIN}, PAINT["plain"], hit

# ---------------------------------------------------------------- tachometer
DIAL = (1.9, 10.25, 8.0)       # the needle's origin: the dial's centre (x, y, z), px
DIAL_R = 2.6

def tacho_signal():
    return signal_plate() + [box(0.95, 1.25, 6.75, 2.0, 3.6, 9.25, "darksteel"),
                             box(0.95, 3.6, 6.6, 2.15, 3.9, 9.4, "steel")]

def tacho():
    m = Model()
    m.add(box(0.95, 0, 4.5, 15.05, 1.25, 11.5, "castiron"))                                 # base plate
    for x in (2.6, 13.4):
        for z in (5.1, 10.9):
            m.add(prism("y", x, z, 0.3, 1.25, 1.6, "brass", n=6, caps=(False, True)))
    m.add(box(5.5, 1.25, 6.0, 10.5, 4.75, 10.0, "castiron"))                                 # pedestal
    m.add(box(5.0, 4.75, 5.5, 11.0, 11.25, 10.5, "paint"))                                   # drive box
    m.add(box(4.8, 11.25, 5.3, 11.2, 11.6, 10.7, "castiron"))                                # lid
    for x in (5.4, 10.6):
        for z in (5.7, 10.3):
            m.add(prism("y", x, z, 0.2, 11.6, 11.85, "brass", n=6, caps=(False, True)))
    for z0, z1 in ((4.25, 5.5), (10.5, 11.75)):                                              # bearing bosses
        m.add(prism("z", AX, AY, 2.0, z0, z1, "castiron", n=10))
        m.add(tube("z", AX, AY, 1.05, 1.5, z0 - 0.2, z0, "darksteel", n=10) if z0 < 8 else
              tube("z", AX, AY, 1.05, 1.5, z1, z1 + 0.2, "darksteel", n=10))
    x, y, z = DIAL
    m.add(prism("x", y, z, 0.7, 3.2, 5.0, "steel", n=8, caps=(False, False)))               # drive neck to the dial
    m.add(prism("x", y, z, DIAL_R, 2.2, 3.2, "brass", n=14))                                 # case
    m.add(tube("x", y, z, DIAL_R - 0.35, DIAL_R + 0.1, 2.0, 2.2, "brass", n=14))             # bezel
    m.add(prism("x", y, z, DIAL_R - 0.3, 2.1, 2.2, "glass", n=14, caps=(True, False)))       # face
    for k in range(7):                                                                        # scale ticks, 7:30 .. 4:30
        a = math.radians(-135 + 45 * k)
        dy, dz = math.cos(a), math.sin(a)
        mat = "red" if k >= 5 else "darksteel"
        m.add(obox((2.05, y + 1.75 * dy, z + 1.75 * dz), (1, 0, 0), (0, dy, dz), (0, -dz, dy), 0.04, 0.3, 0.08, mat))
    t = tacho_signal()
    m.add(*t, *turned(t, 2))
    r = Model()
    r.add(*couplings())
    for za in (3.4, 11.85):
        r.add(prism("z", AX, AY, 1.4, za, za + 0.75, "steel", n=10))
        r.add(prism("y", AX, za + 0.375, 0.2, AY + 1.25, AY + 1.55, "darksteel", n=6, caps=(False, True)))
    nd = Model()
    nd.add(obox((x - 0.1, y + 0.8, z), (1, 0, 0), (0, 1, 0), (0, 0, 1), 0.04, 1.15, 0.09, "red"))   # pointing up: 0
    nd.add(prism("x", y, z, 0.3, x - 0.25, x, "darksteel", n=6, caps=(True, False)))
    hit = [(0, 0, 4.5, 16, 4.0, 11.5), (4.8, 4.0, 5.3, 11.2, 11.85, 10.7), (1.6, y - DIAL_R - 0.1, z - DIAL_R - 0.1, 5.0,
           y + DIAL_R + 0.1, z + DIAL_R + 0.1), SHAFT_BOX]
    return [("main", m), ("rotor", r), ("needle", nd)], {"rotor": CENTRE_ORIGIN, "needle": DIAL}, PAINT["plain"], hit

MODELS = {"shaftgen8kw": lambda: generator("shaftgen8kw"), "shaftgen4kw": lambda: generator("shaftgen4kw"),
          "shaftjoint": joint, "shafthub": hub, "shaftflywheel": flywheel, "shafttacho": tacho}

def main():
    res = sys.argv[1]
    for name, fn in MODELS.items():
        parts, origins, paint, hit = fn()
        d, counts = write_model(res, name, parts, origins, paint, hit)
        print(name, counts, "total", sum(counts.values()), "hitbox", len(hit))

if __name__ == "__main__":
    main()
