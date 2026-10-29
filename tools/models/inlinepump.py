#!/usr/bin/env python3
"""The 200V Inline Pump (Baughn 2026-10-29, the shaft redo): a centrifugal pump in a pipe line. Water in on its left
flank (z = 0: front.left()), out of its right (z = 16), both iron pipe flanges at the face centre; the motor behind the volute; power
on the front floor terminal, the speed signal on the back.

Parts: `main`, `rotor` (the coupling, turned by InlinePumpRender about x through its origin).

Design frame: mcmodel px (0..16, corner origin), front XN (x = 0).

Usage: inlinepump.py RESOURCES_DIR  -> assets/eln/model/inlinepump200v/
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Model, box, prism, tube
from oilkit import atlas, write_parts, out_dir, turned, bolt_ring, terminal_plate, signal_plate, PAINT

C = 8.0
LY = 4.0   # the line's height (InlinePumpElement.LINE_Y)

def main_part():
    m = Model()
    m.add(box(2.0, 0, 3.0, 14.0, 1.0, 13.0, "darksteel", faces="nsewu"))
    # the volute: a drum across the line, its axis along x
    m.add(box(5.5, 1.0, 6.0, 10.0, 4.5, 10.0, "castiron"))   # (its foot)
    m.add(prism("x", C, C, 4.0, 5.0, 10.0, "paint", n=14))
    m.add(tube("x", C, C, 3.9, 4.3, 9.5, 10.0, "steel", n=14, inner=False))
    # the line, low (LINE_Y = 4 px: InlinePumpElement), into the volute's foot: pipe stubs to flanges on z = 0 and 16
    # (an alloy pipe's size: drawn 1.5 voxels, its flange 0.75 wider each way)
    m.add(prism("z", C, LY, 0.75, 0.9, 15.1, "steel", n=8, caps=(False, False)))
    for z0, z1, caps in ((0.0, 1.0, (True, False)), (15.0, 16.0, (False, True))):
        m.add(prism("z", C, LY, 1.5, z0, z1, "steel", n=10, caps=caps))
    # the motor behind, on a saddle
    m.add(box(11.5, 1.0, 5.5, 15.0, 5.5, 10.5, "castiron"))
    m.add(prism("x", C, C, 2.75, 11.0, 15.5, "steel", n=12))
    for x in (12.0, 13.25, 14.5):
        m.add(tube("x", C, C, 2.65, 3.1, x, x + 0.4, "darksteel", n=12, inner=False))
    m.add(*terminal_plate())
    m.add(*turned(signal_plate(), 2))
    return m

def rotor_part():
    m = Model()
    m.add(prism("x", C, C, 1.0, 10.0, 11.0, "brass", n=8))
    m.add(box(10.2, C - 1.75, C - 0.3, 10.8, C + 1.75, C + 0.3, "darksteel"))
    return m

def main():
    res = sys.argv[1]
    name = "inlinepump200v"
    d = out_dir(res, name)
    counts = write_parts(d, name, [("main", main_part()), ("rotor", rotor_part())], origins={"rotor": (10.5, C, C)})
    atlas(os.path.join(d, "tex.png"), PAINT["200v"])
    print(name, counts)

if __name__ == "__main__":
    main()
