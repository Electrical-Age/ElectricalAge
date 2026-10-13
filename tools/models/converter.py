#!/usr/bin/env python3
"""Energy converter (EA -> other) models: a small synchronous condenser on a skid, one per tier.

Orientation: the EA side (the node's front, where EA cables attach) is NORTH (z = 0); the export
side (front.back()) is SOUTH (z = 16). Blockstate variants rotate it for FRONT = 0..6.

Usage: converter.py OUTDIR [--pack]
  Writes assets/eln/models/block/energy_converter_{lvu,mvu,hvu}.{obj,mtl,json},
  assets/eln/textures/blocks/converter/*.png, and (with --pack) blockstate overrides + pack.mcmeta
  so OUTDIR can be dropped into a client's resourcepacks/ for testing.
"""
import json, math, os, random, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Model, box, prism, tube

TIERS = {
    # name: (tier paint, bushing count, bushing height px, sheds per bushing, bushing radius)
    "lvu": dict(paint="paint_green", bushings=2, h=1.6, sheds=0, r=0.75),
    "mvu": dict(paint="paint_blue", bushings=3, h=2.4, sheds=1, r=0.8),
    "hvu": dict(paint="paint_orange", bushings=3, h=3.0, sheds=2, r=0.85),
}

CX, CY = 8.0, 6.75          # stator axis (x, y); the axis runs north-south (z)
R = 4.75                    # stator radius (px)

def condenser(tier):
    t = TIERS[tier]
    m = Model()
    # --- skid: two rails and cross members, full footprint so it sits on the block floor
    m.add(box(1.5, 0, 0.5, 4, 1.5, 15.5, "darksteel"), box(12, 0, 0.5, 14.5, 1.5, 15.5, "darksteel"))
    m.add(box(4, 0.25, 2, 12, 1.25, 3.5, "darksteel"), box(4, 0.25, 12.5, 12, 1.25, 14, "darksteel"))
    # feet / cradle under the stator
    m.add(box(3.5, 1.5, 4, 5.5, 3.5, 6, "steel"), box(10.5, 1.5, 4, 12.5, 3.5, 6, "steel"))
    m.add(box(3.5, 1.5, 10, 5.5, 3.5, 12, "steel"), box(10.5, 1.5, 10, 12.5, 3.5, 12, "steel"))
    # --- stator housing (12-gon), end bells, cooling fins, tier band
    m.add(prism("z", CX, CY, R, 3, 13, "steel", n=12, caps=(False, False)))
    m.add(prism("z", CX, CY, R, 2, 3, "steel", n=12, r_end=R, caps=(False, False)))
    m.add(prism("z", CX, CY, R - 1.25, 1.25, 2, "steel", n=12, r_end=R, caps=(True, False)))      # north end bell
    m.add(prism("z", CX, CY, R, 13, 14, "steel", n=12, caps=(False, False)))
    m.add(prism("z", CX, CY, R, 14, 14.75, "steel", n=12, r_end=R - 1.25, caps=(False, True)))    # south end bell
    for z in (3.5, 5.0, 11.0, 12.5):
        m.add(tube("z", CX, CY, R - 0.1, R + 0.7, z, z + 0.5, "darksteel", n=12, inner=False))
    m.add(tube("z", CX, CY, R - 0.1, R + 0.45, 6.5, 9.5, t["paint"], n=12, inner=False))                     # tier band
    # --- shaft: north stub into the EA terminal plate, south coupling to the export flange
    m.add(prism("z", CX, CY, 1.0, 0.75, 1.25, "darksteel", n=8))
    m.add(prism("z", CX, CY, 1.0, 14.75, 15.0, "darksteel", n=8))
    m.add(prism("z", CX, CY, 2.0, 15.0, 15.5, "copper", n=8))                                   # coupling
    # --- EA side (north): terminal plate flush with the face, down to the skid (cables meet it at floor level)
    m.add(box(4.5, 0, 0, 11.5, 4.0, 0.75, "steel"))
    for x in (5.75, 8.0, 10.25):
        m.add(box(x - 0.5, 1.75, -0.01, x + 0.5, 2.75, 0.01, "copper"))                         # terminal studs
    m.add(box(4.5, 3.75, 0, 11.5, 4.25, 0.75, t["paint"]))                                     # tier stripe on the plate
    # --- export side (south): flange plate flush with the face
    m.add(box(5.0, 0.0, 15.25, 11.0, 2.5, 16.0, "darksteel"))                                 # export pedestal (floor level)
    m.add(box(5.5, 2.5, 15.5, 10.5, 10.0, 16.0, "darksteel"))                                 # export plate frame
    m.add(box(6.25, 3.25, 15.99, 9.75, 9.25, 16.01, "export", faces="s"))                      # export panel
    for bx, byy in ((5.9, 2.9), (10.1, 2.9), (5.9, 9.6), (10.1, 9.6)):
        m.add(box(bx - 0.25, byy - 0.25, 15.99, bx + 0.25, byy + 0.25, 16.02, "brass", faces="s"))
    # --- terminal box on top, with tier-specific bushings
    top = CY + R
    m.add(box(5.0, top - 0.75, 4.5, 11.0, top + 1.25, 11.5, "steel"))
    m.add(box(4.75, top + 1.25, 4.25, 11.25, top + 1.5, 11.75, "darksteel"))                  # lid
    by = top + 1.5
    n = t["bushings"]
    xs = [8.0] if n == 1 else [8.0 + (i - (n - 1) / 2) * 2.25 for i in range(n)]
    for x in xs:
        z = 8.0
        m.add(prism("y", x, z, t["r"], by, by + t["h"], "porcelain", n=8, caps=(False, False)))
        m.add(prism("y", x, z, 0.45, by + t["h"], by + t["h"] + 0.5, "copper", n=6, caps=(False, True)))
        for k in range(t["sheds"]):
            sy = by + (k + 1) * t["h"] / (t["sheds"] + 1) - 0.2
            m.add(prism("y", x, z, t["r"] + 0.55, sy, sy + 0.35, "porcelain", n=8))
        m.add(prism("y", x, z, t["r"] + 0.2, by, by + 0.3, t["paint"], n=8))                    # tier collar
    # nameplate on the east side of the terminal box
    m.add(box(11.0, top - 0.25, 6.0, 11.1, top + 0.75, 10.0, "brass"))
    return m

