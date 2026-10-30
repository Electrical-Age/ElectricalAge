package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;
import static mods.eln.cad.models.Mc.v;
import static mods.eln.cad.models.OilKit.boltRing;
import static mods.eln.cad.models.OilKit.obox;
import static mods.eln.cad.models.OilKit.signalPlate;
import static mods.eln.cad.models.OilKit.terminalPlate;
import static mods.eln.cad.models.OilKit.turned;
import static mods.eln.cad.models.ShaftKit.AX;
import static mods.eln.cad.models.ShaftKit.AY;
import static mods.eln.cad.models.ShaftKit.CENTRE_ORIGIN;
import static mods.eln.cad.models.ShaftKit.halfCoupling;
import static mods.eln.cad.models.ShaftKit.shaft;

/**
 * Shaft parts: 8 kW and 4 kW Generator, Joint, Joint Hub, Flywheel, Tachometer. Front XN (x = 0), right z = 16; the
 * shaft runs along z at x = 8, y = 8 with a half coupling on each shaft face (the Joint Hub also along x, part
 * rotor_x). Parts: main (static), rotor (turns about z through (8, 8, 8)), hub rotor_x (about x through (8, 8, 8)),
 * tachometer needle (modelled pointing up = 0, turned about x through the dial's centre; as the boiler's:
 * draw(-135 + 270 * frac, 1, 0, 0) sweeps from 7:30 clockwise seen from the front to 4:30).
 */
public final class ShaftParts {
    private ShaftParts() {
    }

    static final double[] SHAFT_BOX = {5.75, 5.75, 0, 10.25, 10.25, 16};

    static List<Mesh> couplings() {
        List<Mesh> ms = new ArrayList<>();
        ms.add(shaft(0.5, 15.5));
        ms.addAll(halfCoupling(0));
        ms.addAll(halfCoupling(16));
        return ms;
    }

    /** Skid rails running front to back (x), touching the front/back faces at the floor. */
    static List<Mesh> railsX(double[][] zPairs) {
        List<Mesh> ms = new ArrayList<>();
        for (double[] p : zPairs) ms.add(box(0.0, 0, p[0], 16.0, 1.25, p[1], "darksteel"));
        return ms;
    }

    static NodeModel model(String name, String paint, double[]... hit) {
        NodeModel n = new NodeModel(name, paint);
        for (double[] h : hit) n.hitbox(h);
        return n;
    }

    static void centre(NodeModel n, String part) {
        n.origin(part, CENTRE_ORIGIN[0], CENTRE_ORIGIN[1], CENTRE_ORIGIN[2]);
    }

    // ---------------------------------------------------------------- generators

    /** Power terminal for the front (x = 0): oilkit plate, a junction box behind it, a conduit up into the stator. */
    static List<Mesh> genTerminal(double rg) {
        List<Mesh> ms = terminalPlate();
        // (the checker: the junction box began 0.1 px behind the plate, which hung on nothing; it now reaches the plate,
        // hidden behind it)
        ms.add(box(0.85, 0, 5.0, 2.75, 4.25, 11.0, "darksteel"));
        ms.add(box(0.95, 4.25, 4.8, 2.95, 4.6, 11.2, "steel"));
        double yc = 6.0, xs = AX - Math.sqrt(rg * rg - (AY - yc) * (AY - yc)) + 0.5;
        ms.add(prism('y', 1.85, 8.0, 0.75, 4.6, 5.0, "brass", 6, false, true));      // gland
        ms.add(prism('y', 1.85, 8.0, 0.45, 5.0, yc + 0.45, "steel", 8, false, true));
        ms.add(prism('x', yc, 8.0, 0.45, 1.85, xs, "steel", 8, false, false));
        return ms;
    }

