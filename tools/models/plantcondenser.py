#!/usr/bin/env python3
"""Plant Steam Condenser (README-shaft.md): a big shell-and-tube surface condenser lying front-back on two saddles over
a skid, the hotwell slung under it between the saddles, a condensate pump on its front head and a circulating pump on
its left flank (both driven by a Shaft Motor set against that side: parts `drive_front` / `drive_left`, a half
coupling and pump shaft stub at the shaft line), a steam-jet air ejector on top.

Ports (PlantCondenserElement): exhaust steam in at large flanges on the top centre, back centre and right centre
(z = 16 face at (8, 8)); condensate out: front LOW small; makeup in: left HIGH small; cooling water in: left LOW small,
out: right LOW small. The right LOW flange overlaps the right large one: the large disc is recessed 0.1 px so the small
one lies over it. Thermal pads at the floor of the back (full) and both flanks (low, under the cooling-water flanges).

Parts: `main`, `drive_left`, `drive_front` (origin (8, 8, 8): on both drive axes).

Design frame: shaftkit px (0..16, corner origin), front XN (x = 0), right z = 16, left z = 0.

Usage: plantcondenser.py RESOURCES_DIR [PREVIEW_DIR]  -> assets/eln/model/plantcondenser/
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Model, Mesh, box, prism, tube
from oilkit import turned, heat_pad, bolt_ring, dial
from shaftkit import FLANGE_R, AX, AY, shaft, half_coupling, pipe_flange, write_model, preview, PAINT

C = 8.0
R = 4.75                  # shell radius, axis along x at (y 8, z 8)
SX = (3.75, 14.0)         # cylindrical shell span; heads beyond

def shifted(meshes, dx=0.0, dy=0.0, dz=0.0):
    out = []
    for m in meshes:
        n = Mesh(m.mat)
        for p in m.quads:
            n.poly(*[(x + dx, y + dy, z + dz) for x, y, z in p])
        out.append(n)
    return out

def low_pad(top=2.25, depth=0.75):
    """A thermal floor pad for the x = 0 face, low enough to sit under a LOW port's flange (y 2.5): heat_pad's look
    without the bolts."""
    ms = [box(0.1, 0, 4.0, depth, top, 12.0, "hotcopper"), box(depth, 0, 3.5, depth + 0.5, top + 0.25, 12.5, "castiron")]
    return ms

def flange(face, c1, c2, size="small", stub=1.5, bolts=4):
    """shaftkit.pipe_flange with square bolt heads (5 faces: 8 triangles each rather than a hex prism's 16) on the
    flange's inner side, to keep the many ports within the triangle budget."""
    ms = pipe_flange(face, c1, c2, size, stub=stub, bolts=0)
    fr = FLANGE_R[size]
    axis = face[0]
    at0 = face.endswith("0") and not face.endswith("16")
    a0, a1 = (1.0, 1.25) if at0 else (14.75, 15.0)
    h = 0.25 if size == "large" else 0.18
    skip = {"x": "we", "y": "du", "z": "ns"}[axis][0 if at0 else 1]   # the face against the disc
    for k in range(bolts):
        ang = 2 * math.pi * (k + 0.5) / bolts
        p, q = c1 + (fr - 0.45) * math.cos(ang), c2 + (fr - 0.45) * math.sin(ang)
        lo, hi = {"x": ((a0, p - h, q - h), (a1, p + h, q + h)), "y": ((p - h, a0, q - h), (p + h, a1, q + h)),
                  "z": ((p - h, q - h, a0), (p + h, q + h, a1))}[axis]
        ms.append(box(*lo, *hi, "brass", faces="".join(f for f in "nsewud" if f != skip)))
    return ms

def main_part():
    m = Model()
    # skid, saddles, hotwell
    m.add(box(1.0, 0, 1.5, 15.0, 1.0, 14.5, "darksteel", faces="nsewu"))
    for x in (4.25, 11.0):
        m.add(box(x, 1.0, C - R + 0.75, x + 1.5, AY - R + 1.25, C + R - 0.75, "castiron"))
        m.add(box(x - 0.25, 1.0, C - R + 0.5, x + 1.75, 1.5, C + R - 0.5, "castiron"))
    m.add(box(6.0, 1.0, 5.0, 10.75, AY - R + 0.75, 11.0, "steel"))                 # hotwell
    # the shell: cylinder, dished heads, tube-sheet flanges at the joints, a paint band
    m.add(prism("x", AY, C, R, SX[0], SX[1], "paint", n=14, caps=(False, False)))
    m.add(prism("x", AY, C, R - 1.75, SX[0] - 0.75, SX[0], "paint", n=14, r_end=R, caps=(True, False)))
    m.add(prism("x", AY, C, R, SX[1], SX[1] + 0.75, "paint", n=14, r_end=R - 1.5, caps=(False, True)))
    for x in (SX[0], SX[1] - 0.5):
        m.add(tube("x", AY, C, R - 0.1, R + 0.4, x, x + 0.5, "darksteel", n=12, inner=False))
    # exhaust inlets (large): top riser, back nozzle, right nozzle (its flange recessed 0.1 px under the small one)
    m.add(*flange("y16", C, C, "large", stub=15.0 - (AY + R - 0.75), bolts=4))
    m.add(*flange("x16", AY, C, "large", stub=15.0 - SX[1], bolts=4))
    m.add(*shifted(flange("z16", C, AY, "large", stub=15.0 - (C + R - 0.75), bolts=4), dz=-0.1))
    # cooling water: in on the left LOW, out on the right LOW, pipes into the shell's lower flanks
    m.add(*flange("z0", C, 4.0, "small", stub=5.25))
    m.add(*flange("z16", C, 4.0, "small", stub=5.25))
    # makeup in: left HIGH, into the shell's upper flank
    m.add(*flange("z0", C, 12.0, "small", stub=5.0))
    # circulating pump on the left flank (its shaft at the shaft line, z = 0 face): volute, bearing cap
    m.add(prism("z", C, AY, 2.1, 1.25, 3.0, "paint", n=10))
    m.add(prism("z", C, AY, 1.0, 0.9, 1.25, "darksteel", n=8, caps=(True, False)))
    m.add(prism("z", C, AY, 1.2, 3.0, 4.0, "castiron", n=8, caps=(False, False)))    # discharge into the shell
    # condensate pump on the front head (shaft at (y 8, z 8), x = 0 face): volute, bearing cap
    m.add(prism("x", AY, C, 2.1, 1.25, 3.0, "paint", n=10))
    m.add(prism("x", AY, C, 1.0, 0.9, 1.25, "darksteel", n=8, caps=(True, False)))
    # its suction from the hotwell, its discharge down to the condensate flange (front LOW)
    m.add(*flange("x0", 4.0, C, "small", stub=1.25))
    m.add(prism("y", 2.0, C, 0.75, 3.25, AY - 1.5, "steel", n=8, caps=(True, False)))
    m.add(prism("x", 2.25, 10.75, 0.6, 2.4, 6.0, "steel", n=8, caps=(True, False)))
    m.add(prism("y", 2.4 + 0.6, 10.75, 0.6, 1.65, AY - 1.0, "steel", n=8, caps=(True, False)))
    # steam-jet air ejector on top, front-left: a nozzle and diffuser off the shell
    ez, ey = 4.25, AY + 3.6
    m.add(prism("x", ey, ez, 0.45, 3.0, 5.0, "steel", n=8))
    m.add(prism("x", ey, ez, 0.45, 5.0, 7.25, "steel", n=8, r_end=0.8))
    m.add(prism("y", 7.0, ez, 0.4, AY + 2.0, ey, "steel", n=6, caps=(False, False)))
    # vacuum gauge on the front head
    m.add(prism("x", AY + 3.0, C + 2.75, 0.25, SX[0] - 1.6, SX[0] - 0.4, "brass", n=6, caps=(False, False)))
    m.add(*dial(SX[0] - 2.1, AY + 3.0, C + 2.75, 0.85, facing="xn"))
    # thermal pads: back full height (4 px under the back flange), flanks low (under the cooling-water flanges)
    m.add(*turned(heat_pad(depth=0.75, top=3.0), 2))
    m.add(*turned(low_pad(), 1))
    m.add(*turned(low_pad(), 3))
    return m

def drive_left():
    m = Model()
    m.add(*half_coupling(0))
    m.add(shaft(0.5, 1.0, r=0.8))
    return m

def drive_front():
    m = Model()
    m.add(*turned(half_coupling(0) + [shaft(0.5, 1.0, r=0.8)], 1))    # the z = 0 face's coupling, turned onto x = 0
    return m

HITBOX = [
    (1.0, 0.0, 1.5, 15.0, 1.0, 14.5),                         # skid
    (2.75, 1.0, 3.0, 15.0, 13.0, 13.0),                       # shell, saddles, hotwell
    (4.25, 12.5, 4.25, 11.75, 16.0, 11.75),                   # top inlet
    (14.0, 4.25, 4.25, 16.0, 11.75, 11.75),                   # back inlet
    (4.25, 2.5, 12.5, 11.75, 11.75, 16.0),                    # right inlet, cooling water out
    (5.75, 2.5, 0.0, 10.25, 13.5, 3.0),                       # left: pump and its coupling, makeup, cooling water in
    (0.0, 2.5, 5.75, 3.0, 10.25, 10.25),                      # front: pump and its coupling, condensate out
]

def main():
    res = sys.argv[1]
    name = "plantcondenser"
    d, counts = write_model(res, name, [("main", main_part()), ("drive_left", drive_left()), ("drive_front", drive_front())],
                            origins={"drive_left": (AX, AY, 8.0), "drive_front": (AX, AY, 8.0)}, paint=PAINT["condenser"],
                            hitbox=HITBOX)
    print(name, counts, sum(counts.values()))
    if len(sys.argv) > 2:
        preview(res, name, sys.argv[2])

if __name__ == "__main__":
    main()
