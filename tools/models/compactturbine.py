#!/usr/bin/env python3
"""50V Compact Steam Turbine: the early-game bootstrap generator (300 W, 50 V). Everything on one skid, smaller and
simpler than the 50V Steam Turbine: a little drum boiler fired through a heat plate, a scroll turbine coupled to a
green-banded generator, and a tiny finned air-cooled condenser with a vent stack.

EA transparent node, drawn through Obj3D (README.md, "Path 2: EA 1.12 node OBJ"); parts `main` and `rotor` (the shaft
and coupling, origin on the shaft axis in compactturbine50v.txt, so the render can spin it). Design frame: mcmodel px
(0..16, corner origin), shaft along x:
  x = 0  (XN) FRONT: generator end, terminal plate at the floor (electric out), the drum's sight glass
  x = 16 (XP) BACK:  turbine end, second terminal plate
  z = 0  LEFT (seen from the front): heat plate flush with the face (a heat furnace touches it), the drum behind it
  z = 16 RIGHT: tiny finned air-cooled condenser, vent stack with rain cap on top (ventX/Y/Z in the .txt, part main)
  top: water inlet, a laid pipe's flange (r 2.75 px, 4 bolts) centred on the top face, piped down into the drum

Usage: compactturbine.py RESOURCES_DIR  -> assets/eln/model/compactturbine50v/
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Model, box, prism, tube, volute
from oilkit import atlas, write_parts, out_dir, turned, terminal_plate, bolt_ring, fmt, PAINT

NAME = "compactturbine50v"
CY, CZ = 5.5, 8.5                 # shaft height and z (off centre: the drum takes the left, the condenser the right)
RG, GX = 2.4, (2.5, 6.5)          # generator radius, stator x span
R0, R1, TX = 2.0, 3.25, (9.25, 12.25)   # scroll radii, x span
START = math.radians(40)          # scroll tongue (0 = up, 90 = +z), as steamturbine.py
DY, DZ, DR, DX = 7.0, 3.0, 2.25, (2.75, 13.25)   # drum axis height, z, radius, shell x span
PIPE_R = 2.0                      # a laid pipe's bore (the top stub)

def scroll_r(ang):
    frac = ((ang - START) % (2 * math.pi)) / (2 * math.pi)
    return R0 + (R1 - R0) * frac

def main_part():
    m = Model()
    # --- skid: cross members (the firebox and the condenser base are the rails)
    for x in (3.0, 10.0):
        m.add(box(x, 0.2, 5.0, x + 1.25, 0.9, 12.0, "darksteel", faces="newu"))

    # --- LEFT flank (z = 0): the heat plate, where a heat furnace touches: lapped copper recessed behind vertical
    # copper ribs that reach the face, a heat-red rim, a cast-iron firebox backing; the drum sits on it
    m.add(box(2.5, 0.0, 0.6, 13.5, 6.5, 1.0, "castiron", faces="n"))
    for k in range(7):
        x = 2.95 + 1.6 * k
        m.add(box(x, 0.0, 0.0, x + 0.8, 6.25, 0.6, "hotcopper", faces="newu"))
    m.add(box(2.0, 6.5, 0.1, 14.0, 7.0, 1.0, "red", faces="nsewu"))                      # rim: top
    m.add(box(2.0, 0.0, 0.1, 2.5, 6.5, 1.0, "red", faces="nwe"), box(13.5, 0.0, 0.1, 14.0, 6.5, 1.0, "red", faces="nwe"))
    m.add(box(2.0, 0.0, 1.0, 14.0, 6.0, 5.0, "castiron", faces="ewu"))                     # firebox
    m.add(box(1.75, 6.0, 1.0, 14.25, 6.4, 5.25, "darksteel", faces="sewu"))              # its top plate
    # drum: shell, dished heads, a seam band
    m.add(prism("x", DY, DZ, DR, DX[0], DX[1], "rivet", n=10, caps=(False, False)))
    m.add(prism("x", DY, DZ, DR - 1.0, DX[0] - 0.75, DX[0], "rivet", n=10, r_end=DR, caps=(True, False)))
    m.add(prism("x", DY, DZ, DR, DX[1], DX[1] + 0.75, "rivet", n=10, r_end=DR - 1.0, caps=(False, True)))
    m.add(prism("x", DY, DZ, DR + 0.15, 7.8, 8.2, "darksteel", n=10, caps=(False, False)))
    # sight glass on the front head, facing the front: brass cocks, the glass (water below, steam above), guards
    hx = DX[0] - 0.75
    gz = DZ
    for y in (5.0, 8.75):
        m.add(box(0.6, y, gz - 0.45, hx + 0.3, y + 0.5, gz + 0.45, "brass"))
    m.add(prism("y", 0.95, gz, 0.35, 5.5, 8.75, "water", n=6, caps=(False, False)))
    m.add(box(0.5, 5.5, gz - 0.6, 0.65, 8.75, gz - 0.45, "darksteel", faces="nsew"), box(0.5, 5.5, gz + 0.45, 0.65, 8.75, gz + 0.6, "darksteel", faces="nsew"))

    # --- MIDDLE: generator (green tier band, fins), scroll turbine
    gx0, gx1 = GX
    m.add(prism("x", CY, CZ, RG, gx0, gx1, "steel", n=10, caps=(False, False)))
    m.add(prism("x", CY, CZ, RG - 1.0, gx0 - 0.6, gx0, "steel", n=10, r_end=RG, caps=(True, False)))
    m.add(prism("x", CY, CZ, RG, gx1, gx1 + 0.5, "steel", n=10, r_end=RG - 0.9, caps=(False, True)))
    for x in (gx1 - 0.9,):
        m.add(tube("x", CY, CZ, RG - 0.1, RG + 0.5, x, x + 0.5, "darksteel", n=10, inner=False))
    bx = (gx0 + gx1) / 2
    m.add(tube("x", CY, CZ, RG - 0.1, RG + 0.35, bx - 0.6, bx + 0.6, "paint", n=10, inner=False))
    m.add(prism("x", CY, CZ, 0.6, gx0 - 1.0, gx0 - 0.6, "darksteel", n=8))                 # front bearing cap
    foot = CY - RG + 0.5
    for x in (gx0 + 0.25, gx1 - 1.25):
        m.add(box(x, 1.0, CZ - 2.0, x + 1.0, foot, CZ + 2.0, "steel"))
    # conduit from the generator down to the front terminal (on the skid's floor)
    m.add(box(1.0, 1.0, 7.25, gx0 + 0.25, 1.75, 8.75, "darksteel"))
    # scroll casing, covers, rear bearing, pedestal
    tx0, tx1 = TX
    tcx = (tx0 + tx1) / 2
    m.add(volute("x", CY, CZ, R0, R1, tx0, tx1, "castiron", n=12, start=START))
    m.add(prism("x", CY, CZ, R0 - 0.9, tx0 - 0.5, tx0, "castiron", n=10, r_end=R0 - 0.3, caps=(True, False)))
    m.add(prism("x", CY, CZ, R0 - 0.2, tx1, tx1 + 0.6, "castiron", n=10, r_end=R0 - 1.0, caps=(False, True)))
    m.add(prism("x", CY, CZ, 0.8, tx1 + 0.6, 14.0, "darksteel", n=8))                       # rear bearing
    m.add(box(tx0 + 0.5, 1.0, CZ - 1.75, tx1 - 0.5, CY - scroll_r(math.pi) + 0.4, CZ + 1.75, "darksteel"))
    # steam from the drum to the scroll: a small pipe from the drum's top to the scroll's top, with a tier collar
    st = CY + scroll_r(0.0)
    m.add(prism("y", tcx, CZ, 0.6, st - 0.5, 10.0, "castiron", n=8, caps=(False, False)))
    m.add(prism("y", tcx, CZ, 0.85, st - 0.3, st + 0.4, "paint", n=8))
    m.add(prism("z", tcx, 10.0, 0.6, DZ, CZ + 0.6, "castiron", n=8, caps=(False, True)))
    m.add(prism("y", tcx, DZ, 0.6, DY + DR - 0.3, 10.6, "castiron", n=8, caps=(False, True)))
    # exhaust: out of the scroll's z+ side into the condenser
    m.add(prism("z", tcx, CY - 0.5, 0.9, CZ + 1.0, 12.5, "castiron", n=8, caps=(False, False)))

    # --- RIGHT flank (z = 16): the tiny air-cooled condenser: header box, many thin fins, cap plate, vent stack
    cx0, cx1 = 2.5, 13.5
    m.add(box(cx0, 1.0, 12.25, cx1, 7.5, 13.25, "steel", faces="ns"))                                   # header
    nf = 11
    for k in range(nf):
        x = cx0 + 0.3 + (cx1 - cx0 - 0.6 - 0.25) * k / (nf - 1)
        m.add(box(x, 1.25, 13.25, x + 0.25, 7.25, 15.75, "copper", faces="sewu"))
    m.add(box(cx0 - 0.25, 7.5, 12.0, cx1 + 0.25, 8.0, 15.9, "darksteel", faces="nsewu"))   # cap plate
    m.add(box(cx0 - 0.25, 1.0, 12.0, cx0, 7.5, 15.9, "darksteel", faces="nsew"), box(cx1, 1.0, 12.0, cx1 + 0.25, 7.5, 15.9, "darksteel", faces="nsew"))
    m.add(box(cx0 - 0.25, 0.0, 12.0, cx1 + 0.25, 1.0, 15.9, "darksteel", faces="nsewu"))   # base
    # condensate return to the drum along the skid (back end)
    m.add(prism("z", 14.25, 1.6, 0.4, DZ + 2.0, 12.0, "darksteel", n=6, caps=(False, False)))
    m.add(prism("y", 14.25, DZ + 2.0, 0.4, 1.2, DY - 1.0, "darksteel", n=6, caps=(False, False)))
    # vent stack with a rain cap on three posts
    vx, vz = 10.75, 14.0
    vtop = 11.0
    m.add(prism("y", vx, vz, 1.1, 8.0, 8.5, "darksteel", n=8))                              # base flange
    m.add(prism("y", vx, vz, 0.75, 8.5, vtop, "castiron", n=8, caps=(False, False)))
    m.add(prism("y", vx, vz, 0.7, vtop - 1.0, vtop - 0.8, "rubber", n=8, caps=(False, True)))   # dark inside the bore
    for k in range(3):
        a = 2 * math.pi * k / 3 + math.pi / 6
        m.add(box(vx + 0.8 * math.cos(a) - 0.1, vtop, vz + 0.8 * math.sin(a) - 0.1,
                  vx + 0.8 * math.cos(a) + 0.1, vtop + 0.8, vz + 0.8 * math.sin(a) + 0.1, "darksteel", faces="nsew"))
    m.add(prism("y", vx, vz, 1.5, vtop + 0.8, vtop + 1.0, "darksteel", n=8, caps=(True, False)))
    m.add(prism("y", vx, vz, 1.5, vtop + 1.0, vtop + 1.7, "darksteel", n=8, r_end=0.2))      # rain cap

    # --- TOP: water inlet, a laid pipe's flange centred on the top face, down to the drum
    m.add(prism("y", 8.0, 8.0, PIPE_R + 0.75, 15.0, 16.0, "steel", n=12))
    m.add(*bolt_ring("y", 8.0, 8.0, PIPE_R + 0.4, 14.75, 15.0, 4, rb=0.25))
    m.add(prism("y", 8.0, 8.0, PIPE_R, 14.0, 15.0, "steel", n=12, caps=(False, False)))
    m.add(prism("y", 8.0, 8.0, 0.7, 13.0, 14.0, "steel", n=12, r_end=PIPE_R, caps=(True, False)))   # reducer
    m.add(prism("y", 8.0, 8.0, 0.5, 11.75, 13.0, "steel", n=8, caps=(False, False)))
    m.add(prism("z", 8.0, 11.75, 0.5, DZ - 0.5, 8.5, "steel", n=8, caps=(True, True)))
    m.add(prism("y", 8.0, DZ, 0.5, DY + DR - 0.3, 11.75, "steel", n=8, caps=(False, False)))
    m.add(box(7.4, 12.15, 5.0, 8.6, 13.35, 6.4, "castiron"))                                  # check valve
    m.add(prism("y", 8.0, 5.7, 0.2, 13.35, 13.8, "darksteel", n=6, caps=(False, False)))
    m.add(prism("y", 8.0, 5.7, 0.6, 13.8, 14.0, "red", n=6))                                   # its handle

    # --- electric out: terminal plates flush with the front and back faces
    m.add(*terminal_plate("paint"))
    m.add(*turned(terminal_plate("paint"), 2))
    vent = (vx, vtop, vz)
    return m, vent

def rotor_part():
    r = Model()
    x0, x1 = GX[1] + 0.5, TX[0] - 0.5
    r.add(prism("x", CY, CZ, 0.45, x0, x1, "darksteel", n=8, caps=(False, False)))
    cx = (x0 + x1) / 2
    r.add(prism("x", CY, CZ, 1.2, cx - 0.35, cx + 0.35, "copper", n=8))
    for k in range(4):
        a = math.pi / 2 * k + math.pi / 4
        r.add(prism("x", CY + 0.85 * math.cos(a), CZ + 0.85 * math.sin(a), 0.22, cx - 0.5, cx + 0.5, "brass", n=4))
    # a spoked flywheel on the generator's front end (the coupling alone, 1 px across, didn't show it turning:
    # AdventurAgent): a rim, a hub and four spokes, painted so the spin reads
    fx0, fx1 = 0.5, 1.2
    r.add(prism("x", CY, CZ, 0.3, fx1, GX[0] - 1.0, "darksteel", n=8, caps=(False, False)))
    r.add(tube("x", CY, CZ, 1.7, 2.3, fx0, fx1, "castiron", n=12))
    r.add(prism("x", CY, CZ, 0.55, fx0, fx1, "brass", n=8))
    r.add(box(fx0 + 0.1, CY - 1.75, CZ - 0.25, fx1 - 0.1, CY + 1.75, CZ + 0.25, "paint"))
    r.add(box(fx0 + 0.1, CY - 0.25, CZ - 1.75, fx1 - 0.1, CY + 0.25, CZ + 1.75, "castiron"))
    return r

def main():
    res = sys.argv[1]
    d = out_dir(res, NAME)
    m, vent = main_part()
    counts = write_parts(d, NAME, [("main", m), ("rotor", rotor_part())])
    with open(os.path.join(d, NAME + ".txt"), "w") as f:   # rotor turns about the shaft; the vent outlet (it faces up)
        f.write(f"o rotor\nf originY {fmt(CY / 16 - 0.5)}\nf originZ {fmt(CZ / 16 - 0.5)}\n")
        f.write(f"o main\nf ventX {vent[0] / 16 - 0.5:.5f}\nf ventY {vent[1] / 16 - 0.5:.5f}\nf ventZ {vent[2] / 16 - 0.5:.5f}\n")
    atlas(os.path.join(d, "tex.png"), PAINT["50v"])
    print(NAME, counts, "triangles", sum(counts.values()))

if __name__ == "__main__":
    main()
