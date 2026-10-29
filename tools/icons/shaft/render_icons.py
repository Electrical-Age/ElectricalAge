#!/usr/bin/env python3
"""Shaft-family devices: the automatic pass. Renders each device's OBJ (models.py) in the house cabinet oblique and
reduces it to 16x16 by material (atlas cell -> ramp) and shade (n . light -> ramp step), with despeckling and the
vanilla outline. The result is too noisy to ship at 16 px, so it is only a layout reference: the shipped icons are
drawn by draw_devices.py. Also writes the hi-res oblique reference sheet.

  nix-shell -p 'python3.withPackages(p:[p.pillow p.numpy])' --run 'python3 tools/icons/shaft/render_icons.py [ids...]'
writes notes/ea/shaft/p5-previews/auto/<id>.png and icons_ref_oblique.png (env SHAFT_PREVIEWS overrides the dir).
Reduction: render at 16*K px, quantise the render to a small palette (median cut), take each 16 px cell's majority
colour where the cell is at least COVER covered, then outline the silhouette (top/left edge: shade 0.6, bottom/right
edge: shade 0.38 of the pixel's colour) and darken internal depth steps.
"""
import os, sys
import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from render3d import render
from models import DEVICES, REPO, load
sys.path.insert(0, os.path.join(REPO, "tools/icons/vanilla"))

PREV = os.environ.get("SHAFT_PREVIEWS", "/var/lib/mclab/agent/notes/ea/shaft/p5-previews")
OUT = os.path.join(PREV, "auto")
K = 16                      # supersampling per icon pixel
VIEW = ("oblique", 0.4)     # house oblique: front (x-) face full on, top and right (z+) side receding up-right
LIGHT = (-0.5, 1.0, -0.3)   # top -> ramp 4, front -> 3, right side -> 2

# per icon: view, body box (cols x0..x1, rows y0..y1 inclusive), coverage threshold, colours, saturation boost
CFG = {
    "default": dict(view=VIEW, box=(1, 1, 14, 14), cover=0.45, edges=0.06, maxlev=4,
                    small=("brass", "rivet", "darksteel")),
    "generator": dict(box=(3, 3, 13, 14)),             # the 4 kW is drawn smaller than the 8 kW
    "8kw_generator": dict(box=(1, 1, 14, 14)),
    "joint": dict(box=(1, 2, 14, 14)),
}

# hand touches: id -> list of (x, y, (r, g, b) or None to clear)
TOUCH = {}


def cfg(item):
    c = dict(CFG["default"])
    c.update(CFG.get(item, {}))
    return c


def lum(c):
    return 0.299 * c[..., 0] + 0.587 * c[..., 1] + 0.114 * c[..., 2]


def boost(rgb, sat, gain):
    g = lum(rgb)[..., None]
    return np.clip((g + (rgb - g) * sat) * gain, 0, 1)


def ramp_from(base, lift=1.0):
    """A 6-step ramp (0 dark outline .. 5 bevel) around BASE (= step 3, the front face); darks drift cool, lights warm."""
    b = np.asarray(base[:3], np.float32) * lift
    steps = []
    for f, tint in ((0.36, (-6, -4, 8)), (0.56, (-4, -2, 6)), (0.78, (-2, 0, 3)), (1.0, (0, 0, 0)),
                    (1.22, (6, 4, -4)), (1.42, (10, 8, -6))):
        c = b * f + np.array(tint) if f <= 1 else b + (255 - b) * (f - 1) * 0.9 + np.array(tint)
        steps.append(tuple(int(v) for v in np.clip(c, 22, 255)))
    return steps


def pal5(r):
    r = [tuple(c[:3]) for c in r]
    return r + [r[-1]] if len(r) == 5 else r


CELL = {"steel": 0, "darksteel": 1, "castiron": 2, "copper": 3, "brass": 4, "paint": 5, "glass": 6, "rubber": 7,
        "ceramic": 8, "coil": 9, "hotcopper": 10, "rivet": 11, "water": 12, "red": 13, "froth": 14, "sand": 15,
        "louvre": 16, "oil": 17, "lamp": 18, "firebrick": 19}


