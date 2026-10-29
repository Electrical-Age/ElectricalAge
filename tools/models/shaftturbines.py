#!/usr/bin/env python3
"""Shaft steam turbines (HP Steam Turbine `shaftsteamhp`, LP Steam Turbine `shaftsteamlp`; README-shaft.md).

A steam turbine sitting across the shaft line (along z at x = 8, y = 8 px), half couplings on both z faces: a cast-iron
casing split at the shaft height (bolted horizontal flange), bearing pedestals on a skid, a governor valve chest on top
by the inlet with a pressure gauge facing the front, a paint band, a cast exhaust duct/hood to a large flange at the
back centre and the signal floor terminal on the front.
  HP: compact barrel (r 5), small (alloy) inlet flange at the top centre with the valve chest just below it.
  LP: near full block, double-flow (a central inlet belt, two flows widening to the ends), large inlet flange on top,
      an exhaust hood across the back to the large exhaust flange; the valve chest on the front shoulder.

Parts: `main` (static), `rotor` (shaft, couplings, a turning gear and a bolted hub outside the casing; origin on the
shaft line, (8, 8, 8) px), `valve` (the governor valve's stem and crosshead; the render lifts it by up to 1.5 px, so it
stays under y = 14.5 px at rest).

Usage: shaftturbines.py RESOURCES_DIR  -> assets/eln/model/shaftsteam{hp,lp}/
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Model, box, prism, tube
from oilkit import signal_plate, dial, bolt_ring, obox
from shaftkit import AX, AY, CENTRE_ORIGIN, PAINT, pipe_flange, shaft_ends, shaft, write_model, preview

C = 8.0

# ---------- rotor pieces ----------
def turning_gear(z0, z1, r=2.0, teeth=10, mat="darksteel"):
    """A spur turning gear on the shaft between z0 and z1: a disc with square teeth and a brass hub collar."""
    ms = [prism("z", AX, AY, r, z0, z1, mat, n=teeth)]
    for k in range(teeth):
        a = 2 * math.pi * k / teeth
        ca, sa = math.cos(a), math.sin(a)
        ms.append(obox((AX + (r + 0.2) * ca, AY + (r + 0.2) * sa, (z0 + z1) / 2), (ca, sa, 0), (-sa, ca, 0), (0, 0, 1),
                       0.3, 0.3, (z1 - z0) / 2, mat))
    return ms

def bolted_hub(z0, z1, r=1.8, bolts=4, face=1):
    """A bolted coupling hub: steel disc, brass bolt heads on the FACE side (+1: towards z+, -1: z-)."""
    ms = [prism("z", AX, AY, r, z0, z1, "steel", n=8)]
    zb = (z1, z1 + 0.25) if face > 0 else (z0 - 0.25, z0)
    for k in range(bolts):
        a = 2 * math.pi * (k + 0.5) / bolts
        ms.append(prism("z", AX + (r - 0.5) * math.cos(a), AY + (r - 0.5) * math.sin(a), 0.25, zb[0], zb[1], "brass", n=6,
                        caps=(face < 0, face > 0)))
    return ms

def rotor_part(gear_z, hub_z):
    m = Model()
    m.add(*shaft_ends(0.0, 16.0))     # (one shaft through the block; it disappears into the bearings and casing)
    m.add(*turning_gear(*gear_z))
    m.add(*bolted_hub(*hub_z, face=-1))
    return m

# ---------- static pieces ----------
def pedestal(z0, z1, bottom, half_w=2.0):
    """A bearing pedestal under the shaft: cast block from BOTTOM up to the shaft, a split bearing cap round it."""
    return [box(C - half_w, bottom, z0, C + half_w, AY - 0.5, z1, "castiron"),
            box(C - half_w - 0.4, bottom, z0 - 0.2, C + half_w + 0.4, bottom + 0.5, z1 + 0.2, "castiron"),
            prism("z", AX, AY, 1.7, z0 + 0.1, z1 - 0.1, "darksteel", n=8),
            box(C - 2.1, AY - 0.3, z0 + 0.15, C + 2.1, AY + 0.3, z1 - 0.15, "darksteel")]  # (cap's split lugs)

def split_flange(xc, r, z0, z1, side, bolts):
    """The horizontal split-line flange on one flank of a casing of radius R about (xc, 8): a lip at the shaft height
    along z, brass bolt heads on top. SIDE -1 = the front flank (x-), +1 = the back."""
    xin = xc + side * (r - 0.6)
    xout = xc + side * (r + 0.8)
    x0, x1 = min(xin, xout), max(xin, xout)
    ms = [box(x0, AY - 0.4, z0, x1, AY + 0.4, z1, "castiron")]
    xb = xc + side * (r + 0.35)
    for k in range(bolts):
        z = z0 + (z1 - z0) * (k + 0.5) / bolts
        ms.append(prism("y", xb, z, 0.25, AY + 0.4, AY + 0.7, "brass", n=6, caps=(False, True)))
    return ms

def valve_part(vx, vz, top):
    """Governor valve stem and crosshead, rising out of the bonnet (top at TOP, y); at rest it ends 0.5 px above it,
    so lifted by 1.5 px it stays in the block for TOP <= 14."""
    m = Model()
    m.add(prism("y", vx, vz, 0.3, top - 0.3, top + 0.2, "steel", n=6, caps=(False, False)))
    m.add(box(vx - 0.35, top + 0.2, vz - 0.9, vx + 0.35, top + 0.45, vz + 0.9, "darksteel"))
    m.add(prism("y", vx, vz, 0.3, top + 0.45, top + 0.5, "brass", n=6, caps=(False, True)))
    return m

def bonnet(vx, vz, y0, top):
    """Valve bonnet on the chest (static, top at TOP) and the two yoke posts guiding the stem's crosshead."""
    ms = [prism("y", vx, vz, 0.9, y0, top - 0.25, "steel", n=8, caps=(False, True)),
          prism("y", vx, vz, 0.5, top - 0.25, top, "darksteel", n=8, caps=(False, True))]
    for s in (-1, 1):
        ms.append(box(vx - 0.2, y0, vz + s * 0.95, vx + 0.2, 15.75, vz + s * 1.2, "darksteel", faces="nsewu"))
    return ms

