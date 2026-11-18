#!/usr/bin/env python3
"""The Toolbag's icon (16x16): a brown leather tool bag with an iron frame and handle, a coil of cable peeking out.
Usage: toolbag.py RESOURCES_DIR -> assets/eln/textures/item/toolbag.png"""
import os, sys
from PIL import Image

LEATHER = [(92, 54, 30), (122, 74, 40), (150, 96, 54), (176, 118, 70)]   # dark .. light
IRON = [(70, 72, 78), (130, 134, 140), (196, 200, 206)]
CABLE = [(150, 40, 30), (205, 70, 55)]
OUT = (40, 24, 14)

def icon():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    def put(x, y, c):
        px[x, y] = c + (255,)
    # the handle: an iron arch over the bag's mouth
    for x in range(5, 11):
        put(x, 2, IRON[1] if x not in (5, 10) else IRON[0])
    for y in (3, 4):
        put(5, y, IRON[0]); put(10, y, IRON[1])
    # cable coil peeking out of the mouth, left of centre
    for x, y, c in ((3, 4, 0), (4, 4, 1), (3, 5, 1), (4, 5, 0), (12, 5, 1), (12, 4, 0)):
        put(x, y, CABLE[c])
    # the body: a trapezoid, wider at the bottom, lit from the top left
    for y in range(5, 15):
        half = 5 + (y - 5) // 4
        x0, x1 = 8 - half - 1, 8 + half
        for x in range(x0, x1 + 1):
            if x in (x0, x1) or y == 14:
                put(x, y, OUT)
            else:
                shade = 3 if x < 6 and y < 9 else 2 if x < 9 else 1
                if y > 11: shade = max(0, shade - 1)
                put(x, y, LEATHER[shade])
    # the iron frame along its mouth, and a clasp
    for x in range(2, 14):
        put(x, 5, IRON[2] if x < 7 else IRON[1])
    put(7, 7, IRON[2]); put(8, 7, IRON[1]); put(7, 8, IRON[1]); put(8, 8, IRON[0])
    # a stitched seam down the front
    for y in range(9, 14, 2):
        put(4, y, LEATHER[0]); put(11, y, LEATHER[0])
    return im

if __name__ == "__main__":
    out = os.path.join(sys.argv[1], "assets/eln/textures/item")
    icon().save(os.path.join(out, "toolbag.png"))
    print("toolbag.png")
