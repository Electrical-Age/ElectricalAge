#!/usr/bin/env python3
"""shaftkit: shared pieces for the shaft machines (README-shaft.md): turbines, generators, motor, joints, flywheel,
tachometer, plant condenser, heat exchanger.

Builds on mcmodel / oilkit (px frame, 0..16, corner origin, front XN at x = 0; right = z 16, left = z 0, back = x 16).
  - AXIS: the shaft line every shaft part shares: along z at x = 8, y = 8 (so neighbours' shafts meet).
  - shaft(z0, z1) / half_coupling(face) / shaft_ends(z_lo, z_hi): the rotating shaft and the half coupling at a shaft
    face (z = 0 or 16): a disc 0.5 px deep with four bolts, so two neighbours form one bolted coupling.
  - pipe_flange(axis, c1, c2, a_face, inward, size): a port's flange as the pipe renderer draws a pipe's: 'small'
    (iron/alloy, r 1.5) or 'large' (large alloy, r 3.75), 1 px deep, with a stub of bore inward.
  - write_model(res, name, parts, origins, paint, hitbox): writes assets/eln/model/NAME/ (OBJ, MTL, tex.png, NAME.txt
    origins, NAME.hitbox) via oilkit.write_parts / atlas.
  - preview(res, name, outdir): elncheck preview PNG sheet into OUTDIR.
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Model, box, prism, tube
from oilkit import atlas, write_parts, out_dir, turned, bolt_ring, CELLS

AX, AY = 8.0, 8.0            # the shaft line (x, y) in px; it runs along z
SHAFT_R = 1.0
COUPLING_R = 2.25
COUPLING_DEPTH = 0.5          # each half; two neighbours' halves make one 1 px coupling
PIPE_R = {"small": 0.75, "large": 3.0}       # drawn pipe radius (PipePart.DRAWN / 2: alloy 1.5, large 6)
FLANGE_R = {"small": 1.5, "large": 3.75}     # VoxelGeometry: a pipe's flange at a face is its radius + 0.75, 1 px deep
CENTRE_ORIGIN = (AX, AY, 8.0)                # the rotor's origin (it turns about z through the shaft line)

# device paint colours (the 'paint' cell of the atlas)
PAINT = {"hp": (52, 102, 176), "lp": (40, 84, 150), "gas": (150, 60, 40), "gen": (196, 150, 40), "motor": (52, 102, 176),
         "plain": (90, 96, 104), "condenser": (60, 120, 130), "hx": (120, 110, 60)}

def shaft(z0, z1, mat="darksteel", r=SHAFT_R):
    return prism("z", AX, AY, r, z0, z1, mat, n=8, caps=(False, False))

def half_coupling(face):
    """The half coupling at a shaft face: FACE 0 (z = 0, left) or 16 (z = 16, right). Rotates with the rotor."""
    if face == 0:
        z0, z1, bz = 0.0, COUPLING_DEPTH, (COUPLING_DEPTH, COUPLING_DEPTH + 0.25)
    else:
        z0, z1, bz = 16.0 - COUPLING_DEPTH, 16.0, (16.0 - COUPLING_DEPTH - 0.25, 16.0 - COUPLING_DEPTH)
    ms = [prism("z", AX, AY, COUPLING_R, z0, z1, "steel", n=12)]
    for k in range(4):
        a = math.pi / 4 + math.pi / 2 * k
        ms.append(prism("z", AX + 1.6 * math.cos(a), AY + 1.6 * math.sin(a), 0.3, bz[0], bz[1], "brass", n=6))
    return ms

def shaft_ends(z_lo, z_hi, faces=(0, 16)):
    """Shaft from each coupling face to the body (z_lo: where the body starts on the left, z_hi: where it ends on the
    right), and the half couplings. Returns meshes for the rotor part."""
    ms = []
    if 0 in faces:
        ms += [shaft(COUPLING_DEPTH, z_lo)] + half_coupling(0)
    if 16 in faces:
        ms += [shaft(z_hi, 16.0 - COUPLING_DEPTH)] + half_coupling(16)
    return ms

def pipe_flange(face, c1, c2, size="small", stub=1.5, mat="steel", bolts=None):
    """A port's flange on a block face, as the pipe renderer draws a pipe's end: FACE one of 'x0', 'x16', 'y16', 'z0',
    'z16'; (c1, c2) the spot on the face in the other two axes (xyz order); a stub of the pipe's bore STUB px inward."""
    fr, pr = FLANGE_R[size], PIPE_R[size]
    axis = face[0]
    at0 = face.endswith("0") and not face.endswith("16")
    nb = bolts if bolts is not None else (6 if size == "large" else 4)
    if at0:
        ms = [prism(axis, c1, c2, fr, 0.0, 1.0, mat, n=14 if size == "large" else 10, caps=(True, True)),
              prism(axis, c1, c2, pr, 1.0, 1.0 + stub, mat, n=12 if size == "large" else 8, caps=(False, True))]
        ms += bolt_ring(axis, c1, c2, fr - 0.45, 1.0, 1.25, nb, rb=0.25 if size == "large" else 0.18)
    else:
        ms = [prism(axis, c1, c2, fr, 15.0, 16.0, mat, n=14 if size == "large" else 10, caps=(True, True)),
              prism(axis, c1, c2, pr, 15.0 - stub, 15.0, mat, n=12 if size == "large" else 8, caps=(True, False))]
        for k in range(nb):
            a = 2 * math.pi * (k + 0.5) / nb
            r = fr - 0.45
            ms.append(prism(axis, c1 + r * math.cos(a), c2 + r * math.sin(a), 0.25 if size == "large" else 0.18, 14.75, 15.0, "brass", n=6,
                            caps=(True, False)))
    return ms

def write_model(res, name, parts, origins=None, paint=PAINT["plain"], hitbox=()):
    """Write the model: PARTS [(part, Model)], ORIGINS {part: (x, y, z) px}, HITBOX [(x0, y0, z0, x1, y1, z1) px, front
    XN frame]: NAME.hitbox, one box a line, read by the game for the block's outline and collision (turned with it)."""
    d = out_dir(res, name)
    counts = write_parts(d, name, parts, origins)
    atlas(os.path.join(d, "tex.png"), paint)
    with open(os.path.join(d, name + ".hitbox"), "w") as f:
        for b in hitbox:
            f.write(" ".join(f"{v:g}" for v in b) + "\n")
    return d, counts

def preview(res, name, outdir):
    """The elncheck preview sheet of NAME into OUTDIR (and its checks' output)."""
    import subprocess
    obj = os.path.join(res, "assets/eln/model", name, name + ".obj")
    os.makedirs(outdir, exist_ok=True)
    subprocess.run([sys.executable, os.path.join(os.path.dirname(os.path.abspath(__file__)), "elncheck.py"), obj, "--preview", outdir])
