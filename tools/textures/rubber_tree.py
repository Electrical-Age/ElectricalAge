#!/usr/bin/env python3
"""Rubber tree block textures (2026-11-07): log side and top, leaves, sapling.

Hevea bark is smooth and grey-brown with paler lichen patches; the wood is pale cream; the leaves are glossy, dark
green, in threes. Seeded, so a rerun gives the same pixels.

    python3 tools/textures/rubber_tree.py    (writes src/main/resources/assets/eln/textures/block/rubber_*.png)
"""
import os
import random

from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources', 'assets', 'eln', 'textures', 'block')


def hexc(s, a=255):
    return (int(s[1:3], 16), int(s[3:5], 16), int(s[5:7], 16), a)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (c[3],)


def log_side():
    r = random.Random(11)
    base = [hexc('#6f665a'), hexc('#7b7264'), hexc('#857c6d'), hexc('#91887a')]
    im = Image.new('RGBA', (16, 16))
    for x in range(16):
        col = r.choice(base)
        for y in range(16):
            c = col if r.random() < 0.8 else r.choice(base)
            im.putpixel((x, y), c)
    # a few darker vertical cracks
    for x in (3, 9, 13):
        y0 = r.randrange(16)
        for k in range(r.randrange(4, 9)):
            im.putpixel((x, (y0 + k) % 16), hexc('#554d43'))
    # pale lichen patches
    for _ in range(3):
        cx, cy = r.randrange(16), r.randrange(16)
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1), (-1, 0)):
            if r.random() < 0.8:
                im.putpixel(((cx + dx) % 16, (cy + dy) % 16), hexc('#a8a290'))
    return im


def log_top():
    r = random.Random(12)
    im = Image.new('RGBA', (16, 16))
    rings = [hexc('#d9c79c'), hexc('#cbb68a'), hexc('#d9c79c'), hexc('#c4ad7e'), hexc('#d6c398'), hexc('#cbb68a')]
    for x in range(16):
        for y in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d > 6.5:
                c = r.choice([hexc('#6f665a'), hexc('#7b7264'), hexc('#857c6d')])
            else:
                c = rings[int(d) % len(rings)]
                if r.random() < 0.08:
                    c = shade(c, 0.93)
            im.putpixel((x, y), c)
    return im


def leaves():
    r = random.Random(13)
    greens = [hexc('#1f4d1c'), hexc('#2a6224'), hexc('#36752c'), hexc('#4a8c38'), hexc('#6aa84a')]
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    # leaflets: short diagonal ovals, a highlight on one edge (glossy)
    for _ in range(26):
        cx, cy = r.randrange(16), r.randrange(16)
        horiz = r.random() < 0.5
        pts = [(0, 0), (1, 0), (-1, 0), (2, 0)] if horiz else [(0, 0), (0, 1), (0, -1), (0, 2)]
        for i, (dx, dy) in enumerate(pts):
            g = greens[r.randrange(1, 4)]
            if i == 0 and r.random() < 0.5:
                g = greens[4]
            im.putpixel(((cx + dx) % 16, (cy + dy) % 16), g)
    # darken a few for depth; leave holes (alpha 0) so it reads as leaves
    for x in range(16):
        for y in range(16):
            if im.getpixel((x, y))[3] == 0 and r.random() < 0.35:
                im.putpixel((x, y), greens[0])
    return im


def sapling():
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    stem, dark = hexc('#7b7264'), hexc('#5e564b')
    for y in range(7, 16):
        im.putpixel((8, y), stem if y % 3 else dark)
    for (x, y) in ((7, 15), (9, 15)):
        im.putpixel((x, y), dark)
    g1, g2, g3 = hexc('#2a6224'), hexc('#36752c'), hexc('#5d9e42')
    leaflets = [
        [(8, 2), (8, 3), (8, 4), (8, 5), (7, 3), (9, 3), (7, 4), (9, 4)],   # top leaflet
        [(3, 6), (4, 6), (5, 6), (6, 7), (4, 5), (5, 5), (4, 7), (5, 7)],   # left
        [(13, 6), (12, 6), (11, 6), (10, 7), (12, 5), (11, 5), (12, 7), (11, 7)],   # right
        [(5, 10), (6, 10), (7, 11), (4, 10), (5, 11), (6, 11)],   # lower left
        [(11, 10), (10, 10), (9, 11), (12, 10), (11, 11), (10, 11)],   # lower right
    ]
    for k, pts in enumerate(leaflets):
        for i, p in enumerate(pts):
            im.putpixel(p, g3 if i == 0 else (g2 if i % 2 else g1))
    for (x, y) in ((8, 6), (7, 7), (9, 7), (8, 9)):
        im.putpixel((x, y), stem)
    return im


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    for name, f in (('rubber_log', log_side), ('rubber_log_top', log_top), ('rubber_leaves', leaves), ('rubber_sapling', sapling)):
        f().save(os.path.join(OUT, name + '.png'))
        print('wrote', name)
