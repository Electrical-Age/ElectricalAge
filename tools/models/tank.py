#!/usr/bin/env python3
"""Pipe tanks (Iron Tank / Alloy Tank; Baughn 2026-10-29, the shaft redo): an upright riveted vessel on a skid with a
sight glass on its front. Ports (an iron pipe's flange): the top centre (vapour), and on each side face a low one
(liquid, 4 px up) and a high one (vapour, 12 px up); the bottom port is a plain opening under the skid.

Parts: `shell`, `ports` (the flanges), `glass` (the sight glass) and `level`: the water in it, 2..14 px, which
TankRender scales to the fill. A box of tanks (TankGroup) is drawn as one: shell, glass and level scaled to the box.

Design frame: mcmodel px (0..16, corner origin), front XN (x = 0).

Usage: tank.py RESOURCES_DIR  -> assets/eln/model/tank{iron,alloy}/
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Model, box, prism, tube
from oilkit import atlas, write_parts, out_dir, turned, bolt_ring

TIERS = {"iron": dict(shell="darksteel", band="castiron", paint=(70, 74, 80)),
         "alloy": dict(shell="steel", band="paint", paint=(150, 112, 52))}
C = 8.0
R = 5.5

def flange_x0(y):
    """A large alloy pipe's flange (its ports take one; smaller pipes meet it too) on the x = 0 face at height y, with a
    stub to the shell: the pipe renderer's flange is its drawn radius (3) + 0.75."""
    return [prism("x", y, C, 3.75, 0.0, 0.75, "steel", n=14, caps=(True, False)),
            prism("x", y, C, 3.0, 0.75, C - R + 0.5, "steel", n=12, caps=(False, False))] + \
        bolt_ring("x", y, C, 3.3, 0.75, 1.0, 6, rb=0.25)

def shell_part(t):
    m = Model()
    m.add(box(1.5, 0, 1.5, 14.5, 1.0, 14.5, "darksteel", faces="nsewu"))
    m.add(prism("y", C, C, R, 1.0, 14.5, t["shell"], n=14, caps=(False, False)))
    m.add(prism("y", C, C, R, 14.5, 15.0, t["shell"], n=14, r_end=R - 1.5, caps=(False, True)))
    m.add(prism("y", C, C, R - 0.5, 1.0, 1.5, t["shell"], n=14, caps=(True, False)))
    for y in (3.0, 8.0, 13.0):
        m.add(tube("y", C, C, R - 0.1, R + 0.4, y, y + 0.6, t["band"], n=14, inner=False))
    return m

def ports_part(t):
    """The flanges: top port, and a low (liquid) and high (vapour) one on each face but the front (the sight glass)."""
    m = Model()
    m.add(prism("y", C, C, 3.0, 14.5, 15.25, "steel", n=12, caps=(False, False)))
    m.add(prism("y", C, C, 3.75, 15.25, 16.0, "steel", n=14))
    m.add(*bolt_ring("y", C, C, 3.3, 15.0, 15.25, 6, rb=0.25))
    for k in (1, 2, 3):
        for y in (4.0, 12.0):
            m.add(*turned(flange_x0(y), k))
    return m

def glass_part(t):
    """The sight glass on the front: brass frame, dark backing, 1.5..14.5 px."""
    m = Model()
    m.add(box(C - R - 0.75, 1.5, C - 1.25, C - R + 0.5, 14.5, C - 0.75, "brass"))
    m.add(box(C - R - 0.75, 1.5, C + 0.75, C - R + 0.5, 14.5, C + 1.25, "brass"))
    m.add(box(C - R - 0.6, 1.5, C - 0.75, C - R - 0.5, 14.5, C + 0.75, "darksteel", faces="w"))   # (behind the water)
    return m

def level_part():
    m = Model()
    m.add(box(C - R - 0.8, 2.0, C - 0.7, C - R - 0.65, 14.0, C + 0.7, "water", faces="w"))
    return m

def main():
    res = sys.argv[1]
    for tier, t in TIERS.items():
        name = "tank" + tier
        d = out_dir(res, name)
        counts = write_parts(d, name, [("shell", shell_part(t)), ("ports", ports_part(t)), ("glass", glass_part(t)), ("level", level_part())])
        atlas(os.path.join(d, "tex.png"), t["paint"])
        print(name, counts)

if __name__ == "__main__":
    main()
