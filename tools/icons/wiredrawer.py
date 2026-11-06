"""Item icons for the wire-drawing machines (Wires stage 4, Baughn 2026-11-06) and the Variable DC-DC Converter.
A machine: a painted cabinet (green 48 V, blue 230 V) seen from the front, a copper spool on top at the back, a capstan
wound with black wire at the front, the wire between them. The variable converter: the DC-DC Converter's icon with a
signal tap (a small yellow arrow) in its corner.
Usage: wiredrawer.py RESOURCES_DIR -> assets/eln/textures/item/{wire_drawing_machine_48v,wire_drawing_machine_230v,
variable_dc_dc_converter}.png"""
import os, sys
from PIL import Image


def machine(paint):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); px = im.load()
    dark = tuple(int(c * 0.65) for c in paint)
    for x in range(1, 15):                        # base
        px[x, 15] = (60, 62, 66, 255)
    for y in range(9, 15):                        # cabinet, shaded at its right side and top edge
        for x in range(1, 15):
            c = dark if x >= 13 else paint
            if y == 9: c = tuple(min(255, int(v * 1.2)) for v in paint)
            px[x, y] = c + (255,)
    for y in range(11, 14):                       # the control panel
        for x in range(3, 7):
            px[x, y] = (40, 42, 46, 255)
    px[4, 12] = (230, 200, 60, 255)
    for y in range(2, 9):                         # the copper spool (back right) between steel flanges
        for x in range(9, 15):
            if x in (9, 14): px[x, y] = (130, 136, 142, 255)
            elif 3 <= y <= 7: px[x, y] = (196 - 12 * (y % 2), 110, 70, 255)
    for y in range(5, 9):                         # the capstan (front left), wound with black wire
        for x in range(2, 7):
            px[x, y] = (34, 34, 36, 255) if y in (6, 7) else (128, 134, 140, 255)
    for x in range(7, 10):                        # the wire from spool to capstan
        px[x, 6] = (196, 110, 70, 255)
    return im


def variable(base):
    im = base.copy(); px = im.load()
    pts = [(12, 1), (13, 1), (14, 1), (13, 2), (13, 3), (12, 2), (14, 2)]   # a small arrow pointing down: the signal tap
    for x, y in pts: px[x, y] = (240, 210, 40, 255)
    px[13, 4] = (240, 210, 40, 255)
    return im


if __name__ == "__main__":
    out = os.path.join(sys.argv[1], "assets", "eln", "textures", "item")
    machine((56, 156, 84)).save(os.path.join(out, "wire_drawing_machine_48v.png"))
    machine((52, 102, 176)).save(os.path.join(out, "wire_drawing_machine_230v.png"))
    variable(Image.open(os.path.join(out, "dc_dc_converter.png")).convert("RGBA")).save(os.path.join(out, "variable_dc_dc_converter.png"))
    print("wire drawing machines, variable converter")