def front_terminal():
    """Signal floor terminal on the front, and the junction box behind it (on the skid line)."""
    return signal_plate() + [box(0.85, 0.0, 6.75, 1.75, 3.25, 9.25, "darksteel", faces="nsewu")]

def gauge(x, y, z, r=0.9):
    """Pressure gauge facing the front with its dial face at x."""
    return dial(x, y, z, r, "xn", depth=0.5)

# ---------- HP ----------
def hp():
    m = Model()
    R = 5.0
    # skid with two cross sills, bearing pedestals, casing saddles
    m.add(box(1.75, 0, 1.5, 14.25, 0.75, 14.5, "darksteel", faces="nsewu"))
    for z0, z1 in ((1.75, 3.25), (12.75, 14.25)):
        m.add(*pedestal(z0, z1, 0.75))
    for z in (4.5, 10.75):
        m.add(box(4.25, 0.75, z, 11.75, 4.0, z + 0.75, "castiron"))
    # casing: barrel with end covers, a paint band at each end of the barrel
    m.add(prism("z", AX, AY, R, 4.5, 11.5, "castiron", n=12, caps=(False, False)))
    m.add(prism("z", AX, AY, 2.25, 3.5, 4.5, "castiron", n=12, r_end=R, caps=(True, False)))
    m.add(prism("z", AX, AY, R, 11.5, 12.5, "castiron", n=12, r_end=2.25, caps=(False, True)))
    for z in (4.6, 10.65):
        m.add(tube("z", AX, AY, R - 0.1, R + 0.3, z, z + 0.75, "paint", n=12, inner=False))
    # split-line flange: front full length, back either side of the exhaust duct
    m.add(*split_flange(C, R, 4.5, 11.5, -1, 4))
    m.add(*split_flange(C, R, 4.5, 5.4, 1, 1), *split_flange(C, R, 10.6, 11.5, 1, 1))
    # exhaust: cast duct from the barrel to the large flange at the back centre, a stand under it to the back floor
    m.add(prism("x", AY, C, 3.25, 11.0, 14.5, "castiron", n=12, caps=(False, False)))
    m.add(*pipe_flange("x16", AY, C, "large", stub=0.5))
    m.add(box(12.25, 0.75, 6.5, 14.5, 5.0, 9.5, "castiron"))
    m.add(box(12.0, 0, 5.75, 15.9, 0.75, 10.25, "darksteel", faces="nsewu"))
    # governor valve chest on top, the small inlet flange above its middle, the valve on its front end
    m.add(box(3.75, 10.0, 5.75, 10.25, 12.75, 10.25, "castiron"))
    m.add(box(3.5, 12.75, 5.5, 10.5, 13.1, 10.5, "castiron"))          # chest cover
    m.add(*bolt_ring("y", C, C, 1.9, 13.1, 13.3, 4, rb=0.2))
    m.add(prism("y", C, C, 1.1, 13.1, 15.0, "castiron", n=10, caps=(False, False)))   # inlet neck
    m.add(*pipe_flange("y16", C, C, "small", stub=0.5, mat="steel"))
    vx, vz, vtop = 5.0, 8.0, 14.0
    m.add(*bonnet(vx, vz, 13.1, vtop))
    # pressure gauge on the chest's front
    m.add(prism("x", 11.5, 9.0, 0.2, 3.0, 3.75, "brass", n=6, caps=(False, False)))
    m.add(*gauge(2.5, 11.5, 9.0))
    # signal terminal on the front; a conduit up to the governor
    m.add(*front_terminal())
    m.add(prism("y", 1.3, 9.5, 0.3, 3.25, 7.6, "darksteel", n=6, caps=(False, False)))
    m.add(box(1.0, 7.6, 9.2, 2.6, 7.6 + 0.6, 9.8, "darksteel"))
    rotor = rotor_part(gear_z=(14.5, 15.1), hub_z=(1.0, 1.5))
    valve = valve_part(vx, vz, vtop)
    hitbox = [(1.75, 0, 1.5, 14.25, 3.0, 14.5),           # skid, pedestals, saddles
              (2.25, 3.0, 3.5, 13.75, 13.0, 12.5),        # casing and split flange
              (3.5, 10.0, 5.5, 10.5, 16.0, 10.5),         # valve chest, inlet flange, valve
              (12.0, 4.25, 4.25, 16.0, 11.75, 11.75),     # exhaust duct and flange
              (5.75, 5.75, 0.0, 10.25, 10.25, 16.0),      # shaft, couplings, gear, hub
              (0.0, 0.0, 6.5, 1.75, 4.0, 9.5)]            # signal terminal
    return [("main", m), ("rotor", rotor), ("valve", valve)], {"rotor": CENTRE_ORIGIN, "valve": (vx, vtop, vz)}, hitbox

