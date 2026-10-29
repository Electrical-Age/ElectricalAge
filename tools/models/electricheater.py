#!/usr/bin/env python3
"""Electric heater (50V / 200V): a resistor cabinet that pushes all its heat out of its back into a boiler or column.

Drawn by ElectricHeaterRender -> ElectricHeaterDescriptor.draw(), which draws the parts main/dissipator/body that
exist (this model has `main` and `element`, the coils, which glow when hot), after front.glRotateXnRef(). Connections (ElectricHeaterElement): electrical power
on the two side faces (z = 0 and z = 16 in the model, LRDU Down = at the floor), thermal on front.back() (XP, x = 16),
nothing on the front.

Design frame: mcmodel px (0..16, corner origin), front XN (x = 0):
  front: framed window with a grille over glowing coiled elements on ceramic end insulators, a dial, an amber pilot
         lamp and a nameplate on the top rail (decoration: the model is static, the regulator lives in the GUI);
  top:   tier-painted cap with a louvred vent;
  sides: floor terminal plates (copper studs, tier stripe) with a terminal box behind each;
  back:  copper heat pipes into a cast-iron backing plate and a full-height heat-tinted copper contact flange that
         presses against the boiler (down to the floor, 11 px wide at the bottom middle).

Usage: electricheater.py RESOURCES_DIR  -> assets/eln/model/electricheater{50v,200v}/
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcmodel import Model, box, prism
from oilkit import atlas, write_parts, out_dir, turned, terminal_plate, dial, PAINT

TIERS = {
    # fx: frame front x; bx: body front (window depth); z: cabinet z span; top: cabinet top; win: window y span;
    # el: element heights; er: element radius; fl: flange (z0, z1, top); pipes: heat pipe (y, z); bolts: (y, z)
    "50v": dict(fx=1.5, bx=3.25, z=(3.0, 13.0), top=11.25, win=(2.75, 9.75), el=(4.0, 6.25, 8.5), er=0.65,
                fl=(2.75, 13.25, 11.75), pipes=[(3.5, 5.5), (3.5, 10.5), (8.0, 5.5), (8.0, 10.5)],
                bolts=[(1.0, 2.75), (1.0, 13.25), (11.5, 2.75), (11.5, 13.25), (6.25, 2.75), (6.25, 13.25)]),
    "200v": dict(fx=1.0, bx=2.9, z=(2.0, 14.0), top=12.75, win=(2.75, 11.25), el=(3.9, 6.0, 8.1, 10.2), er=0.7,
                 fl=(1.75, 14.25, 13.25), pipes=[(3.0, 4.75), (3.0, 11.25), (7.0, 4.75), (7.0, 11.25), (11.0, 4.75), (11.0, 11.25)],
                 bolts=[(1.0, 1.75), (1.0, 14.25), (13.0, 1.75), (13.0, 14.25), (5.0, 1.75), (5.0, 14.25), (9.0, 1.75), (9.0, 14.25)]),
}

def heater(t):
    """The cabinet (part main) and the coils (part element, drawn again glowing when hot: Incandescence)."""
    m = Model()
    hot = Model()
    fx, bx = t["fx"], t["bx"]
    z0, z1 = t["z"]
    top = t["top"]
    w0, w1 = t["win"]
    zi0, zi1 = z0 + 1.25, z1 - 1.25          # window z span
    back = 12.5                               # cabinet back
    bp = 13.6                                 # backing plate front
    # plinth (reaches the front face at the floor)
    m.add(box(0.1, 0, z0 - 0.25, bp, 1.25, z1 + 0.25, "darksteel", faces="nsewu"))
    # cabinet body behind the window, dark inner wall, framed front
    m.add(box(bx, 1.25, z0, back, top, z1, "steel", faces="nseu"))
    m.add(box(bx - 0.1, w0, zi0, bx, w1, zi1, "darksteel", faces="w"))
    m.add(box(fx, 1.25, z0, bx, top, zi0, "steel", faces="nswu"), box(fx, 1.25, zi1, bx, top, z1, "steel", faces="nswu"))
    m.add(box(fx, w1, zi0, bx, top, zi1, "steel", faces="wud"), box(fx, 1.25, zi0, bx, w0, zi1, "steel", faces="wu"))
    # heating elements: coil on a rod between ceramic end insulators
    ex = (fx + bx) / 2 + 0.25
    for y in t["el"]:
        hot.add(prism("z", ex, y, t["er"], zi0 + 0.75, zi1 - 0.75, "coil", n=6, caps=(False, False)))
        m.add(prism("z", ex, y, t["er"] + 0.25, zi0 + 0.05, zi0 + 0.75, "ceramic", n=6, caps=(False, True)))
        m.add(prism("z", ex, y, t["er"] + 0.25, zi1 - 0.75, zi1 - 0.05, "ceramic", n=6, caps=(True, False)))
    # grille: vertical bars and one cross bar in front of the elements
    nb = int((zi1 - zi0) / 1.25)
    for k in range(1, nb):
        z = zi0 + (zi1 - zi0) * k / nb
        m.add(box(fx + 0.05, w0, z - 0.15, fx + 0.35, w1, z + 0.15, "darksteel", faces="nsw"))
    m.add(box(fx - 0.05, (w0 + w1) / 2 - 0.15, zi0, fx + 0.05, (w0 + w1) / 2 + 0.15, zi1, "darksteel", faces="wud"))
    # top rail: dial, pilot lamp, nameplate
    ry = (w1 + top) / 2
    rr = min(0.7, (top - w1) / 2 - 0.1)
    m.add(*dial(fx, ry, zi1 - 1.2, rr, "xn", depth=0.3))
    m.add(prism("x", ry, zi0 + 1.0, rr * 0.6, fx - 0.3, fx, "lamp", n=6, caps=(True, False)))
    m.add(prism("x", ry, zi0 + 1.0, rr * 0.85, fx - 0.15, fx, "darksteel", n=6, caps=(True, False)))
    m.add(box(fx - 0.1, ry - rr * 0.6, zi0 + 2.2, fx, ry + rr * 0.6, zi1 - 2.4, "brass", faces="w"))
    # tier cap and louvred vent hood
    m.add(box(fx - 0.15, top, z0 - 0.15, back + 0.15, top + 0.75, z1 + 0.15, "paint"))
    m.add(box(fx + 2.0, top + 0.75, z0 + 1.0, back - 1.25, top + 1.2, z1 - 1.0, "louvre", faces="nsewu"))
    # side terminals: floor plates on both z faces, terminal boxes behind them, conduit into the cabinet
    side = [box(5.0, 1.25, 0.85, 11.0, 5.5, z0, "steel", faces="nweu"),
            box(4.85, 5.5, 0.7, 11.15, 5.85, z0, "darksteel", faces="nweud"),
            prism("y", 8.0, (0.85 + z0) / 2, 0.45, 5.85, 6.5, "brass", n=6, caps=(False, True))]
    m.add(*turned(terminal_plate(), 1), *turned(terminal_plate(), 3))
    m.add(*side, *turned(side, 2))
    # heat path: copper heat pipes into the backing plate, bolts, hot contact flange on the back face
    fz0, fz1, ftop = t["fl"]
    for y, z in t["pipes"]:
        m.add(prism("x", y, z, 0.6, back, bp, "copper", n=6, caps=(False, False)))
    m.add(box(bp, 0, fz0 - 0.5, 14.1, ftop + 0.5, fz1 + 0.5, "castiron", faces="nsewu"))
    m.add(box(14.1, 0, fz0, 15.9, ftop, fz1, "hotcopper", faces="nseu"))
    for y, z in t["bolts"]:
        zz = z - 0.25 if z < 8 else z + 0.25
        m.add(prism("x", y, zz, 0.3, bp - 0.3, bp, "brass", n=6, caps=(True, False)))
    return m, hot

def main():
    res = sys.argv[1]
    for tier, t in TIERS.items():
        name = "electricheater" + tier
        d = out_dir(res, name)
        main, element = heater(t)
        counts = write_parts(d, name, [("main", main), ("element", element)])
        atlas(os.path.join(d, "tex.png"), PAINT[tier])
        print(name, counts)

if __name__ == "__main__":
    main()
