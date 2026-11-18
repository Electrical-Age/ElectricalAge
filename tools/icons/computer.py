#!/usr/bin/env python3
"""The robotics computer's items (16x16): RAM chip (a green board with four black chips and gold contacts), Janet ROM
chip (a black chip with a quartz window and a purple label), floppy disk (3.5": dark body, metal shutter, white
label), the Elevator floppy (the label shows an up/down arrow) and the Applications floppy (four coloured squares).
Usage: computer.py RESOURCES_DIR -> assets/eln/textures/item/{ram_chip,janet_rom_chip,boot_rom_chip,floppy_disk,elevator_disk,applications_disk}.png"""
import os, sys
from PIL import Image

def blank():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))

def rect(px, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[x, y] = c + (255,) if len(c) == 3 else c

def ram():
    im = blank(); px = im.load()
    rect(px, 0, 4, 15, 11, (40, 120, 60))          # the board
    rect(px, 0, 4, 15, 4, (60, 150, 80))           # its lit top edge
    rect(px, 0, 11, 15, 11, (25, 85, 40))
    for x0 in (1, 5, 9, 13):                         # four chips
        rect(px, x0, 5, x0 + 1 if x0 == 13 else x0 + 2, 8, (30, 30, 34))
        rect(px, x0, 5, x0 + 1 if x0 == 13 else x0 + 2, 5, (60, 60, 66))
    for x in range(1, 15, 2):                         # gold contacts
        rect(px, x, 10, x, 11, (220, 180, 60))
    return im

def rom(label=(140, 70, 190)):
    im = blank(); px = im.load()
    rect(px, 2, 3, 13, 12, (34, 34, 38))            # the package
    rect(px, 2, 3, 13, 3, (70, 70, 78))
    for y in range(4, 12, 2):                         # pins
        px[1, y] = (190, 190, 200, 255); px[14, y] = (190, 190, 200, 255)
    rect(px, 5, 5, 10, 8, (150, 160, 175))           # the quartz window
    rect(px, 6, 6, 9, 7, (190, 200, 215))
    rect(px, 4, 10, 11, 11, label)                    # the label: purple Janet, orange Floppy Boot
    return im

def floppy(arrow=False, apps=False):
    im = blank(); px = im.load()
    rect(px, 1, 1, 14, 14, (40, 50, 70))            # the body
    rect(px, 1, 1, 14, 1, (70, 80, 105))
    rect(px, 1, 1, 1, 14, (60, 70, 95))
    px[14, 1] = (0, 0, 0, 0)                          # the cut corner
    rect(px, 4, 1, 11, 5, (175, 180, 190))           # the metal shutter
    rect(px, 8, 2, 9, 4, (60, 65, 75))               # its window
    rect(px, 3, 8, 12, 14, (230, 230, 225))          # the label
    if arrow:
        for (x, y) in ((7, 9), (6, 10), (7, 10), (8, 10), (7, 11), (7, 12), (6, 12), (8, 12), (7, 13)):
            px[x, y] = (200, 60, 50, 255)
    elif apps:
        for (x0, y0, c) in ((5, 9, (200, 60, 50)), (8, 9, (60, 150, 70)), (5, 12, (60, 90, 200)), (8, 12, (220, 170, 40))):
            rect(px, x0, y0, x0 + 1, y0 + 1, c)
    else:
        for y in (10, 12):
            rect(px, 4, y, 11, y, (150, 150, 160))
    return im

def computer():
    """The front: the CRT monitor on the case, painted as the three-channel logger (paint_condenser 60,120,130; ComputerParts)."""
    im = blank(); px = im.load()
    paint, lit, dark, steel = (60, 120, 130), (90, 150, 160), (35, 80, 90), (45, 48, 55)
    # the monitor: shell, glass with two lines of text, power lamp
    rect(px, 2, 0, 13, 7, paint)
    rect(px, 2, 0, 13, 0, lit)
    rect(px, 3, 1, 12, 5, (16, 22, 20))
    rect(px, 4, 2, 8, 2, (200, 210, 190))
    rect(px, 4, 4, 10, 4, (120, 200, 130))
    rect(px, 11, 6, 11, 6, (90, 230, 90))
    rect(px, 6, 8, 9, 8, steel)                       # its foot
    # the case: drives, lamp, vent
    rect(px, 0, 9, 15, 15, paint)
    rect(px, 0, 9, 15, 9, lit)
    rect(px, 0, 15, 15, 15, dark)
    for x0 in (1, 8):
        rect(px, x0, 11, x0 + 5, 12, steel)
        rect(px, x0 + 1, 11, x0 + 4, 11, (150, 155, 165))
    rect(px, 14, 11, 14, 11, (90, 230, 90))
    rect(px, 2, 14, 13, 14, dark)
    return im

if __name__ == "__main__":
    out = os.path.join(sys.argv[1], "assets", "eln", "textures", "item")
    os.makedirs(out, exist_ok=True)
    for name, im in (("computer", computer()), ("ram_chip", ram()), ("janet_rom_chip", rom()), ("boot_rom_chip", rom((220, 130, 40))), ("floppy_disk", floppy()), ("elevator_disk", floppy(True)), ("applications_disk", floppy(apps=True))):
        im.save(os.path.join(out, name + ".png"))
        print(name)
