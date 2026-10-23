#!/usr/bin/env python3
"""Oil pump (50V / 200V): an electric well-head pump (progressing-cavity style drive head) on top of the well casing.

Drawn by OilPumpRender -> OilPumpDescriptor.draw() (part `main`), after front.glRotateXnRef(). Connections
(OilPumpElement): electrical power on all four horizontal faces at the floor (LRDU Down). The block below is the
well casing, so the wellhead stands at the bottom centre. Crude/water leaves through the fluid capability (pipes on
any side): the discharge line is decoration.

Design frame: mcmodel px (0..16, corner origin), front XN (x = 0):
  floor: skid with a floor terminal plate + junction box on every face; casing-head flange with bolts at the centre;
  stack: tubing head, flanged flow tee (pressure gauge to the front, discharge line with gate valve and red handwheel
         to z+), brass stuffing box, polished rod, drive-head frame and cast-iron gearbox;
  drive: vertical finned motor at the back (x+) on a pedestal, tier paint band, terminal box with conduit, belt
         guard in tier paint over gearbox and motor.

Usage: oilpump.py RESOURCES_DIR  -> assets/eln/model/oilpump{50v,200v}/
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Model, box, prism
from oilkit import atlas, write_parts, out_dir, turned, terminal_plate, dial, handwheel, bolt_ring, PAINT

TIERS = {
    # fr: casing flange radius; mx/mr: motor axis x and radius; fins: motor fin heights; gb: gearbox x span;
    # gtop: gearbox top; hw: handwheel radius
    "50v": dict(fr=3.0, mx=12.75, mr=2.25, fins=(6.0, 8.0, 10.0), gb=(5.75, 10.25), gtop=12.5, hw=1.1),
    "200v": dict(fr=3.4, mx=12.85, mr=2.6, fins=(5.75, 7.25, 8.75, 10.25), gb=(5.6, 10.15), gtop=12.75, hw=1.3),
}
C = 8.0

def pump(t):
    m = Model()
    # skid, four floor terminals with junction boxes
    m.add(box(1.0, 0, 1.0, 15.0, 0.75, 15.0, "darksteel", faces="nsewu"))
    jb = [box(0.85, 0.75, 5.5, 2.0, 3.0, 10.5, "steel", faces="nsewu"), box(0.85, 3.0, 5.35, 2.15, 3.3, 10.65, "darksteel", faces="nsewu")]
    for k in range(4):
        m.add(*turned(terminal_plate(), k), *turned(jb, k))
    # casing head flange (meets the well casing below), tubing head, flanged flow tee
    fr = t["fr"]
    m.add(prism("y", C, C, fr, 0, 1.5, "castiron", n=12, caps=(False, True)))
    m.add(*bolt_ring("y", C, C, fr - 0.55, 1.5, 1.85, 6, rb=0.3))
    m.add(prism("y", C, C, 2.0, 1.5, 3.5, "castiron", n=10, caps=(False, False)))
    m.add(prism("y", C, C, 2.6, 3.5, 4.1, "steel", n=12))
    m.add(*bolt_ring("y", C, C, 2.25, 4.1, 4.35, 6, rb=0.22))
    m.add(prism("y", C, C, 1.4, 4.1, 7.0, "steel", n=8, caps=(False, False)))
    m.add(prism("y", C, C, 1.8, 7.0, 7.4, "steel", n=10))
    # discharge line to z+: pipe, gate valve with bonnet + handwheel, flange at the face
    dy = 6.0
    m.add(prism("z", C, dy, 0.75, C + 1.2, 15.3, "darksteel", n=6, caps=(False, False)))
    m.add(box(C - 1.0, dy - 1.1, 10.75, C + 1.0, dy + 1.1, 12.5, "castiron"))
    m.add(prism("y", C, 11.6, 0.5, dy + 1.1, dy + 2.4, "castiron", n=6, caps=(False, True)))
    m.add(prism("y", C, 11.6, 0.2, dy + 2.4, dy + 3.0, "darksteel", n=6, caps=(False, False)))
    m.add(*handwheel(C, dy + 3.1, 11.6, r=t["hw"], axis="y"))
    m.add(prism("z", C, dy, 1.2, 15.3, 15.85, "steel", n=8))
    # pressure gauge on the tee, facing the front
    m.add(prism("x", 5.6, C, 0.2, C - 2.6, C - 1.3, "brass", n=6, caps=(False, False)))
    m.add(*dial(C - 3.0, 5.6, C, 0.8, "xn", depth=0.4))
    # stuffing box, polished rod, drive-head frame, gearbox
    m.add(prism("y", C, C, 1.0, 7.4, 8.6, "brass", n=8, caps=(False, True)))
    m.add(*bolt_ring("y", C, C, 0.8, 8.6, 8.8, 2, rb=0.2, phase=math.pi / 2))
    m.add(prism("y", C, C, 0.35, 8.6, 10.0, "steel", n=6, caps=(False, False)))
    for z in (C - 1.9, C + 1.4):
        m.add(box(C - 0.5, 7.4, z, C + 0.5, 10.0, z + 0.5, "darksteel"))
    g0, g1 = t["gb"]
    m.add(box(g0, 10.0, C - 2.25, g1, t["gtop"], C + 2.25, "castiron"))
    m.add(box(g0 - 0.15, 10.0, C - 2.4, g1 + 0.15, 10.4, C + 2.4, "darksteel"))
    m.add(prism("x", 11.25, C, 0.6, g0 - 0.3, g0, "darksteel", n=6, caps=(True, False)))     # inspection plug
    # motor: pedestal, bottom bell, finned frame, tier band, adapter into the belt guard
    mx, mr = t["mx"], t["mr"]
    m.add(box(mx - 1.75, 0.75, C - 2.0, mx + 1.75, 3.5, C + 2.0, "darksteel"))
    m.add(prism("y", mx, C, mr - 0.5, 3.5, 4.25, "darksteel", n=10, caps=(False, False)))
    m.add(prism("y", mx, C, mr, 4.25, 12.0, "steel", n=10, caps=(False, False)))
    for y in t["fins"]:
        m.add(prism("y", mx, C, mr + 0.35, y - 0.2, y + 0.2, "darksteel", n=10))
    m.add(prism("y", mx, C, mr + 0.3, 10.9, 11.6, "paint", n=10))
    m.add(prism("y", mx, C, 1.5, 12.0, t["gtop"] + 0.1, "darksteel", n=8, caps=(False, False)))
    # terminal box on the motor's z- side, conduit to the skid
    m.add(box(mx - 1.0, 6.5, C - mr - 0.9, mx + 1.0, 9.0, C - mr + 0.3, "steel"))
    m.add(box(mx - 1.15, 9.0, C - mr - 1.05, mx + 1.15, 9.3, C - mr + 0.3, "darksteel"))
    m.add(box(mx - 0.35, 0.75, C - mr - 0.75, mx + 0.35, 6.5, C - mr - 0.15, "darksteel"))
    # belt guard (tier paint): rounded ends over the gearbox and the motor, lid
    gy0, gy1 = t["gtop"] + 0.1, t["gtop"] + 1.9
    m.add(prism("y", C, C, 2.0, gy0, gy1, "paint", n=10))
    m.add(prism("y", mx, C, 1.7, gy0, gy1, "paint", n=10))
    m.add(box(C, gy0, C - 1.7, mx, gy1, C + 1.7, "paint", faces="nsud"))
    m.add(box(C - 0.5, gy1, C - 0.6, mx + 0.5, gy1 + 0.3, C + 0.6, "darksteel"))              # lid seam / lifting bar
    return m

def main():
    res = sys.argv[1]
    for tier, t in TIERS.items():
        name = "oilpump" + tier
        d = out_dir(res, name)
        counts = write_parts(d, name, [("main", pump(t))])
        atlas(os.path.join(d, "tex.png"), PAINT[tier])
        print(name, counts)

if __name__ == "__main__":
    main()