    /**
     * A generator. rg: stator radius; zs: stator z span; fins: cooling fin z starts (0.4 px thick); rails: skid rail z
     * spans.
     */
    static NodeModel generator(String name, double rg, double zs0, double zs1, double[] fins, double[][] rails) {
        List<Mesh> m = new ArrayList<>();
        double ap = rg * Math.cos(Math.PI / 12);
        // stator (painted), cooling fins
        m.add(prism('z', AX, AY, rg, zs0, zs1, "paint", 12));
        for (double z : fins) m.add(tube('z', AX, AY, rg - 0.1, rg + 0.55, z, z + 0.4, "darksteel", 12, Double.NaN, false));
        // nameplate on the top flat
        double hw = rg > 5 ? 0.9 : 0.7;
        m.add(box(AX - hw, AY + ap - 0.2, 7.0, AX + hw, AY + ap + 0.12, 9.0, "steel"));
        // left end: a fan cage (ring, bars, three-arm spider with the bearing hub); the fan in rotor shows through it
        double cz0 = zs0 - 1.5;
        m.add(tube('z', AX, AY, rg - 1.0, rg - 0.4, cz0, cz0 + 0.5, "castiron", 12));
        for (int k = 0; k < 6; k++) {
            double a = 2 * Math.PI * k / 6;
            m.add(prism('z', AX + (rg - 0.7) * Math.cos(a), AY + (rg - 0.7) * Math.sin(a), 0.3, cz0 + 0.5, zs0, "castiron", 4, false, false));
        }
        for (int k = 0; k < 3; k++) {
            double a = Math.PI / 2 + 2 * Math.PI * k / 3, rr = (rg - 0.7 + 1.3) / 2;
            double[] u = {Math.cos(a), Math.sin(a), 0};
            m.add(obox(v(AX + rr * u[0], AY + rr * u[1], cz0 + 0.25), u, v(-u[1], u[0], 0), v(0, 0, 1), (rg - 0.7 - 1.3) / 2, 0.3, 0.22, "castiron"));
        }
        m.add(prism('z', AX, AY, 1.5, cz0 - 0.25, cz0 + 0.6, "castiron", 10));
        m.add(prism('z', AX, AY, 1.25, 1.0, cz0 - 0.25, "darksteel", 10, true, false));
        // right end: closed end bell, bearing cap
        m.add(prism('z', AX, AY, rg, zs1, zs1 + 1.2, "castiron", 12, rg - 1.6, false, true, Double.NaN));
        m.add(prism('z', AX, AY, 1.4, zs1 + 1.2, zs1 + 1.8, "darksteel", 10, false, true));
        m.addAll(boltRing('z', AX, AY, rg - 2.2, zs1 + 1.2, zs1 + 1.4, 4, "brass", 0.3, 0.0));
        // skid rails (front-back) with cradle saddles, cross ties
        for (double[] r : rails) {
            double za = r[0], zb = r[1];
            m.add(box(2.75, 0, za, 13.25, 1.5, zb, "darksteel"));
            m.add(box(AX - 0.75 * rg, 1.5, za + 0.25, AX + 0.75 * rg, AY - 0.62 * rg, zb - 0.25, "castiron"));
            for (double x : new double[]{AX - 0.75 * rg + 0.6, AX + 0.75 * rg - 0.6})   // hold-down bolts
                m.add(prism('y', x, (za + zb) / 2, 0.3, AY - 0.62 * rg, AY - 0.62 * rg + 0.35, "brass", 6, false, true));
        }
        double z0 = rails[0][1], z1 = rails[1][0];
        for (double x : new double[]{3.25, 11.5}) m.add(box(x, 0.25, z0, x + 1.25, 1.25, z1, "darksteel"));
        // power terminals front and back
        List<Mesh> t = genTerminal(rg);
        m.addAll(t);
        m.addAll(turned(t, 2));
        // rotor: shaft, couplings, the fan in the cage
        List<Mesh> r = couplings();
        double fz = zs0 - 0.5;
        r.add(prism('z', AX, AY, 1.1, cz0 + 0.6, zs0, "steel", 8, true, false));
        double pitch = Math.toRadians(35), R = rg - 1.15;
        for (int k = 0; k < 6; k++) {
            double a = 2 * Math.PI * k / 6;
            double[] u = {Math.cos(a), Math.sin(a), 0}, tg = {-u[1], u[0], 0};
            double[] w = {tg[0] * Math.cos(pitch), tg[1] * Math.cos(pitch), Math.sin(pitch)};
            double[] nn = {u[1] * w[2] - u[2] * w[1], u[2] * w[0] - u[0] * w[2], u[0] * w[1] - u[1] * w[0]};
            double rr = (R + 1.0) / 2;
            r.add(obox(v(AX + rr * u[0], AY + rr * u[1], fz), u, w, nn, (R - 1.0) / 2, 0.55, 0.06, "steel"));
        }
        NodeModel n = model(name, ShaftKit.PAINT_GEN,
            new double[]{AX - rg - 0.55, AY - rg - 0.55, 1.0, AX + rg + 0.55, AY + rg + 0.55, zs1 + 1.8},
            new double[]{2.75, 0, rails[0][0], 13.25, AY - 0.62 * rg, rails[1][1]},
            new double[]{0, 0, 4.25, 2.95, 4.6, 11.75}, new double[]{13.05, 0, 4.25, 16, 4.6, 11.75}, SHAFT_BOX);
        n.part("main", m);
        n.part("rotor", r);
        centre(n, "rotor");
        return n;
    }

