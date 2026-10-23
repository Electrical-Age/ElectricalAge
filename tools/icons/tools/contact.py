#!/usr/bin/env python3
"""Contact sheet for item icons, as they look in a Minecraft inventory.

Usage:
    python3 contact.py OUT.png DIR [ids...] [--zoom N] [--cols 9] [--cvd]

DIR holds <item_id>.png files (16x16 or 32x32; any square size works).  With no ids,
every PNG in DIR is used (sorted).  An id may also be a path to a PNG.

Sections, top to bottom:
  1. inventory panel at GUI scale 2 (vanilla #c6c6c6 panel, 18-GUI-px bevelled slots)
  2. inventory panel at GUI scale 3, item ids as small labels under each slot
  3. dark panel at GUI scale 3 (tooltip / JEI / dark-theme check)
  4. (--zoom N, default 8, 0 = off) every icon magnified on a checkerboard, so the pixel
     grid can be inspected; a 32px icon is shown at the same physical size as a 16px one
     (N/2 per pixel), exactly as the game shows it.

  5. (--cvd) the GUI-2 panel again as seen with protanopia, deuteranopia and tritanopia
     (Machado 2009, full severity) - use it to check that tier siblings stay apart.

Icons are wrapped in rows of `--cols` (default 9, like a real inventory).  An icon of
size S is drawn at 16*gs screen pixels with nearest-neighbour scaling, as the game does
(so a 32px icon at GUI 3 gets the same uneven 1.5x pixels it gets in game).

Labels need a TTF: $(fc-match -f '%{file}' sans) is used automatically.
"""
import os
import subprocess
import sys

from PIL import Image, ImageDraw, ImageFont

PANEL = (198, 198, 198, 255)
SLOT = (139, 139, 139, 255)
SLOT_DARK = (55, 55, 55, 255)
SLOT_LIGHT = (255, 255, 255, 255)
DARK_BG = (30, 30, 36, 255)
SHEET_BG = (68, 68, 68, 255)


def _font(size):
    try:
        path = subprocess.run(['fc-match', '-f', '%{file}', 'sans'], capture_output=True,
                              text=True, timeout=10).stdout.strip()
        if path:
            return ImageFont.truetype(path, size)
    except Exception:
        pass
    return ImageFont.load_default()


def _load(d, ident):
    p = ident if ident.endswith('.png') and os.path.exists(ident) else os.path.join(d, ident + '.png')
    name = os.path.splitext(os.path.basename(p))[0]
    try:
        return name, Image.open(p).convert('RGBA')
    except Exception as e:  # missing icon -> magenta placeholder, keep going
        print(f'contact: cannot load {p}: {e}', file=sys.stderr)
        im = Image.new('RGBA', (16, 16), (255, 0, 255, 255))
        return name, im


def _icon_at(im, px):
    return im.resize((px, px), Image.NEAREST)


def _wrap(text, font, width):
    """Wrap an item id at '_' (or anywhere if a token is too long) to fit `width` px."""
    lines, cur = [], ''
    for tok in text.replace('_', '_\x00').split('\x00'):
        cand = cur + tok
        if font.getlength(cand) <= width or not cur:
            cur = cand
            while font.getlength(cur) > width and len(cur) > 1:   # hard split
                k = len(cur)
                while k > 1 and font.getlength(cur[:k]) > width:
                    k -= 1
                lines.append(cur[:k]); cur = cur[k:]
        else:
            lines.append(cur); cur = tok
    if cur:
        lines.append(cur)
    return lines


def inventory(icons, gs, cols, dark=False, labels=False, font=None):
    rows = (len(icons) + cols - 1) // cols
    slot = 18 * gs
    lab_h = 0
    if labels:
        lh = font.size + 1
        nl = max(len(_wrap(n, font, slot - 2)) for n, _ in icons)
        lab_h = nl * lh + 2 * gs
    pad = 7 * gs
    W = 2 * pad + cols * slot
    H = 2 * pad + rows * (slot + lab_h)
    im = Image.new('RGBA', (W, H), DARK_BG if dark else PANEL)
    dr = ImageDraw.Draw(im)
    for i, (name, ic) in enumerate(icons):
        cx, cy = i % cols, i // cols
        sx, sy = pad + cx * slot, pad + cy * (slot + lab_h)
        if not dark:
            dr.rectangle([sx, sy, sx + slot - 1, sy + slot - 1], fill=SLOT)
            dr.rectangle([sx, sy, sx + slot - 1, sy + gs - 1], fill=SLOT_DARK)
            dr.rectangle([sx, sy, sx + gs - 1, sy + slot - 1], fill=SLOT_DARK)
            dr.rectangle([sx, sy + slot - gs, sx + slot - 1, sy + slot - 1], fill=SLOT_LIGHT)
            dr.rectangle([sx + slot - gs, sy, sx + slot - 1, sy + slot - 1], fill=SLOT_LIGHT)
        im.alpha_composite(_icon_at(ic, 16 * gs), (sx + gs, sy + gs))
        if labels:
            ty = sy + slot + gs
            for ln in _wrap(name, font, slot - 2):
                tw = font.getlength(ln)
                dr.text((sx + (slot - tw) / 2, ty), ln, font=font, fill=(40, 40, 40, 255))
                ty += font.size + 1
    return im


