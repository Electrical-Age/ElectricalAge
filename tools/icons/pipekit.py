#!/usr/bin/env python3
"""Pipe kit icons (16x16): a bundle of three pipe lengths tied with a strap, iron (light grey) and alloy (dark blue-grey).
Usage: pipekit.py RESOURCES_DIR -> assets/eln/textures/item/pipe_kit_{iron,alloy}.png"""
import os, sys
from PIL import Image

TIERS = {"iron": ((214, 214, 214), (150, 150, 155), (95, 95, 100)),
         "alloy": ((128, 136, 150), (88, 94, 108), (52, 56, 66))}

def icon(hi, mid, lo):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for k, (y0, x0, x1) in enumerate(((3, 1, 13), (7, 2, 14), (11, 1, 13))):   # three pipes, slightly staggered
        for x in range(x0, x1 + 1):
            px[x, y0] = hi
            px[x, y0 + 1] = mid
            px[x, y0 + 2] = lo
        for y in range(y0, y0 + 3):                                           # open ends (bore)
            px[x1 + 1, y] = lo
        px[x1 + 1, y0 + 1] = (30, 30, 34, 255)
    for y in range(2, 15):                                                    # strap
        for x in (6, 7):
            px[x, y] = (140, 90, 40, 255) if x == 6 else (110, 70, 30, 255)
    return im

def valve():
    """A gate valve: a pipe stub through a body, a stem and a red handwheel on top."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    hi, mid, lo = (214, 214, 214, 255), (150, 150, 155, 255), (95, 95, 100, 255)
    for x in range(1, 15):
        px[x, 10], px[x, 11], px[x, 12] = hi, mid, lo
    for x in range(5, 11):
        for y in range(8, 15):
            px[x, y] = mid if 5 < x < 10 else lo
    for y in range(4, 8):
        px[7, y], px[8, y] = lo, mid
    for x in range(3, 13):
        px[x, 3] = (190, 40, 30, 255)
        px[x, 4] = (130, 25, 20, 255)
    return im

def safety_valve():
    """A pipe stub with a brass body standing on it and a red cap."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for x in range(1, 15):
        px[x, 12], px[x, 13], px[x, 14] = (150, 150, 155, 255), (112, 112, 118, 255), (80, 80, 86, 255)
    for y in range(4, 12):
        for x in range(6, 10):
            px[x, y] = (205, 160, 80, 255) if x < 8 else (150, 110, 50, 255)
    for x in range(5, 11):
        px[x, 2], px[x, 3] = (200, 45, 35, 255), (140, 30, 25, 255)
    return im

def adapter():
    """A brass flange plate with a short pipe stub."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for y in range(3, 13):
        for x in (2, 3):
            px[x, y] = (205, 160, 80, 255) if x == 2 else (150, 110, 50, 255)
    for x in range(4, 14):
        px[x, 6], px[x, 7], px[x, 8], px[x, 9] = (214, 214, 214, 255), (170, 170, 175, 255), (150, 150, 155, 255), (95, 95, 100, 255)
    return im

def pipe_texture(base, seed):
    """16x16 metal: the base colour with faint streaks along v (the pipe's length) and a little noise."""
    import random
    rnd = random.Random(seed)
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    streak = [rnd.randint(-6, 6) for _ in range(16)]
    for x in range(16):
        for y in range(16):
            d = streak[x] + rnd.randint(-4, 4)
            px[x, y] = tuple(max(0, min(255, c + d)) for c in base) + (255,)
    return im

def main():
    d = os.path.join(sys.argv[1], "assets/eln/textures/item")
    os.makedirs(d, exist_ok=True)
    for name, cols in TIERS.items():
        icon(*[c + (255,) for c in cols]).save(os.path.join(d, f"pipe_kit_{name}.png"))
    valve().save(os.path.join(d, "pipe_valve.png"))
    adapter().save(os.path.join(d, "pipe_adapter.png"))
    safety_valve().save(os.path.join(d, "pipe_safety_valve.png"))
    b = os.path.join(sys.argv[1], "assets/eln/textures/block")
    os.makedirs(b, exist_ok=True)
    pipe_texture((112, 112, 118), 1).save(os.path.join(b, "pipe_iron.png"))     # wrought iron: dark grey
    pipe_texture((84, 96, 112), 2).save(os.path.join(b, "pipe_alloy.png"))      # alloy steel: blue-grey

if __name__ == "__main__":
    main()