    // ---------------------------------------------------------------- joint (plummer block)

    /** A split plain bearing pedestal round the shaft line between z0 and z1, standing on baseY. */
    static void plummer(List<Mesh> m, double z0, double z1, double rh, double baseY, double fx0, double fx1, boolean bolts) {
        double zc = (z0 + z1) / 2;
        // seals (the checker: listed after the housing they weren't seen as joined to it, a tube with a bore can't hold
        // anything; listed first, each is seen against the housing)
        for (double[] s : new double[][]{{z0 - 0.15, z0 + 0.25}, {z1 - 0.25, z1 + 0.15}})
            m.add(tube('z', AX, AY, 1.05, 1.6, s[0], s[1], "darksteel", 10));
        m.add(box(fx0, baseY, z0, fx1, baseY + 1.0, z1, "castiron"));                                     // sole flange
        m.add(box(AX - rh + 0.6, baseY + 1.0, z0 + 0.25, AX + rh - 0.6, AY - 1.0, z1 - 0.25, "castiron"));   // web
        m.add(prism('z', AX, AY, rh, z0 + 0.25, z1 - 0.25, "castiron", 12));                               // housing
        m.add(box(AX - rh - 0.6, AY - 0.2, z0 + 0.1, AX + rh + 0.6, AY + 0.3, z1 - 0.1, "paint"));          // split line
        if (bolts) {
            for (double x : new double[]{fx0 + 0.75, fx1 - 0.75})                                          // hold-down bolts
                m.add(prism('y', x, zc, 0.35, baseY + 1.0, baseY + 1.5, "brass", 6, false, true));
            for (double x : new double[]{AX - rh - 0.15, AX + rh + 0.15})                                  // cap bolts
                m.add(prism('y', x, zc, 0.3, AY + 0.3, AY + 1.3, "brass", 6, false, true));
        }
        m.add(prism('y', AX, zc, 0.35, AY + rh - 0.3, AY + rh + 0.6, "brass", 6, false, false));          // grease cup
        m.add(prism('y', AX, zc, 0.6, AY + rh + 0.6, AY + rh + 1.2, "brass", 8));
    }

    static NodeModel joint() {
        List<Mesh> m = railsX(new double[][]{{2.0, 4.0}, {12.0, 14.0}});
        m.add(box(1.5, 1.25, 1.5, 14.5, 2.0, 14.5, "steel"));
        plummer(m, 5.5, 10.5, 3.0, 2.0, 2.5, 13.5, true);
        List<Mesh> r = couplings();
        for (double za : new double[]{4.6, 10.65}) {                                     // collars with set screws
            r.add(prism('z', AX, AY, 1.45, za, za + 0.75, "steel", 10));
            r.add(prism('y', AX, za + 0.375, 0.2, AY + 1.3, AY + 1.6, "darksteel", 6, false, true));
        }
        NodeModel n = model("shaftjoint", ShaftKit.PAINT_PLAIN, new double[]{0, 0, 1.5, 16, 2.0, 14.5},
            new double[]{1.5 + 1.0, 2.0, 5.5, 13.5, 12.2, 10.5}, SHAFT_BOX);
        n.part("main", m);
        n.part("rotor", r);
        centre(n, "rotor");
        return n;
    }

    // ---------------------------------------------------------------- joint hub (bevel gearbox, open top)

    /** Bevel gear on the z shaft, z+ side of the centre: pitch cone apex at (8, 8, 8), r = z - 8 from 2.5 to 3.5. */
    static List<Mesh> bevelGearZp(int teeth) {
        List<Mesh> ms = new ArrayList<>(List.of(prism('z', AX, AY, 2.5, 10.5, 11.5, "brass", teeth, 3.5, true, true, Double.NaN),
            prism('z', AX, AY, 3.5, 11.5, 11.9, "brass", teeth, false, true),
            prism('z', AX, AY, 1.5, 11.9, 12.6, "steel", 8, false, true)));
        double s2 = Math.sqrt(0.5);
        for (int k = 0; k < teeth; k++) {
            double a = 2 * Math.PI * (k + 0.5) / teeth;
            double[] u = {Math.cos(a), Math.sin(a), 0};
            double[] sl = {u[0] * s2, u[1] * s2, s2};                // along the slant
            double[] nn = {u[0] * s2, u[1] * s2, -s2};               // out of the cone face
            double[] tg = {-u[1], u[0], 0};
            double[] c = {AX + 3.0 * u[0] + 0.12 * nn[0], AY + 3.0 * u[1] + 0.12 * nn[1], 11.0 + 0.12 * nn[2]};
            ms.add(obox(c, sl, tg, nn, 0.65, 0.28, 0.25, "brass"));
        }
        return ms;
    }

