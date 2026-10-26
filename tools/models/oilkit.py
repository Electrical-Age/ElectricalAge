#!/usr/bin/env python3
"""oilkit: shared pieces for the oil-chain device models (electric heater, boiler, oil pump, tar sand separator).

Builds on mcmodel (px frame, 0..16, corner origin) without changing it:
  - ATLAS / CELLS / GRID and atlas(path, paint): one 64x80 atlas of procedural 16 px cells shared by all four devices.
  - write_parts(outdir, name, parts, origins): EA Obj3D writer for several parts (o NAME + its own v/vt + usemtl + f
    per part: Obj3D attaches faces to the FaceGroup of the last `usemtl`, so every part needs its own) and the
    NAME.txt with rotation origins (centred block coordinates).
  - turned(meshes, k): rotate meshes k quarter turns about the vertical axis through the block centre, so a piece
    designed for the front face (x = 0) can be put on any side: k=1 -> z = 16 face, k=2 -> x = 16, k=3 -> z = 0.
  - obox(): oriented box (paddles, gear teeth, levers); terminal_plate(), signal_plate(), heat_pad(), bolt_ring(), handwheel(), dial().
  - compose(): preview several OBJs side by side through elncheck.preview.
"""
import math, os, random, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Model, Mesh, box, prism, tube, oriented, normal, planar_uv

# material -> (col, row); row 0 is the top of the image
CELLS = {"steel": (0, 0), "darksteel": (1, 0), "castiron": (2, 0), "copper": (3, 0),
         "brass": (0, 1), "paint": (1, 1), "glass": (2, 1), "rubber": (3, 1),
         "ceramic": (0, 2), "coil": (1, 2), "hotcopper": (2, 2), "rivet": (3, 2),
         "water": (0, 3), "red": (1, 3), "froth": (2, 3), "sand": (3, 3),
         "louvre": (0, 4), "oil": (1, 4), "lamp": (2, 4), "firebrick": (3, 4)}
GRID = (4, 5)
PAINT = {"50v": (56, 156, 84), "200v": (52, 102, 176), "boiler": (122, 44, 36)}