def ramps(tex):
    """Ramp per atlas cell: shared vanilla-kit ramps where the material has one, else derived from the cell colour."""
    from kit import palette as P
    R = {}
    for name, i in CELL.items():
        col, row = i % 4, i // 4
        base = tex[row * 16:(row + 1) * 16, col * 16:(col + 1) * 16].reshape(-1, 3).mean(0) * 255
        R[i] = ramp_from(base)
    R[CELL["steel"]] = pal5(P.STEEL)
    R[CELL["brass"]] = pal5(P.GOLD)
    R[CELL["glass"]] = pal5(P.GLASS)
    R[CELL["rubber"]] = pal5(P.RUBBER_BLACK)
    R[CELL["copper"]] = pal5(P.VCOPPER)
    R[CELL["ceramic"]] = pal5(P.WHITE_PLASTIC)
    R[CELL["castiron"]] = ramp_from((104, 100, 96))
    R[CELL["darksteel"]] = ramp_from((80, 82, 90))
    R[CELL["rivet"]] = ramp_from((112, 118, 124))
    R[CELL["louvre"]] = ramp_from((70, 72, 78))
    return R


def level(lam):
    """Ramp step from n . light: top 4, front (screen-left) face 3, right side 2, undersides 1."""
    return np.select([lam > 0.9, lam > 0.5, lam > 0.0, lam > -0.65], [5, 4, 3, 2], 1)


