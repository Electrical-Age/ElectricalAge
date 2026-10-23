#!/usr/bin/env python3
"""Cable kit icons (16x16): a coil of cable (grey insulation, two turns) with a coloured tag for the tier.
Usage: cablekit.py RESOURCES_DIR -> assets/eln/textures/item/cable_kit_{signal,lv,mv,hv,vhv}.png"""
import math, os, sys
from PIL import Image

TAGS = {"signal": (230, 230, 230), "lv": (200, 60, 50), "mv": (230, 190, 40), "hv": (60, 110, 210), "vhv": (130, 60, 170), "thermal": (205, 120, 60)}
THICK = {"signal": 1, "lv": 1.4, "mv": 1.7, "hv": 2.1, "vhv": 2.4, "thermal": 2.4}

def icon(tag, thick):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8, y + 0.5 - 8.5
            r = math.hypot(dx, dy)
            for ring in (6.0, 3.6):
                d = abs(r - ring)
                if d < thick / 2 + 0.35:
                    shade = 175 - int(60 * max(0.0, dy / 8))   # (lit from above)
                    edge = d > thick / 2 - 0.4
                    c = shade - 45 if edge else shade
                    px[x, y] = (c, c, c + 5, 255)
    # the tier's tag: a square on the right
    for y in range(9, 14):
        for x in range(11, 15):
            px[x, y] = tag + (255,) if (x, y) not in ((11, 9), (14, 13)) else (40, 40, 40, 255)
    return im

if __name__ == "__main__":
    out = os.path.join(sys.argv[1], "assets/eln/textures/item")
    for name, tag in TAGS.items():
        icon(tag, THICK[name]).save(os.path.join(out, "cable_kit_%s.png" % name))
        print("cable_kit_%s.png" % name)
