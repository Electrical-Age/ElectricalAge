#!/usr/bin/env python3
"""Writes the transformer coils' winding sprites (assets/eln/sprites/winding/*.png), stdlib only.

Usage: python3 -I tools/winding_sprites.py OUTDIR

They are greyscale: the coil is tinted by its vertex colour (WindingTexture.tint), so one set serves every wire
colour and the sprites can sit in the block atlas (ModelSprites), where the chunk mesh can use them.
  turn.png          16x32, one turn of a coil's side: shade 1.12 at its top falling to 0.72, a dark line at its foot
                    (one row of 32: about what one texel of the painted 256-row texture was on a coil of 8 turns a layer)
  end_N.png         32x16, N = 1..10 rings of a coil's end (one a layer): shade 1.2 falling to 0.8, a dark line
  turn_bright.png, end_bright_N.png   the same for bright wire (a channel over 212, which can't be tinted up by 1.2)
  turn_dark.png, end_dark_N.png       the same for dark wire (black): the line is a highlight instead
White (255) stands for shade SCALE = 1.2; in the bright set for shade 1 (brighter is cut off, as a painted texture's
channel was at 255); the dark set's white is the highlight line and its body is DARK_BODY of it.
WindingTextureTest checks the files against the same formulas in WindingTexture: change both together.
"""
import struct, sys, zlib, os

SCALE = 1.2
DARK_BODY = 0.4


NORMAL, BRIGHT, DARK = 0, 1, 2
NAMES = ["", "_bright", "_dark"]


def px(shade, line, kind):
    if kind == DARK:
        return 255 if line else min(255, int(255 * shade * DARK_BODY + 0.5))
    if kind == BRIGHT:
        return min(255, int(255 * shade + 0.5))
    return min(255, int(255 * shade / SCALE + 0.5))


def turn(kind):
    rows = []
    for y in range(32):
        line = y == 31
        t = (y + 0.5) / 32
        rows.append([px(0.45 if line else 1.12 - 0.4 * t, line, kind)] * 16)
    return rows


def end(n, kind):
    row = []
    for x in range(32):
        k = x * n // 32
        x0, x1 = k * 32 // n, (k + 1) * 32 // n
        t = (x - x0 + 0.5) / max(1, x1 - x0)
        line = x1 - x0 >= 3 and x == x1 - 1
        row.append(px(0.5 if line else 1.2 - 0.4 * t, line, kind))
    return [row] * 16


def png(path, rows):
    h, w = len(rows), len(rows[0])
    raw = b"".join(b"\0" + b"".join(bytes((v, v, v, 255)) for v in r) for r in rows)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


out = sys.argv[1]
for kind in (NORMAL, BRIGHT, DARK):
    png(os.path.join(out, "turn%s.png" % NAMES[kind]), turn(kind))
    for n in range(1, 11):
        png(os.path.join(out, "end%s_%d.png" % (NAMES[kind], n)), end(n, kind))
