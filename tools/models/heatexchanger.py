#!/usr/bin/env python3
"""Counterflow Heat Exchanger (README-shaft.md): a horizontal shell-and-tube exchanger along x. Blocks stack 2 x 2
across (y, z) and 1..8 long (x) into one exchanger; a lone block works too.

Parts:
  shell   one block (front x = 0, back x = 16). The render scales it over the whole stack box (x by N, y by 2, z by 2),
          so everything here is either at the ends (square channel headers that carry the flanges, girth flanges,
          feet) or periodic along x (hoop bands every 4 px), and round things are round in y/z (scaled equally).
  flange  the end connection on the x = 0 face of ONE block, unscaled, drawn on every end-face block of a stack (and
          turned 180 deg about y for the back end): stream A = large flange at the face centre (y 8, z 8), stream B =
          small flange at (y 12, z 8), 0.1 px proud of A's face (B's face at x 0.1, A's at 0.2, so both stay inside
          the block), short nozzles into the header. Labels: a paint ring on A's face, a copper ring on B's.

Design frame: mcmodel px (0..16, corner origin), front XN (x = 0).

Usage: heatexchanger.py RESOURCES_DIR [--stack N] [--preview OUTDIR]
  writes assets/eln/model/heatexchanger/, then (default OUTDIR notes/ea/shaft/p5-previews) a lone-block preview
  (shell + flange at both ends) and a 2 x 2 x N stack preview (default N = 3) composed as the render draws it.
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Model, box, prism, tube
from oilkit import bolt_ring
from shaftkit import pipe_flange, write_model, PAINT

NAME = "heatexchanger"
C = 8.0               # centre (y and z) of the shell and of the headers
R = 6.0               # shell radius
N_SH = 16             # shell sides
HDR = (0.3, 0.75)     # channel header plate (x range at the front; mirrored at the back). Front at 0.3 px: scaled by
                      # the stack length (0.3 N <= 2.4 px) it stays behind the flange discs (front 0.1, 0.2) and the nozzles reach it (3.2)
GIRTH = (0.75, 1.25)  # girth flange (tube sheet ring) between header and shell
BANDS = (2.0, 6.0, 10.0, 14.0)   # hoop bands, period 4 px (divides 16)
DEFAULT_PREVIEW = "/var/lib/mclab/agent/notes/ea/shaft/p5-previews"

def mirror_x(ms):
    """Mirror meshes across x = 8 (front end -> back end), winding restored."""
    from oilkit import mirrored_x
    return mirrored_x(ms, 8.0)

def front_end():
    """Header, girth flange and foot at the front end (x = 0 side); mirrored for the back."""
    ms = []
    # square channel header plate: covers the face area so every flange of a 2 x 2 stack lands on it
    ms.append(box(HDR[0], 1.0, 1.0, HDR[1], 15.0, 15.0, "castiron", faces="nsewu"))
    # header rim band (darker edge) and its bolts along the rim
    ms.append(box(HDR[1], 1.0, 1.0, GIRTH[0] + 0.3, 15.0, 1.6, "darksteel", faces="nsewu"))
    ms.append(box(HDR[1], 1.0, 14.4, GIRTH[0] + 0.3, 15.0, 15.0, "darksteel", faces="nsewu"))
    ms.append(box(HDR[1], 14.4, 1.6, GIRTH[0] + 0.3, 15.0, 14.4, "darksteel", faces="nsewu"))
    # girth flange (tube sheet ring) and bolts
    ms.append(prism("x", C, C, R + 0.75, GIRTH[0], GIRTH[1], "steel", n=N_SH, caps=(False, True)))
    ms += bolt_ring("x", C, C, R + 0.4, GIRTH[1], GIRTH[1] + 0.25, 8, rb=0.22)
    # foot under the header, on the floor at the face
    ms.append(box(0.0, 0.0, 2.0, 1.75, 1.0, 14.0, "darksteel", faces="nsewu"))
    ms.append(box(1.75, 0.0, 3.0, 2.5, 0.6, 13.0, "darksteel", faces="nsewu"))
    return ms

def shell_part():
    m = Model()
    m.add(prism("x", C, C, R, GIRTH[1], 16.0 - GIRTH[1], "paint", n=N_SH, caps=(False, False)))
    for x in BANDS:
        m.add(tube("x", C, C, R - 0.1, R + 0.35, x - 0.2, x + 0.2, "darksteel", n=N_SH, inner=False))
    # mid saddle (one, between the bands at 6 and 10) with a skid rail each side, floor to the shell
    m.add(box(7.0, 0.0, 3.0, 9.0, 1.0, 13.0, "castiron", faces="nsewu"))
    m.add(box(7.25, 1.0, 4.5, 8.75, 2.75, 11.5, "castiron", faces="nsew"))
    for z0 in (3.0, 12.25):
        m.add(box(2.5, 0.0, z0, 13.5, 0.5, z0 + 0.75, "darksteel", faces="nsu"))
    fe = front_end()
    m.add(*fe)
    m.add(*mirror_x(fe))
    return m

def flange_part():
    m = Model()
    # stream A: large flange at the face centre, recessed 0.2 px, nozzle into the header; label: a paint ring on its
    # face (0.1 px proud of it, inside the bolt-free middle, below B's disc)
    m.add(*shift_x(pipe_flange("x0", C, C, "large", stub=2.0), 0.2))
    m.add(tube("x", C, C, 1.5, 2.4, 0.1, 0.2, "paint", n=12, inner=True))
    # stream B: small flange at (y 12, z 8), its face (x 0.1) 0.1 px proud of A's (x 0.2); label: a copper ring on it
    m.add(*shift_x(pipe_flange("x0", 12.0, C, "small", stub=2.0), 0.1))
    m.add(tube("x", 12.0, C, 0.75, 1.25, 0.0, 0.1, "copper", n=10, inner=True))
    return m

def shift_x(ms, dx):
    from mcmodel import Mesh
    out = []
    for mm in ms:
        n = Mesh(mm.mat)
        for p in mm.quads:
            n.poly(*[(x + dx, y, z) for x, y, z in p])
        out.append(n)
    return out

# ---------- composed preview (as the render draws it) ----------
def stack_faces(obj, n, w):
    """Faces (centred block units) of a W x W x N stack: shell scaled (x*N, y*W, z*W) from the stack's lowest corner;
    flange on each of the W x W end blocks at x = 0, and turned 180 deg about y at the back end."""
    import elncheck
    _, shell, tex, _ = elncheck.load(obj, only="shell")
    _, fl, _, _ = elncheck.load(obj, only="flange")
    faces = []
    blk = lambda v: tuple(c + 0.5 for c in v)            # centred -> block-corner units
    for mat, vs, uv in shell:
        faces.append((mat, [(b[0] * n, b[1] * w, b[2] * w) for b in map(blk, vs)], uv))
    for j in range(w):
        for k in range(w):
            for mat, vs, uv in fl:
                faces.append((mat, [(b[0], b[1] + j, b[2] + k) for b in map(blk, vs)], uv))
                faces.append((mat, [(1 - b[0] + n - 1, b[1] + j, 1 - b[2] + k) for b in map(blk, vs)], uv))
    s = 1.0 / max(1.0, 0.75 * (n + w))
    cen = (n / 2, w / 2, w / 2)
    return [(m, [tuple((p[i] - cen[i]) * s for i in range(3)) for p in vs], uv) for m, vs, uv in faces], tex

def main():
    a = sys.argv[1:]
    n = int(a[a.index("--stack") + 1]) if "--stack" in a else 3
    out = a[a.index("--preview") + 1] if "--preview" in a else DEFAULT_PREVIEW
    res = [x for i, x in enumerate(a) if not x.startswith("--") and (i == 0 or a[i - 1] not in ("--stack", "--preview"))][0]
    d, counts = write_model(res, NAME, [("shell", shell_part()), ("flange", flange_part())], paint=PAINT["hx"],
                            hitbox=[(0, 0, 1, 16, 15, 15)])
    print(NAME, counts)
    import elncheck
    obj = os.path.join(d, NAME + ".obj")
    elncheck.check(obj, "shell")
    f1, tex = stack_faces(obj, 1, 1)
    elncheck.preview(f1, tex, out, NAME)
    fs, tex = stack_faces(obj, n, 2)
    elncheck.preview(fs, tex, out, NAME + "_stack")

if __name__ == "__main__":
    main()
