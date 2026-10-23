#!/usr/bin/env python3
"""Fractionating column (base + stackable segment) and flare stack models (mods.eln.oil.Column*, FlareStack*).

EA transparent nodes drawn through Obj3D (see README.md, "Path 2"); README-column.md describes the design. Design space
is mcmodel's px frame (0..16, corner origin, Y up); write_eln centres it. Front = XN (x = 0).
  columnbase:    reboiler: cast-iron heat jacket with copper heater rings, copper thermal pads at the floor centre of
                 all 4 sides (thermal cables, LRDU Down), fire porthole and level gauge on the front, crude inlet (z+)
                 and residue drain (z-) nozzles at face centre height, column shell + top half-flange.
  columnsegment: full-height lagged shell, half-flanges top and bottom (a joint reads as one bolted 1 px flange),
                 draw-off nozzle + valve on the front, temperature dial (z-), manway (z+), ladder with cage on the back.
  flarestack:    base plate, knockout drum on saddles, inlet elbow from the front face centre, slim red/white stack,
                 guy-wire collar and wires, hollow flare tip with wind shield and pilot line; opening at the top centre.

Usage: column.py RESOURCES_DIR            writes RESOURCES_DIR/assets/eln/model/{columnbase,columnsegment,flarestack}/
       column.py --stack RESOURCES_DIR OUT  composes a base + 4 segments + flare (and a variant with one segment
                                            turned 90 deg) and writes preview sheets into OUT (uses elncheck.py).
"""
import math, os, random, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Mesh, Model, box, prism, tube, oriented, normal

C = 8.0                 # block centre (x and z)
N = 12                  # sides of every column-shell cylinder (same phase everywhere: flat faces towards +-x, +-z)
SR = 5.0                # column shell radius (base top and segment: must match)
FR = 6.0                # flange outer radius
NUT_R = 5.6             # flange bolt circle
APO = SR * math.cos(math.pi / N)    # shell apothem: the flat front face sits at x = C - APO

# atlas: material -> (col, row), 4x3 cells of 16 px (64x48 png)
CELLS = {"steel": (0, 0), "darksteel": (1, 0), "castiron": (2, 0), "copper": (3, 0),
         "brass": (0, 1), "paint": (1, 1), "white": (2, 1), "glass": (3, 1),
         "lagging": (0, 2), "soot": (1, 2), "fire": (2, 2), "rubber": (3, 2)}
GRID = (4, 3)

# ---------- helpers (local to this script) ----------
def rot_y(mesh, quarter):
    """Copy of MESH turned QUARTER x 90 deg about the vertical axis through the block centre (a proper rotation,
    so the winding stays outward); quarter=1 takes the XN face to ZN."""
    m = Mesh(mesh.mat)
    for p in mesh.quads:
        q = []
        for x, y, z in p:
            dx, dz = x - C, z - C
            for _ in range(quarter % 4):
                dx, dz = -dz, dx
            q.append((C + dx, y, C + dz))
        m.poly(*q)
    return m

def rod(a, b, r, mat, n=4):
    """Thin n-sided bar from point A to point B (guy wires): side walls only."""
    d = [b[i] - a[i] for i in range(3)]
    l = math.sqrt(sum(c * c for c in d)); d = [c / l for c in d]
    h = (0, 1, 0) if abs(d[1]) < 0.9 else (1, 0, 0)
    u = [h[1] * d[2] - h[2] * d[1], h[2] * d[0] - h[0] * d[2], h[0] * d[1] - h[1] * d[0]]
    ul = math.sqrt(sum(c * c for c in u)); u = [c / ul for c in u]
    v = [d[1] * u[2] - d[2] * u[1], d[2] * u[0] - d[0] * u[2], d[0] * u[1] - d[1] * u[0]]
    m = Mesh(mat)
    off = lambda k: [r * (math.cos(2 * math.pi * k / n + math.pi / n) * u[i] + math.sin(2 * math.pi * k / n + math.pi / n) * v[i]) for i in range(3)]
    for k in range(n):
        o0, o1 = off(k), off(k + 1)
        q = [tuple(a[i] + o0[i] for i in range(3)), tuple(a[i] + o1[i] for i in range(3)),
             tuple(b[i] + o1[i] for i in range(3)), tuple(b[i] + o0[i] for i in range(3))]
        mid = [(o0[i] + o1[i]) / 2 for i in range(3)]
        m.poly(*oriented(q, mid))
    return m