    static NodeModel hub() {
        List<Mesh> m = railsX(new double[][]{{1.5, 3.5}, {12.5, 14.5}});
        m.add(box(2.0, 1.25, 2.0, 14.0, 2.0, 14.0, "steel"));
        m.add(box(3.5, 2.0, 3.5, 12.5, 3.0, 12.5, "castiron"));
        double lo = 3.25, hi = 12.75, top = 10.5;
        m.add(box(lo, 3.0, lo, hi, 3.75, hi, "castiron"));                                     // floor of the case
        m.add(box(lo, 3.75, lo, hi, top, 4.0, "paint"));
        m.add(box(lo, 3.75, 12.0, hi, top, hi, "paint"));
        m.add(box(lo, 3.75, 4.0, 4.0, top, 12.0, "paint"));
        m.add(box(12.0, 3.75, 4.0, hi, top, 12.0, "paint"));
        // coping on the walls
        m.add(box(lo - 0.15, top, lo - 0.15, hi + 0.15, top + 0.35, 4.15, "castiron"));
        m.add(box(lo - 0.15, top, 11.85, hi + 0.15, top + 0.35, hi + 0.15, "castiron"));
        m.add(box(lo - 0.15, top, 4.15, 4.15, top + 0.35, 11.85, "castiron"));
        m.add(box(11.85, top, 4.15, hi + 0.15, top + 0.35, 11.85, "castiron"));
        // bearing bosses on the four faces of the case, with seals
        List<Mesh> boss = new ArrayList<>(List.of(prism('z', AX, AY, 2.0, 1.75, lo, "castiron", 10, true, false)));
        List<Mesh> bolts = boltRing('z', AX, AY, 2.4, hi - 0.05, hi + 0.3, 4, "brass", 0.3, 0.0);   // (heads +z; turned for the z- face)
        List<Mesh> boss2 = new ArrayList<>(List.of(prism('z', AX, AY, 2.0, hi, 14.25, "castiron", 10, false, true)));
        boss2.addAll(bolts);
        boss.addAll(turned(bolts, 2));
        List<Mesh> both = new ArrayList<>(boss);
        both.addAll(boss2);
        for (int k : new int[]{0, 1}) m.addAll(turned(both, k));                     // z faces, then x faces
        List<Mesh> r = couplings();
        r.addAll(bevelGearZp(10));
        List<Mesh> xs = new ArrayList<>();
        xs.add(shaft(0.5, 5.0));
        xs.addAll(halfCoupling(0));
        List<Mesh> xs2 = new ArrayList<>();
        xs2.add(shaft(11.0, 15.5));
        xs2.addAll(halfCoupling(16));
        // (the checker: the x shaft's two halves, each with its gear, meet only through the z gear in rotor; a shaft
        // between them would cross the z shaft in the open case, so the x+ half is marked free)
        String why = "the x+ half of the x shaft: held by its bearing boss and meshed through the z gear, the render turns both halves together";
        List<Mesh> rx = new ArrayList<>();
        rx.addAll(turned(xs, 1));
        for (Mesh mm : turned(xs2, 1)) rx.add(mm.free(why));
        for (Mesh mm : turned(bevelGearZp(10), 1)) rx.add(mm.free(why));
        rx.addAll(turned(bevelGearZp(10), 3));
        NodeModel n = model("shafthub", ShaftKit.PAINT_PLAIN, new double[]{0, 0, 1.5, 16, 3.0, 14.5},
            new double[]{lo - 0.4, 2.0, lo - 0.4, hi + 0.4, 12.0, hi + 0.4}, SHAFT_BOX, new double[]{0, 5.75, 5.75, 16, 10.25, 10.25});
        n.part("main", m);
        n.part("rotor", r);
        n.part("rotor_x", rx);
        centre(n, "rotor");
        centre(n, "rotor_x");
        return n;
    }

