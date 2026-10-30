#!/usr/bin/env python3
"""The bikes' 16x16 item icons (Baughn 2026-10-28). The 64x64 entity texture is painted in Java from the model's
regions (mods.eln.bike.BikeTexture: ./gradlew bikeTexture).

usage: texture.py ITEM_PNG [TERRAIN_ITEM_PNG]
"""
import sys

from PIL import Image, ImageDraw


def item(path, terrain=False):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    tyre, paint, metal, dark, trunk, seat = (40, 40, 44), (52, 104, 180), (180, 186, 192), (70, 74, 80), (86, 98, 110), (60, 46, 38)
    if terrain:   # the terrain bike: orange, fat tyres, a dark suspension fork
        paint = (200, 100, 42)
    for cx in (3.5, 12.5):   # wheels: rear (left), front (right)
        d.ellipse((cx - 3.5, 8, cx + 3.5, 15), outline=tyre, width=2 if terrain else 1)
        d.point((int(cx), 11), fill=metal)
    d.line((8, 12, 6, 6), fill=paint)       # seat tube
    d.line((6, 7, 11, 6), fill=paint)       # top tube
    d.line((11, 6, 8, 12), fill=paint)      # down tube
    d.line((8, 12, 4, 11), fill=paint)      # chain stay
    d.line((6, 7, 4, 11), fill=paint)       # seat stay
    d.line((11, 6, 12, 11), fill=(58, 62, 68) if terrain else paint)     # fork
    d.line((10, 8, 9, 10), fill=dark)       # battery
    d.line((11, 6, 11, 4), fill=metal)      # stem
    d.line((10, 4, 13, 4), fill=dark)       # bars
    d.line((5, 5, 7, 5), fill=seat)         # saddle
    d.rectangle((1, 5, 4, 7), fill=trunk)   # trunk over the back wheel
    d.line((1, 5, 4, 5), fill=(120, 132, 144))
    img.save(path)


if __name__ == "__main__":
    item(sys.argv[1])
    if len(sys.argv) > 2:
        item(sys.argv[2], terrain=True)
