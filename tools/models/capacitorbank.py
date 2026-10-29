#!/usr/bin/env python3
"""Capacitor bank (Baughn 2026-10-28): six big capacitor cans in two layers of three on a steel rack, joined by copper
busbars, with a terminal plate on each flank: + (red band, Baughn: polarities red/blue) on the left of the front, - (blue band)
on the right, as you face its front. Internally it is one large capacitor (CapacitorBankElement). A small window on the front shows a charge
gauge the renderer doesn't animate (the cans' band carries the tier colour instead).

EA transparent node drawn by CapacitorBankRender through Obj3D. px frame (0..16, corner origin); front = x 0 (XN).
After glRotateXnRef the front is XN, and front.left() of XN is ZN (Direction.right() of XN is ZP): the + plate
(left, as you face the front) sits at z = 0, the - plate at z = 16.
Usage: capacitorbank.py RESOURCES_DIR -> assets/eln/model/capacitorbank/capacitorbank.{obj,mtl} + tex.png,
       and the item icons assets/eln/textures/item/capacitor_bank.png, capacitor_cell.png
"""
import os, random, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Model, box, prism, tube

CELLS = {"steel": (0, 0), "darksteel": (1, 0), "can": (2, 0), "cantop": (3, 0),
         "copper": (0, 1), "hvtier": (1, 1), "blue": (2, 1), "label": (3, 1),
         "red": (0, 2)}
GRID = (4, 3)
CAN_R = 2.3
ZS = (3.0, 8.0, 13.0)          # the three cans of a layer, along z
LAYERS = ((1.5, 7.0), (8.5, 14.0))


def bank():
    m = Model()
    # the rack: base plate, four corner posts, a shelf between the layers, a top plate
    m.add(box(1.0, 0, 0.6, 15.0, 1.0, 15.4, "darksteel"))
    for x, z in ((1.0, 0.6), (13.8, 0.6), (1.0, 14.2), (13.8, 14.2)):
        m.add(box(x, 1.0, z, x + 1.2, 14.4, z + 1.2, "steel"))
    m.add(box(1.1, 7.4, 0.7, 14.9, 8.1, 15.3, "darksteel"))   # (inset: no shared faces with the posts)
    # an open top frame (the can tops and busbars stay visible): rails round the edge, a carrying bar across
    m.add(box(1.0, 14.4, 0.6, 15.0, 15.0, 1.8, "darksteel"), box(1.0, 14.4, 14.2, 15.0, 15.0, 15.4, "darksteel"))
    m.add(box(1.0, 14.4, 1.8, 2.2, 15.0, 14.2, "darksteel"), box(13.8, 14.4, 1.8, 15.0, 15.0, 14.2, "darksteel"))
    m.add(box(7.7, 15.0, 1.8, 8.3, 15.6, 14.2, "steel"))
    # six cans: a can body, a band with the tier colour, a dark top with two studs, joined by busbars over the tops
    for y0, y1 in LAYERS:
        for z in ZS:
            m.add(prism("y", 8.0, z, CAN_R, y0, y1 - 0.6, "can", n=12))
            m.add(tube("y", 8.0, z, CAN_R - 0.05, CAN_R + 0.08, y0 + 1.0, y0 + 1.8, "hvtier", n=12, inner=False))
            m.add(prism("y", 8.0, z, CAN_R - 0.2, y1 - 0.6, y1 - 0.3, "cantop", n=12))
            # the studs, marked: + red (the + busbar's side), - blue
            m.add(prism("y", 6.9, z, 0.45, y1 - 0.3, y1, "red", n=6), prism("y", 9.1, z, 0.45, y1 - 0.3, y1, "blue", n=6))
        m.add(box(6.6, y1, 1.0, 7.2, y1 + 0.35, 15.0, "copper"))            # the + busbar of the layer
        m.add(box(8.8, y1, 1.0, 9.4, y1 + 0.35, 15.0, "copper"))            # the - busbar
    # the risers joining the layers' busbars, down the flanks
    m.add(box(6.6, LAYERS[0][1], 0.5, 7.2, LAYERS[1][1] + 0.35, 1.0, "copper"))     # + side (z 0)
    m.add(box(8.8, LAYERS[0][1], 15.0, 9.4, LAYERS[1][1] + 0.35, 15.5, "copper"))   # - side (z 16)
    # the terminal plates, low on the flanks: + (left of the front: z = 0) red, - (z = 16) blue (Baughn); the cans' sleeve
    # bands keep the HV tier colour
    for z0, z1, band in ((0.0, 0.6, "red"), (15.4, 16.0, "blue")):
        m.add(box(4.5, 0.5, z0, 11.5, 4.5, z1, "steel"), box(4.5, 4.5, z0, 11.5, 5.0, z1, band))
        for x in (5.75, 8.0, 10.25):
            m.add(box(x - 0.5, 1.75, z0 - 0.1, x + 0.5, 2.75, z1 + 0.1, "copper"))
    # a rating label on the front of the base
    m.add(box(0.9, 0.2, 4.0, 1.0, 0.9, 12.0, "label", faces="w"))
    return m


