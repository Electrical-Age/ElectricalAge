#!/usr/bin/env python3
"""A deepslate variant of an ore texture: the ore's specks (its pixels that differ from vanilla stone) over vanilla
deepslate, slightly darkened as vanilla's deepslate ores are. Usage: deepslate_ore.py ORE.png STONE.png DEEPSLATE.png OUT.png"""
import sys
from PIL import Image

ore, stone, deep, out = sys.argv[1:5]
o = Image.open(ore).convert('RGBA')
s = Image.open(stone).convert('RGBA').resize(o.size)
d = Image.open(deep).convert('RGBA').resize(o.size)
r = d.copy()
for y in range(o.size[1]):
    for x in range(o.size[0]):
        po, ps = o.getpixel((x, y)), s.getpixel((x, y))
        diff = sum(abs(a - b) for a, b in zip(po[:3], ps[:3]))
        grey = max(po[:3]) - min(po[:3])
        if diff > 60 or grey > 18:
            r.putpixel((x, y), tuple(int(c * 0.9) for c in po[:3]) + (255,))
r.save(out)
