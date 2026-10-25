#!/usr/bin/env python3
"""Boiler: a riveted horizontal steam drum on saddles over a cast-iron heat-collector base.

Drawn by BoilerRender -> BoilerDescriptor.draw(), which draws the parts main/body/furnace that exist (this model:
`main`), after front.glRotateXnRef(). Connections (BoilerElement): thermal on all four horizontal faces at the floor
(LRDU Down); pipe ports: the top flange (steam), the feedwater flange on the right flank (water, through a check
valve) and the dome port under it (centred), which couples to a turbine's inlet below; also the fluid capability.

Design frame: mcmodel px (0..16, corner origin), front XN (x = 0):
  base:  cast-iron heat collector with a heat-tinted copper contact pad (8 px wide, floor to 4 px) on every face;
  drum:  riveted plate, axis front-back, dished heads, two seam bands, oxide-red saddles;
  front: gauge glass (water level) between brass cocks, pressure gauge; top: steam dome with the outlet flange,
         z+ flank (the right): feedwater inlet, check valve, handwheel and a pipe flange at the face's centre;
  dome pipe: from the dome down the z- flank into the base, out of a flange centred on the underside (the dome port).

Usage: boiler.py RESOURCES_DIR  -> assets/eln/model/boiler/
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Model, box, prism
from oilkit import atlas, write_parts, out_dir, turned, heat_pad, dial, handwheel, bolt_ring, PAINT

CY, CZ, R = 9.25, 8.0, 5.25        # drum axis height, z, radius
DX = (2.25, 13.75)                 # cylindrical shell x span; heads beyond it

def boiler():
    m = Model()
    # base: heat collector, top plate, four heat pads
    m.add(box(1.25, 0, 1.25, 14.75, 3.25, 14.75, "castiron", faces="nsewu"))
    m.add(box(1.5, 3.25, 1.5, 14.5, 3.6, 14.5, "darksteel", faces="nsewu"))
    for k in range(4):
        m.add(*turned(heat_pad(depth=1.0, top=4.0), k))
    for k in range(4):                                       # corner ribs
        m.add(*turned([box(1.0, 0, 1.0, 2.0, 3.0, 2.0, "darksteel", faces="nsewu")], k))
    # saddles (cradle the drum)
    for x in (3.25, 11.25):
        m.add(box(x, 3.6, 4.5, x + 1.5, 5.4, 11.5, "paint", faces="nsewu"))
        m.add(box(x - 0.25, 3.6, 4.25, x + 1.75, 4.1, 11.75, "paint", faces="nsewu"))
    # drum shell, dished heads, seam bands
    m.add(prism("x", CY, CZ, R, DX[0], DX[1], "rivet", n=16, caps=(False, False)))
    m.add(prism("x", CY, CZ, R - 1.5, DX[0] - 1.0, DX[0], "rivet", n=16, r_end=R, caps=(True, False)))
    m.add(prism("x", CY, CZ, R, DX[1], DX[1] + 1.0, "rivet", n=16, r_end=R - 1.5, caps=(False, True)))
    for x in (5.6, 10.0):
        m.add(prism("x", CY, CZ, R + 0.15, x, x + 0.4, "darksteel", n=16, caps=(False, False)))
    hx = DX[0] - 1.0                                         # front head face x
    # manhole on the back head
    m.add(prism("x", CY - 0.5, CZ, 1.6, DX[1] + 1.0, DX[1] + 1.4, "darksteel", n=8))
    m.add(box(DX[1] + 1.4, CY - 0.75, CZ - 1.0, DX[1] + 1.7, CY - 0.25, CZ + 1.0, "darksteel"))
    # gauge glass on the front head: glass tube between two brass cocks, guard rods
    gz = CZ - 2.4
    m.add(prism("y", 0.9, gz, 0.4, 6.4, 11.6, "water", n=6, caps=(False, False)))
    for y in (5.9, 11.6):
        m.add(box(0.35, y, gz - 0.55, hx + 1.3, y + 0.6, gz + 0.55, "brass"))
    m.add(box(0.3, 6.5, gz - 0.65, 0.45, 11.5, gz - 0.5, "darksteel"), box(0.3, 6.5, gz + 0.5, 0.45, 11.5, gz + 0.65, "darksteel"))
    m.add(prism("y", 0.9, gz, 0.2, 5.1, 5.9, "brass", n=6))   # drain cock
    # pressure gauge on a siphon above the head
    m.add(prism("x", CY + 2.6, CZ + 1.6, 0.2, 0.9, hx + 0.6, "brass", n=6, caps=(False, False)))
    m.add(*dial(0.5, CY + 2.6, CZ + 1.6, 1.1, "xn", depth=0.4))
    # steam dome + outlet flange on top
    sx = 8.0   # (centred: the top port is the face's centre, where a pipe's flange meets it)
    top = CY + R * math.cos(math.pi / 16)
    m.add(prism("y", sx, CZ, 1.7, top - 1.0, top + 0.5, "rivet", n=10, caps=(False, True)))
    m.add(prism("y", sx, CZ, 2.0, top + 0.5, 15.0, "steel", n=12, caps=(False, False)))     # (an iron pipe's bore)
    m.add(prism("y", sx, CZ, 2.75, 15.0, 16.0, "steel", n=12))                               # flange: an iron pipe's

    # (no built-in safety valve: Baughn, pressure is real; fit a safety valve part on the steam line)
    # feedwater inlet on the z+ flank (BoilerElement.feed: the face's centre, where a pipe's flange meets it): pipe,
    # check valve with its handwheel, reducer, an iron pipe's flange at the face
    fx, fy = 8.0, 8.0
    m.add(prism("z", fx, fy, 0.7, CZ + 4.0, 12.9, "darksteel", n=8, caps=(False, False)))
    m.add(box(fx - 1.3, fy - 1.3, 12.9, fx + 1.3, fy + 1.3, 14.3, "castiron"))
    m.add(prism("y", fx, 13.6, 0.25, fy + 1.3, fy + 2.3, "darksteel", n=6, caps=(False, False)))
    m.add(*handwheel(fx, fy + 2.4, 13.6, r=0.9, axis="y"))
    m.add(prism("z", fx, fy, 1.0, 14.3, 14.7, "steel", n=8, r_end=2.0, caps=(False, False)))
    m.add(prism("z", fx, fy, 2.0, 14.7, 15.0, "steel", n=12, caps=(False, False)))
    m.add(prism("z", fx, fy, 2.75, 15.0, 16.0, "steel", n=12))                                 # flange: an iron pipe's
    # dome pipe (pipes phase 2): steam taken at the dome comes down the z- flank into the base and out of a flange on
    # the underside (the dome port: a boiler set on a turbine feeds its inlet), as a locomotive's steam pipe does
    py, pz, pr = 14.4, 1.9, 0.6
    m.add(prism("z", sx, py, pr, pz, CZ - 1.0, "steel", n=8, caps=(False, False)))            # dome -> z- flank
    m.add(prism("y", sx, pz, pr + 0.15, py - 0.75, py + 0.75, "darksteel", n=8))               # elbow
    m.add(prism("y", sx, pz, pr, 3.6, py - 0.75, "steel", n=8, caps=(False, False)))           # down the flank
    m.add(prism("y", sx, pz, pr + 0.35, 3.6, 4.1, "steel", n=8))                               # gland into the base
    m.add(prism("y", 8.0, 8.0, 2.75, 0.0, 1.0, "steel", n=12, caps=(True, False)))             # bottom flange (an iron pipe's)
    m.add(*bolt_ring("y", 8.0, 8.0, 1.5, 0.0, 0.05, 4, rb=0.22))
    # nameplate on the z- flank of the base
    m.add(box(12.7, 1.0, 1.15, 13.9, 2.5, 1.25, "brass", faces="n"))
    return m

def main():
    res = sys.argv[1]
    d = out_dir(res, "boiler")
    counts = write_parts(d, "boiler", [("main", boiler())])
    atlas(os.path.join(d, "tex.png"), PAINT["boiler"])
    print("boiler", counts)

if __name__ == "__main__":
    main()
