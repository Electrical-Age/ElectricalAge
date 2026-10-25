#!/usr/bin/env python3
"""Submersible pump (50V / 200V): a motor in a can standing in water, an intake screen round its foot, a pump head on
top and a riser up to an iron pipe's flange at the centre of the top face (SubmersiblePumpElement's outlet port).

Drawn by SubmersiblePumpRender -> SubmersiblePumpDescriptor.draw() (part `main`), after front.glRotateXnRef().
Connections: the speed signal on the front (x = 0), power on both flanks (z = 0 and z = 16), all at the floor
(LRDU Down) on terminal plates; the outlet is the flange on top.

Design frame: mcmodel px (0..16, corner origin), front XN (x = 0):
  skid:  cast-iron foot plate with three terminal plates, each wired to the can by a conduit; a lifting eye at the
         back;
  can:   tier-painted motor can with seam bands; the intake screen (louvres) just above the skid;
  head:  steel pump head, the riser and the flange with its bolts.

Usage: submersiblepump.py RESOURCES_DIR  -> assets/eln/model/submersiblepump{50v,200v}/
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Model, box, prism
from oilkit import atlas, write_parts, out_dir, turned, terminal_plate, bolt_ring, PAINT

TIERS = {
    # r: can radius; top: can top; head: pump head radius
    "50v": dict(r=3.5, top=9.5, head=2.75),
    "200v": dict(r=4.25, top=10.5, head=3.25),
}
C = 8.0

def pump(t):
    m = Model()
    r = t["r"]
    # skid, terminal plates (signal on the front, power on the flanks) and their conduits to the can
    m.add(box(2.0, 0, 2.0, 14.0, 1.25, 14.0, "castiron", faces="nsewu"))
    m.add(box(14.0, 0, 6.5, 16.0, 0.75, 9.5, "castiron", faces="nsewu"))          # tongue with the lifting eye, back
    m.add(box(14.75, 0.75, 7.5, 15.5, 2.25, 8.5, "darksteel", faces="nsewu"))
    for k in (0, 1, 3):
        m.add(*turned([*terminal_plate(), box(0.85, 1.9, 7.4, C - r + 0.2, 2.6, 8.6, "darksteel")], k))
    # intake screen, can, seam bands
    m.add(prism("y", C, C, r - 0.3, 1.25, 1.5, "darksteel", n=12))
    m.add(prism("y", C, C, r, 1.5, 3.75, "louvre", n=12, caps=(False, False)))
    m.add(prism("y", C, C, r, 3.75, t["top"], "paint", n=12, caps=(False, True)))
    for y in (3.75, t["top"] - 0.6):
        m.add(prism("y", C, C, r + 0.15, y, y + 0.5, "darksteel", n=12, caps=(False, False)))
    # pump head, riser, flange (an iron pipe's) with bolts
    m.add(prism("y", C, C, t["head"], t["top"], t["top"] + 1.75, "steel", n=12, caps=(False, True)))
    m.add(prism("y", C, C, 2.0, t["top"] + 1.75, 15.0, "steel", n=12, caps=(False, False)))
    m.add(prism("y", C, C, 2.75, 15.0, 16.0, "steel", n=12))
    m.add(*bolt_ring("y", C, C, 2.35, 14.75, 15.0, 4, rb=0.22))
    return m

def main():
    res = sys.argv[1]
    for tier, t in TIERS.items():
        name = "submersiblepump" + tier
        d = out_dir(res, name)
        counts = write_parts(d, name, [("main", pump(t))])
        atlas(os.path.join(d, "tex.png"), PAINT[tier])
        print(name, counts)

if __name__ == "__main__":
    main()