def half_flange(m, y0, nuts_up):
    """0.5 px half-flange at Y0..Y0+0.5 with 8 bolt nuts on its free side (above if NUTS_UP, else below). Two halves
    of neighbouring blocks meet at the block boundary and read as one bolted 1 px flange."""
    m.add(tube("y", C, C, SR - 0.1, FR, y0, y0 + 0.5, "steel", n=N, inner=False))
    ny0, ny1 = (y0 + 0.5, y0 + 0.8) if nuts_up else (y0 - 0.3, y0)
    for k in range(8):
        a = 2 * math.pi * (k + 0.5) / 8
        m.add(prism("y", C + NUT_R * math.cos(a), C + NUT_R * math.sin(a), 0.3, ny0, ny1, "darksteel", n=6,
                    caps=(not nuts_up, nuts_up)))

def ladder(m, y0, y1, cage_from=0.0):
    """Ladder with a safety cage on the back (x+) of the shell, between Y0 and Y1. Rungs every 2 px (y = 1, 3, ...)
    and cage hoops every 8 px (y = 4, 12) so the ladder continues across stacked blocks."""
    xr0, xr1 = 13.6, 14.0
    for z0 in (6.25, 9.35):
        m.add(box(xr0, y0, z0, xr1, y1, z0 + 0.4, "darksteel", faces="nsewud" if y0 > 0 else "nsew"))
        for ys in (2.0, 10.0):                                                   # stand-offs to the shell
            if y0 <= ys <= y1 - 1:
                m.add(box(C + APO - 0.2, ys, z0 + 0.05, xr0, ys + 0.4, z0 + 0.35, "darksteel"))
    for k in range(8):
        y = 1 + 2 * k
        if y0 + 0.5 <= y <= y1 - 0.5:
            m.add(box(xr0 + 0.05, y, 6.65, xr1 - 0.05, y + 0.35, 9.35, "steel", faces="ewud"))
    for y in (4.0, 12.0):                                                        # cage hoops
        if y >= max(y0, cage_from):
            m.add(box(xr1, y, 5.2, 15.5, y + 0.4, 5.6, "steel"), box(xr1, y, 10.4, 15.5, y + 0.4, 10.8, "steel"),
                  box(15.5, y, 5.2, 15.9, y + 0.4, 10.8, "steel"))
    cy0 = max(y0, cage_from)
    for z in (5.25, 7.0, 9.0, 10.75):                                            # cage bars
        x0 = 15.55 if 6 < z < 10 else 14.6
        x1 = 15.85 if 6 < z < 10 else 14.9
        z0, z1 = (z - 0.15, z + 0.15) if 6 < z < 10 else ((5.25, 5.55) if z < 6 else (10.45, 10.75))
        m.add(box(x0, cy0, z0, x1, y1, z1, "darksteel", faces="nsewud" if cy0 > 0 else "nsew"))

def bolts_on_face_x(m, x0, x1, cy, cz, r, nb, br=0.25, phase=0.25):
    """NB bolt heads on a flange face normal to x, circle radius R round (CY, CZ)."""
    for k in range(nb):
        a = 2 * math.pi * (k + phase) / nb
        m.add(prism("x", cy + r * math.cos(a), cz + r * math.sin(a), br, x0, x1, "brass", n=6, caps=(True, False)))

def nozzle_z(m, side, cy=8.0, valve=False):
    """Pipe nozzle along z to the side face (SIDE = +1 for z+, -1 for z-), flange flush with the face (recessed 0.1 px)
    at face centre height, where a pipe from another mod would meet it."""
    face = 16.0 if side > 0 else 0.0
    fl0, fl1 = sorted((face - side * 0.6, face - side * 0.1))
    p0, p1 = sorted((face - side * 0.6, C + side * 4.0))
    m.add(prism("z", C, cy, 1.1, p0, p1, "steel", n=8, caps=(False, False)))
    m.add(prism("z", C, cy, 1.8, fl0, fl1, "steel", n=10))
    m.add(prism("z", C, cy, 0.7, min(fl0, face), max(fl1, face), "soot", n=8, caps=(side < 0, side > 0)))   # bore, at the face
    for k in range(4):
        a = 2 * math.pi * (k + 0.5) / 4
        b0, b1 = sorted((face - side * 0.1, face))
        m.add(prism("z", C + 1.35 * math.cos(a), cy + 1.35 * math.sin(a), 0.25, b0, b1, "brass", n=6, caps=(side < 0, side > 0)))
    if valve:   # gate valve with a red handwheel on top
        vz = face - side * 2.2
        m.add(box(C - 1.3, cy - 1.3, vz - 0.7, C + 1.3, cy + 1.3, vz + 0.7, "darksteel"))
        m.add(prism("y", C, vz, 0.25, cy + 1.3, cy + 2.9, "steel", n=6, caps=(False, True)))
        m.add(tube("y", C, vz, 0.8, 1.2, cy + 2.7, cy + 3.05, "paint", n=10))
        m.add(box(C - 0.8, cy + 2.75, vz - 0.12, C + 0.8, cy + 3.0, vz + 0.12, "paint", faces="nsud"))