def zoomrow(icons, zoom, cols, font):
    cell = 16 * zoom
    gap = 10
    rows = (len(icons) + cols - 1) // cols
    lh = font.size + 4
    W = gap + cols * (cell + gap)
    H = gap + rows * (cell + lh + gap)
    im = Image.new('RGBA', (W, H), SHEET_BG)
    dr = ImageDraw.Draw(im)
    for i, (name, ic) in enumerate(icons):
        x = gap + (i % cols) * (cell + gap)
        y = gap + (i // cols) * (cell + lh + gap)
        n = ic.width
        step = cell // n if cell % n == 0 else cell / n
        for yy in range(n):
            for xx in range(n):
                c = (154, 154, 154, 255) if (xx + yy) % 2 else (176, 176, 176, 255)
                dr.rectangle([x + int(xx * step), y + int(yy * step),
                              x + int((xx + 1) * step) - 1, y + int((yy + 1) * step) - 1], fill=c)
        im.alpha_composite(_icon_at(ic, cell), (x, y))
        lab = f'{name} ({ic.width}px)'
        while font.getlength(lab) > cell and len(lab) > 4:
            lab = lab[:-2]
        dr.text((x, y + cell + 2), lab, font=font, fill=(220, 220, 220, 255))
    return im


_MACHADO = {
    'protan': ((0.152286, 1.052583, -0.204868), (0.114503, 0.786281, 0.099216),
               (-0.003882, -0.048116, 1.051998)),
    'deutan': ((0.367322, 0.860646, -0.227968), (0.280085, 0.672501, 0.047413),
               (-0.011820, 0.042940, 0.968881)),
    'tritan': ((1.255528, -0.076749, -0.178779), (-0.078411, 0.930809, 0.147602),
               (0.004733, 0.691367, 0.303900)),
}


def cvd_image(im, mode):
    """``im`` as seen with full protan/deutan/tritan colour-vision deficiency."""
    lin = [(v / 255 / 12.92 if v <= 10.31 else ((v / 255 + 0.055) / 1.055) ** 2.4) for v in range(256)]

    def gam(v):
        v = min(1.0, max(0.0, v))
        return int(round((12.92 * v if v <= 0.0031308 else 1.055 * v ** (1 / 2.4) - 0.055) * 255))
    m = _MACHADO[mode]
    cache = {}
    out = im.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            c = px[x, y]
            if c[:3] not in cache:
                l3 = [lin[v] for v in c[:3]]
                cache[c[:3]] = tuple(gam(sum(m[i][j] * l3[j] for j in range(3))) for i in range(3))
            px[x, y] = cache[c[:3]] + (c[3],)
    return out


def main(argv):
    zoom, cols, rest, cvd = 8, 9, [], False
    it = iter(argv)
    for a in it:
        if a == '--zoom':
            zoom = int(next(it))
        elif a == '--cols':
            cols = int(next(it))
        elif a == '--cvd':
            cvd = True
        else:
            rest.append(a)
    if len(rest) < 2:
        print(__doc__)
        return 2
    out, d, ids = rest[0], rest[1], rest[2:]
    if not ids:
        ids = sorted(f[:-4] for f in os.listdir(d) if f.endswith('.png'))
    icons = [_load(d, i) for i in ids]
    if not icons:
        print('contact: no icons'); return 1
    cols = max(1, min(cols, len(icons)))
    font = _font(11)
    parts = [inventory(icons, 2, cols),
             inventory(icons, 3, cols, labels=True, font=font),
             inventory(icons, 3, cols, dark=True)]
    if zoom:
        parts.append(zoomrow(icons, zoom, cols, font))
    if cvd:
        g2 = parts[0]
        for mode in ('protan', 'deutan', 'tritan'):
            sim = cvd_image(g2, mode)
            ImageDraw.Draw(sim).text((4, 2), mode, font=font, fill=(40, 40, 40, 255))
            parts.append(sim)
    W = max(p.width for p in parts) + 16
    H = sum(p.height for p in parts) + 8 * (len(parts) + 1)
    sheet = Image.new('RGBA', (W, H), SHEET_BG)
    y = 8
    for p in parts:
        sheet.paste(p, (8, y)); y += p.height + 8
    sheet.save(out)
    print('contact: wrote', out, f'({len(icons)} icons)')
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
