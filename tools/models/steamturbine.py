#!/usr/bin/env python3
"""Small steam turbine (50V / 200V Steam Turbine) models: a scroll-cased steam turbine coupled to a generator on a skid.

EA transparent node, drawn by SteamGeneratorRender through Obj3D (see README.md, "1.12 node OBJ"). Design space is
mcmodel's px frame (0..16, corner origin); write_eln centres it. The shaft runs front-back along x:
  x = 0  (west, XN)  FRONT: generator end, terminal plate at floor level (electric out, LRDU Down)
  x = 16 (east, XP)  BACK:  turbine end, second terminal plate, fed by a bus duct along the z- skid rail
  top: steam inlet stub + flange on the scroll, a riser to a flange centred on the top face (a boiler set on top feeds it); z+ side: exhaust vent stack (no steam output, it's a vent).

Usage: steamturbine.py RESOURCES_DIR   (e.g. src/main/resources)
  Writes RESOURCES_DIR/assets/eln/model/steamturbine{50v,200v}/steamturbine*.{obj,mtl} + tex.png (atlas).
"""
import math, os, random, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Model, box, prism, tube, volute

TIERS = {
    # cy: shaft height; rg/gx: generator radius and x span; r0/r1/tx: scroll radii and x span; ri/rf: inlet pipe and
    # flange radius; fins: generator cooling fins; bolts: on the inlet flange
    "50v": dict(paint="paint_green", cy=6.75, rg=3.75, gx=(2.25, 7.25), r0=2.75, r1=4.75, tx=(9.25, 13.25),
                ri=1.1, rf=1.9, fins=(3.0, 6.5), bolts=4),
    "200v": dict(paint="paint_blue", cy=7.25, rg=4.5, gx=(1.75, 7.75), r0=3.25, r1=5.5, tx=(9.0, 13.75),
                 ri=1.4, rf=2.4, fins=(2.5, 3.75, 5.75, 7.0), bolts=6),
}
CZ = 8.0
START = math.radians(40)          # scroll tongue angle (0 = up, 90 = +z); the radius grows from there round to it

# atlas: material -> (col, row), 4x2 cells of 16 px (64x32 png)
CELLS = {"steel": (0, 0), "darksteel": (1, 0), "castiron": (2, 0), "copper": (3, 0),
         "brass": (0, 1), "paint": (1, 1), "glass": (2, 1), "rubber": (3, 1)}
GRID = (4, 2)

def scroll_r(t, ang):
    """Scroll radius at angle ANG (radians, 0 = up, towards +z)."""
    frac = ((ang - START) % (2 * math.pi)) / (2 * math.pi)
    return t["r0"] + (t["r1"] - t["r0"]) * frac

def terminal_plate(m, x0, x1, face_x, stud_out):
    """Steel plate flush with the face (recessed 0.1 px), down to the floor, copper studs, tier stripe on top."""
    m.add(box(x0, 0, 4.5, x1, 4.0, 11.5, "steel"))
    m.add(box(x0, 4.0, 4.5, x1, 4.5, 11.5, "paint"))
    for z in (5.75, 8.0, 10.25):
        m.add(box(min(face_x, stud_out), 1.75, z - 0.5, max(face_x, stud_out), 2.75, z + 0.5, "copper"))
    m.add(box(x0, 0.5, 4.25, x1, 3.5, 4.5, "rubber"), box(x0, 0.5, 11.5, x1, 3.5, 11.75, "rubber"))   # gasket edges

