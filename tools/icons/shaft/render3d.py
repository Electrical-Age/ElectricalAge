#!/usr/bin/env python3
"""Small z-buffered software renderer for EA Obj3D OBJs (numpy), used by render_icons.py.

load_parts(path, parts, xforms) -> faces [(part, verts3, uvs3)], tex image (RGB numpy)
render(faces, tex, view, size) -> dict of buffers: rgb (HxWx3 float 0..1, lit), base (unlit texture colour),
    mask, depth, normal id, part id, shade class (0 top, 1 left face, 2 right face, 3 other).
Same OBJ reading rules as tools/models/elncheck.py (single-space split, v flipped, triangles only).
"""
import math, os
import numpy as np
from PIL import Image


def load_parts(path, parts=None, xforms=None):
    """PARTS: list of part names to keep (None = all). XFORMS: {part: [fn(v)->v, ...]}: extra copies of a part,
    each copy's vertices mapped through fn (the original is kept only if 'self' is in parts)."""
    vs, vts, faces, cur, mtllib = [], [], [], None, None
    for line in open(path):
        line = line.rstrip("\n")
        w = line.split(" ")
        if w[0] == "o":
            cur = w[1]
        elif w[0] == "v":
            vs.append(tuple(map(float, w[1:4])))
        elif w[0] == "vt":
            vts.append((float(w[1]), 1 - float(w[2])))
        elif w[0] == "mtllib":
            mtllib = w[1]
        elif w[0] == "f":
            if len(w) != 4:
                continue
            ids = [x.split("/") for x in w[1:]]
            V = [vs[int(i[0]) - 1] for i in ids]
            T = [vts[int(i[1]) - 1] if len(i) > 1 and i[1] else (0, 0) for i in ids]
            if parts is None or cur in parts:
                faces.append((cur, V, T))
            for fn in (xforms or {}).get(cur, []):
                faces.append((cur, [fn(p) for p in V], T))
    d = os.path.dirname(path)
    tex = None
    for line in open(os.path.join(d, mtllib)):
        w = line.split()
        if w and w[0] == "map_Kd":
            tex = np.asarray(Image.open(os.path.join(d, w[1].lower())).convert("RGB"), dtype=np.float32) / 255.0
    return faces, tex


def rot_y180(p):
    return (-p[0], p[1], -p[2])


def _n(a):
    a = np.asarray(a, dtype=np.float64)
    l = np.linalg.norm(a)
    return a / l if l > 1e-12 else a


def view_basis(d, up=(0, 1, 0)):
    """Orthographic basis (view dir, screen right, screen up) for view direction D, or an oblique ('cabinet') basis
    for D = ('oblique', a): the front (x-) face full on, screen right = z+, depth x drawn up-right at A px per px."""
    if isinstance(d, tuple) and d and d[0] == "oblique":
        a = d[1]
        return np.array([1.0, -a, -a]), np.array([a, 0.0, 1.0]), np.array([a, 1.0, 0.0])
    d = _n(d)
    r = _n(np.cross(d, up))
    u = np.cross(r, d)
    return d, r, u