# ---------- textures (16x16, procedural, deterministic) ----------
def textures(outdir):
    from PIL import Image
    rnd = random.Random(1234)
    def tex(name, base, noise=10, stripes=None):
        im = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                d = rnd.randint(-noise, noise)
                c = [max(0, min(255, b + d)) for b in base]
                if stripes and stripes(x, y):
                    c = [max(0, v - 28) for v in c]
                im.putpixel((x, y), tuple(c) + (255,))
        os.makedirs(outdir, exist_ok=True)
        im.save(os.path.join(outdir, name + ".png"))
    tex("steel", (128, 134, 140), 8)
    tex("darksteel", (62, 64, 68), 6)
    tex("copper", (196, 110, 70), 14)
    tex("brass", (200, 170, 80), 10)
    tex("porcelain", (226, 222, 210), 6, stripes=lambda x, y: y % 4 == 3)
    tex("paint_green", (56, 156, 84), 8)
    tex("paint_blue", (52, 102, 176), 8)
    tex("paint_orange", (226, 132, 44), 8)
    tex("export", (150, 46, 40), 8, stripes=lambda x, y: (x + y) % 8 < 2)

def model_json(tier, mats):
    tx = {m: f"eln:blocks/converter/{m}" for m in mats}
    tx["particle"] = "eln:blocks/converter/steel"
    return {"parent": "minecraft:block/block", "loader": "neoforge:obj",
            "model": f"eln:models/block/energy_converter_{tier}.obj", "flip_v": True,
            "automatic_culling": True, "shade_quads": True, "textures": tx}

# FRONT (vanilla 3D data value) -> rotation of the north-facing model
ROT = {0: {"x": 90}, 1: {"x": 270}, 2: {}, 3: {"y": 180}, 4: {"y": 270}, 5: {"y": 90}, 6: {}}

def blockstate(tier):
    return {"variants": {f"front={f}": dict({"model": f"eln:block/energy_converter_{tier}"}, **r) for f, r in ROT.items()}}

def main():
    out = sys.argv[1]
    pack = "--pack" in sys.argv
    mdir = os.path.join(out, "assets/eln/models/block")
    os.makedirs(mdir, exist_ok=True)
    for tier in TIERS:
        m = condenser(tier)
        m.write_obj(os.path.join(mdir, f"energy_converter_{tier}.obj"), f"energy_converter_{tier}.mtl")
        m.write_mtl(os.path.join(mdir, f"energy_converter_{tier}.mtl"))
        with open(os.path.join(mdir, f"energy_converter_{tier}.json"), "w") as f:
            json.dump(model_json(tier, m.materials()), f, indent=1)
        print(tier, "triangles", m.triangles(), "materials", m.materials())
        if pack:
            bdir = os.path.join(out, "assets/eln/blockstates"); os.makedirs(bdir, exist_ok=True)
            idir = os.path.join(out, "assets/eln/items"); os.makedirs(idir, exist_ok=True)
            name = f"energy_converter_eln_to_other_{tier}block"
            with open(os.path.join(bdir, name + ".json"), "w") as f: json.dump(blockstate(tier), f, indent=1)
            with open(os.path.join(idir, name + ".json"), "w") as f:
                json.dump({"model": {"type": "minecraft:model", "model": f"eln:block/energy_converter_{tier}"}}, f, indent=1)
    textures(os.path.join(out, "assets/eln/textures/blocks/converter"))
    if pack:
        with open(os.path.join(out, "pack.mcmeta"), "w") as f:
            json.dump({"pack": {"description": "EA energy converter models (test)", "min_format": 97, "max_format": 97}}, f)

if __name__ == "__main__":
    main()