def turbine(tier):
    t = TIERS[tier]
    cy, rg, (gx0, gx1), (tx0, tx1) = t["cy"], t["rg"], t["gx"], t["tx"]
    tcx = (tx0 + tx1) / 2
    m = Model()
    # --- skid: two rails along x, cross members
    m.add(box(1.0, 0, 2.0, 15.0, 1.5, 4.0, "darksteel"), box(1.0, 0, 12.0, 15.0, 1.5, 14.0, "darksteel"))
    feet = (gx0 + 0.5, gx1 - 2.0)
    for x in feet + (tcx - 0.75,):                                                            # cross members
        m.add(box(x, 0.25, 4.0, x + 1.5, 1.25, 12.0, "darksteel"))
    # --- generator: ribbed stator, end bells, tier band, cradle feet, terminal box
    m.add(prism("x", cy, CZ, rg, gx0, gx1, "steel", n=12, caps=(False, False)))
    m.add(prism("x", cy, CZ, rg - 1.25, gx0 - 0.75, gx0, "steel", n=12, r_end=rg, caps=(True, False)))
    m.add(prism("x", cy, CZ, rg, gx1, gx1 + 0.6, "steel", n=12, r_end=rg - 1.0, caps=(False, True)))
    for x in t["fins"]:
        m.add(tube("x", cy, CZ, rg - 0.1, rg + 0.6, x, x + 0.5, "darksteel", n=12, inner=False))
    bx = (gx0 + gx1) / 2
    m.add(tube("x", cy, CZ, rg - 0.1, rg + 0.4, bx - 0.75, bx + 0.75, "paint", n=12, inner=False))
    foot = cy - rg + 0.6
    for x in feet:
        m.add(box(x, 1.25, 4.75, x + 1.5, foot, 6.5, "steel"), box(x, 1.25, 9.5, x + 1.5, foot, 11.25, "steel"))
    m.add(prism("x", cy, CZ, 0.8, gx0 - 1.25, gx0 - 0.75, "darksteel", n=8))                 # front bearing cap
    # terminal box on the z- flank, conduit down to the bus duct on the skid
    tz0 = CZ - rg - 1.25
    m.add(box(bx - 1.6, cy - 1.75, tz0, bx + 1.6, cy + 1.25, CZ - rg + 0.75, "steel"))
    m.add(box(bx - 1.85, cy + 1.25, tz0 - 0.25, bx + 1.85, cy + 1.6, CZ - rg + 0.75, "darksteel"))   # lid
    m.add(box(bx - 0.75, 1.5, tz0 + 0.25, bx + 0.75, cy - 1.75, tz0 + 1.75, "darksteel"))      # conduit
    m.add(box(bx - 1.0, 1.5, 2.5, 15.25, 3.0, 4.5, "darksteel"))                               # bus duct (on the rail) to the back plate
    # nameplate on the z+ flank
    m.add(box(bx - 1.5, cy - 0.75, CZ + rg + 0.0, bx + 1.5, cy + 0.75, CZ + rg + 0.1, "brass"))
    # --- shaft and coupling
    m.add(prism("x", cy, CZ, 0.6, gx1 + 0.6, tx0 - 0.5, "darksteel", n=8, caps=(False, False)))
    cx = (gx1 + 0.6 + tx0 - 0.5) / 2
    m.add(prism("x", cy, CZ, 1.5, cx - 0.4, cx + 0.4, "copper", n=8))
    # --- turbine: scroll casing, end covers, pedestal
    m.add(volute("x", cy, CZ, t["r0"], t["r1"], tx0, tx1, "castiron", n=20, start=START))
    m.add(prism("x", cy, CZ, t["r0"] - 1.25, tx0 - 0.5, tx0, "castiron", n=10, r_end=t["r0"] - 0.5, caps=(True, False)))
    m.add(prism("x", cy, CZ, t["r0"] - 0.25, tx1, tx1 + 0.75, "castiron", n=10, r_end=t["r0"] - 1.25, caps=(False, True)))
    m.add(prism("x", cy, CZ, 1.1, tx1 + 0.75, 15.0, "darksteel", n=8))                        # rear bearing
    bottom = cy - scroll_r(t, math.pi) + 0.5
    m.add(box(tx0 + 0.5, 1.25, 5.75, tx1 - 0.5, bottom, 10.25, "darksteel"))                   # pedestal
    # governor housing on the back of the casing
    m.add(box(tx1 - 0.25, cy + 1.25, CZ - 1.5, tx1 + 1.5, cy + 3.25, CZ + 1.5, "steel"))
    m.add(box(tx1 - 0.25, cy + 3.25, CZ - 1.6, tx1 + 1.6, cy + 3.5, CZ + 1.6, "paint"))
    # --- steam inlet on top: stub, flange, bolts, tier collar, pressure gauge facing the front
    top = cy + scroll_r(t, 0.0)
    fy = min(15.25, top + 2.25)
    m.add(prism("y", tcx, CZ, t["ri"], cy + t["r0"], fy, "castiron", n=10, caps=(False, False)))
    m.add(prism("y", tcx, CZ, t["ri"] + 0.25, top - 0.25, top + 0.25, "paint", n=10, caps=(False, True)))
    m.add(prism("y", tcx, CZ, t["rf"], fy, fy + 0.75, "steel", n=12))
    m.add(prism("y", tcx, CZ, t["ri"] - 0.2, fy + 0.75, fy + 0.76, "darksteel", n=10, caps=(False, True)))   # bore
    br = (t["ri"] + t["rf"]) / 2 + 0.1
    for k in range(t["bolts"]):
        a = 2 * math.pi * (k + 0.5) / t["bolts"]
        m.add(prism("y", tcx + br * math.cos(a), CZ + br * math.sin(a), 0.25, fy + 0.75, fy + 1.0, "brass", n=6))
    # inlet riser to the top centre (pipes phase 2): a boiler set on top couples its dome port here
    ry = 15.0
    m.add(prism("y", tcx, CZ, t["ri"] - 0.3, fy + 1.0, ry + 0.4, "castiron", n=8, caps=(False, False)))
    m.add(prism("x", ry, CZ, t["ri"] - 0.3, 8.0, tcx + 0.5, "castiron", n=8, caps=(False, True)))
    m.add(prism("y", 8.0, CZ, t["ri"] - 0.3, ry - 0.4, 15.5, "castiron", n=8, caps=(True, False)))
    m.add(prism("y", 8.0, CZ, 1.9, 15.5, 16.0, "steel", n=12, caps=(False, True)))           # top flange (centred)
    gy = (top + fy) / 2 + 0.25
    m.add(prism("x", gy, CZ, 0.25, tcx - t["ri"] - 0.75, tcx - t["ri"] + 0.2, "brass", n=6, caps=(False, False)))
    m.add(prism("x", gy, CZ, 0.9, tcx - t["ri"] - 1.25, tcx - t["ri"] - 0.75, "brass", n=10))
    m.add(prism("x", gy, CZ, 0.7, tcx - t["ri"] - 1.3, tcx - t["ri"] - 1.25, "glass", n=10, caps=(True, False)))
    # --- exhaust vent stack on the z+ side (vents to air; the turbine has no steam output)
    ez = CZ + scroll_r(t, math.pi / 2) + 1.0
    m.add(prism("z", tcx, cy - 0.25, 0.9, CZ + 1.0, ez + 0.9, "castiron", n=8, caps=(False, True)))
    m.add(prism("y", tcx, ez, 0.9, cy - 0.25, 12.75, "castiron", n=8, caps=(True, False)))
    m.add(prism("y", tcx, ez, 1.25, 12.75, 13.75, "darksteel", n=8))                          # rain cap / silencer
    # --- terminal plates (electric out, front and back, floor level)
    terminal_plate(m, 0.1, 0.85, 0.1, 0.0)
    terminal_plate(m, 15.15, 15.9, 15.9, 16.0)
    return m

