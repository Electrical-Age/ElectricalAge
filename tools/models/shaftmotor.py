#!/usr/bin/env python3
"""200V Shaft Motor (README-shaft.md): a totally enclosed fan-cooled industrial motor lying across the shaft line, on a
skid. Ribbed frame (axial cooling fins), drive-end bell on the left (z = 0), a fan cowl with a grille on the right
(z = 16) and the external cooling fan behind it, terminal box on the front flank with a conduit down to the power
floor terminal; signal terminal on the back. The shaft comes out of both sides to half couplings.

Parts: `main`, `rotor` (shaft, couplings, cooling fan; origin on the shaft line, turned about z).

Design frame: shaftkit px (0..16, corner origin), front XN (x = 0), shaft along z at x = 8, y = 8.

Usage: shaftmotor.py RESOURCES_DIR  -> assets/eln/model/shaftmotor/
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Model, box, prism, tube
from oilkit import turned, terminal_plate, signal_plate, obox, bolt_ring
import shaftkit
from shaftkit import AX, AY, shaft, shaft_ends, write_model, preview, PAINT, CENTRE_ORIGIN

R = 4.6                 # frame radius
Z0, Z1 = 2.75, 12.25    # ribbed frame span
COWL = (12.25, 15.0)    # fan cowl span
FAN = (13.1, 13.9)      # fan blades (inside the cowl)

def main_part():
    m = Model()
    # skid and feet
    m.add(box(2.0, 0, 1.75, 14.0, 1.0, 14.25, "darksteel", faces="nsewu"))
    for z in (3.5, 10.0):
        m.add(box(3.25, 1.0, z, 12.75, 1.6, z + 2.0, "castiron"))
        m.add(box(4.25, 1.6, z + 0.25, 11.75, AY - R + 1.25, z + 1.75, "paint"))
        for x in (3.75, 12.25):
            m.add(prism("y", x, z + 1.0, 0.3, 1.6, 1.9, "brass", n=6, caps=(False, True)))
    # ribbed frame: the drum and axial cooling fins
    m.add(prism("z", AX, AY, R, Z0, Z1, "paint", n=14, caps=(False, False)))
    nf = 14
    for k in range(nf):
        a = 2 * math.pi * (k + 0.5) / nf
        if math.sin(a) < -0.75:            # (none under it, over the feet)
            continue
        u = (math.cos(a), math.sin(a), 0.0)
        c = (AX + (R + 0.3) * u[0], AY + (R + 0.3) * u[1], (Z0 + Z1) / 2 + 0.25)
        m.add(obox(c, u, (-u[1], u[0], 0.0), (0, 0, 1), 0.45, 0.17, (Z1 - Z0) / 2 - 0.25, "paint"))
    # drive-end bell (left) and bearing cap, non-drive end shield (right)
    m.add(prism("z", AX, AY, R - 1.4, 1.5, Z0, "castiron", n=14, r_end=R + 0.2, caps=(True, False)))
    m.add(tube("z", AX, AY, R - 0.1, R + 0.3, Z0, Z0 + 0.4, "darksteel", n=14, inner=False))
    m.add(prism("z", AX, AY, 1.6, 1.0, 1.5, "darksteel", n=10, caps=(True, False)))
    m.add(*bolt_ring("z", AX, AY, R - 0.6, 1.95, 2.2, 6, rb=0.2))
    m.add(tube("z", AX, AY, R - 0.1, R + 0.3, Z1 - 0.4, Z1, "darksteel", n=14, inner=False))
    # fan cowl: a sheet-steel shroud, open (dark) inside, a grille over its end; the fan turns behind it
    m.add(tube("z", AX, AY, R - 0.2, R + 0.15, COWL[0], COWL[1], "steel", n=14))
    m.add(prism("z", AX, AY, R - 0.15, Z1 - 0.01, Z1 + 0.2, "darksteel", n=14, caps=(False, True)))   # end shield face (dark)
    gz = COWL[1] - 0.35
    for y in (-3.0, -1.5, 1.5, 3.0):                     # horizontal grille bars (the hub bar carries the bearing)
        w = math.sqrt(max(0.0, (R - 0.2) ** 2 - y * y))
        m.add(box(AX - w, AY + y - 0.25, gz, AX + w, AY + y + 0.25, COWL[1], "louvre"))
    for x in (-3.0, -1.5, 1.5, 3.0):
        w = math.sqrt(max(0.0, (R - 0.2) ** 2 - x * x))
        m.add(box(AX + x - 0.2, AY - w, gz - 0.2, AX + x + 0.2, AY + w, gz, "louvre"))
    m.add(tube("z", AX, AY, 1.2, 1.9, gz - 0.2, COWL[1], "darksteel", n=10))   # grille hub (the shaft through it)
    m.add(tube("z", AX, AY, R - 0.2, R + 0.35, COWL[1] - 0.4, COWL[1], "darksteel", n=14, inner=False))   # rim
    # terminal box on the front flank, conduit down to the power terminal
    tx0 = AX - R - 1.0
    m.add(box(tx0, 6.0, 5.5, AX - R + 1.0, 10.0, 10.5, "paint"))
    m.add(box(tx0 - 0.3, 5.75, 5.25, tx0, 10.25, 10.75, "darksteel"))                 # lid
    for y, z in ((6.25, 5.5), (6.25, 10.5), (9.75, 5.5), (9.75, 10.5)):
        m.add(prism("x", y, z, 0.2, tx0 - 0.5, tx0 - 0.3, "brass", n=6, caps=(True, False)))
    m.add(box(tx0 + 0.25, 1.0, 7.0, tx0 + 1.5, 6.0, 9.0, "darksteel"))                # conduit
    m.add(box(0.85, 0.0, 6.5, tx0 + 1.5, 1.4, 9.5, "darksteel", faces="nsewu"))       # into the plate
    m.add(*terminal_plate())
    # signal: a small junction box on the back flank, conduit down to the back terminal
    sx = AX + R + 0.5
    m.add(box(sx - 0.5, 6.5, 6.75, sx + 1.0, 8.5, 9.25, "steel"))
    m.add(box(sx, 1.0, 7.5, sx + 0.75, 6.5, 8.5, "darksteel"))
    m.add(box(sx, 0.0, 7.25, 15.15, 1.2, 8.75, "darksteel", faces="nsewu"))
    m.add(*turned(signal_plate(), 2))
    # lifting eye
    m.add(box(AX - 0.25, AY + R + 0.4, 7.25, AX + 0.25, AY + R + 1.6, 8.75, "steel"))
    m.add(tube("x", AY + R + 1.6, 8.0, 0.4, 0.9, AX - 0.25, AX + 0.25, "steel", n=8))
    return m

def rotor_part():
    m = Model()
    m.add(*shaft_ends(1.0, COWL[1] - 0.4))
    m.add(shaft(COWL[0], COWL[1] - 0.4))
    # external cooling fan: hub and pitched radial blades
    m.add(prism("z", AX, AY, 1.4, FAN[0] - 0.3, FAN[1] + 0.1, "darksteel", n=10))
    nb = 8
    for k in range(nb):
        a = 2 * math.pi * k / nb
        u = (math.cos(a), math.sin(a), 0.0)
        t = (-u[1], u[0], 0.0)
        p = math.radians(35)                                   # blade pitch
        bt = tuple(math.cos(p) * t[i] + math.sin(p) * (0, 0, 1)[i] for i in range(3))
        bn = tuple(-math.sin(p) * t[i] + math.cos(p) * (0, 0, 1)[i] for i in range(3))
        rc = (1.3 + R - 0.5) / 2
        c = (AX + rc * u[0], AY + rc * u[1], (FAN[0] + FAN[1]) / 2)
        m.add(obox(c, u, bt, bn, (R - 0.5 - 1.3) / 2, 0.75, 0.08, "steel"))
    return m

HITBOX = [
    (2.0, 0.0, 1.75, 14.0, 1.0, 14.25),                      # skid
    (AX - R - 0.6, 1.0, 1.0, AX + R + 0.6, AY + R + 0.6, COWL[1]),    # feet, frame, fins, cowl
    (AX - R - 1.3, 1.0, 5.25, AX - R + 1.0, 10.25, 10.75),    # terminal box and conduit
    (0.0, 0.0, 4.25, 1.0, 4.5, 11.75),                        # power terminal
    (15.0, 0.0, 6.5, 16.0, 8.5, 9.5),                         # signal box and terminal
    (AX - 2.25, AY - 2.25, 0.0, AX + 2.25, AY + 2.25, 16.0),  # shaft and couplings
]

def main():
    res = sys.argv[1]
    name = "shaftmotor"
    d, counts = write_model(res, name, [("main", main_part()), ("rotor", rotor_part())],
                            origins={"rotor": CENTRE_ORIGIN}, paint=PAINT["motor"], hitbox=HITBOX)
    print(name, counts, sum(counts.values()))
    if len(sys.argv) > 2:
        preview(res, name, sys.argv[2])

if __name__ == "__main__":
    main()