def atlas(path):
    from PIL import Image
    rnd = random.Random(2810)
    im = Image.new("RGBA", (16 * GRID[0], 16 * GRID[1]))

    def cell(name, base, noise=6, shade=None):
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
    cell("can", (64, 92, 150), 6, shade=lambda x, y: 24 if x in (3, 4) else (-14 if x in (12, 13) else 0))   # blue sleeve, a highlight
    cell("cantop", (34, 36, 40), 4, shade=lambda x, y: 30 if (x - 8) ** 2 + (y - 8) ** 2 in range(20, 26) else 0)   # a vent ring
    cell("copper", (196, 110, 70), 12)
    cell("hvtier", (245, 204, 143), 8)        # the 800 V (HV) tier colour
    cell("blue", (40, 84, 200), 6)
    cell("red", (200, 36, 32), 6)
    cell("label", (232, 228, 214), 4, shade=lambda x, y: -150 if y in (6, 9) and 3 <= x <= 12 else 0)   # two printed lines
    im.save(path)


def icon(path):
    """16 px item icon: the bank from the front, two rows of three cans in the rack."""
    from PIL import Image
    P = {"s": (150, 155, 160), "d": (58, 60, 64), "b": (64, 92, 150), "B": (88, 116, 174), "h": (245, 204, 143),
         "t": (34, 36, 40), "c": (196, 110, 70)}
    rows = [
        "dddddddddddddddd",
        "s.ccccccccccccc.",
        "s.tt..tt..tt..s.",
        "s.Bb..Bb..Bb..s.",
        "s.hh..hh..hh..s.",
        "s.Bb..Bb..Bb..s.",
        "s.Bb..Bb..Bb..s.",
        "dddddddddddddddd",
        "s.ccccccccccccc.",
        "s.tt..tt..tt..s.",
        "s.Bb..Bb..Bb..s.",
        "s.hh..hh..hh..s.",
        "s.Bb..Bb..Bb..s.",
        "s.Bb..Bb..Bb..s.",
        "dddddddddddddddd",
        "................",
    ]
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if P.get(ch):
                im.putpixel((x, y), P[ch] + (255,))
    im.save(path)


def cell_icon(path):
    """16 px icon of a Capacitor Cell (the bank's can, crafted from Dielectrics): a blue can, tier band, two studs."""
    from PIL import Image
    P = {"b": (64, 92, 150), "B": (96, 124, 182), "D": (44, 64, 110), "h": (245, 204, 143), "t": (34, 36, 40), "c": (196, 110, 70)}
    rows = [
        "................",
        "......c..c......",
        "....tttttttt....",
        "...tttttttttt...",
        "...BBbbbbbbbD...",
        "...BBbbbbbbbD...",
        "...hhhhhhhhhh...",
        "...BBbbbbbbbD...",
        "...BBbbbbbbbD...",
        "...BBbbbbbbbD...",
        "...BBbbbbbbbD...",
        "...BBbbbbbbbD...",
        "...BBbbbbbbbD...",
        "...DDDDDDDDDD...",
        "................",
        "................",
    ]
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if P.get(ch):
                im.putpixel((x, y), P[ch] + (255,))
    im.save(path)


def main():
    res = sys.argv[1]
    out = os.path.join(res, "assets/eln/model/capacitorbank")
    os.makedirs(out, exist_ok=True)
    tris = bank().write_eln(out, "capacitorbank", CELLS, GRID)
    atlas(os.path.join(out, "tex.png"))
    tex = os.path.join(res, "assets/eln/textures/item")
    os.makedirs(tex, exist_ok=True)
    icon(os.path.join(tex, "capacitor_bank.png"))
    cell_icon(os.path.join(tex, "capacitor_cell.png"))
    print("capacitorbank triangles", tris)


if __name__ == "__main__":
    main()