    // ---------------------------------------------------------------- flywheel

    static NodeModel flywheel() {
        List<Mesh> m = railsX(new double[][]{{1.25, 4.75}, {11.25, 14.75}});
        // (the checker: the two rails and their pedestals stood apart, nothing joined them; two cross ties between the
        // rails, as the generators', clear of the wheel; a visible addition)
        for (double x : new double[]{3.25, 11.5}) m.add(box(x, 0.25, 4.75, x + 1.25, 1.25, 11.25, "darksteel"));
        for (double[] zz : new double[][]{{1.75, 4.25}, {11.75, 14.25}}) {
            double z0 = zz[0], z1 = zz[1], zc = (z0 + z1) / 2;
            m.add(box(3.5, 1.25, z0, 12.5, 2.0, z1, "castiron"));
            m.add(box(AX - 1.6, 2.0, z0 + 0.25, AX + 1.6, AY - 1.0, z1 - 0.25, "castiron"));
            m.add(prism('z', AX, AY, 2.25, z0 + 0.25, z1 - 0.25, "castiron", 10));
            m.add(box(AX - 2.75, AY - 0.15, z0 + 0.35, AX + 2.75, AY + 0.3, z1 - 0.35, "paint"));
            for (double x : new double[]{AX - 2.45, AX + 2.45}) m.add(prism('y', x, zc, 0.25, AY + 0.3, AY + 1.0, "brass", 6, false, true));
            for (double x : new double[]{4.2, 11.8}) m.add(prism('y', x, zc, 0.35, 2.0, 2.5, "brass", 6, false, true));
            m.add(prism('y', AX, zc, 0.5, AY + 2.0, AY + 2.9, "brass", 8));
        }
        List<Mesh> r = couplings();
        r.add(tube('z', AX, AY, 6.1, 7.45, 6.5, 9.5, "castiron", 16));                       // rim
        for (double[] zz : new double[][]{{6.4, 6.5}, {9.5, 9.6}})
            r.add(tube('z', AX, AY, 6.4, 7.15, zz[0], zz[1], "red", 16));                        // painted bands on the rim's faces
        r.add(prism('z', AX, AY, 1.9, 6.6, 9.4, "castiron", 10));                             // hub
        r.add(prism('z', AX, AY, 1.3, 5.8, 10.2, "steel", 8));
        r.addAll(boltRing('z', AX, AY, 1.6, 9.4, 9.65, 4));
        r.addAll(turned(boltRing('z', AX, AY, 1.6, 9.4, 9.65, 4), 2));
        for (int k = 0; k < 6; k++) {
            double a = 2 * Math.PI * k / 6 + Math.PI / 6, rr = (1.8 + 6.2) / 2;
            double[] u = {Math.cos(a), Math.sin(a), 0};
            r.add(obox(v(AX + rr * u[0], AY + rr * u[1], 8.0), u, v(-u[1], u[0], 0), v(0, 0, 1), (6.2 - 1.8) / 2, 0.5, 0.6, "castiron"));
        }
        NodeModel n = model("shaftflywheel", ShaftKit.PAINT_PLAIN, new double[]{0.5, 0.5, 6.4, 15.5, 15.5, 9.6},
            new double[]{0, 0, 1.25, 16, 2.0, 14.75}, new double[]{5.75, 2.0, 1.75, 10.25, 11.0, 4.25},
            new double[]{5.75, 2.0, 11.75, 10.25, 11.0, 14.25}, SHAFT_BOX);
        n.part("main", m);
        n.part("rotor", r);
        centre(n, "rotor");
        return n;
    }

    // ---------------------------------------------------------------- tachometer

    /** The needle's origin: the dial's centre (x, y, z), px. */
    static final double[] DIAL = {1.9, 10.25, 8.0};
    static final double DIAL_R = 2.6;

    static List<Mesh> tachoSignal() {
        List<Mesh> ms = signalPlate();
        // (the checker: the box began 0.1 px behind the plate, which hung on nothing; it now reaches the plate, hidden
        // behind it)
        ms.add(box(0.85, 1.25, 6.75, 2.0, 3.6, 9.25, "darksteel"));
        ms.add(box(0.95, 3.6, 6.6, 2.15, 3.9, 9.4, "steel"));
        return ms;
    }

