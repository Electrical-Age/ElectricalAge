#!/usr/bin/env python3
"""Rubber fig block textures (2026-11-07): log side and top, leaves, sapling, aerial root.

Ficus elastica: smooth grey-brown bark with paler patches, pale wood, big glossy dark leaves with red sheaths at new
shoots, and thin aerial roots. Seeded, so a rerun gives the same pixels.

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
    """Ficus elastica: big leathery oval leaves, dark and glossy, a pale midrib, a bright glint; a red sheath at a tip."""
    r = random.Random(13)
    dark, mid, light, glint = hexc('#14361a'), hexc('#1f4d24'), hexc('#2e6a32'), hexc('#7fb07a')
    rib, sheath = hexc('#4f7a45'), hexc('#a8322c')
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    # five leaves, each an ellipse 6-7 x 3 px, tilted by its axis; they tile across the edges
    leaves = [(3, 3, 0), (11, 4, 1), (6, 10, 1), (13, 12, 0), (1, 13, 1)]
    for (cx, cy, vert) in leaves:
        a, b = (1.6, 3.4) if vert else (3.4, 1.6)
        for dx in range(-4, 5):
            for dy in range(-4, 5):
                if (dx / a) ** 2 + (dy / b) ** 2 > 1.0:
                    continue
                x, y = (cx + dx) % 16, (cy + dy) % 16
                edge = (dx / a) ** 2 + (dy / b) ** 2 > 0.55
                c = mid if edge else light
                if (vert and dx == 0) or (not vert and dy == 0):
                    c = rib
                if (dx, dy) == ((-1, -1) if vert else (-2, -1)):
                    c = glint
                im.putpixel((x, y), c)
    # shadows between leaves, holes kept
    for x in range(16):
        for y in range(16):
            if im.getpixel((x, y))[3] == 0 and r.random() < 0.3:
                im.putpixel((x, y), dark)
    # the red sheath of a new shoot: two pixels at one leaf's tip
    # (AdventurAgent: two dots barely showed from the ground; three sheaths, each a bright red spike of three pixels)
    for p in ((11, 0), (11, 1), (12, 0), (3, 7), (3, 8), (14, 9), (14, 10)):
        im.putpixel(p, sheath)
    for p in ((11, 15), (3, 9), (14, 11)):
        im.putpixel(p, hexc('#d4483c'))
    return im


def aerial_root():
    """Thin strands hanging straight down, grey-brown, with a few knots; mostly transparent (a cross model)."""
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    c1, c2, c3 = hexc('#6b5a48'), hexc('#857260'), hexc('#4f4236')
    for x, top, col in ((6, 0, c1), (7, 0, c2), (9, 0, c1), (8, 3, c3), (10, 5, c2)):
        for y in range(top, 16):
            im.putpixel((x, y), col if y % 5 else c3)
    for (x, y) in ((7, 6), (9, 11), (6, 13)):
        im.putpixel((x, y), hexc('#9c8a74'))
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
    for name, f in (('rubber_log', log_side), ('rubber_log_top', log_top), ('rubber_leaves', leaves), ('rubber_sapling', sapling),
                    ('rubber_aerial_root', aerial_root)):
        f().save(os.path.join(OUT, name + '.png'))
        print('wrote', name)