# ---------- the models ----------
def column_base():
    m = Model()
    m.add(box(1.0, 0, 1.0, 15.0, 0.75, 15.0, "darksteel"))                           # plinth
    m.add(prism("y", C, C, 6.5, 0.75, 7.5, "castiron", n=N, caps=(False, False)))     # heat jacket / reboiler
    m.add(tube("y", C, C, 6.4, 6.75, 6.6, 7.5, "paint", n=N, inner=False))            # heat band (top of jacket)
    m.add(prism("y", C, C, 6.5, 7.5, 10.0, "steel", n=N, r_end=SR, caps=(False, False)))   # shoulder
    m.add(prism("y", C, C, SR, 10.0, 15.9, "lagging", n=N, caps=(False, False)))      # column shell
    m.add(prism("y", C, C, SR - 0.1, 15.5, 16.0, "darksteel", n=N, caps=(False, True)))   # head
    m.add(tube("y", C, C, SR - 0.1, SR + 0.25, 12.5, 12.9, "darksteel", n=N, inner=False))  # lagging strap
    half_flange(m, 15.5, nuts_up=False)
    # thermal pads: copper plate flush with each side face (floor centre, 7 px wide), heat bus into the jacket
    pad = Model()
    pad.add(box(0.1, 0, 4.5, 0.7, 3.5, 11.5, "copper"))
    pad.add(box(0.7, 0.75, 6.0, 2.2, 3.0, 10.0, "copper"))
    for y, z in ((0.6, 5.1), (0.6, 10.9), (2.9, 5.1), (2.9, 10.9)):
        pad.add(prism("x", y, z, 0.35, 0.0, 0.1, "brass", n=4, caps=(True, False)))
    pad.add(box(0.1, 3.5, 4.3, 0.8, 3.85, 11.7, "darksteel"))                         # pad clamp bar
    pad.add(box(0.0, 1.75, 7.5, 0.1, 2.75, 8.5, "brass", faces="nsudw"))             # terminal stud (README: cable terminals)
    for q in range(4):
        for mesh in pad.meshes:
            m.add(rot_y(mesh, q))
    # front: fire porthole on the jacket, level gauge on the shell
    jx = C - 6.5 * math.cos(math.pi / N)
    m.add(prism("x", 5.1, C, 1.15, jx - 0.5, jx + 0.2, "brass", n=10))
    m.add(prism("x", 5.1, C, 0.8, jx - 0.6, jx - 0.5, "fire", n=10, caps=(True, False)))
    for k in range(4):
        a = 2 * math.pi * (k + 0.5) / 4
        m.add(prism("x", 5.1 + 0.98 * math.cos(a), C + 0.98 * math.sin(a), 0.15, jx - 0.65, jx - 0.5, "darksteel", n=4, caps=(True, False)))
    sx = C - APO
    m.add(box(sx - 1.2, 10.4, 7.35, sx + 0.1, 11.1, 8.65, "brass"), box(sx - 1.2, 14.4, 7.35, sx + 0.1, 15.1, 8.65, "brass"))
    m.add(prism("y", sx - 0.6, C, 0.4, 11.1, 14.4, "glass", n=6, caps=(False, False)))
    m.add(box(sx - 1.4, 11.1, 7.15, sx - 0.6, 14.4, 7.35, "darksteel"), box(sx - 1.4, 11.1, 8.65, sx - 0.6, 14.4, 8.85, "darksteel"))
    # crude inlet (z+) and residue drain with valve (z-), both at face centre height
    nozzle_z(m, +1)
    nozzle_z(m, -1, valve=True)
    # ladder starts on the jacket top (back)
    ladder(m, 7.5, 16.0, cage_from=11.0)
    return m

