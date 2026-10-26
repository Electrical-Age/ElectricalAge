#!/usr/bin/env python3
"""Small steam turbine (50V / 200V Steam Turbine) models: a scroll-cased steam turbine coupled to a generator on a skid.

EA transparent node, drawn by SteamGeneratorRender through Obj3D (see README.md, "1.12 node OBJ"). Design space is
mcmodel's px frame (0..16, corner origin); write_eln centres it. The shaft runs front-back along x:
  x = 0  (west, XN)  FRONT: generator end, terminal plate at floor level (electric out, LRDU Down)
  x = 16 (east, XP)  BACK:  turbine end, second terminal plate, fed by a bus duct along the z- skid rail
  top: steam inlet stub + flange on the scroll, a riser to a flange centred on the top face (a boiler set on top feeds it); z+ side: exhaust vent stack, open at the top: the renderer puffs steam from it (part "vent"); with a condenser against z+, part "exhaust" instead: a duct to a flange on that face.

Usage: steamturbine.py RESOURCES_DIR   (e.g. src/main/resources)
  Writes RESOURCES_DIR/assets/eln/model/steamturbine{50v,200v}/steamturbine*.{obj,mtl} + tex.png (atlas).
"""
import math, os, random, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Model, box, prism, tube, volute

TIERS = {
    # cy: shaft height; rg/gx: generator radius and x span; r0/r1/tx: scroll radii and x span; fins: generator cooling
    # fins; bolts: on the top flange
    "50v": dict(paint="paint_green", cy=6.75, rg=3.75, gx=(2.25, 7.25), r0=2.75, r1=4.75, tx=(9.25, 13.25),
                fins=(3.0, 6.5), bolts=4),
    "200v": dict(paint="paint_blue", cy=7.25, rg=4.5, gx=(1.75, 7.75), r0=3.25, r1=5.5, tx=(9.0, 13.75),
                 fins=(2.5, 3.75, 5.75, 7.0), bolts=6),
}
CZ = 8.0
PIPE_R = 2.0                      # an iron pipe's radius (PipePart.BORE[0] / 2 voxels)
EXHAUST_R = 2.25                  # the exhaust duct into a condenser (condenser.py: the same, at the tier's shaft height)
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
    # --- shaft and coupling: a part of their own, which the renderer turns with the shaft's speed (Baughn); the coupling's
    # bolts show it turning
    rotor = Model()
    rotor.add(prism("x", cy, CZ, 0.6, gx1 + 0.6, tx0 - 0.5, "darksteel", n=8, caps=(False, False)))
    cx = (gx1 + 0.6 + tx0 - 0.5) / 2
    rotor.add(prism("x", cy, CZ, 1.5, cx - 0.4, cx + 0.4, "copper", n=8))
    for k in range(4):
        a = math.pi / 2 * k + math.pi / 4
        rotor.add(prism("x", cy + 1.45 * math.cos(a), CZ + 1.45 * math.sin(a), 0.3, cx - 0.55, cx + 0.55, "brass", n=6))
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
    # --- steam inlet: an iron pipe's bore (4 px, Baughn: the standard for this tier) the whole way, as a laid pipe looks:
    # up out of the scroll, across to the middle, up to a pipe's flange on the top face (a boiler set on top, or a pipe
    # laid to it, couples there)
    top = cy + scroll_r(t, 0.0)
    yh = min(15.75 - PIPE_R, top + PIPE_R)  # (the run across: clear of the scroll where there's room, under the top flange's face)
    m.add(prism("y", tcx, CZ, PIPE_R, top - 1.0, yh + PIPE_R, "castiron", n=12, caps=(False, True)))
    m.add(prism("y", tcx, CZ, PIPE_R + 0.25, top - 0.5, min(top + 0.25, yh - PIPE_R + 0.25), "paint", n=12, caps=(False, True)))   # tier collar
    m.add(prism("x", yh, CZ, PIPE_R, 8.0, tcx, "castiron", n=12, caps=(False, False)))
    m.add(prism("y", 8.0, CZ, PIPE_R, yh - PIPE_R, 15.0, "castiron", n=12, caps=(True, False)))
    m.add(prism("y", 8.0, CZ, PIPE_R + 0.75, 15.0, 16.0, "steel", n=12))                     # top flange, a laid pipe's
    br = PIPE_R + 0.4
    for k in range(t["bolts"]):
        a = 2 * math.pi * (k + 0.5) / t["bolts"]
        m.add(prism("y", 8.0 + br * math.cos(a), CZ + br * math.sin(a), 0.25, 14.75, 15.0, "brass", n=6))
    # pressure gauge on the bend over the generator, facing the front
    gx = 8.0 - PIPE_R
    m.add(prism("x", yh, CZ, 0.25, gx - 0.5, gx + 0.2, "brass", n=6, caps=(False, False)))
    m.add(prism("x", yh, CZ, 0.9, gx - 1.0, gx - 0.5, "brass", n=10))
    m.add(prism("x", yh, CZ, 0.7, gx - 1.05, gx - 1.0, "glass", n=10, caps=(True, False)))
    # --- exhaust vent stack on the z+ side (vents to air): a part of its own ("vent"), drawn unless a condenser stands
    # against that side; then the "exhaust" part, a duct out to a flange on the z+ face, joins the condenser's
    ez = CZ + scroll_r(t, math.pi / 2) + 1.0
    vent_m = Model()
    vent_m.add(prism("z", tcx, cy - 0.25, 0.9, CZ + 1.0, ez + 0.9, "castiron", n=8, caps=(False, True)))
    # straight up to an open outlet: the steam leaves there (Baughn). (A boiler set on top would hide it, but nobody
    # builds like that, Baughn: pipes are cheap.)
    vent_m.add(prism("y", tcx, ez, 0.9, cy - 0.25, 12.75, "castiron", n=8, caps=(True, False)))
    vent_m.add(tube("y", tcx, ez, 0.8, 1.25, 12.75, 13.75, "darksteel", n=8))                      # open outlet
    vent_m.add(prism("y", tcx, ez, 0.85, 12.0, 12.5, "rubber", n=8, caps=(False, True)))           # (dark inside the bore)
    # (out of the scroll along z, an elbow, along x to the face's middle, out to the flange: the condenser's duct meets
    # it at the middle of its face whichever way it faces)
    ex_m = Model()
    zr = 13.0
    ex_m.add(prism("z", tcx, cy, EXHAUST_R, CZ + 1.0, zr + EXHAUST_R, "castiron", n=12, caps=(False, True)))
    ex_m.add(prism("x", cy, zr, EXHAUST_R, 8.0 - EXHAUST_R, tcx + EXHAUST_R, "castiron", n=12, caps=(True, True)))
    ex_m.add(prism("z", 8.0, cy, EXHAUST_R, zr, 15.0, "castiron", n=12, caps=(False, False)))
    ex_m.add(prism("z", 8.0, cy, EXHAUST_R + 0.75, 15.0, 16.0, "steel", n=12, caps=(True, False)))   # flange on the face
    for k in range(t["bolts"]):
        a = 2 * math.pi * (k + 0.5) / t["bolts"]
        br = EXHAUST_R + 0.4
        ex_m.add(prism("z", 8.0 + br * math.cos(a), cy + br * math.sin(a), 0.25, 14.75, 15.0, "brass", n=6))
    # --- terminal plates (electric out, front and back, floor level)
    terminal_plate(m, 0.1, 0.85, 0.1, 0.0)
    terminal_plate(m, 15.15, 15.9, 15.9, 16.0)
    # --- signal plate on the z- flank (left, seen from the front): external control of the throttle (Baughn 2026-10-26)
    from oilkit import signal_plate, turned
    m.add(*turned(signal_plate(), 3))
    vent = (tcx / 16 - 0.5, 13.75 / 16 - 0.5, ez / 16 - 0.5)   # (the outlet's centre, model coordinates; it faces up)
    return m, rotor, vent, vent_m, ex_m

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
        m, rotor, vent, vent_m, ex_m = turbine(tier)
        tris = m.write_eln(out, name, CELLS, GRID, extra=[("rotor", rotor), ("vent", vent_m), ("exhaust", ex_m)])
        with open(os.path.join(out, name + ".txt"), "w") as f:   # the rotor turns about the shaft (Obj3D part origin); the vent
            f.write(f"o rotor\nf originY {TIERS[tier]['cy'] / 16 - 0.5}\nf originZ {CZ / 16 - 0.5}\n")
            f.write(f"o main\nf ventX {vent[0]:.5f}\nf ventY {vent[1]:.5f}\nf ventZ {vent[2]:.5f}\n")
        atlas(os.path.join(out, "tex.png"), tier)
        print(name, "triangles", tris)

if __name__ == "__main__":
    main()
