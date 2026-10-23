#!/usr/bin/env python3
"""Check (and preview) an EA 1.12 node OBJ the way mods.eln.misc.Obj3D reads it.

Usage: elncheck.py MODEL.obj [...] [--part main] [--only PART] [--preview OUTDIR]
(--only: check/preview the faces of one part, e.g. to look at an old model without its helper parts)
Checks: lines split on single spaces; `o` before the first `v`; `usemtl` before the first `f`; triangles only (Obj3D
drops other faces); the MTL's map_Kd file exists; bounds within the centred block (-0.5..0.5) and touching the floor;
geometry on the front (x = -0.5) and back (x = +0.5) faces near the floor; UVs within 0..1; no degenerate triangles
(geometry or UV). Preview: orthographic views (front/back/sides/top + two isometric) with back faces culled
(normals from the winding, as Obj3D does) and simple Lambert shading, texture sampled at each triangle's UV centroid.
"""
import math, os, sys

def load(path, only=None):
    errs, parts, cur, mtllib, mats, faces = [], {}, None, None, None, []
    vs, vts = [], []
    for ln, line in enumerate(open(path), 1):
        line = line.rstrip("\n")
        w = line.split(" ")
        if w[0] == "o":
            cur = w[1]; parts[cur] = 0
        elif w[0] == "v":
            if cur is None: errs.append(f"{ln}: v before o")
            vs.append(tuple(map(float, w[1:4])))
        elif w[0] == "vt":
            vts.append((float(w[1]), 1 - float(w[2])))             # as Obj3D: v flipped (GL: v = 0 at the image top)
        elif w[0] == "usemtl":
            mats = w[1]
        elif w[0] == "f":
            if only and cur != only: continue
            if mats is None: errs.append(f"{ln}: f before usemtl")
            if len(w) != 4: errs.append(f"{ln}: face with {len(w) - 1} vertices (Obj3D drops it)"); continue
            ids = [x.split("/") for x in w[1:]]
            faces.append((mats, [vs[int(i[0]) - 1] for i in ids], [vts[int(i[1]) - 1] if len(i) > 1 and i[1] else None for i in ids]))
            parts[cur] += 1
        elif w[0] == "mtllib":
            mtllib = w[1]
        if line != line.strip() or "  " in line: errs.append(f"{ln}: stray spaces")
    tex = {}
    d = os.path.dirname(path)
    if mtllib is None or not os.path.exists(os.path.join(d, mtllib)):
        errs.append(f"mtl {mtllib} missing")
    else:
        m = None
        for line in open(os.path.join(d, mtllib)):
            w = line.split()
            if w and w[0] == "newmtl": m = w[1]
            if w and w[0] == "map_Kd":
                tex[m] = os.path.join(d, w[1].lower())
                if not os.path.exists(tex[m]): errs.append(f"texture {w[1]} missing")
    return parts, faces, tex, errs

def sub(a, b): return tuple(x - y for x, y in zip(a, b))
def cross(a, b): return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])
def nrm(a):
    l = math.sqrt(sum(x * x for x in a)); return tuple(x / l for x in a) if l > 1e-12 else None

def check(path, part, only=None):
    parts, faces, tex, errs = load(path, only)
    if part not in parts: errs.append(f"part '{part}' missing (parts: {list(parts)})")
    pts = [p for _, vs, _ in faces for p in vs]
    lo = [min(p[i] for p in pts) for i in range(3)]; hi = [max(p[i] for p in pts) for i in range(3)]
    if min(lo) < -0.5 - 1e-6 or max(hi) > 0.5 + 1e-6: errs.append(f"out of the block: {lo} {hi}")
    if abs(lo[1] + 0.5) > 1e-6: errs.append(f"does not touch the floor (min y {lo[1]})")
    for sx, nm in ((-0.5, "front (x-)"), (0.5, "back (x+)")):
        at = [p for p in pts if abs(p[0] - sx) < 0.01 and p[1] < -0.3]
        if not at: errs.append(f"nothing on the {nm} face near the floor")
    bad_uv = sum(1 for _, _, uv in faces for t in uv if t is None or not (0 <= t[0] <= 1 and 0 <= t[1] <= 1))
    if bad_uv: errs.append(f"{bad_uv} UVs missing or outside 0..1")
    degen = sum(1 for _, v, _ in faces if nrm(cross(sub(v[1], v[0]), sub(v[2], v[0]))) is None)
    duv = sum(1 for _, _, t in faces if abs((t[1][0] - t[0][0]) * (t[2][1] - t[0][1]) - (t[2][0] - t[0][0]) * (t[1][1] - t[0][1])) < 1e-12)
    if degen: errs.append(f"{degen} degenerate triangles")
    if duv: errs.append(f"{duv} triangles with zero UV area")
    print(f"{path}: parts {parts}, {len(faces)} triangles, bounds x {lo[0]:.3f}..{hi[0]:.3f} y {lo[1]:.3f}..{hi[1]:.3f} "
          f"z {lo[2]:.3f}..{hi[2]:.3f}, textures {list(tex.values())}")
    for e in errs: print("  ERROR", e)
    return faces, tex, not errs

