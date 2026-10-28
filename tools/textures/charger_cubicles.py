#!/usr/bin/env python3
"""The battery charger's cubicles grey (Baughn 2026-10-28: not the same colour as the front).

model/batterychargera/batterychargera.obj maps every cubicle face (floor and walls: the recesses from x 0.125 to
0.1875) into u 0..0.156, v 0.844..1 of its 32 px texture, which Obj3D flips to the top-left 5 x 5 pixels; nothing else
uses them. Paint them a mid grey, the walls (the region's edges) a shade darker.

usage: charger_cubicles.py PNG   (edits in place)
"""
import random
import sys

from PIL import Image

random.seed(2026102809)
im = Image.open(sys.argv[1]).convert("RGBA")
for y in range(5):
    for x in range(5):
        edge = x in (0, 4) or y in (0, 4)
        g = (104 if edge else 120) + random.randint(-3, 3)
        im.putpixel((x, y), (g, g + 2, g + 4, 255))
im.save(sys.argv[1])
