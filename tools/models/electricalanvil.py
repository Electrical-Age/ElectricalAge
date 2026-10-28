#!/usr/bin/env python3
"""Electrical anvil (Baughn 2026-10-28): a workshop desk. A steel-topped bench on four legs with a shelf, a pegboard
at the back with tools on it, a vise at the front left, an arc welder (the 800 V set) on the right with its cable and
torch, and an angled lamp over the work. The repaired tool lies on the bench in front of the vise (the renderer draws
it at the .txt's toolX/Y/Z) and the sparks fly from the torch tip (sparkX/Y/Z). The 800 V terminal plate sits low on
the back face.

EA transparent node drawn by ElectricalAnvilRender through Obj3D. px frame (0..16, corner origin); front = x 0 (XN).
Usage: electricalanvil.py RESOURCES_DIR -> assets/eln/model/electricalanvil/electricalanvil.{obj,mtl,txt} + tex.png,
       and the item icon assets/eln/textures/item/electrical_anvil.png
"""
import os, random, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Model, box, prism, tube

CELLS = {"steel": (0, 0), "darksteel": (1, 0), "wood": (2, 0), "plate": (3, 0),
         "welder": (0, 1), "dials": (1, 1), "rubber": (2, 1), "copper": (3, 1),
         "peg": (0, 2), "glow": (1, 2), "brass": (2, 2), "hvtier": (3, 2)}
GRID = (4, 3)
TOP = 11.0          # the bench top's surface (px)


def desk():
    m = Model()
    # legs and the lower shelf
    for x, z in ((1, 1), (13.5, 1), (1, 13.5), (13.5, 13.5)):
        m.add(box(x, 0, z, x + 1.5, TOP - 1.5, z + 1.5, "darksteel"))
    m.add(box(1.5, 2.5, 1.5, 14.5, 3.2, 14.5, "wood"))                         # the shelf
    m.add(box(2.5, 3.2, 9.5, 7.0, 5.5, 13.5, "plate", faces="nsewu"))          # a box of offcuts on it
    # the bench top: a wooden slab with a steel work plate let into the front half
    m.add(box(0.5, TOP - 1.5, 0.5, 15.5, TOP - 0.3, 15.5, "wood"))
    m.add(box(0.5, TOP - 0.3, 0.5, 8.5, TOP, 15.5, "plate", faces="nsewu"))
    m.add(box(8.5, TOP - 0.3, 0.5, 15.5, TOP, 15.5, "wood", faces="nsewu"))
    m.add(box(0.3, TOP - 1.7, 0.3, 15.7, TOP - 1.5, 15.7, "darksteel", faces="nsewd"))   # the apron's edge
    # the pegboard at the back, with a hammer, a spanner and a file hanging on it
    m.add(box(14.6, TOP, 0.8, 15.6, 16, 15.2, "peg"))
    m.add(box(14.1, 13.6, 2.0, 14.6, 14.1, 6.0, "wood"), box(14.1, 13.2, 5.4, 14.6, 14.6, 6.6, "darksteel"))   # hammer
    m.add(box(14.2, 12.4, 7.5, 14.6, 15.2, 8.2, "steel"), box(14.2, 15.0, 7.2, 14.6, 15.6, 8.5, "steel"))      # spanner
    m.add(box(14.2, 12.2, 9.6, 14.6, 15.4, 10.1, "darksteel"), box(14.2, 11.6, 9.5, 14.6, 12.2, 10.2, "wood"))  # file
    # the vise, front left (jaws open along z)
    m.add(box(1.0, TOP, 1.0, 5.0, TOP + 0.8, 5.0, "darksteel"))               # its turntable base
    m.add(box(1.5, TOP + 0.8, 1.5, 4.5, TOP + 3.0, 2.6, "welder"))            # fixed jaw (painted)
    m.add(box(1.5, TOP + 0.8, 3.6, 4.5, TOP + 3.0, 4.6, "welder"))            # moving jaw
    m.add(box(1.4, TOP + 2.2, 2.6, 4.6, TOP + 3.0, 2.8, "steel"), box(1.4, TOP + 2.2, 3.4, 4.6, TOP + 3.0, 3.6, "steel"))   # jaw plates
    m.add(prism("z", 3.0, TOP + 1.6, 0.35, 0.2, 7.2, "steel", n=8))           # the screw
    m.add(box(2.9, TOP + 0.9, 6.6, 3.1, TOP + 2.3, 7.4, "steel"))             # its handle (a T)
    m.add(prism("x", TOP + 1.6, 7.0, 0.3, 0.8, 5.2, "steel", n=6))
    # the arc welder, back right: a painted case with dials, a cable to a torch lying on the plate
    m.add(box(9.0, TOP, 10.5, 14.4, TOP + 4.5, 15.2, "welder"))
    m.add(box(8.9, TOP + 1.0, 11.0, 9.0, TOP + 4.0, 14.7, "dials", faces="w"))
    m.add(box(9.5, TOP + 4.5, 12.0, 13.5, TOP + 4.9, 12.6, "darksteel"))      # carry handle
    m.add(box(9.5, TOP + 4.5, 11.6, 9.9, TOP + 4.9, 13.0, "darksteel"), box(13.1, TOP + 4.5, 11.6, 13.5, TOP + 4.9, 13.0, "darksteel"))
    m.add(prism("x", TOP + 0.9, 12.0, 0.4, 7.5, 9.0, "rubber", n=6))          # the torch cable, out of the case...
    m.add(prism("z", 7.7, TOP + 0.5, 0.4, 7.5, 12.0, "rubber", n=6))          # ...along the bench
    m.add(prism("z", 7.7, TOP + 0.5, 0.55, 5.6, 7.5, "darksteel", n=8))       # the torch's grip
    m.add(prism("z", 7.7, TOP + 0.5, 0.35, 4.8, 5.6, "brass", n=8, r_end=0.2))   # its nozzle
    m.add(prism("x", TOP + 0.6, 14.0, 0.3, 7.6, 9.0, "copper", n=6))          # earth clamp lead
    # the lamp: a post on the back right corner, an arm over the bench, a shade with a bulb under it
    m.add(prism("y", 14.2, 1.8, 0.45, TOP, 15.4, "darksteel", n=8))
    m.add(box(5.6, 15.0, 1.5, 14.6, 15.5, 2.1, "steel"))                      # the arm
    m.add(prism("y", 6.0, 1.8, 1.6, 13.4, 15.0, "darksteel", n=10, r_end=0.7))   # the shade
    m.add(prism("y", 6.0, 1.8, 0.9, 13.1, 13.4, "glow", n=10))               # the bulb's face
    # the 800 V terminal plate, low on the back face (the 800 V tier colour)
    m.add(box(15.6, 0.5, 4.5, 16.0, 5.5, 11.5, "steel"), box(15.6, 5.5, 4.5, 16.0, 6.0, 11.5, "hvtier"))
    for z in (5.75, 8.0, 10.25):
        m.add(box(15.5, 2.0, z - 0.5, 16.0, 3.0, z + 0.5, "copper"))
    tool = (4.5 / 16 - 0.5, (TOP + 0.05) / 16 - 0.5, 9.0 / 16 - 0.5)          # where the tool lies (block units, centred)
    spark = (7.7 / 16 - 0.5, (TOP + 0.5) / 16 - 0.5, 4.8 / 16 - 0.5)          # the torch tip
    return m, tool, spark