VIEWS = {  # name: (view direction the camera looks along, up vector)
    "front": ((1, 0, 0), (0, 1, 0)), "back": ((-1, 0, 0), (0, 1, 0)),
    "side_z+": ((0, 0, -1), (0, 1, 0)), "side_z-": ((0, 0, 1), (0, 1, 0)), "top": ((0, -1, 0), (-1, 0, 0)),
    "iso_front": ((1, -0.8, 1), (0, 1, 0)), "iso_back": ((-1, -0.8, -1), (0, 1, 0)),
}

def preview(faces, tex, out, name, size=360):
    """Z-buffered orthographic views; per-pixel texture lookup (nearest), Lambert shading, back faces culled."""
    from PIL import Image, ImageDraw
    img = {m: Image.open(p).convert("RGB") for m, p in tex.items()}
    pix = {m: (im.load(), im.width, im.height) for m, im in img.items()}
    light = nrm((-0.4, 1.0, -0.6))
    tiles = []
    for vname, (d, up) in VIEWS.items():
        d = nrm(d); r = nrm(cross(d, up)); u = cross(r, d)
        im = Image.new("RGB", (size, size), (40, 44, 52)); px = im.load()
        zb = [[1e9] * size for _ in range(size)]
        s = size * 0.9
        for mat, v, t in faces:
            n = nrm(cross(sub(v[1], v[0]), sub(v[2], v[0])))
            if n is None or sum(a * b for a, b in zip(n, d)) >= 0: continue    # back face: culled, as in game
            k = 0.55 + 0.45 * max(0, sum(a * b for a, b in zip(n, light)))
            P = [(size / 2 + s * sum(p[i] * r[i] for i in range(3)), size / 2 - s * sum(p[i] * u[i] for i in range(3)),
                  sum(p[i] * d[i] for i in range(3))) for p in v]
            (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = P
            den = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
            if abs(den) < 1e-9: continue
            src = pix.get(mat)
            for yy in range(max(0, int(min(y0, y1, y2))), min(size, int(max(y0, y1, y2)) + 1)):
                for xx in range(max(0, int(min(x0, x1, x2))), min(size, int(max(x0, x1, x2)) + 1)):
                    cx, cy = xx + 0.5, yy + 0.5
                    w0 = ((y1 - y2) * (cx - x2) + (x2 - x1) * (cy - y2)) / den
                    w1 = ((y2 - y0) * (cx - x2) + (x0 - x2) * (cy - y2)) / den
                    w2 = 1 - w0 - w1
                    if w0 < -1e-6 or w1 < -1e-6 or w2 < -1e-6: continue
                    z = w0 * z0 + w1 * z1 + w2 * z2
                    if z >= zb[yy][xx]: continue
                    zb[yy][xx] = z
                    if src:
                        tu = w0 * t[0][0] + w1 * t[1][0] + w2 * t[2][0]; tv = w0 * t[0][1] + w1 * t[1][1] + w2 * t[2][1]
                        p, W, H = src
                        col = p[min(W - 1, max(0, int(tu * W))), min(H - 1, max(0, int(tv * H)))]
                    else:
                        col = (200, 0, 200)
                    px[xx, yy] = tuple(int(c * k) for c in col)
        ImageDraw.Draw(im).text((4, 4), vname, fill=(230, 230, 230))
        tiles.append(im)
    cols = 4
    sheet = Image.new("RGB", (size * cols, size * ((len(tiles) + cols - 1) // cols)), (40, 44, 52))
    for i, im in enumerate(tiles): sheet.paste(im, ((i % cols) * size, (i // cols) * size))
    os.makedirs(out, exist_ok=True)
    p = os.path.join(out, name + "_preview.png"); sheet.save(p); print("  preview", p)

def main():
    a = sys.argv[1:]
    part = a[a.index("--part") + 1] if "--part" in a else "main"
    out = a[a.index("--preview") + 1] if "--preview" in a else None
    only = a[a.index("--only") + 1] if "--only" in a else None
    objs = [x for x in a if x.endswith(".obj")]
    ok = True
    for o in objs:
        faces, tex, good = check(o, part, only)
        ok &= good
        if out: preview(faces, tex, out, os.path.splitext(os.path.basename(o))[0])
    sys.exit(0 if ok else 1)

if __name__ == "__main__":
    main()