def column_segment():
    m = Model()
    m.add(prism("y", C, C, SR, 0.0, 15.9, "lagging", n=N, caps=(False, False)))
    m.add(prism("y", C, C, SR - 0.1, 15.5, 16.0, "darksteel", n=N, caps=(False, True)))   # top head (hidden when stacked)
    half_flange(m, 0.0, nuts_up=True)
    half_flange(m, 15.5, nuts_up=False)
    for y in (4.5, 12.5):                                                             # lagging straps
        m.add(tube("y", C, C, SR - 0.1, SR + 0.25, y, y + 0.4, "darksteel", n=N, inner=False))
    m.add(tube("y", C, C, SR - 0.1, SR + 0.2, 13.4, 14.4, "paint", n=N, inner=False))  # painted band
    # tray draw-off nozzle on the front (face centre), gate valve with handwheel
    sx = C - APO
    m.add(prism("x", 8.0, C, 1.7, sx - 0.4, sx + 0.3, "steel", n=10))                  # reinforcing pad
    m.add(prism("x", 8.0, C, 1.1, 0.6, sx - 0.4, "steel", n=8, caps=(False, False)))
    m.add(prism("x", 8.0, C, 1.8, 0.1, 0.6, "steel", n=10))
    m.add(prism("x", 8.0, C, 0.7, 0.0, 0.6, "soot", n=8, caps=(True, False)))
    bolts_on_face_x(m, 0.0, 0.1, 8.0, C, 1.35, 4, phase=0.5)
    vx = 1.9
    m.add(box(vx - 0.7, 6.7, 6.7, vx + 0.7, 9.3, 9.3, "darksteel"))
    m.add(prism("y", vx, C, 0.25, 9.3, 10.9, "steel", n=6, caps=(False, True)))
    m.add(tube("y", vx, C, 0.8, 1.2, 10.7, 11.05, "paint", n=10))
    m.add(box(vx - 0.12, 10.75, C - 0.8, vx + 0.12, 11.0, C + 0.8, "paint", faces="ewud"))
    m.add(box(vx - 0.8, 10.75, C - 0.12, vx + 0.8, 11.0, C + 0.12, "paint", faces="nsud"))
    # temperature dial on the z- flank
    sz = C - APO
    m.add(prism("z", 8.0, C, 0.25, sz - 1.0, sz + 0.2, "steel", n=6, caps=(False, False)))
    m.add(prism("z", 8.0, C, 1.0, sz - 1.6, sz - 1.0, "brass", n=10))
    m.add(prism("z", 8.0, C, 0.8, sz - 1.7, sz - 1.6, "white", n=10, caps=(True, False)))
    m.add(box(C - 0.15, 7.9, sz - 1.8, C + 0.65, 8.1, sz - 1.7, "darksteel", faces="nsewud"))   # needle
    # manway on the z+ flank
    mz = C + APO
    m.add(prism("z", C, 9.0, 1.6, mz - 0.3, mz + 0.6, "steel", n=10, caps=(False, False)))
    m.add(prism("z", C, 9.0, 2.0, mz + 0.6, mz + 1.1, "steel", n=10))
    for k in range(6):
        a = 2 * math.pi * (k + 0.5) / 6
        m.add(prism("z", C + 1.7 * math.cos(a), 9.0 + 1.7 * math.sin(a), 0.2, mz + 1.1, mz + 1.3, "brass", n=6, caps=(False, True)))
    m.add(box(C - 0.6, 9.0 - 0.2, mz + 1.1, C + 0.6, 9.0 + 0.2, mz + 1.5, "darksteel"))   # handle
    ladder(m, 0.0, 16.0)
    return m