# ---------- textures: 16x16 cells of one atlas (procedural, deterministic) ----------
def atlas(path, tier):
    from PIL import Image
    rnd = random.Random(1234)
    im = Image.new("RGBA", (16 * GRID[0], 16 * GRID[1]))
    def cell(name, base, noise=10, shade=None):
        col, row = CELLS[name]
        for y in range(16):
            for x in range(16):
                d = rnd.randint(-noise, noise)
                c = [max(0, min(255, b + d)) for b in base]
                if shade:
                    k = shade(x, y)
                    c = [max(0, min(255, v + k)) for v in c]
                im.putpixel((16 * col + x, 16 * row + y), tuple(c) + (255,))
    cell("steel", (128, 134, 140), 8)
    cell("darksteel", (62, 64, 68), 6)
    cell("castiron", (88, 84, 80), 10, shade=lambda x, y: -10 if (x * 7 + y * 3) % 11 == 0 else 0)
    cell("copper", (196, 110, 70), 14)
    cell("brass", (200, 170, 80), 10)
    cell("paint", (56, 156, 84) if tier == "50v" else (52, 102, 176), 8)
    cell("glass", (228, 230, 222), 4, shade=lambda x, y: -120 if x == 8 and y < 9 else 0)
    cell("rubber", (34, 34, 36), 4)
    im.save(path)

def main():
    res = sys.argv[1]
    for tier in TIERS:
        name = "steamturbine" + tier
        out = os.path.join(res, "assets/eln/model", name)
        os.makedirs(out, exist_ok=True)
        tris = turbine(tier).write_eln(out, name, CELLS, GRID)
        atlas(os.path.join(out, "tex.png"), tier)
        print(name, "triangles", tris)

if __name__ == "__main__":
    main()