def atlas(path):
    from PIL import Image
    rnd = random.Random(1028)
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
    cell("darksteel", (58, 60, 64), 5)
    cell("wood", (128, 92, 56), 6, shade=lambda x, y: -18 if y % 4 == 0 else (10 if (x * 3 + y) % 11 == 0 else 0))   # planks
    cell("plate", (112, 116, 120), 5, shade=lambda x, y: 22 if (x + 2 * y) % 13 == 0 else (-12 if (x * 7 + y * 3) % 17 == 0 else 0))   # worn steel
    cell("welder", (214, 150, 40), 7)          # workshop orange
    cell("dials", (40, 42, 46), 3, shade=lambda x, y: (150 if (x - 4) ** 2 + (y - 5) ** 2 in (4, 5) or (x - 11) ** 2 + (y - 5) ** 2 in (4, 5) else 0)
         + (180 if (x, y) in ((4, 4), (11, 6)) else 0) + (120 if y == 11 and 3 <= x <= 12 else 0))   # two dials, a scale
    cell("rubber", (24, 24, 26), 3)
    cell("copper", (196, 110, 70), 12)
    cell("peg", (176, 140, 96), 5, shade=lambda x, y: -90 if x % 4 == 2 and y % 4 == 2 else 0)   # pegboard holes
    cell("glow", (255, 236, 170), 4)
    cell("brass", (200, 170, 80), 10)
    cell("hvtier", (245, 204, 143), 8)        # the HV (800 V) tier colour
    im.save(path)


def icon(path):
    """The 16 px item icon (the vanilla set's rule: like the model): the desk from the front, three-quarter."""
    from PIL import Image
    P = {".": None, "w": (128, 92, 56), "W": (96, 66, 38), "s": (112, 116, 120), "d": (58, 60, 64), "o": (214, 150, 40),
         "O": (170, 112, 24), "k": (40, 42, 46), "g": (255, 236, 170), "b": (196, 170, 128), "h": (110, 88, 60), "c": (196, 110, 70), "l": (150, 155, 160)}
    rows = [
        "................",
        "..ddddddddd.....",
        "..d...bbbbbbbbb.",
        ".ddd..bhbbhbbhb.",
        ".dgd..bbbbbbbbb.",
        "......bhOOOOOhb.",
        "..oo.oobOokkoOb.",
        "..oollobOooooOb.",
        "sssssssssssssss.",
        "wwwwwwwwwwwwwwwW",
        "WWWWWWWWWWWWWWWW",
        ".dd..........dd.",
        ".dd..ssss....dd.",
        ".ddwwwwwwwwwwdd.",
        ".dd..........dd.",
        ".dd..........dd.",
    ]
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if P.get(ch):
                im.putpixel((x, y), P[ch] + (255,))
    im.save(path)


def main():
    res = sys.argv[1]
    out = os.path.join(res, "assets/eln/model/electricalanvil")
    os.makedirs(out, exist_ok=True)
    m, tool, spark = desk()
    tris = m.write_eln(out, "electricalanvil", CELLS, GRID)
    with open(os.path.join(out, "electricalanvil.txt"), "w") as f:
        f.write("o main\n" + "".join(f"f {k} {v:.5f}\n" for k, v in zip(("toolX", "toolY", "toolZ", "sparkX", "sparkY", "sparkZ"), tool + spark)))
    atlas(os.path.join(out, "tex.png"))
    tex = os.path.join(res, "assets/eln/textures/item")
    os.makedirs(tex, exist_ok=True)
    icon(os.path.join(tex, "electrical_anvil.png"))
    print("electricalanvil triangles", tris)


if __name__ == "__main__":
    main()