def flare_stack():
    m = Model()
    m.add(box(0.1, 0, 0.1, 15.9, 0.6, 15.9, "steel"))                                  # base plate
    for x, z in ((1.3, 1.3), (14.7, 1.3), (1.3, 14.7), (14.7, 14.7)):                   # anchor bolts / wire anchors
        m.add(prism("y", x, z, 0.45, 0.6, 0.9, "brass", n=6))
        m.add(box(x - 0.3, 0.6, z - 0.3, x + 0.3, 1.4, z + 0.3, "darksteel"))
    # knockout drum on two saddles, along x
    dy, dr = 3.7, 2.7
    for x in (4.5, 10.5):
        m.add(box(x, 0.6, 5.6, x + 1.0, 1.8, 10.4, "darksteel"))
    m.add(prism("x", dy, C, dr, 3.5, 12.5, "steel", n=N, caps=(False, False)))
    m.add(prism("x", dy, C, dr - 1.0, 2.8, 3.5, "steel", n=N, r_end=dr, caps=(True, False)))     # dished heads
    m.add(prism("x", dy, C, dr, 12.5, 13.2, "steel", n=N, r_end=dr - 1.0, caps=(False, True)))
    for x in (5.6, 10.0):
        m.add(tube("x", dy, C, dr - 0.1, dr + 0.25, x, x + 0.4, "paint", n=N, inner=False))
    m.add(box(7.3, 0.6, 7.3, 8.7, dy - dr + 0.3, 8.7, "darksteel"))                     # drain leg
    # inlet: flange at the front face centre, elbow down into the drum
    m.add(prism("x", 8.0, C, 1.8, 0.1, 0.6, "steel", n=10))
    m.add(prism("x", 8.0, C, 0.7, 0.0, 0.6, "soot", n=8, caps=(True, False)))
    bolts_on_face_x(m, 0.0, 0.1, 8.0, C, 1.35, 4, phase=0.5)
    m.add(prism("x", 8.0, C, 0.9, 0.6, 3.6, "steel", n=8, caps=(False, False)))
    m.add(box(3.4, 7.0, 7.0, 5.4, 9.0, 9.0, "castiron"))                                # elbow
    m.add(prism("y", 4.4, C, 0.9, dy + dr - 0.6, 7.0, "steel", n=8, caps=(False, False)))
    # stack: plain lower part, red/white bands, guy collar
    sr = 1.4
    m.add(prism("y", C, C, sr + 0.6, dy + dr - 0.4, dy + dr + 0.4, "steel", n=10))      # nozzle boss on the drum
    m.add(prism("y", C, C, sr, dy + dr + 0.4, 9.5, "steel", n=10, caps=(False, False)))
    for k, y in enumerate((9.5, 10.5, 11.5, 12.5)):
        m.add(prism("y", C, C, sr, y, y + 1.0, "paint" if k % 2 == 0 else "white", n=10, caps=(False, False)))
    m.add(tube("y", C, C, sr - 0.1, sr + 0.5, 8.6, 9.2, "darksteel", n=10, inner=False))   # guy collar
    lugs = []
    for k in range(4):
        a = math.pi / 4 + k * math.pi / 2
        ux, uz = math.cos(a), math.sin(a)
        lx, lz = C + (sr + 1.0) * ux, C + (sr + 1.0) * uz
        m.add(box(lx - 0.3, 8.4, lz - 0.3, lx + 0.3, 9.4, lz + 0.3, "darksteel"))
        lugs.append((lx, 8.9, lz))
    for (lx, ly, lz), (ax, az) in zip(lugs, ((14.7, 14.7), (1.3, 14.7), (1.3, 1.3), (14.7, 1.3))):
        m.add(rod((lx, ly, lz), (ax, 1.4, az), 0.12, "darksteel"))
    # flare tip: flared cone, hollow barrel (opening at the top centre), soot floor, wind shield on struts
    m.add(prism("y", C, C, sr, 13.5, 14.5, "castiron", n=10, r_end=2.1, caps=(False, False)))
    m.add(tube("y", C, C, 1.75, 2.1, 14.5, 16.0, "castiron", n=10))
    m.add(prism("y", C, C, 1.8, 14.8, 14.9, "soot", n=10, caps=(False, True)))
    m.add(tube("y", C, C, 2.6, 2.85, 14.6, 15.8, "steel", n=12))
    for k in range(3):
        a = 2 * math.pi * k / 3 + 0.3
        x0, z0 = C + 2.0 * math.cos(a), C + 2.0 * math.sin(a)
        x1, z1 = C + 2.7 * math.cos(a), C + 2.7 * math.sin(a)
        m.add(rod((x0, 14.9, z0), (x1, 14.9, z1), 0.15, "darksteel"))
    # pilot line: igniter box on the plate (z+), brass line up beside the stack, pilot head over the rim
    pz = 11.4
    m.add(box(6.8, 0.6, 11.9, 9.2, 3.0, 13.6, "darksteel"))
    m.add(box(6.6, 3.0, 11.7, 9.4, 3.3, 13.8, "paint"))
    m.add(prism("y", C, 12.75, 0.35, 3.3, 3.9, "brass", n=6))
    m.add(rod((C, 3.6, 12.75), (C, 4.6, pz), 0.22, "brass", n=6))
    m.add(prism("y", C, pz, 0.22, 4.6, 15.3, "brass", n=6, caps=(False, False)))
    m.add(box(C - 0.3, 15.1, 10.2, C + 0.3, 15.7, pz + 0.3, "brass"))                   # pilot head
    for y in (6.0, 11.0):                                                                # line clips
        m.add(box(C - 0.15, y, sr + C - 0.1, C + 0.15, y + 0.3, pz, "darksteel"))
    return m