def atlas(path, paint):
    """Write the atlas; PAINT is the tier colour (RGB) of the 'paint' cell."""
    from PIL import Image
    rnd = random.Random(1234)
    im = Image.new("RGBA", (16 * GRID[0], 16 * GRID[1]))
    def cell(name, base, noise=10, shade=None):
        col, row = CELLS[name]
        for y in range(16):
            for x in range(16):
                d = rnd.randint(-noise, noise)
                c = [b + d for b in base]
                if shade:
                    k = shade(x, y)
                    c = [v + k for v in c] if isinstance(k, int) else [v + kk for v, kk in zip(c, k)]
                im.putpixel((16 * col + x, 16 * row + y), tuple(max(0, min(255, v)) for v in c) + (255,))
    cell("steel", (128, 134, 140), 8)
    cell("darksteel", (62, 64, 68), 6)
    cell("castiron", (88, 84, 80), 10, shade=lambda x, y: -10 if (x * 7 + y * 3) % 11 == 0 else 0)
    cell("copper", (196, 110, 70), 14)
    cell("brass", (200, 170, 80), 10)
    cell("paint", paint, 8)
    cell("glass", (230, 232, 224), 4, shade=lambda x, y: -60 if (x in (0, 15) or y in (0, 15)) else 0)
    cell("rubber", (34, 34, 36), 4)
    cell("ceramic", (226, 222, 208), 5, shade=lambda x, y: -22 if y % 4 == 3 else 0)
    # heating coil: hot orange turns with dark gaps, diagonal so it reads as a helix on every face
    cell("coil", (238, 120, 40), 12, shade=lambda x, y: (-150, -90, -30) if (x + y) % 3 == 0 else (12, 20, 0) if (x + y) % 3 == 1 else 0)
    # heat-tinted contact face (texture rows are block heights: row 15 = floor): lapped copper with a cross-hatch,
    # blue-violet temper high up, straw below it, bright copper near the floor
    def temper(x, y):
        k = -26 if (x % 4 == 0 or y % 4 == 0) else 0
        if 3 <= y <= 6: return (-46 + k, -6 + k, 50 + k)
        if 7 <= y <= 9: return (24 + k, 34 + k, -24 + k)
        return k
    cell("hotcopper", (214, 116, 64), 10, shade=temper)
    # riveted boiler plate: rows of rivet heads along the lap seams every 8 px (texture rows run along the drum)
    def rivets(x, y):
        if y % 8 == 2 and x % 2 == 0: return 52
        if y % 8 == 3 and x % 2 == 0: return -26
        if y % 8 == 4: return -22
        return 0
    cell("rivet", (100, 106, 112), 4, shade=rivets)
    # gauge glass: water below ~9 px of block height (image rows 7..15), steam above, a meniscus line
    cell("water", (200, 214, 222), 4, shade=lambda x, y: (-150, -90, -10) if y > 7 else (-60, -60, -60) if y == 7 else 0)
    cell("red", (176, 36, 30), 10)
    # bitumen froth: dark brown-black with light bubbles
    cell("froth", (58, 44, 30), 10, shade=lambda x, y: 70 if (x * 5 + y * 3) % 13 == 0 else -14 if (x + y * 7) % 5 == 0 else 0)
    cell("sand", (214, 196, 140), 14)
    cell("louvre", (72, 74, 78), 4, shade=lambda x, y: -48 if y % 3 == 0 else 14 if y % 3 == 1 else 0)
    cell("oil", (24, 22, 20), 4, shade=lambda x, y: 40 if (x + y) % 9 == 0 else 0)
    cell("lamp", (250, 186, 60), 6, shade=lambda x, y: 30 if 5 <= x <= 10 and 5 <= y <= 10 else 0)
    cell("firebrick", (164, 92, 62), 10, shade=lambda x, y: -60 if y % 4 == 0 or (x + (y // 4) * 4) % 8 == 0 else 0)
    im.save(path)

# ---------- geometry helpers ----------
def obox(c, ax, ay, az, hx, hy, hz, mat):
    """Oriented box: centre C, unit axes AX, AY, AZ (orthonormal), half sizes HX, HY, HZ."""
    m = Mesh(mat)
    def P(sx, sy, sz):
        return tuple(c[i] + sx * hx * ax[i] + sy * hy * ay[i] + sz * hz * az[i] for i in range(3))
    for axis, (u, v) in ((0, (1, 2)), (1, (0, 2)), (2, (0, 1))):
        for s in (-1, 1):
            quad = []
            for a, b in ((-1, -1), (1, -1), (1, 1), (-1, 1)):
                sg = [0, 0, 0]; sg[axis] = s; sg[u] = a; sg[v] = b
                quad.append(P(*sg))
            hint = [ax, ay, az][axis]
            m.poly(*oriented(quad, tuple(s * h for h in hint)))
    return m

def turned(meshes, k):
    """Rotate meshes k quarter turns about the block's vertical centre line: (x, z) -> (z, 16 - x) per turn,
    which takes the x = 0 face to z = 16 (k=1), x = 16 (k=2), z = 0 (k=3). Proper rotation, windings stay outward."""
    out = []
    for m in meshes:
        n = Mesh(m.mat)
        for p in m.quads:
            q = []
            for x, y, z in p:
                for _ in range(k % 4):
                    x, z = z, 16 - x
                q.append((x, y, z))
            n.poly(*q)
        out.append(n)
    return out

def mirrored_x(meshes, cx=8.0):
    """Mirror across the plane x = CX (reverses the winding back to outward)."""
    out = []
    for m in meshes:
        n = Mesh(m.mat)
        for p in m.quads:
            n.poly(*reversed([(2 * cx - x, y, z) for x, y, z in p]))
        out.append(n)
    return out

def terminal_plate(paint="paint"):
    """Electric floor terminal for the x = 0 face (turn it for other faces): steel plate recessed 0.1 px, 7 px wide,
    floor to 4 px, three copper studs reaching the face, tier stripe on top, rubber gasket edges."""
    ms = [box(0.1, 0, 4.5, 0.85, 4.0, 11.5, "steel", faces="nsewu"), box(0.1, 4.0, 4.5, 0.85, 4.5, 11.5, paint, faces="nsewu"),
          box(0.1, 0.5, 4.25, 0.85, 3.5, 4.5, "rubber", faces="nweu"), box(0.1, 0.5, 11.5, 0.85, 3.5, 11.75, "rubber", faces="sweu")]
    for z in (5.75, 8.0, 10.25):
        ms.append(box(0.0, 1.75, z - 0.5, 0.1, 2.75, z + 0.5, "copper", faces="nsewu"))
    return ms

def signal_plate():
    """Signal terminal for the x = 0 face (turn it for other faces): a narrow plate, 3 px wide, floor to 4 px, with a
    single small copper stud at the conductor height (Baughn: signal I/O has one small conductor, power three)."""
    return [box(0.1, 0, 6.5, 0.85, 3.5, 9.5, "steel", faces="nsewu"), box(0.1, 3.5, 6.5, 0.85, 3.9, 9.5, "darksteel", faces="nsewu"),
            box(0.0, 1.9, 7.6, 0.1, 2.6, 8.4, "copper", faces="nsewu")]

def heat_pad(depth=1.0, top=4.0):
    """Thermal floor contact for the x = 0 face: a heat-tinted copper pad 8 px wide from the floor, recessed
    0.1 px, on a cast-iron backing, with four brass bolt heads on its face."""
    ms = [box(0.1, 0, 4.0, depth, top, 12.0, "hotcopper"),
          box(depth, 0, 3.5, depth + 0.5, top + 0.5, 12.5, "castiron")]
    for y, z in ((top - 0.9, 4.9), (top - 0.9, 11.1), (0.9, 4.9), (0.9, 11.1)):
        ms.append(prism("x", y, z, 0.35, 0.0, 0.1, "brass", n=6, caps=(True, False)))
    return ms

def bolt_ring(axis, c1, c2, r, a0, a1, count, mat="brass", rb=0.3, phase=0.0):
    ms = []
    for k in range(count):
        a = phase + 2 * math.pi * (k + 0.5) / count
        ms.append(prism(axis, c1 + r * math.cos(a), c2 + r * math.sin(a), rb, a0, a1, mat, n=6, caps=(False, True)))
    return ms

def handwheel(x, y, z, r=1.2, axis="y"):
    """Red valve handwheel (rim + two spokes) centred at (x, y, z), turning about AXIS ('y' or 'z')."""
    if axis == "y":
        ms = [tube("y", x, z, r - 0.3, r, y - 0.15, y + 0.15, "red", n=6)]
        ms.append(box(x - r + 0.2, y - 0.1, z - 0.12, x + r - 0.2, y + 0.1, z + 0.12, "red"))
        ms.append(box(x - 0.12, y - 0.1, z - r + 0.2, x + 0.12, y + 0.1, z + r - 0.2, "red"))
        ms.append(prism("y", x, z, 0.3, y - 0.2, y + 0.3, "darksteel", n=6))
    else:
        ms = [tube("z", x, y, r - 0.3, r, z - 0.15, z + 0.15, "red", n=6)]
        ms.append(box(x - r + 0.2, y - 0.12, z - 0.1, x + r - 0.2, y + 0.12, z + 0.1, "red"))
        ms.append(box(x - 0.12, y - r + 0.2, z - 0.1, x + 0.12, y + r - 0.2, z + 0.1, "red"))
        ms.append(prism("z", x, y, 0.3, z - 0.2, z + 0.3, "darksteel", n=6))
    return ms

def dial(x, y, z, r, facing="xn", depth=0.5):
    """Round gauge: brass case, glass face, darksteel needle (geometry, so it sits right at any size/place).
    FACING 'xn' (looking out of the front, x-) or 'zn'/'zp'."""
    if facing == "xn":
        return [prism("x", y, z, r, x, x + depth, "brass", n=10),
                prism("x", y, z, r - 0.25, x - 0.05, x, "glass", n=10, caps=(True, False)),
                obox((x - 0.1, y + 0.25 * r, z + 0.2 * r), (1, 0, 0), (0, 0.8, 0.6), (0, -0.6, 0.8), 0.04, 0.5 * r, 0.08, "darksteel")]
    s = -1 if facing == "zn" else 1
    a0, a1 = (z - depth, z) if s > 0 else (z, z + depth)
    return [prism("z", x, y, r, a0, a1, "brass", n=10),
            prism("z", x, y, r - 0.25, *((a1, a1 + 0.05) if s > 0 else (a0 - 0.05, a0)), "glass", n=10,
                  caps=(False, True) if s > 0 else (True, False)),
            obox((x - 0.3 * r, y + 0.22 * r, (a1 + 0.1) if s > 0 else (a0 - 0.1)), (0.6, 0.8, 0), (-0.8, 0.6, 0), (0, 0, 1),
                 0.08, 0.5 * r, 0.04, "darksteel")]

# ---------- writer ----------
def write_parts(outdir, name, parts, origins=None, tex="tex.png", inset=0.3 / 16):
    """EA Obj3D OBJ with several parts. PARTS: list of (part_name, Model). ORIGINS: {part: (x, y, z) in px} written
    to NAME.txt as centred block coordinates (px / 16 - 0.5). Returns {part: triangles}."""
    cols, rows = GRID
    lines = ["# generated by oilkit.py (EA Obj3D format)", f"mtllib {name}.mtl"]
    nv = nt = 0
    counts = {}
    for pname, model in parts:
        vs, vts, fs, vidx, tidx = [], [], [], {}, {}
        def ref(table, lst, key):
            if key not in table:
                lst.append(key)
                table[key] = len(lst)
            return table[key]
        for m in model.meshes:
            col, row = CELLS[m.mat]
            for p in m.quads:
                n = normal(p)
                if n is None:
                    continue
                ids = []
                for v, (u, w) in zip(p, planar_uv(p, n)):
                    u = inset + u * (1 - 2 * inset)
                    w = inset + w * (1 - 2 * inset)
                    t = ((col + u) / cols, (rows - 1 - row + w) / rows)
                    vi = ref(vidx, vs, tuple(round(c / 16.0 - 0.5, 6) for c in v)) + nv
                    ti = ref(tidx, vts, (round(t[0], 6), round(t[1], 6))) + nt
                    ids.append(f"{vi}/{ti}")
                for k in range(1, len(ids) - 1):
                    fs.append(f"f {ids[0]} {ids[k]} {ids[k + 1]}")
        lines.append(f"o {pname}")
        lines += [f"v {fmt(x)} {fmt(y)} {fmt(z)}" for x, y, z in vs] + [f"vt {fmt(u)} {fmt(v)}" for u, v in vts]
        lines.append("usemtl atlas")
        lines += fs
        nv += len(vs); nt += len(vts)
        counts[pname] = len(fs)
    with open(os.path.join(outdir, name + ".obj"), "w") as f:
        f.write("\n".join(lines) + "\n")
    with open(os.path.join(outdir, name + ".mtl"), "w") as f:
        f.write(f"newmtl atlas\nKa 0 0 0\nKd 1 1 1\nd 1\nmap_Kd {tex}\n")
    if origins:
        txt = []
        for pname, (x, y, z) in origins.items():
            txt += [f"o {pname}", f"f originX {fmt(x / 16 - 0.5)}", f"f originY {fmt(y / 16 - 0.5)}", f"f originZ {fmt(z / 16 - 0.5)}"]
        with open(os.path.join(outdir, name + ".txt"), "w") as f:
            f.write("\n".join(txt) + "\n")
    return counts

def fmt(v):
    v = round(v, 6)
    return repr(0.0 if v == 0 else v)

def out_dir(res, name):
    d = os.path.join(res, "assets/eln/model", name)
    os.makedirs(d, exist_ok=True)
    return d

# ---------- composed preview ----------
def compose(objs, offsets, out, name, scale=0.5):
    """Preview several OBJs together: OBJS paths, OFFSETS (dx, dy, dz) in blocks, scene scaled by SCALE."""
    import elncheck
    faces, tex = [], {}
    for i, (o, off) in enumerate(zip(objs, offsets)):
        _, fs, tx, _ = elncheck.load(o)
        for mat, vs, uv in fs:
            faces.append((f"{i}:{mat}", [tuple((p[k] + off[k]) * scale for k in range(3)) for p in vs], uv))
        for m, p in tx.items():
            tex[f"{i}:{m}"] = p
    cx = sum(o[0] for o in offsets) / len(offsets) * scale
    faces = [(m, [(p[0] - cx, p[1], p[2]) for p in vs], uv) for m, vs, uv in faces]
    elncheck.preview(faces, tex, out, name)
