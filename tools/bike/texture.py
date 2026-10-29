#!/usr/bin/env python3
"""The electric bike's textures (Baughn 2026-10-28): the 64x64 entity texture for BikeModel and the 16x16 item icon.

The entity texture is regions of one material each (BikeModel's PAINT, TIRE, METAL, ...); every box of a part samples
inside its region, so a region only needs to be big enough for its largest box's UV layout. A little noise and a
vertical shade keep the flats from looking like plastic.

usage: texture.py ENTITY_PNG ITEM_PNG
"""
import random
import sys

from PIL import Image, ImageDraw

random.seed(20261028)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c) + (255,)


def region(img, u, v, w, h, color, noise=0.06, stripe=None):
    for y in range(v, v + h):
        for x in range(u, u + w):
            f = 1 + random.uniform(-noise, noise) - 0.10 * ((y - v) / max(1, h - 1) - 0.5)
            c = color
            if stripe and stripe(x - u, y - v):
                c = stripe(x - u, y - v)
            img.putpixel((x, y), shade(c, f))


def entity(path):
    # regions as BikeModel's (u, v); each fits its largest box's UV layout. FRAME paint, trunk, lid and saddle are
    # neutral greys: BikeRenderer tints them per dye group (their default colours are tints too).
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    NEUTRAL, TIRE, METAL = (226, 226, 226), (38, 38, 40), (176, 182, 188)
    BATTERY, MOTOR, BLACK = (54, 58, 62), (44, 46, 50), (28, 28, 30)
    region(img, 0, 0, 28, 14, NEUTRAL, 0.05)
    region(img, 28, 0, 16, 7, TIRE, 0.08, lambda x, y: (24, 24, 26) if (x + y) % 3 == 0 else None)
    region(img, 44, 0, 16, 7, NEUTRAL, 0.08, lambda x, y: (250, 250, 250) if y == 1 else None)
    # the trunk: a strap and rivets, in neutral shades
    region(img, 0, 14, 28, 12, NEUTRAL, 0.05,
           lambda x, y: (150, 150, 150) if y in (5, 6) else (255, 255, 255) if (x % 7 == 3 and y in (2, 9)) else None)
    region(img, 0 + 28, 14, 32, 9, NEUTRAL, 0.05, lambda x, y: (255, 255, 255) if y == 0 else None)
    region(img, 0, 26, 28, 10, METAL, 0.05, lambda x, y: (210, 214, 218) if y == 0 else None)
    # the battery: dark case, a green charge stripe along it
    region(img, 28, 23, 18, 9, BATTERY, 0.05, lambda x, y: (60, 190, 80) if y == 5 and 4 <= x % 9 <= 6 else None)
    # motor and lamp share it: the lamp box (d 1.5) has its face where the motor box (d 4) has an unused corner
    region(img, 46, 23, 16, 8, MOTOR, 0.06, lambda x, y: (236, 232, 196) if 1 <= x <= 3 and 1 <= y <= 3 else None)
    region(img, 28, 32, 12, 8, BLACK, 0.06)
    img.save(path)


def item(path):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    tyre, paint, metal, dark, trunk, seat = (40, 40, 44), (52, 104, 180), (180, 186, 192), (70, 74, 80), (86, 98, 110), (60, 46, 38)
    for cx in (3.5, 12.5):   # wheels: rear (left), front (right)
        d.ellipse((cx - 3.5, 8, cx + 3.5, 15), outline=tyre)
        d.point((int(cx), 11), fill=metal)
    d.line((8, 12, 6, 6), fill=paint)       # seat tube
    d.line((6, 7, 11, 6), fill=paint)       # top tube
    d.line((11, 6, 8, 12), fill=paint)      # down tube
    d.line((8, 12, 4, 11), fill=paint)      # chain stay
    d.line((6, 7, 4, 11), fill=paint)       # seat stay
    d.line((11, 6, 12, 11), fill=paint)     # fork
    d.line((10, 8, 9, 10), fill=dark)       # battery
    d.line((11, 6, 11, 4), fill=metal)      # stem
    d.line((10, 4, 13, 4), fill=dark)       # bars
    d.line((5, 5, 7, 5), fill=seat)         # saddle
    d.rectangle((1, 5, 4, 7), fill=trunk)   # trunk over the back wheel
    d.line((1, 5, 4, 5), fill=(120, 132, 144))
    img.save(path)


if __name__ == "__main__":
    entity(sys.argv[1])
    item(sys.argv[2])