# ---------- textures ----------
def atlas(path):
    from PIL import Image
    rnd = random.Random(4321)
    im = Image.new("RGBA", (16 * GRID[0], 16 * GRID[1]))
    def cell(name, base, noise=10, shade=None):
        col, row = CELLS[name]
        for y in range(16):
            for x in range(16):
                d = rnd.randint(-noise, noise)
                c = [b + d for b in base]
                if shade:
                    k = shade(x, y)
                    c = [v + k for v in c] if isinstance(k, int) else list(k)
                im.putpixel((16 * col + x, 16 * row + y), tuple(max(0, min(255, v)) for v in c) + (255,))
    cell("steel", (128, 134, 140), 8)
    cell("darksteel", (62, 64, 68), 6)
    cell("castiron", (88, 84, 80), 10, shade=lambda x, y: -10 if (x * 7 + y * 3) % 11 == 0 else 0)
    cell("copper", (196, 110, 70), 14, shade=lambda x, y: 18 if (x + y) % 7 == 0 else 0)
    cell("brass", (200, 170, 80), 10)
    cell("paint", (176, 52, 40), 8)                         # signal red: heat band, handwheels, flare stack bands
    cell("white", (226, 226, 220), 6)
    # sight glass: clear at the top, amber oil below (the base's gauge spans image rows ~1.5..5.5)
    cell("glass", (210, 222, 226), 4, shade=lambda x, y: (150 + rnd.randint(-8, 8), 96, 24) if y >= 3 else (-40 if x % 5 == 0 else 0))
    # lagging: aluminium cladding with a sheet seam
    cell("lagging", (176, 180, 184), 5, shade=lambda x, y: -34 if x == 0 or y == 15 else (12 if x == 1 else 0))
    cell("soot", (26, 24, 24), 5)
    cell("fire", (232, 96, 26), 22, shade=lambda x, y: 40 if (x * 5 + y * 3) % 7 == 0 else 0)   # glow behind the porthole
    cell("rubber", (34, 34, 36), 4)
    im.save(path)

MODELS = {"columnbase": column_base, "columnsegment": column_segment, "flarestack": flare_stack}

def write(res):
    for name, fn in MODELS.items():
        out = os.path.join(res, "assets/eln/model", name)
        os.makedirs(out, exist_ok=True)
        tris = fn().write_eln(out, name, CELLS, GRID)
        atlas(os.path.join(out, "tex.png"))
        print(name, "triangles", tris)

def stack_preview(res, out):
    """Compose base + 4 segments (+ flare beside it) in OBJ space and render with elncheck.preview."""
    import elncheck
    loaded = {}
    for name in MODELS:
        faces, tex, _ = elncheck.check(os.path.join(res, "assets/eln/model", name, name + ".obj"), "main")
        loaded[name] = ([(name, v, t) for _, v, t in faces], {name: tex["atlas"]})
    def place(name, dx, dy, dz, quarter=0):
        res_f = []
        for mat, v, t in loaded[name][0]:
            vv = []
            for x, y, z in v:
                for _ in range(quarter % 4):
                    x, z = -z, x
                vv.append((x + dx, y + dy, z + dz))
            res_f.append((mat, vv, t))
        return res_f
    tex = {}
    for name in MODELS: tex.update(loaded[name][1])
    for tag, turns in (("tower", (0, 0, 0, 0)), ("tower_turned", (0, 1, 0, 2))):
        faces = place("columnbase", 0, 0, 0)
        for i, q in enumerate(turns):
            faces += place("columnsegment", 0, i + 1, 0, q)
        faces += place("flarestack", 0, 0, 1.6)
        # fit x -0.5..0.5, y -0.5..4.5, z -0.5..2.1 into the preview's -0.5..0.5 cube
        k, cy, cz = 1 / 5.2, 2.0, 0.8
        faces = [(m, [((x) * k, (y - cy) * k, (z - cz) * k) for x, y, z in v], t) for m, v, t in faces]
        elncheck.preview(faces, tex, out, tag, size=520)

if __name__ == "__main__":
    if sys.argv[1] == "--stack":
        stack_preview(sys.argv[2], sys.argv[3])
    else:
        write(sys.argv[1])
