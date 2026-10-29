#!/usr/bin/env python3
"""Gas Turbine (README-shaft.md): a compact industrial gas turbine lying across the shaft line, on a skid.

Along the shaft (z): the starter/accessory end and compressor casing on the left (z = 0: the starter motor couples
there), the air intake plenum in the middle with a louvred bellmouth grille on the front (the compressor wheel, in
`rotor`, turns behind it), the hot exhaust collector behind the plenum with two combustor cans on top, the turbine
casing on the right. Ports: hot exhaust at the back centre (large flange, the collector and duct heat-tinted); fuel
at a small flange on the top centre, piped through the fuel valve (`valve`: its stem and handwheel) to the cans.
Signal floor terminal on the front under the grille.

Parts: `main`, `rotor` (shaft, couplings, compressor wheel; turned about z through the shaft line), `valve` (lifted
by the render with the valve's opening).

Design frame: shaftkit px (0..16, corner origin), front XN (x = 0), shaft along z at x = 8, y = 8.

Usage: shaftgas.py RESOURCES_DIR [PREVIEW_DIR]  -> assets/eln/model/shaftgas/
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Model, box, prism, tube
from oilkit import signal_plate, obox, bolt_ring, handwheel
from shaftkit import AX, AY, shaft, shaft_ends, pipe_flange, write_model, preview, PAINT, CENTRE_ORIGIN

PX = (1.0, AX)            # intake plenum x span (front open, grille; back: the collector)
PY = (4.25, 12.75)        # plenum y span (outside)
PZ = (5.5, 10.5)          # plenum z span (outside)
T = 0.5                   # plate thickness
FAN_R = 3.4
FUEL_Y = 13.0             # fuel line height
VX = 9.9                  # fuel valve x
CANS = ((11.5, 6.5), (11.5, 9.5))

def main_part():
    m = Model()
    # skid, with feet out to the back face at the corners (the floor middle stays clear)
    m.add(box(1.0, 0, 1.25, 15.0, 1.0, 14.75, "darksteel", faces="nsewu"))
    for z0 in (1.5, 12.5):
        m.add(box(15.0, 0, z0, 16.0, 0.75, z0 + 2.0, "darksteel", faces="nsewu"))
    # compressor casing (left), its saddle; the starter end bell with a bolted pad
    m.add(prism("z", AX, AY, 4.25, 1.75, PZ[0] + 0.25, "paint", n=12, caps=(False, True)))
    m.add(prism("z", AX, AY, 2.6, 1.0, 1.75, "castiron", n=12, r_end=4.25, caps=(True, False)))
    m.add(tube("z", AX, AY, 4.15, 4.6, 4.75, 5.15, "darksteel", n=12, inner=False))
    m.add(box(4.25, 1.0, 2.25, 11.75, AY - 3.5, 4.25, "castiron"))
    # intake plenum: four plates round an open front (the grille) and the collector behind
    m.add(box(PX[0], PY[1] - T, PZ[0], PX[1], PY[1], PZ[1], "paint"))             # roof
    m.add(box(PX[0], PY[0], PZ[0], PX[1], PY[0] + T, PZ[1], "paint"))             # floor
    m.add(box(PX[0], PY[0] + T, PZ[0], PX[1], PY[1] - T, PZ[0] + T, "paint"))     # side walls
    m.add(box(PX[0], PY[0] + T, PZ[1] - T, PX[1], PY[1] - T, PZ[1], "paint"))
    m.add(box(PX[0] + 0.5, 1.0, 6.0, PX[1], PY[0], 10.0, "castiron"))              # its plinth
    # bellmouth: a steel frame round the opening, standing 0.5 px proud of the plenum
    fx = (PX[0] - 0.5, PX[0] + 0.25)
    m.add(box(fx[0], PY[1] - 0.75, PZ[0] - 0.25, fx[1], PY[1] + 0.25, PZ[1] + 0.25, "steel"))
    m.add(box(fx[0], PY[0] - 0.25, PZ[0] - 0.25, fx[1], PY[0] + 0.75, PZ[1] + 0.25, "steel"))
    m.add(box(fx[0], PY[0] + 0.75, PZ[0] - 0.25, fx[1], PY[1] - 0.75, PZ[0] + 0.75, "steel"))
    m.add(box(fx[0], PY[0] + 0.75, PZ[1] - 0.75, fx[1], PY[1] - 0.75, PZ[1] + 0.25, "steel"))
    # louvres: tilted slats across the opening (the wheel shows between them)
    y = PY[0] + 1.4
    while y < PY[1] - 1.0:
        m.add(obox((PX[0] + 0.6, y, 8.0), (0.8, 0.6, 0), (-0.6, 0.8, 0), (0, 0, 1), 0.55, 0.08, (PZ[1] - PZ[0]) / 2 - 0.75, "louvre"))
        y += 1.2
    m.add(box(PX[0] + 0.2, PY[0] + 0.75, 7.85, PX[0] + 0.5, PY[1] - 0.75, 8.15, "darksteel"))   # mullion
    # exhaust collector behind the plenum (heat-tinted), on a pedestal, the duct to the back flange
    m.add(box(PX[1] + 0.5, 1.0, 5.0, 14.5, 3.0, 11.0, "darksteel"))
    m.add(box(PX[1], 3.0, 5.0, 14.5, 11.0, 11.0, "hotcopper"))
    for x in (10.5, 13.0):
        m.add(box(x, 2.9, 4.9, x + 0.4, 11.1, 11.1, "darksteel", faces="nsewu"))   # stiffener bands
    m.add(prism("x", AY, 8.0, 3.25, 14.5, 15.0, "hotcopper", n=14, caps=(False, False)))
    m.add(*pipe_flange("x16", AY, 8.0, "large", stub=0.5))
    # turbine casing (right): flanged, a cone down to the shaft
    m.add(prism("z", AX, AY, 3.75, 11.0, 14.25, "castiron", n=14, caps=(True, False)))
    m.add(prism("z", AX, AY, 3.75, 14.25, 15.0, "castiron", n=14, r_end=2.6, caps=(False, True)))
    m.add(tube("z", AX, AY, 3.65, 4.15, 13.0, 13.4, "darksteel", n=14, inner=False))
    m.add(box(4.5, 1.0, 12.0, 11.5, AY - 3.25, 13.5, "castiron"))
    # combustor cans on the collector, the fuel manifold and line to the top flange through the valve
    for x, z in CANS:
        m.add(prism("y", x, z, 1.1, 10.75, 12.25, "steel", n=8, caps=(False, False)))
        m.add(prism("y", x, z, 1.1, 12.25, 12.75, "steel", n=8, r_end=0.5, caps=(False, True)))
        m.add(tube("y", x, z, 1.0, 1.35, 11.25, 11.6, "darksteel", n=8, inner=False))
        m.add(prism("y", x, z, 0.3, 12.75, FUEL_Y, "brass", n=6, caps=(False, False)))    # injector
    m.add(prism("z", CANS[0][0], FUEL_Y, 0.35, CANS[0][1] - 0.35, CANS[1][1] + 0.35, "copper", n=6))
    m.add(prism("x", FUEL_Y, 8.0, 0.35, 8.0, CANS[0][0], "copper", n=6, caps=(False, False)))
    m.add(*pipe_flange("y16", AX, 8.0, "small", stub=15.0 - FUEL_Y + 0.35))
    m.add(prism("y", AX, 8.0, 0.75, FUEL_Y - 0.6, 13.5, "steel", n=8, caps=(True, False)))
    m.add(box(VX - 0.55, FUEL_Y - 0.6, 7.45, VX + 0.55, FUEL_Y + 0.6, 8.55, "castiron"))      # valve body
    m.add(prism("y", VX, 8.0, 0.45, FUEL_Y + 0.6, FUEL_Y + 1.0, "steel", n=8))               # bonnet
    # signal terminal on the front, a conduit back into the skid
    m.add(*signal_plate())
    m.add(box(0.85, 0, 7.25, 1.75, 1.25, 8.75, "darksteel", faces="nsewu"))
    return m

def rotor_part():
    m = Model()
    m.add(*shaft_ends(1.0, 15.0))
    m.add(shaft(PZ[0], PZ[1]))
    # compressor wheel: hub with a nose cone and pitched blades
    m.add(prism("z", AX, AY, 1.3, 7.0, 9.0, "darksteel", n=10))
    nb = 8
    for k in range(nb):
        a = 2 * math.pi * (k + 0.5) / nb
        u = (math.cos(a), math.sin(a), 0.0)
        t = (-u[1], u[0], 0.0)
        p = math.radians(40)
        bt = tuple(math.cos(p) * t[i] + math.sin(p) * (0, 0, 1)[i] for i in range(3))
        bn = tuple(-math.sin(p) * t[i] + math.cos(p) * (0, 0, 1)[i] for i in range(3))
        rc = (1.2 + FAN_R) / 2
        m.add(obox((AX + rc * u[0], AY + rc * u[1], 8.0), u, bt, bn, (FAN_R - 1.2) / 2, 1.0, 0.08, "steel"))
    return m

def valve_part():
    m = Model()
    top = FUEL_Y + 1.0
    m.add(prism("y", VX, 8.0, 0.18, top, top + 0.25, "steel", n=6, caps=(False, True)))
    m.add(*handwheel(VX, top + 0.25, 8.0, r=0.8))
    return m

HITBOX = [
    (1.0, 0.0, 1.25, 16.0, 1.0, 14.75),                       # skid and feet
    (3.75, 1.0, 1.0, 12.25, 12.25, 5.5),                      # compressor casing
    (0.5, 1.0, 5.0, 14.5, 12.75, 11.0),                       # plenum, collector
    (4.25, 1.0, 11.0, 11.75, 12.25, 15.0),                    # turbine casing
    (14.5, 4.25, 4.25, 16.0, 11.75, 11.75),                   # exhaust duct and flange
    (7.25, 10.75, 5.25, 12.75, 16.0, 10.75),                  # cans, fuel line, valve, fuel flange
    (0.0, 0.0, 6.5, 1.0, 4.0, 9.5),                           # signal terminal
    (5.75, 5.75, 0.0, 10.25, 10.25, 16.0),                    # shaft and couplings
]

def main():
    res = sys.argv[1]
    name = "shaftgas"
    d, counts = write_model(res, name, [("main", main_part()), ("rotor", rotor_part()), ("valve", valve_part())],
                            origins={"rotor": CENTRE_ORIGIN, "valve": (VX, FUEL_Y + 1.0, 8.0)}, paint=PAINT["gas"], hitbox=HITBOX)
    print(name, counts, sum(counts.values()))
    if len(sys.argv) > 2:
        preview(res, name, sys.argv[2])

if __name__ == "__main__":
    main()