    static NodeModel tacho() {
        List<Mesh> m = new ArrayList<>();
        m.add(box(0.95, 0, 4.5, 15.05, 1.25, 11.5, "castiron"));                                 // base plate
        for (double x : new double[]{2.6, 13.4})
            for (double z : new double[]{5.1, 10.9}) m.add(prism('y', x, z, 0.3, 1.25, 1.6, "brass", 6, false, true));
        m.add(box(5.5, 1.25, 6.0, 10.5, 4.75, 10.0, "castiron"));                                 // pedestal
        m.add(box(5.0, 4.75, 5.5, 11.0, 11.25, 10.5, "paint"));                                   // drive box
        m.add(box(4.8, 11.25, 5.3, 11.2, 11.6, 10.7, "castiron"));                                // lid
        for (double x : new double[]{5.4, 10.6})
            for (double z : new double[]{5.7, 10.3}) m.add(prism('y', x, z, 0.2, 11.6, 11.85, "brass", 6, false, true));
        for (double[] zz : new double[][]{{4.25, 5.5}, {10.5, 11.75}}) {                         // bearing bosses
            double z0 = zz[0], z1 = zz[1];
            // (the checker: the seal, listed after its boss, wasn't seen as joined to it; listed first it is)
            m.add(z0 < 8 ? tube('z', AX, AY, 1.05, 1.5, z0 - 0.2, z0, "darksteel", 10) : tube('z', AX, AY, 1.05, 1.5, z1, z1 + 0.2, "darksteel", 10));
            m.add(prism('z', AX, AY, 2.0, z0, z1, "castiron", 10));
        }
        double x = DIAL[0], y = DIAL[1], z = DIAL[2];
        m.add(prism('x', y, z, 0.7, 3.2, 5.0, "steel", 8, false, false));               // drive neck to the dial
        m.add(prism('x', y, z, DIAL_R, 2.2, 3.2, "brass", 14));                          // case
        m.add(tube('x', y, z, DIAL_R - 0.35, DIAL_R + 0.1, 2.0, 2.2, "brass", 14));      // bezel
        m.add(prism('x', y, z, DIAL_R - 0.3, 2.1, 2.2, "glass", 14, true, false));       // face
        for (int k = 0; k < 7; k++) {                                                     // scale ticks, 7:30 .. 4:30
            double a = Math.toRadians(-135 + 45 * k), dy = Math.cos(a), dz = Math.sin(a);
            m.add(obox(v(2.05, y + 1.75 * dy, z + 1.75 * dz), v(1, 0, 0), v(0, dy, dz), v(0, -dz, dy), 0.04, 0.3, 0.08, k >= 5 ? "red" : "darksteel"));
        }
        List<Mesh> t = tachoSignal();
        m.addAll(t);
        m.addAll(turned(t, 2));
        List<Mesh> r = couplings();
        for (double za : new double[]{3.4, 11.85}) {
            r.add(prism('z', AX, AY, 1.4, za, za + 0.75, "steel", 10));
            r.add(prism('y', AX, za + 0.375, 0.2, AY + 1.25, AY + 1.55, "darksteel", 6, false, true));
        }
        List<Mesh> nd = new ArrayList<>();
        nd.add(obox(v(x - 0.1, y + 0.8, z), v(1, 0, 0), v(0, 1, 0), v(0, 0, 1), 0.04, 1.15, 0.09, "red"));   // pointing up: 0
        nd.add(prism('x', y, z, 0.3, x - 0.25, x, "darksteel", 6, true, false));
        NodeModel n = model("shafttacho", ShaftKit.PAINT_PLAIN, new double[]{0, 0, 4.5, 16, 4.0, 11.5},
            new double[]{4.8, 4.0, 5.3, 11.2, 11.85, 10.7},
            new double[]{1.6, y - DIAL_R - 0.1, z - DIAL_R - 0.1, 5.0, y + DIAL_R + 0.1, z + DIAL_R + 0.1}, SHAFT_BOX);
        n.part("main", m);
        n.part("rotor", r);
        n.part("needle", nd);
        centre(n, "rotor");
        n.origin("needle", x, y, z);
        return n;
    }

    public static List<NodeModel> models() {
        return List.of(
            generator("shaftgen8kw", 5.5, 3.0, 13.0, new double[]{3.3, 6.3, 9.3, 12.3}, new double[][]{{4.0, 5.75}, {10.25, 12.0}}),
            generator("shaftgen4kw", 4.1, 4.0, 12.0, new double[]{4.3, 6.4, 9.2, 11.3}, new double[][]{{4.8, 6.3}, {9.7, 11.2}}),
            joint(), hub(), flywheel(), tacho());
    }
}