def render(faces, tex, view=(1, -0.8, 1), size=256, margin=0.0, light=None, scale=None, centre=None, flat=False,
           levels=None):
    """Orthographic render looking along VIEW. Fits the model's projected bounds to SIZE (or uses SCALE/CENTRE).
    FLAT: each triangle takes the mean texture colour of its UV box (no texture noise). LEVELS: quantise the light
    factor to these steps (pixel-art banding)."""
    d, r, u = view_basis(view)
    if light is None:  # top-left of the screen, a little towards the viewer
        light = _n(0.9 * u - 0.55 * r - 0.35 * d)
    light = _n(light)
    pts = np.array([p for _, V, _ in faces for p in V])
    sx = pts @ r
    sy = pts @ u
    if scale is None:
        span = max(sx.max() - sx.min(), sy.max() - sy.min())
        scale = size * (1 - 2 * margin) / span
        centre = ((sx.max() + sx.min()) / 2, (sy.max() + sy.min()) / 2)
    cx, cy = centre
    H = W = size
    rgb = np.zeros((H, W, 3), np.float32)
    base = np.zeros((H, W, 3), np.float32)
    zb = np.full((H, W), np.inf)
    nid = np.full((H, W), -1, np.int32)
    pid = np.full((H, W), -1, np.int32)
    kbuf = np.zeros((H, W), np.float32)
    lbuf = np.zeros((H, W), np.float32)       # raw n . light (-1..1)
    mbuf = np.full((H, W), -1, np.int32)      # atlas cell index (row * 4 + col) at the triangle's UV centroid
    partnames = []
    normals = []
    th, tw = tex.shape[:2]
    gy, gx = np.mgrid[0:H, 0:W]
    for part, V, T in faces:
        V = np.array(V)
        n = np.cross(V[1] - V[0], V[2] - V[0])
        ln = np.linalg.norm(n)
        if ln < 1e-12:
            continue
        n = n / ln
        if n @ d >= -1e-9:
            continue
        if part not in partnames:
            partnames.append(part)
        pi = partnames.index(part)
        # quantised normal id (so coplanar faces share an id)
        key = tuple(np.round(n, 2))
        if key not in normals:
            normals.append(key)
        ni = normals.index(key)
        lam = float(n @ light)
        k = 0.45 + 0.55 * max(0.0, lam)
        if levels:
            k = min(levels, key=lambda q: abs(q - k))
        X = W / 2 + scale * (V @ r - cx)
        Y = H / 2 - scale * (V @ u - cy)
        Z = V @ d
        x0, x1 = max(0, int(math.floor(X.min()))), min(W - 1, int(math.ceil(X.max())))
        y0, y1 = max(0, int(math.floor(Y.min()))), min(H - 1, int(math.ceil(Y.max())))
        if x1 < x0 or y1 < y0:
            continue
        den = (Y[1] - Y[2]) * (X[0] - X[2]) + (X[2] - X[1]) * (Y[0] - Y[2])
        if abs(den) < 1e-9:
            continue
        px = gx[y0:y1 + 1, x0:x1 + 1] + 0.5
        py = gy[y0:y1 + 1, x0:x1 + 1] + 0.5
        w0 = ((Y[1] - Y[2]) * (px - X[2]) + (X[2] - X[1]) * (py - Y[2])) / den
        w1 = ((Y[2] - Y[0]) * (px - X[2]) + (X[0] - X[2]) * (py - Y[2])) / den
        w2 = 1 - w0 - w1
        m = (w0 >= -1e-6) & (w1 >= -1e-6) & (w2 >= -1e-6)
        if not m.any():
            continue
        z = w0 * Z[0] + w1 * Z[1] + w2 * Z[2]
        sub = zb[y0:y1 + 1, x0:x1 + 1]
        m &= z < sub
        if not m.any():
            continue
        T = np.array(T)
        cu, cv = T[:, 0].mean(), T[:, 1].mean()
        cell = min(int(cv * th / 16), th // 16 - 1) * (tw // 16) + min(int(cu * tw / 16), tw // 16 - 1)
        tu = w0 * T[0, 0] + w1 * T[1, 0] + w2 * T[2, 0]
        tv = w0 * T[0, 1] + w1 * T[1, 1] + w2 * T[2, 1]
        iu = np.clip((tu * tw).astype(int), 0, tw - 1)
        iv = np.clip((tv * th).astype(int), 0, th - 1)
        col = tex[iv, iu]
        if flat:
            a0, a1 = int(T[:, 0].min() * tw), int(math.ceil(T[:, 0].max() * tw))
            b0, b1 = int(T[:, 1].min() * th), int(math.ceil(T[:, 1].max() * th))
            region = tex[max(0, b0):max(b0 + 1, b1), max(0, a0):max(a0 + 1, a1)]
            col = np.broadcast_to(region.reshape(-1, 3).mean(0), col.shape)
        sub[m] = z[m]
        b = base[y0:y1 + 1, x0:x1 + 1]
        b[m] = col[m]
        rgb[y0:y1 + 1, x0:x1 + 1][m] = col[m] * k
        nid[y0:y1 + 1, x0:x1 + 1][m] = ni
        pid[y0:y1 + 1, x0:x1 + 1][m] = pi
        kbuf[y0:y1 + 1, x0:x1 + 1][m] = k
        lbuf[y0:y1 + 1, x0:x1 + 1][m] = lam
        mbuf[y0:y1 + 1, x0:x1 + 1][m] = cell
    mask = np.isfinite(zb)
    return dict(rgb=rgb, base=base, mask=mask, depth=np.where(mask, zb, 0), nid=nid, pid=pid, k=kbuf, lam=lbuf, mat=mbuf,
                parts=partnames, normals=normals, scale=scale)


def to_image(buf, bg=(40, 44, 52)):
    out = np.zeros(buf["rgb"].shape, np.uint8)
    out[:] = bg
    out[buf["mask"]] = np.clip(buf["rgb"][buf["mask"]] * 255, 0, 255).astype(np.uint8)
    return Image.fromarray(out)
