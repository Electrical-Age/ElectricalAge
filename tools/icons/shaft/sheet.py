#!/usr/bin/env python3
"""Contact sheet of the shaft-family icons: each at 8x (nearest) with its id, plus a 1x/2x row on slot grey.
Usage: sheet.py [OUT.png]  (default: notes/ea/shaft/p5-previews/icons_sheet.png)"""
import os, sys
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from models import DEVICES, REPO

ITEMS = list(DEVICES) + ["turbine_blade_set", "pipe_kit_large_alloy", "pipe_quality_sensor", "pipe_pressure_valve"]
TEX = os.path.join(REPO, "src/main/resources/assets/eln/textures/item")
OUT = sys.argv[1] if len(sys.argv) > 1 else "/var/lib/mclab/agent/notes/ea/shaft/p5-previews/icons_sheet.png"

Z, COLS, LAB = 8, 5, 14
cw, ch = 16 * Z + 16, 16 * Z + LAB + 8
rows = (len(ITEMS) + COLS - 1) // COLS
small_h = 2 * 18 + 4
sheet = Image.new("RGBA", (cw * COLS, ch * rows + small_h * 2), (139, 139, 139, 255))
dr = ImageDraw.Draw(sheet)
for i, it in enumerate(ITEMS):
    p = os.path.join(TEX, it + ".png")
    x, y = (i % COLS) * cw + 8, (i // COLS) * ch + 4
    dr.rectangle((x, y, x + 16 * Z - 1, y + 16 * Z - 1), fill=(198, 198, 198, 255))
    if os.path.exists(p):
        im = Image.open(p).convert("RGBA").resize((16 * Z, 16 * Z), Image.NEAREST)
        sheet.alpha_composite(im, (x, y))
    dr.text((x, y + 16 * Z + 2), it, fill=(0, 0, 0, 255))
# inventory-like strips at GUI scale 1 and 2
y0 = ch * rows
for s, yy in ((1, y0 + 4), (2, y0 + 4 + small_h)):
    for i, it in enumerate(ITEMS):
        p = os.path.join(TEX, it + ".png")
        x = 8 + i * (18 * s)
        dr.rectangle((x, yy, x + 18 * s - 1, yy + 18 * s - 1), fill=(55, 55, 55, 255))
        dr.rectangle((x + s, yy + s, x + 17 * s - 1, yy + 17 * s - 1), fill=(139, 139, 139, 255))
        if os.path.exists(p):
            im = Image.open(p).convert("RGBA").resize((16 * s, 16 * s), Image.NEAREST)
            sheet.alpha_composite(im, (x + s, yy + s))
os.makedirs(os.path.dirname(OUT), exist_ok=True)
sheet.save(OUT)
print(OUT)
