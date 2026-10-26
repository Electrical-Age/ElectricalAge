#!/usr/bin/env python3
"""Steam condenser (Small Steam Condenser / Steam Condenser, for the 50V / 200V Steam Turbine): a shell-and-tube condenser
lying on two saddles, ringed by copper cooling fins, with its condensate pump and outlet flange at the front, a makeup
flange on top, and a duct stub toward the turbine beside it.

Drawn by CondenserRender -> CondenserDescriptor.draw() after front.glRotateXnRef(): part `main` always, and one of
`duct_right` (z = 16), `duct_back` (x = 16), `duct_left` (z = 0) toward the turbine exhausting into it (its exhaust
flange meets the stub's, at the turbine's shaft height: steamturbine.py's EXHAUST_R and cy).

Design frame: mcmodel px (0..16, corner origin), front XN (x = 0): the condensate outlet (an iron pipe's flange, low on
the front face, the pump behind it); top: makeup flange at the centre; thermal cables on the other sides at the floor.

Usage: condenser.py RESOURCES_DIR  -> assets/eln/model/condenser{50v,200v}/
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Model, box, prism, tube
from oilkit import atlas, write_parts, out_dir, turned, bolt_ring, heat_pad, PAINT

TIERS = {
    # r: shell radius; cy: the turbine's shaft height (its exhaust flange); fins: x of the cooling fins
    "50v": dict(r=4.5, cy=6.75, fins=(4.0, 6.0, 8.0, 10.0, 12.0)),
    "200v": dict(r=5.25, cy=7.25, fins=(3.5, 5.25, 7.0, 8.75, 10.5, 12.25)),
}
C = 8.0
SY = 7.5                 # the shell's axis height
EXHAUST_R = 2.25         # steamturbine.py's

def main_part(t):
    m = Model()
    r = t["r"]
    # skid and saddles
    m.add(box(1.5, 0, 3.0, 14.5, 1.0, 13.0, "darksteel", faces="nsewu"))
    for x in (3.0, 11.5):
        m.add(box(x, 1.0, C - r + 0.75, x + 1.5, SY - r + 1.0, C + r - 0.75, "castiron"))
    # the shell (tier band), end caps, cooling fins
    m.add(prism("x", SY, C, r, 2.0, 14.0, "steel", n=14, caps=(False, False)))
    m.add(prism("x", SY, C, r - 1.0, 1.25, 2.0, "steel", n=14, r_end=r, caps=(True, False)))
    m.add(prism("x", SY, C, r, 14.0, 14.75, "steel", n=14, r_end=r - 1.0, caps=(False, True)))
    for x in t["fins"]:
        m.add(tube("x", SY, C, r - 0.1, r + 0.7, x, x + 0.4, "copper", n=14, inner=False))
    m.add(tube("x", SY, C, r - 0.1, r + 0.3, 13.0, 13.75, "paint", n=14, inner=False))
    # makeup: a riser to an iron pipe's flange on the top face
    m.add(prism("y", C, C, 2.0, SY + r - 0.5, 15.0, "steel", n=12, caps=(False, False)))
    m.add(prism("y", C, C, 2.75, 15.0, 16.0, "steel", n=12))
    m.add(*bolt_ring("y", C, C, 2.35, 14.75, 15.0, 4, rb=0.22))
    # condensate pump (under the front cap) and the outlet: an iron pipe's flange low on the front face
    m.add(prism("y", 3.5, C, 1.75, 1.0, 3.75, "paint", n=10))                    # pump motor
    m.add(prism("x", 2.75, C, 1.25, 1.0, 2.5, "steel", n=10, caps=(False, True)))
    m.add(prism("x", 2.75, C, 2.0, 0.0, 1.0, "steel", n=12, caps=(True, False)))  # outlet flange on the face
    m.add(*bolt_ring("x", 2.75, C, 1.6, 1.0, 1.25, 4, rb=0.2))
    # thermal pads on the floor of the three other sides (cables there take its heat)
    for k in (1, 2, 3):
        m.add(*turned(heat_pad(depth=0.75, top=3.0), k))
    return m

def duct(t):
    """Toward the turbine on the x = 0 side (turned for the others): the shell's nozzle to a flange on that face."""
    ms = [prism("x", t["cy"], C, EXHAUST_R, 0.75, C - t["r"] + 1.0, "castiron", n=12, caps=(False, False)),
          prism("x", t["cy"], C, EXHAUST_R + 0.75, 0.0, 0.75, "steel", n=12, caps=(True, False))]
    ms += bolt_ring("x", t["cy"], C, EXHAUST_R + 0.4, 0.75, 1.0, 4, rb=0.25)
    return ms

def main():
    res = sys.argv[1]
    for tier, t in TIERS.items():
        name = "condenser" + tier
        d = out_dir(res, name)
        parts = [("main", main_part(t))]
        for pname, k in (("duct_right", 1), ("duct_back", 2), ("duct_left", 3)):
            dm = Model()
            dm.add(*turned(duct(t), k))
            parts.append((pname, dm))
        counts = write_parts(d, name, parts)
        atlas(os.path.join(d, "tex.png"), PAINT[tier])
        print(name, counts)

if __name__ == "__main__":
    main()
