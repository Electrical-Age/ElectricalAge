#!/usr/bin/env python3
"""Smelter model (Baughn 2026-10-23): a firebrick furnace in a cast-iron frame on short legs, a hopper's square funnel on
top (a hopper above feeds it) and a chute underneath (a hopper below empties it), a firebox window in front that the
renderer lights by temperature (part `glow`), a copper thermal plate on the back down to the floor (the thermal cable),
and 50 V terminal plates low on both sides (its controls).

EA transparent node drawn by SmelterRender through Obj3D. px frame (0..16, corner origin); front = x 0 (XN).
Usage: smelter.py RESOURCES_DIR  -> assets/eln/model/smelter/smelter.{obj,mtl} + tex.png
"""
import math, os, random, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Model, Mesh, box, prism

CELLS = {"brick": (0, 0), "castiron": (1, 0), "darksteel": (2, 0), "copper": (3, 0),
         "steel": (0, 1), "glow": (1, 1), "soot": (2, 1), "paint": (3, 1)}
GRID = (4, 2)

def flipped(m):
    """The same faces wound the other way (seen from inside: a funnel's inner wall)."""
    out = Mesh(m.mat)
    for p in m.quads:
        out.poly(*reversed(p))
    return out

def smelter():
    m, glow = Model(), Model()
    # legs and the chute below
    for x, z in ((1.5, 1.5), (13, 1.5), (1.5, 13), (13, 13)):
        m.add(box(x, 0, z, x + 1.5, 3, z + 1.5, "darksteel"))
    m.add(prism("y", 8, 8, 1.6, 0.4, 3.0, "castiron", n=4, r_end=2.8, caps=(True, False), phase=math.pi / 4))
    m.add(prism("y", 8, 8, 1.2, 0.35, 0.41, "soot", n=4, caps=(True, False), phase=math.pi / 4))   # (its mouth)
    # the body: firebrick between cast-iron corner posts and bands
    m.add(box(1.5, 3, 1.5, 14.5, 12.5, 14.5, "brick"))
    for x, z in ((1, 1), (14, 1), (1, 14), (14, 14)):
        m.add(box(x, 3, z, x + 1, 13, z + 1, "castiron"))
    for y in (3, 12):
        m.add(box(1, y, 1, 15, y + 1, 15, "castiron", faces="nsewu" if y == 12 else "nsewd"))
    # the firebox window in front, its frame and door hinges
    m.add(box(0.6, 5, 4.5, 1.5, 10, 11.5, "steel"))
    glow.add(box(0.5, 5.75, 5.25, 0.6, 9.25, 10.75, "glow", faces="w"))
    for y in (5.5, 8.5):
        m.add(box(0.3, y, 11.5, 1.0, y + 1, 12.3, "darksteel"))
    # the funnel on top: a square hopper mouth, walls inside and out
    out = prism("y", 8, 8, 4.0, 13, 16, "castiron", n=4, r_end=8.2, caps=(False, False), phase=math.pi / 4)
    inner = prism("y", 8, 8, 3.4, 13.4, 16, "darksteel", n=4, r_end=7.6, caps=(False, False), phase=math.pi / 4)
    m.add(out, flipped(inner))
    m.add(prism("y", 8, 8, 3.4, 13.35, 13.4, "soot", n=4, caps=(False, True), phase=math.pi / 4))   # (the throat)
    m.add(prism("y", 8, 8, 8.2, 15.6, 16, "steel", n=4, r_end=8.2, caps=(False, False), phase=math.pi / 4))   # (rim)
    # the back: a copper thermal plate down to the floor, where the thermal cable joins
    m.add(box(15.1, 0, 4.5, 15.9, 7, 11.5, "copper"))
    m.add(box(14.5, 1, 5.5, 15.1, 6, 10.5, "darksteel"))
    # the sides: 50 V terminal plates, low (the controls), with a tier stripe
    for z0, z1 in ((0.1, 0.85), (15.15, 15.9)):
        m.add(box(4.5, 0, z0, 11.5, 4.0, z1, "steel"), box(4.5, 4.0, z0, 11.5, 4.5, z1, "paint"))
        zc = (z0 + z1) / 2
        for x in (5.75, 8.0, 10.25):
            m.add(box(x - 0.5, 1.75, min(zc, z0 - 0.1 if z0 < 8 else z1), x + 0.5, 2.75, max(zc, z1 + 0.1 if z0 > 8 else z0), "copper"))
    return m, glow

def atlas(path):
    from PIL import Image
    rnd = random.Random(77)
    im = Image.new("RGBA", (16 * GRID[0], 16 * GRID[1]))
    def cell(name, base, noise=8, shade=None):
        col, row = CELLS[name]
        for y in range(16):
            for x in range(16):
                c = [max(0, min(255, b + rnd.randint(-noise, noise))) for b in base]
                if shade:
                    k = shade(x, y)
                    c = [max(0, min(255, v + k)) for v in c]
                im.putpixel((16 * col + x, 16 * row + y), tuple(c) + (255,))
    cell("brick", (150, 72, 52), 10, shade=lambda x, y: -55 if y % 4 == 3 or (x + (4 if (y // 4) % 2 else 0)) % 8 == 7 else 0)
    cell("castiron", (78, 74, 72), 9)
    cell("darksteel", (52, 53, 57), 5)
    cell("copper", (196, 110, 70), 12)
    cell("steel", (132, 137, 142), 7)
    cell("glow", (255, 255, 255), 0)           # (tinted by the renderer: dull red to yellow-white)
    cell("soot", (22, 20, 20), 4)
    cell("paint", (56, 156, 84), 8)             # (50 V green, the cable colour)
    im.save(path)

def main():
    res = sys.argv[1]
    out = os.path.join(res, "assets/eln/model/smelter")
    os.makedirs(out, exist_ok=True)
    m, glow = smelter()
    tris = m.write_eln(out, "smelter", CELLS, GRID, extra=[("glow", glow)])
    atlas(os.path.join(out, "tex.png"))
    print("smelter triangles", tris)

if __name__ == "__main__":
    main()