# ---------- LP ----------
def lp():
    m = Model()
    RB, RE = 6.0, 6.75          # belt radius, flow end radius
    # skid, pedestals
    m.add(box(1.75, 0, 1.25, 14.25, 0.75, 14.75, "darksteel", faces="nsewu"))
    for z0, z1 in ((1.75, 3.25), (12.75, 14.25)):
        m.add(*pedestal(z0, z1, 0.75, half_w=1.75))
    # casing: central inlet belt, two flows widening to the ends, end walls
    m.add(prism("z", AX, AY, RB, 6.0, 10.0, "paint", n=14, caps=(False, False)))      # (the painted inlet belt)
    m.add(prism("z", AX, AY, RE, 4.0, 6.0, "castiron", n=14, r_end=RB, caps=(False, False)))
    m.add(prism("z", AX, AY, RB, 10.0, 12.0, "castiron", n=14, r_end=RE, caps=(False, False)))
    m.add(prism("z", AX, AY, 2.5, 3.4, 4.0, "castiron", n=14, r_end=RE, caps=(True, False)))
    m.add(prism("z", AX, AY, RE, 12.0, 12.6, "castiron", n=14, r_end=2.5, caps=(False, True)))
    # feet under the flows (on the skid)
    for z in (4.5, 10.75):
        m.add(box(3.0, 0.75, z, 11.0, 2.25, z + 0.75, "castiron"))
    # split-line flange on the front flank (the hood covers the back)
    m.add(*split_flange(C, RB, 6.0, 10.0, -1, 3))
    m.add(*split_flange(C, RE - 0.35, 4.0, 5.6, -1, 1), *split_flange(C, RE - 0.35, 10.4, 12.0, -1, 1))
    # exhaust hood across the back (both flows), the large flange at the back centre, a foot to the back floor
    m.add(box(11.5, 0.75, 3.75, 14.75, 12.0, 12.25, "castiron"))
    m.add(box(11.25, 12.0, 3.5, 15.0, 12.4, 12.5, "castiron"))               # hood cover lip
    for z in (5.5, 10.5):                                                     # ribs
        m.add(box(14.75, 1.0, z - 0.3, 15.25, 11.5, z + 0.3, "castiron"))
    m.add(*pipe_flange("x16", AY, C, "large", stub=0.25))
    m.add(box(11.25, 0, 4.0, 15.9, 0.75, 12.0, "darksteel", faces="nsewu"))
    # inlet: neck from the belt to the large flange at the top centre
    m.add(prism("y", C, C, 3.0, 13.0, 14.75, "castiron", n=12, caps=(False, False)))   # (the flange stub carries on)
    m.add(tube("y", C, C, 2.95, 3.4, 13.5, 14.1, "castiron", n=12, inner=False))
    m.add(*pipe_flange("y16", C, C, "large", stub=0.25))
    # governor valve chest on the front shoulder, feeding the neck
    m.add(box(2.0, 9.5, 6.0, 6.5, 12.75, 10.0, "castiron"))
    m.add(box(1.75, 12.75, 5.75, 6.0, 13.1, 10.25, "castiron"))               # chest cover
    vx, vz, vtop = 3.25, 8.0, 14.0
    m.add(*bonnet(vx, vz, 13.1, vtop))
    for z in (6.25, 9.75):
        m.add(prism("y", 5.4, z, 0.2, 13.1, 13.3, "brass", n=6))
    # pressure gauge on the chest's front
    m.add(prism("x", 11.5, 9.0, 0.2, 1.5, 2.0, "brass", n=6, caps=(False, False)))
    m.add(*gauge(1.0, 11.5, 9.0))
    # signal terminal on the front, conduit up to the governor
    m.add(*front_terminal())
    m.add(prism("y", 1.3, 9.6, 0.3, 3.25, 7.6, "darksteel", n=6, caps=(False, False)))
    rotor = rotor_part(gear_z=(14.6, 15.2), hub_z=(1.0, 1.5))
    valve = valve_part(vx, vz, vtop)
    hitbox = [(1.75, 0, 1.25, 14.25, 2.0, 14.75),          # skid, feet
              (1.0, 1.25, 3.4, 15.0, 15.0, 12.6),          # casing, hood
              (3.75, 13.0, 3.75, 12.25, 16.0, 12.25),      # inlet neck and flange
              (1.75, 9.5, 5.75, 6.5, 16.0, 10.25),         # valve chest and valve
              (14.0, 4.25, 4.25, 16.0, 11.75, 11.75),      # exhaust flange
              (5.75, 5.75, 0.0, 10.25, 10.25, 16.0),       # shaft, couplings, gear, hub
              (0.0, 0.0, 6.5, 1.75, 4.0, 9.5)]             # signal terminal
    return [("main", m), ("rotor", rotor), ("valve", valve)], {"rotor": CENTRE_ORIGIN, "valve": (vx, vtop, vz)}, hitbox

def main():
    res = sys.argv[1]
    for name, fn, paint in (("shaftsteamhp", hp, PAINT["hp"]), ("shaftsteamlp", lp, PAINT["lp"])):
        parts, origins, hitbox = fn()
        d, counts = write_model(res, name, parts, origins, paint, hitbox)
        print(name, counts, "total", sum(counts.values()))
        if "--preview" in sys.argv:
            preview(res, name, sys.argv[sys.argv.index("--preview") + 1])

if __name__ == "__main__":
    main()
