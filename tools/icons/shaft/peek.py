#!/usr/bin/env python3
"""Working view: each given icon at 10x next to its model's oblique render, plus 1x/2x/3x on slot grey.
Usage: peek.py OUT.png id [id ...]"""
import os, sys
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from models import DEVICES, REPO, load
from render3d import render, to_image

TEX = os.path.join(REPO, "src/main/resources/assets/eln/textures/item")
out, ids = sys.argv[1], sys.argv[2:]
Z = 10
W = 16 * Z * 2 + 16 * 6 + 40
sheet = Image.new("RGBA", (W, (16 * Z + 20) * len(ids)), (139, 139, 139, 255))
dr = ImageDraw.Draw(sheet)
for i, it in enumerate(ids):
    y = i * (16 * Z + 20)
    im = Image.open(os.path.join(TEX, it + ".png")).convert("RGBA")
    big = im.resize((16 * Z, 16 * Z), Image.NEAREST)
    bg = Image.new("RGBA", big.size, (198, 198, 198, 255))
    bg.alpha_composite(big)
    sheet.paste(bg, (4, y + 4))
    if it in DEVICES:
        f, t = load(it)
        ref = to_image(render(f, t, ("oblique", 0.4), 16 * Z, margin=0.04, light=(-0.5, 1, -0.3)))
        sheet.paste(ref, (16 * Z + 12, y + 4))
    x = 16 * Z * 2 + 20
    for s in (1, 2, 3):
        dr.rectangle((x, y + 4, x + 16 * s + 1, y + 4 + 16 * s + 1), fill=(139, 139, 139, 255), outline=(55, 55, 55, 255))
        sheet.alpha_composite(im.resize((16 * s, 16 * s), Image.NEAREST), (x + 1, y + 5))
        x += 16 * s + 6
    dr.text((4, y + 16 * Z + 6), it, fill=(0, 0, 0, 255))
sheet.save(out)
