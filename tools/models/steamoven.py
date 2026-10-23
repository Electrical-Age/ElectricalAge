#!/usr/bin/env python3
"""Steam oven model (Baughn 2026-10-23): a steel cabinet on feet with a glass door in front, the steam pipe coming in on
top (an iron pipe's 4 px bore to a laid pipe's flange on the top face, where a pipe couples), a vent stack at the back
that the renderer puffs steam from (the .txt's ventX/Y/Z), and 50 V terminal plates low on both sides (the fan).

EA transparent node drawn by SteamOvenRender through Obj3D. px frame (0..16, corner origin); front = x 0 (XN).
Usage: steamoven.py RESOURCES_DIR  -> assets/eln/model/steamoven/steamoven.{obj,mtl,txt} + tex.png
"""
import os, random, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Model, box, prism, tube

CELLS = {"steel": (0, 0), "darksteel": (1, 0), "castiron": (2, 0), "copper": (3, 0),
         "glass": (0, 1), "brass": (1, 1), "soot": (2, 1), "paint": (3, 1)}
GRID = (4, 2)
PIPE_R = 2.0

def oven():
    m = Model()
    for x, z in ((1.5, 1.5), (13, 1.5), (1.5, 13), (13, 13)):          # feet
        m.add(box(x, 0, z, x + 1.5, 1.5, z + 1.5, "darksteel"))
    m.add(box(1, 1.5, 1, 15, 13, 15, "steel"))                         # the cabinet
    m.add(box(0.8, 1.5, 0.8, 15.2, 2.0, 15.2, "darksteel", faces="nsewd"), box(0.8, 12.5, 0.8, 15.2, 13.0, 15.2, "darksteel", faces="nsew"))   # trim (bands round the sides)
    m.add(box(0.5, 3, 2.5, 1.0, 12, 13.5, "darksteel"))                # the door frame
    m.add(box(0.4, 4, 3.5, 0.5, 11, 12.5, "glass", faces="w"))          # its window
    m.add(box(0.0, 6.5, 12.6, 0.5, 8.5, 13.2, "brass"))                # the handle
    for y in (4.5, 10.0):
        m.add(box(0.2, y, 2.3, 0.8, y + 1, 2.9, "darksteel"))          # hinges
    # the steam pipe in on top: an iron pipe's bore to a laid pipe's flange
    m.add(prism("y", 8, 8, PIPE_R, 13, 15, "castiron", n=12, caps=(False, False)))
    m.add(prism("y", 8, 8, PIPE_R + 0.75, 15, 16, "steel", n=12))
    m.add(prism("y", 8, 8, PIPE_R + 0.35, 13, 13.6, "paint", n=12, caps=(False, True)))   # (50 V green collar)
    # the vent stack at the back, open at the top
    m.add(prism("y", 12.5, 12.5, 0.9, 13, 14.8, "castiron", n=8, caps=(False, False)))
    m.add(tube("y", 12.5, 12.5, 0.8, 1.2, 14.8, 15.6, "darksteel", n=8))
    m.add(prism("y", 12.5, 12.5, 0.85, 14.2, 14.3, "soot", n=8, caps=(False, True)))
    # the fan's 50 V terminal plates, low on both sides
    for z0, z1 in ((0.1, 0.85), (15.15, 15.9)):
        m.add(box(4.5, 0, z0, 11.5, 4.0, z1, "steel"), box(4.5, 4.0, z0, 11.5, 4.5, z1, "paint"))
        for x in (5.75, 8.0, 10.25):
            m.add(box(x - 0.5, 1.75, z0 - 0.1, x + 0.5, 2.75, z1 + 0.1, "copper"))
    vent = (12.5 / 16 - 0.5, 15.6 / 16 - 0.5, 12.5 / 16 - 0.5)
    return m, vent

def atlas(path):
    from PIL import Image
    rnd = random.Random(42)
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
    cell("steel", (150, 155, 160), 6)
    cell("darksteel", (60, 62, 66), 5)
    cell("castiron", (88, 84, 80), 9)
    cell("copper", (196, 110, 70), 12)
    cell("glass", (70, 92, 104), 4, shade=lambda x, y: 70 if x + y in (6, 7, 14) else 0)   # (dark glass, two glints)
    cell("brass", (200, 170, 80), 10)
    cell("soot", (22, 20, 20), 4)
    cell("paint", (56, 156, 84), 8)
    im.save(path)

def main():
    res = sys.argv[1]
    out = os.path.join(res, "assets/eln/model/steamoven")
    os.makedirs(out, exist_ok=True)
    m, vent = oven()
    tris = m.write_eln(out, "steamoven", CELLS, GRID)
    with open(os.path.join(out, "steamoven.txt"), "w") as f:
        f.write(f"o main\nf ventX {vent[0]:.5f}\nf ventY {vent[1]:.5f}\nf ventZ {vent[2]:.5f}\n")
    atlas(os.path.join(out, "tex.png"))
    print("steamoven triangles", tris)

if __name__ == "__main__":
    main()
