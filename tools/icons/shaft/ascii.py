#!/usr/bin/env python3
"""Print icons as letter grids with their palette (for pixel work). Usage: ascii.py id [id...]"""
import os, sys
from PIL import Image
TEX = os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../../src/main/resources/assets/eln/textures/item")
for n in sys.argv[1:]:
    im = Image.open(os.path.join(TEX, n + ".png")).convert("RGBA")
    pal = {}
    rows = []
    for y in range(16):
        r = ""
        for x in range(16):
            p = im.getpixel((x, y))
            if p[3] == 0:
                r += "."
                continue
            k = "#%02x%02x%02x" % p[:3]
            pal.setdefault(k, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"[len(pal)])
            r += pal[k]
        rows.append("%2d %s" % (y, r))
    print(n, " ".join(f"{v}={k}" for k, v in pal.items()))
    print("   0123456789abcdef")
    print("\n".join(rows))
