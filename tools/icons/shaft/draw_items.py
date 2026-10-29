#!/usr/bin/env python3
"""Shaft-family non-placeable icons (16 px), hand pixel art:
  turbine_blade_set     a turbine blade ring (steel blades round a dark hub), vanilla-kit outline and ramps
  pipe_kit_large_alloy  pipe_kit_alloy's look (flat side view, its palette and strap), but fat pipes with a big
                        bore showing at the end and a flange
  pipe_quality_sensor   pipe_pressure_sensor's frame (round head on the pipe plate) with a sight glass and a
                        water droplet instead of the white dial
  pipe_pressure_valve   pipe_valve's body and pipe stubs, with a spring bonnet (coils between two posts) under a
                        green adjusting cap instead of the handwheel bar

  nix-shell -p 'python3.withPackages(p:[p.pillow])' --run 'python3 tools/icons/shaft/draw_items.py'
"""
import math, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
sys.path.insert(0, os.path.join(REPO, "tools/icons/vanilla"))
from PIL import Image  # noqa: E402
from kit import Canvas, outline_region, disc, STEEL, h  # noqa: E402
from kit.check import lint  # noqa: E402

OUT = os.path.join(REPO, "src/main/resources/assets/eln/textures/item")


def grid(rows, pal):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, r in enumerate(rows):
        assert len(r) == 16, (y, r, len(r))
        for x, ch in enumerate(r):
            if ch != ".":
                im.putpixel((x, y), h(pal[ch]))
    return im


# ------------------------------------------------------------------ turbine blade set
DARK = (h('#22232a'), h('#2c2e35'), h('#3d4048'), h('#555861'), h('#737680'))


def turbine_blade_set():
    """A turbine blade stage seen along its axis: the steel shroud ring, a row of slanted blades (lit faces, dark
    gaps between them) and the dark hub."""
    from kit import CAV
    cv = Canvas(16)
    cx, cy = 7.5, 7.5
    reg = {}
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.4:
                continue
            a = math.atan2(dy, dx)
            lit = math.cos(a - math.radians(225))
            if r > 5.6:                                   # shroud
                reg[(x, y)] = STEEL[4] if lit > 0.3 else STEEL[3] if lit > -0.4 else STEEL[2]
            elif r > 2.4:                                 # blades: 10 slanted vanes
                f = ((a + 0.35 * (r - 2.4)) / (2 * math.pi) * 10) % 1.0
                reg[(x, y)] = (STEEL[4] if f < 0.3 else STEEL[3]) if f < 0.58 else CAV[1]
            else:
                reg[(x, y)] = DARK[3] if lit > 0 else DARK[2]
    for p, c in reg.items():
        cv.set(*p, c)
    outline_region(cv, set(reg), STEEL[0], STEEL[1])
    cv.set(7, 7, DARK[4]); cv.set(8, 8, DARK[1])
    return cv.image()


# ------------------------------------------------------------------ pipes (flat side-view family)
PIPE = {'H': '#a6adba', 'c': '#808896', 'e': '#585e6c', 'd': '#343842', 'f': '#1e1e22',
        'a': '#8c5a28', 'b': '#6e461e', 'g': '#9aa1ae'}
PIPE['f'] = '#24242a'


def pipe_kit_large_alloy():
    """Two fat alloy pipe lengths (5 px bore against pipe_kit_alloy's 3) with a flange at the left end and the
    open bore at the right end, tied with the same brown strap."""
    rows = [
        "................",
        ".ce.....ab......",
        ".cHcccccabcccce.",
        ".cHHHHHHabHHHcfe",
        ".cccccccabcccfff",
        ".ceeeeeeabeeecfd",
        ".cddddddabdddde.",
        ".dd.....ab......",
        "..ce....ab......",
        "..cHccccabccccce",
        "..cHHHHHabHHHHcf",
        "..cccccccbccccff",
        "..ceeeeeabeeeecf",
        "..cdddddabddddde",
        "..dd....ab......",
        "................",
    ]
    return grid(rows, PIPE)


def pipe_quality_sensor():
    """pipe_pressure_sensor's round head and pipe plate, but the head is a sight glass (pale blue) with a blue
    water droplet in it."""
    pal = {'a': '#28282c', 'G': '#b4d6e2', 'L': '#e2f1f5', 'S': '#86b4c8', 'w': '#2f6fc0', 'W': '#1d4a8a',
           'h': '#9fd0f0', 'g': '#3c3c40', 'H': '#96969b', 'i': '#6e6e73'}
    rows = [
        "................",
        "......aaaa......",
        "....aaLLGGaa....",
        "...aLLGGwGGGa...",
        "..aLGGGwwGGGGa..",
        "..aLGGwwwwGGSa..",
        "..aGGGwhwwGGSa..",
        "..aGGwhwwwwGSa..",
        "..aGGwwwwwWGSa..",
        "...aGGwwwWGSa...",
        "....aaSGSSaa....",
        "......aaaa......",
        "..ggggghhggggg..".replace('h', 'H'),
        "..giiiiiiiiiig..",
        "..giiiiiiiiiig..",
        "..gggggggggggg..",
    ]
    return grid(rows, pal)


def pipe_pressure_valve():
    """pipe_valve's body and pipe stubs; on top a spring bonnet (light coils between two dark posts) and a green
    adjusting cap with its screw."""
    pal = {'c': '#5f5f64', 'd': '#96969b', 'e': '#d6d6d6', 's': '#c0c0c4', 'k': '#3c3c40',
           'g': '#3caa3c', 'G': '#1f6e28', 'l': '#7fd27f'}
    rows = [
        ".......dc.......",
        ".....lggggg.....",
        ".....GGGGGG.....",
        ".....cddddc.....",
        ".....ceeeec.....",
        ".....ckkkkc.....",
        ".....ceeeec.....",
        ".....ckkkkc.....",
        ".....cddddc.....",
        ".....cddddc.....",
        ".eeeecddddceeee.",
        ".ddddcddddcdddd.",
        ".cccccddddccccc.",
        ".....cddddc.....",
        ".....cddddc.....",
        "................",
    ]
    return grid(rows, pal)


DRAW = {
    "turbine_blade_set": turbine_blade_set,
    "pipe_kit_large_alloy": pipe_kit_large_alloy,
    "pipe_quality_sensor": pipe_quality_sensor,
    "pipe_pressure_valve": pipe_pressure_valve,
}

if __name__ == "__main__":
    for it in sys.argv[1:] or list(DRAW):
        im = DRAW[it]()
        im.save(os.path.join(OUT, it + ".png"))
        print(it, lint(im))
