#!/usr/bin/env python3
"""Tar sand separator (50V / 200V): a heated twin-shaft paddle trough (hot-water extraction / conditioning mixer).

Drawn by MaceratorDescriptor (OilSeparatorDescriptor extends it), which needs parts main, rot1, rot2 and turns
rot1 about (0,0,-1) and rot2 about (0,0,1) through their .txt origins by the same angle: two counter-rotating
paddle shafts along z, coupled by a pair of equal spur gears on the z- end (meshed with a half-tooth offset, so they
stay meshed at every angle). It also draws the input item at the block centre ~12.5 px up (in the open trough,
above the froth) and the output item ~2.4 px up (on the sand tray under the trough, between the legs).

Design frame: mcmodel px (0..16, corner origin). x = 0 is the front (XN); power is on the two z faces (powerLrdu:
not front/back), so the floor terminal plates are on z = 0 and z = 16.
  trough (tier-painted heating jacket, steel rims) on four legs, froth surface with paddles breaking through it,
  feed hopper on the front rim, motor standing on a gearbox at the z+ end, froth overflow launder + oil outlet
  pipe with valve on the back (x+), sand spout and sand tray under the trough.

Usage: tarsandseparator.py RESOURCES_DIR  -> assets/eln/model/tarsandseparator{50v,200v}/
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Model, Mesh, box, prism, tube, oriented
from oilkit import (atlas, write_parts, out_dir, obox, turned, terminal_plate, handwheel, bolt_ring, PAINT)

TIERS = {
    # sx: shaft x (A, B); sy: shaft height; tx/tz: trough outer span; ty: trough bottom, froth, rim top;
    # pr: paddle radius; np: paddles per shaft; mr: motor radius; fins: motor fin heights
    "50v": dict(sx=(5.75, 10.25), sy=9.5, tx=(2.5, 13.5), tz=(2.5, 12.0), ty=(6.0, 9.6, 13.0),
                pr=2.1, np=3, mr=1.6, fins=(14.25,), teeth=8),
    "200v": dict(sx=(5.5, 10.5), sy=9.75, tx=(2.0, 14.0), tz=(2.25, 12.25), ty=(6.0, 9.85, 13.5),
                 pr=2.3, np=4, mr=1.9, fins=(13.9, 14.7), teeth=8),
}
GZ = (0.9, 1.7)          # gear z span (outside the z- end wall)

TOOTH = ((-0.27, 0.0), (-0.12, 1.0), (0.12, 1.0), (0.27, 0.0))   # (fraction of pitch, 0 = root, 1 = tip)

def gear_outline(r_root, r_tip, teeth, phase):
    pitch = 2 * math.pi / teeth
    return [(phase + k * pitch + da * pitch, r_root + h * (r_tip - r_root)) for k in range(teeth) for da, h in TOOTH]

def gear(cx, cy, z0, z1, r_root, r_tip, teeth, phase, mat="brass"):
    """Spur gear along z: trapezoid teeth; walls as quads, faces fanned from the centre (outline not convex)."""
    pts = gear_outline(r_root, r_tip, teeth, phase)
    m = Mesh(mat)
    P = lambda z, a, r: (cx + r * math.cos(a), cy + r * math.sin(a), z)
    n = len(pts)
    for i in range(n):
        (a0, r0), (a1, r1) = pts[i], pts[(i + 1) % n]
        mid = (a0 + a1) / 2
        m.poly(*oriented([P(z0, a0, r0), P(z0, a1, r1), P(z1, a1, r1), P(z1, a0, r0)], (math.cos(mid), math.sin(mid), 0)))
        for z, s in ((z0, -1), (z1, 1)):
            m.poly(*oriented([(cx, cy, z), P(z, a0, r0), P(z, a1, r1)], (0, 0, s)))
    return m

def gear_radii(t):
    rp = (t["sx"][1] - t["sx"][0]) / 2          # pitch radius: the gears are equal, centres 2 rp apart
    return rp - 0.5, rp + 0.35

def rotor(t, which):
    """One paddle shaft with its gear. WHICH 0 = rot1 (x-, turns about -z), 1 = rot2 (x+, turns about +z)."""
    cx, cy = t["sx"][which], t["sy"]
    tz0, tz1 = t["tz"]
    m = Model()
    zend = tz1 - 0.4 if which == 0 else tz1 + 1.5              # B runs on into the gearbox
    m.add(prism("z", cx, cy, 0.45, GZ[0] - 0.3, zend, "darksteel", n=6))
    # gear: pitch radius = half the centre distance; A has a tooth facing B (angle 0), B a gap facing A (angle pi)
    rp = (t["sx"][1] - t["sx"][0]) / 2
    pitch = 2 * math.pi / t["teeth"]
    ph = 0.0 if which == 0 else math.pi + pitch / 2
    m.add(gear(cx, cy, GZ[0], GZ[1], *gear_radii(t), t["teeth"], ph))
    m.add(prism("z", cx, cy, 0.8, GZ[0] - 0.25, GZ[0], "darksteel", n=6, caps=(True, False)))   # hub nut
    # paddles: a two-armed paddle every `step` along the trough, staggered between the shafts, helical angles
    zi0, zi1 = tz0 + 1.2, tz1 - 1.2
    step = (zi1 - zi0) / t["np"]
    pr = t["pr"]
    for k in range(t["np"]):
        z = zi0 + step * (k + 0.25 + 0.5 * which)
        ang = math.radians(60 * k + 30 * which)
        d = (math.cos(ang), math.sin(ang), 0.0)
        tg = (-d[1], d[0], 0.0)
        m.add(obox((cx, cy, z), d, tg, (0, 0, 1), pr - 0.5, 0.18, 0.18, "steel"))         # arm through the shaft
        for s in (1, -1):
            c = (cx + s * d[0] * (pr - 0.45), cy + s * d[1] * (pr - 0.45), z)
            # blade, pitched 25 degrees about the arm
            p = math.radians(25 * s)
            bz = (tg[0] * math.sin(p), tg[1] * math.sin(p), math.cos(p))
            bt = (tg[0] * math.cos(p), tg[1] * math.cos(p), -math.sin(p))
            m.add(obox(c, d, bt, bz, 0.45, 0.12, 0.7, "steel"))
    return m

def body(t, tier):
    m = Model()
    (x0, x1), (z0, z1), (yb, yf, yr) = t["tx"], t["tz"], t["ty"]
    sxa, sxb = t["sx"]; sy = t["sy"]
    w = 0.6
    # skid rails (reach the front and back faces at the floor), legs, cross bracing
    for z in (z0 + 0.5, z1 - 1.75):
        m.add(box(0.1, 0, z, 15.9, 1.0, z + 1.25, "darksteel", faces="nsewu"))
        for x in (x0 + 0.5, x1 - 1.5):
            m.add(box(x, 1.0, z + 0.1, x + 1.0, yb, z + 1.15, "darksteel"))
        m.add(box(x0 + 1.5, 3.0, z + 0.4, x1 - 1.5, 3.6, z + 0.85, "darksteel"))
    # trough: heated jacket (tier paint) below the froth line, solid inside (the froth hides it), steel rims above
    m.add(box(x0, yb, z0, x1, yf - 0.6, z1, "paint"))
    m.add(box(x0 + 0.1, yf - 0.6, z0 + 0.1, x1 - 0.1, yf, z1 - 0.1, "steel", faces="nsewd"))
    m.add(box(x0 + w, yf - 0.6, z0 + w, x1 - w, yf, z1 - w, "froth", faces="u"))
    m.add(box(x0 - 0.15, yb + 1.0, z0 - 0.15, x1 + 0.15, yb + 1.6, z1 + 0.15, "darksteel", faces="nsewud"))   # jacket band
    rims = [(x0 + 0.1, z0 + 0.1, x0 + w, z1 - 0.1), (x1 - w, z0 + 0.1, x1 - 0.1, z1 - 0.1),
            (x0 + w, z0 + 0.1, x1 - w, z0 + w), (x0 + w, z1 - w, x1 - w, z1 - 0.1)]
    for a0, b0, a1, b1 in rims:
        m.add(box(a0, yf, b0, a1, yr, b1, "steel"))
    for a0, b0, a1, b1 in rims:                                   # rim lip
        m.add(box(a0 - (0.15 if a0 < x0 + 0.2 else 0), yr, b0 - (0.15 if b0 < z0 + 0.2 else 0),
                  a1 + (0.15 if a1 > x1 - 0.2 else 0), yr + 0.4, b1 + (0.15 if b1 > z1 - 0.2 else 0), "darksteel"))
    # bearing blocks on the z- end wall, gears turn in front of them
    for sx in (sxa, sxb):
        m.add(box(sx - 0.9, sy - 0.9, GZ[1] + 0.15, sx + 0.9, sy + 0.9, z0, "darksteel"))
    m.add(box(sxa - 0.6, yb - 0.1, GZ[1] + 0.4, sxb + 0.6, sy - 1.4, z0, "castiron"))         # bearing bracket
    # sand spout + slide gate under the trough, sand tray with a pile on the floor (the output item sits on it)
    m.add(prism("y", 8.0, 8.0, 1.25, 5.25, yb, "castiron", n=4, r_end=1.75, phase=math.pi / 4, caps=(True, False)))
    m.add(box(6.6, 5.25, 9.25, 9.4, 5.6, 10.5, "darksteel"))                                   # gate handle side
    m.add(box(4.75, 0, 4.75, 11.25, 0.6, 11.25, "darksteel", faces="nsewu"))
    m.add(prism("y", 8.0, 8.0, 2.6, 0.6, 1.5, "sand", n=8, r_end=1.2))
    # feed chute over the front rim (tar sand in): a sloped steel pan with side lips, tier-painted hood
    a = math.atan2(2.6, 4.5)
    dx, dy = (math.cos(a), -math.sin(a), 0.0), (math.sin(a), math.cos(a), 0.0)
    cc = (x0 + 0.8, min(yr + 0.6, 13.6), 8.0)
    m.add(obox(cc, dx, dy, (0, 0, 1), 2.4, 0.15, 1.6, "steel"))
    for s in (-1, 1):
        m.add(obox((cc[0] + 0.45 * dy[0], cc[1] + 0.45 * dy[1], 8.0 + s * 1.65), dx, dy, (0, 0, 1), 2.4, 0.6, 0.12, "darksteel"))
    m.add(box(0.4, cc[1] - 2.2, 6.9, 1.0, cc[1] + 1.0, 9.1, "darksteel"))                      # chute bracket
    # heating element terminals on the front of the jacket + conduit to the z- floor terminal
    for z in (z0 + 2.5, z1 - 2.5):
        m.add(prism("x", yb + 2.6, z, 0.55, x0 - 0.9, x0, "ceramic", n=6))
        m.add(prism("x", yb + 2.6, z, 0.3, x0 - 1.2, x0 - 0.9, "copper", n=6))
    m.add(box(x0 - 0.7, yb + 1.6, z0 + 1.6, x0, yb + 2.0, z1 - 1.6, "darksteel"))              # bus bar strap
    # drive: gearbox at the z+ end on B's shaft, on a pedestal; motor standing on it
    gx0, gx1 = sxb - 2.25, sxb + 2.25
    gz0, gz1 = z1 + 0.1, 15.3
    m.add(box(gx0 + 0.5, 0, gz0 + 0.4, gx1 - 0.5, yb + 0.5, gz1 - 0.5, "darksteel", faces="nsewu"))
    m.add(box(gx0, yb + 0.5, gz0, gx1, sy + 2.25, gz1, "castiron"))
    m.add(box(gx0 - 0.15, sy + 2.25, gz0 - 0.1, gx1 + 0.15, sy + 2.6, gz1 + 0.1, "paint"))
    mr = t["mr"]
    mz = min((gz0 + gz1) / 2, 15.85 - mr - 0.35)
    mx = sxb + 0.25
    m.add(prism("y", mx, mz, mr, sy + 2.6, 15.0, "steel", n=10))
    m.add(prism("y", mx, mz, mr - 0.6, 15.0, 15.7, "darksteel", n=10))                         # fan cowl
    for y in t["fins"]:
        m.add(prism("y", mx, mz, mr + 0.35, y - 0.2, y + 0.2, "darksteel", n=10))
    m.add(prism("y", mx, mz, mr + 0.3, sy + 2.9, sy + 3.5, "paint", n=10))
    # motor terminal box (x- side) and cable down the pedestal to the z+ floor terminal
    m.add(box(mx - mr - 0.9, sy + 3.0, mz - 0.8, mx - mr + 0.2, sy + 4.4, mz + 0.8, "steel"))
    m.add(box(gx0 - 0.6, 0.5, mz - 0.35, gx0, sy + 3.0, mz + 0.35, "darksteel"))
    # oil outlet: froth overflow launder on the back rim, down pipe, valve, outlet stub through the back face
    lz0, lz1 = 4.5, 7.5
    lc = (lz0 + lz1) / 2
    m.add(box(x1 + 0.15, yf, lz0, 15.2, yf + 0.5, lz1, "darksteel"))
    m.add(box(x1 + 0.15, yf + 0.5, lz0, 15.2, yf + 1.2, lz0 + 0.3, "darksteel"), box(x1 + 0.15, yf + 0.5, lz1 - 0.3, 15.2, yf + 1.2, lz1, "darksteel"))
    m.add(box(x1 + 0.15, yf + 0.5, lz0 + 0.3, 15.2, yf + 0.6, lz1 - 0.3, "oil", faces="u"))
    px = 14.5
    m.add(prism("y", px, lc, 0.55, 3.0, yf, "darksteel", n=6, caps=(False, False)))
    m.add(box(px - 0.8, 5.0, lc - 0.8, px + 0.8, 6.4, lc + 0.8, "castiron"))                    # valve body
    m.add(prism("z", px, 5.7, 0.25, lc + 0.8, lc + 1.6, "darksteel", n=6, caps=(False, False)))
    m.add(*handwheel(px, 5.7, lc + 1.7, r=0.9, axis="z"))
    m.add(prism("y", px, lc, 0.7, 2.4, 3.0, "darksteel", n=6, caps=(True, False)))              # elbow
    m.add(prism("x", 2.7, lc, 0.55, px, 15.9, "darksteel", n=6, caps=(False, True)))
    m.add(prism("x", 2.7, lc, 0.95, 15.3, 15.8, "steel", n=8))                                  # outlet flange
    # nameplate on the front of the jacket
    m.add(box(x0 - 0.1, yb + 2.4, 7.0, x0, yb + 3.4, 9.5, "brass"))
    # power: floor terminals on the two z faces
    m.add(*turned(terminal_plate(), 1), *turned(terminal_plate(), 3))
    return m

def main():
    res = sys.argv[1]
    for tier, t in TIERS.items():
        name = "tarsandseparator" + tier
        d = out_dir(res, name)
        parts = [("main", body(t, tier)), ("rot1", rotor(t, 0)), ("rot2", rotor(t, 1))]
        origins = {"rot1": (t["sx"][0], t["sy"], 8.0), "rot2": (t["sx"][1], t["sy"], 8.0)}
        counts = write_parts(d, name, parts, origins)
        atlas(os.path.join(d, "tex.png"), PAINT[tier])
        print(name, counts, "total", sum(counts.values()))

if __name__ == "__main__":
    main()