def make(item):
    c = cfg(item)
    faces, tex = load(item)
    R = ramps(tex)
    R.update({CELL[k]: (ramp_from(v) if len(v) == 3 else v) for k, v in c.get("ramps", {}).items()})
    x0, y0, x1, y1 = c["box"]
    bw, bh = x1 - x0 + 1, y1 - y0 + 1
    probe = render(faces, tex, c["view"], 64, margin=0, light=LIGHT)
    ys, xs = np.nonzero(probe["mask"])
    pw, ph = (xs.max() - xs.min() + 1) / 64, (ys.max() - ys.min() + 1) / 64
    span_px = min(bw / pw, bh / ph)
    S = 16 * K
    buf = render(faces, tex, c["view"], S, light=LIGHT, scale=probe["scale"] * span_px * K / 64, centre=_centre(faces, c["view"]))
    mask = buf["mask"]
    ys, xs = np.nonzero(mask)
    w_px = (xs.max() - xs.min() + 1) / K
    h_px = (ys.max() - ys.min() + 1) / K
    ox = int(round((x0 + (bw - w_px) / 2) * K - xs.min()))
    oy = int(round((y1 + 1 - h_px) * K - ys.min()))
    mat = np.roll(np.roll(np.where(mask, buf["mat"], -1), oy, 0), ox, 1)
    lev = np.roll(np.roll(level(buf["lam"]), oy, 0), ox, 1)
    dep = np.roll(np.roll(buf["depth"], oy, 0), ox, 1)
    lam = np.roll(np.roll(buf["lam"], oy, 0), ox, 1)
    cm = mat >= 0
    M = np.full((16, 16), -1)
    Lv = np.zeros((16, 16), int)
    D = np.zeros((16, 16))
    inner = np.ones((K, K))
    inner[K // 4:3 * K // 4, K // 4:3 * K // 4] = 2.5
    small = set(CELL[k] for k in c["small"])
    for j in range(16):
        for i in range(16):
            sl = np.s_[j * K:(j + 1) * K, i * K:(i + 1) * K]
            cmask = cm[sl]
            cov = cmask.mean()
            if cov < c["cover"]:
                continue
            w = inner[cmask]
            mats = mat[sl][cmask]
            wt = np.bincount(mats, weights=w, minlength=20)
            frac = np.bincount(mats, minlength=20) / (K * K)
            cand = [m for m in range(20) if frac[m] > 0 and frac[m] >= 0.25 * cov and (m not in small or frac[m] > 0.7 * cov)]
            if c.get("keep_paint", True) and frac[CELL["paint"]] >= 0.3 * cov and CELL["paint"] not in cand:
                cand.append(CELL["paint"])
            mm = max(cand, key=lambda m: wt[m]) if cand else int(np.argmax(wt))
            sel = mats == mm
            M[j, i] = mm
            Lv[j, i] = int(level(np.median(lam[sl][cmask][sel])))
            D[j, i] = np.median(dep[sl][cmask])
    M, Lv = despeckle(M, Lv, CELL["paint"])
    out = np.zeros((16, 16, 4), np.uint8)
    for j in range(16):
        for i in range(16):
            if M[j, i] >= 0:
                out[j, i, :3] = R[M[j, i]][min(Lv[j, i], c["maxlev"])]
                out[j, i, 3] = 255
    return outline(out, M, D, R, c["edges"]), c


def nb4(A, y, x):
    return [A[y + dy, x + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if 0 <= x + dx < 16 and 0 <= y + dy < 16]


def despeckle(M, Lv, keep):
    """Silhouette: drop pixels with <= 1 filled neighbour, fill holes with >= 3. Material and shade: a pixel unlike
    3+ agreeing neighbours takes theirs (never overwriting the paint colour)."""
    M, Lv = M.copy(), Lv.copy()
    for y in range(16):
        for x in range(16):
            n = nb4(M, y, x)
            f = sum(1 for v in n if v >= 0)
            if M[y, x] >= 0 and f <= 1:
                M[y, x] = -1
            elif M[y, x] < 0 and f >= 3:
                vals = [v for v in n if v >= 0]
                M[y, x] = max(set(vals), key=vals.count)
                ls = [l for l, v in zip(nb4(Lv, y, x), n) if v >= 0]
                Lv[y, x] = max(set(ls), key=ls.count)
    for A, guard in ((M, True), (Lv, False)):
        B = A.copy()
        for y in range(16):
            for x in range(16):
                if M[y, x] < 0 or (guard and M[y, x] == keep):
                    continue
                n = [v for v, m in zip(nb4(A, y, x), nb4(M, y, x)) if m >= 0]
                for v in set(n):
                    if v != A[y, x] and n.count(v) >= 3:
                        B[y, x] = v
        A[:] = B
    return M, Lv


def _centre(faces, view):
    from render3d import view_basis
    d, r, u = view_basis(view)
    pts = np.array([p for _, V, _ in faces for p in V])
    sx, sy = pts @ r, pts @ u
    return ((sx.max() + sx.min()) / 2, (sy.max() + sy.min()) / 2)


def outline(img, M, D, R, edge_step):
    """Silhouette: ramp[1] where open to the left/top, ramp[0] where open to the right/bottom (vanilla rule).
    Inside: a pixel well behind the pixel above or to its left (a depth step) of the same material gets ramp[1]."""
    a = M >= 0
    res = img.copy()
    def open_(x, y):
        return not (0 <= x < 16 and 0 <= y < 16) or not a[y, x]
    for y in range(16):
        for x in range(16):
            if not a[y, x]:
                continue
            r = R[M[y, x]]
            L, Rt, U, Dn = open_(x - 1, y), open_(x + 1, y), open_(x, y - 1), open_(x, y + 1)
            if L:
                res[y, x, :3] = r[1]
            elif Rt or Dn:
                res[y, x, :3] = r[0]
            elif U:
                res[y, x, :3] = r[1]
            elif edge_step:
                for dx, dy in ((-1, 0), (0, -1)):
                    if a[y + dy, x + dx] and D[y, x] - D[y + dy, x + dx] > edge_step:
                        res[y, x, :3] = r[1]
    return res


def write(item):
    img, c = make(item)
    for x, y, col in TOUCH.get(item, []):
        if col is None:
            img[y, x] = 0
        else:
            img[y, x] = (*col, 255)
    im = Image.fromarray(img, "RGBA")
    im.save(os.path.join(OUT, item + ".png"))
    from kit.check import lint
    print("wrote", item, "colours", len({tuple(p) for p in img.reshape(-1, 4) if p[3]}), lint(im))


def ref_sheet(path, S=192):
    """Every device's oblique render at S px (the drawing reference)."""
    from PIL import ImageDraw
    from render3d import to_image
    ids = list(DEVICES)
    sh = Image.new("RGB", (S * 5, S * ((len(ids) + 4) // 5)), (40, 44, 52))
    for i, it in enumerate(ids):
        f, t = load(it)
        im = to_image(render(f, t, VIEW, S, margin=0.04, light=LIGHT))
        x, y = (i % 5) * S, (i // 5) * S
        sh.paste(im, (x, y))
        ImageDraw.Draw(sh).text((x + 3, y + 3), it, fill=(255, 255, 255))
    sh.save(path)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for it in sys.argv[1:] or list(DEVICES):
        write(it)
    ref_sheet(os.path.join(PREV, "icons_ref_oblique.png"))
