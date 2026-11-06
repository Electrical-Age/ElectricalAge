"""Icons for the ferromagnetic cores' intermediates (Baughn 2026-11-06): Silicon Steel Laminations (a stack of thin
grey-blue sheets, E-shaped) and the Nanocrystalline Core Blank (a dull dark ring of wound ribbon).
Usage: cores.py RESOURCES_DIR -> assets/eln/textures/item/{silicon_steel_laminations,nanocrystalline_core_blank}.png"""
import os, sys
from PIL import Image

def blank():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))

def laminations():
    im = blank(); px = im.load()
    for i, y0 in enumerate(range(3, 13, 2)):       # five sheets, offset a little each
        dx = i % 2
        for x in range(2 + dx, 14 + dx - 1):
            shade = 150 + 12 * ((x + i) % 3)
            px[x, y0] = (shade - 20, shade - 10, shade + 15, 255)       # the steel, bluish (silicon steel's sheen)
            px[x, y0 + 1] = (70, 75, 90, 255)                           # the coat between sheets
    for y in range(3, 14):                          # the E's window, cut through every sheet
        for x in (6, 7, 10, 11):
            if 5 <= y <= 11: px[x, y] = (0, 0, 0, 0)
    return im

def ring():
    im = blank(); px = im.load()
    for y in range(16):
        for x in range(16):
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 3.2 <= r <= 6.8:
                band = int(r * 3) % 2                 # the wound ribbon's turns
                v = 85 + 25 * band - int(4 * abs(x - y) / 3)
                px[x, y] = (v, v - 5, v - 12, 255)
    return im

if __name__ == "__main__":
    out = os.path.join(sys.argv[1], "assets", "eln", "textures", "item")
    for name, im in (("silicon_steel_laminations", laminations()), ("nanocrystalline_core_blank", ring())):
        im.save(os.path.join(out, name + ".png"))
        print(name)
