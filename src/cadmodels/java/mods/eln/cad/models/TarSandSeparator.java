package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.oriented;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.v;
import static mods.eln.cad.models.OilKit.handwheel;
import static mods.eln.cad.models.OilKit.obox;
import static mods.eln.cad.models.OilKit.terminalPlate;
import static mods.eln.cad.models.OilKit.turned;

/**
 * Tar sand separator (50V / 200V): a heated twin-shaft paddle trough (hot-water extraction / conditioning mixer).
 * Drawn by MaceratorDescriptor (OilSeparatorDescriptor extends it), which needs parts main, rot1, rot2 and turns rot1
 * about (0,0,-1) and rot2 about (0,0,1) through their .txt origins by the same angle: two counter-rotating paddle
 * shafts along z, coupled by a pair of equal spur gears on the z- end (meshed with a half-tooth offset, so they stay
 * meshed at every angle). It also draws the input item at the block centre ~12.5 px up (in the open trough, above the
 * froth) and the output item ~2.4 px up (on the sand tray under the trough, between the legs).
 * <p>
 * Front XN (x = 0); power is on the two z faces (powerLrdu: not front/back), so the floor terminal plates are on z = 0
 * and z = 16. Trough (tier-painted heating jacket, steel rims) on four legs, froth surface with paddles breaking
 * through it, feed hopper on the front rim, motor standing on a gearbox at the z+ end, froth overflow launder and oil
 * outlet pipe with valve on the back (x+), sand spout and sand tray under the trough.
 */
public final class TarSandSeparator {
    private TarSandSeparator() {
    }

    /**
     * sx: shaft x (A, B); sy: shaft height; tx/tz: trough outer span; ty: trough bottom, froth, rim top; pr: paddle
     * radius; np: paddles per shaft; mr: motor radius; fins: motor fin heights.
     */
    record Tier(String name, String paint, double[] sx, double sy, double[] tx, double[] tz, double[] ty, double pr, int np, double mr,
                double[] fins, int teeth) {
    }

    static final List<Tier> TIERS = List.of(
        new Tier("50v", OilKit.PAINT_50V, new double[]{5.75, 10.25}, 9.5, new double[]{2.5, 13.5}, new double[]{2.5, 12.0},
            new double[]{6.0, 9.6, 13.0}, 2.1, 3, 1.6, new double[]{14.25}, 8),
        new Tier("200v", OilKit.PAINT_200V, new double[]{5.5, 10.5}, 9.75, new double[]{2.0, 14.0}, new double[]{2.25, 12.25},
            new double[]{6.0, 9.85, 13.5}, 2.3, 4, 1.9, new double[]{13.9, 14.7}, 8));
    /** Gear z span (outside the z- end wall). */
    static final double[] GZ = {0.9, 1.7};
    /** (fraction of pitch, 0 = root, 1 = tip). */
    static final double[][] TOOTH = {{-0.27, 0.0}, {-0.12, 1.0}, {0.12, 1.0}, {0.27, 0.0}};

    static List<double[]> gearOutline(double rRoot, double rTip, int teeth, double phase) {
        double pitch = 2 * Math.PI / teeth;
        List<double[]> pts = new ArrayList<>();
        for (int k = 0; k < teeth; k++)
            for (double[] t : TOOTH) pts.add(new double[]{phase + k * pitch + t[0] * pitch, rRoot + t[1] * (rTip - rRoot)});
        return pts;
    }

    /** Spur gear along z: trapezoid teeth; walls as quads, faces fanned from the centre (outline not convex). */
    static Mesh gear(double cx, double cy, double z0, double z1, double rRoot, double rTip, int teeth, double phase, String mat) {
        List<double[]> pts = gearOutline(rRoot, rTip, teeth, phase);
        Mesh m = new Mesh(mat);
        int n = pts.size();
        for (int i = 0; i < n; i++) {
            double a0 = pts.get(i)[0], r0 = pts.get(i)[1], a1 = pts.get((i + 1) % n)[0], r1 = pts.get((i + 1) % n)[1];
            double mid = (a0 + a1) / 2;
            m.poly(oriented(new double[][]{gp(cx, cy, z0, a0, r0), gp(cx, cy, z0, a1, r1), gp(cx, cy, z1, a1, r1), gp(cx, cy, z1, a0, r0)},
                v(Math.cos(mid), Math.sin(mid), 0)));
            for (double[] zs : new double[][]{{z0, -1}, {z1, 1}})
                m.poly(oriented(new double[][]{v(cx, cy, zs[0]), gp(cx, cy, zs[0], a0, r0), gp(cx, cy, zs[0], a1, r1)}, v(0, 0, zs[1])));
        }
        m.closed = true;
        return m;
    }

    static double[] gp(double cx, double cy, double z, double a, double r) {
        return v(cx + r * Math.cos(a), cy + r * Math.sin(a), z);
    }

    /** Root and tip radii: the pitch radius is half the centre distance (the gears are equal, centres 2 rp apart). */
    static double[] gearRadii(Tier t) {
        double rp = (t.sx[1] - t.sx[0]) / 2;
        return new double[]{rp - 0.5, rp + 0.35};
    }

    /** One paddle shaft with its gear. which 0 = rot1 (x-, turns about -z), 1 = rot2 (x+, turns about +z). */
    static List<Mesh> rotor(Tier t, int which) {
        double cx = t.sx[which], cy = t.sy, tz0 = t.tz[0], tz1 = t.tz[1];
        List<Mesh> m = new ArrayList<>();
        double zend = which == 0 ? tz1 - 0.4 : tz1 + 1.5;   // B runs on into the gearbox
        m.add(prism('z', cx, cy, 0.45, GZ[0] - 0.3, zend, "darksteel", 6));
        // gear: pitch radius = half the centre distance; A has a tooth facing B (angle 0), B a gap facing A (angle pi)
        double pitch = 2 * Math.PI / t.teeth;
        double ph = which == 0 ? 0.0 : Math.PI + pitch / 2;
        double[] gr = gearRadii(t);
        m.add(gear(cx, cy, GZ[0], GZ[1], gr[0], gr[1], t.teeth, ph, "brass"));
        m.add(prism('z', cx, cy, 0.8, GZ[0] - 0.25, GZ[0], "darksteel", 6, true, false));   // hub nut
        // paddles: a two-armed paddle every step along the trough, staggered between the shafts, helical angles
        double zi0 = tz0 + 1.2, zi1 = tz1 - 1.2, step = (zi1 - zi0) / t.np, pr = t.pr;
        for (int k = 0; k < t.np; k++) {
            double z = zi0 + step * (k + 0.25 + 0.5 * which);
            double ang = Math.toRadians(60 * k + 30 * which);
            double[] d = v(Math.cos(ang), Math.sin(ang), 0.0), tg = v(-d[1], d[0], 0.0);
            m.add(obox(v(cx, cy, z), d, tg, v(0, 0, 1), pr - 0.5, 0.18, 0.18, "steel"));   // arm through the shaft
            for (int s : new int[]{1, -1}) {
                double[] c = v(cx + s * d[0] * (pr - 0.45), cy + s * d[1] * (pr - 0.45), z);
                // blade, pitched 25 degrees about the arm
                double p = Math.toRadians(25 * s);
                double[] bz = v(tg[0] * Math.sin(p), tg[1] * Math.sin(p), Math.cos(p)), bt = v(tg[0] * Math.cos(p), tg[1] * Math.cos(p), -Math.sin(p));
                m.add(obox(c, d, bt, bz, 0.45, 0.12, 0.7, "steel"));
            }
        }
        return m;
    }

    static List<Mesh> body(Tier t) {
        List<Mesh> m = new ArrayList<>();
        double x0 = t.tx[0], x1 = t.tx[1], z0 = t.tz[0], z1 = t.tz[1], yb = t.ty[0], yf = t.ty[1], yr = t.ty[2];
        double sxa = t.sx[0], sxb = t.sx[1], sy = t.sy;
        double w = 0.6;
        // skid rails (reach the front and back faces at the floor), legs, cross bracing
        for (double z : new double[]{z0 + 0.5, z1 - 1.75}) {
            m.add(box(0.1, 0, z, 15.9, 1.0, z + 1.25, "darksteel", "nsewu"));
            for (double x : new double[]{x0 + 0.5, x1 - 1.5}) m.add(box(x, 1.0, z + 0.1, x + 1.0, yb, z + 1.15, "darksteel"));
            m.add(box(x0 + 1.5, 3.0, z + 0.4, x1 - 1.5, 3.6, z + 0.85, "darksteel"));
        }
        // trough: heated jacket (tier paint) below the froth line, solid inside (the froth hides it), steel rims above
        m.add(box(x0, yb, z0, x1, yf - 0.6, z1, "paint"));
        m.add(box(x0 + 0.1, yf - 0.6, z0 + 0.1, x1 - 0.1, yf, z1 - 0.1, "steel", "nsewd"));
        m.add(box(x0 + w, yf - 0.6, z0 + w, x1 - w, yf, z1 - w, "froth", "u"));
        m.add(box(x0 - 0.15, yb + 1.0, z0 - 0.15, x1 + 0.15, yb + 1.6, z1 + 0.15, "darksteel", "nsewud"));   // jacket band
        double[][] rims = {{x0 + 0.1, z0 + 0.1, x0 + w, z1 - 0.1}, {x1 - w, z0 + 0.1, x1 - 0.1, z1 - 0.1},
            {x0 + w, z0 + 0.1, x1 - w, z0 + w}, {x0 + w, z1 - w, x1 - w, z1 - 0.1}};
        for (double[] r : rims) m.add(box(r[0], yf, r[1], r[2], yr, r[3], "steel"));
        for (double[] r : rims)                                                                               // rim lip
            m.add(box(r[0] - (r[0] < x0 + 0.2 ? 0.15 : 0), yr, r[1] - (r[1] < z0 + 0.2 ? 0.15 : 0),
                r[2] + (r[2] > x1 - 0.2 ? 0.15 : 0), yr + 0.4, r[3] + (r[3] > z1 - 0.2 ? 0.15 : 0), "darksteel"));
        // bearing blocks on the z- end wall, gears turn in front of them
        for (double sx : new double[]{sxa, sxb}) m.add(box(sx - 0.9, sy - 0.9, GZ[1] + 0.15, sx + 0.9, sy + 0.9, z0, "darksteel"));
        // (the checker: the bearing blocks floated 0.5 px above the bracket, held by the end wall over a quarter of their
        // face; the bracket reaches up to them (top sy - 0.9, was sy - 1.4). Its front, 0.4 px off the gears, lay in the
        // 200V's jacket band's front plane and z-fought it; it starts 0.1 px further out, GZ + 0.3)
        m.add(box(sxa - 0.6, yb - 0.1, GZ[1] + 0.3, sxb + 0.6, sy - 0.9, z0, "castiron"));                  // bearing bracket
        // sand spout and slide gate under the trough, sand tray with a pile on the floor (the output item sits on it)
        m.add(prism('y', 8.0, 8.0, 1.25, 5.25, yb, "castiron", 4, 1.75, true, false, Math.PI / 4));
        // (the checker: the gate's handle floated 0.3 px off the spout; it reaches into the spout, z from 8.75,
        // and its underside, then in the spout's bottom plane, is 0.1 px higher)
        m.add(box(6.6, 5.35, 8.75, 9.4, 5.6, 10.5, "darksteel"));                                            // gate handle side
        m.add(box(4.75, 0, 4.75, 11.25, 0.6, 11.25, "darksteel", "nsewu"));
        m.add(prism('y', 8.0, 8.0, 2.6, 0.6, 1.5, "sand", 8, 1.2, true, true, Double.NaN));
        // feed chute over the front rim (tar sand in): a sloped steel pan with side lips, tier-painted hood
        double a = Math.atan2(2.6, 4.5);
        double[] dx = v(Math.cos(a), -Math.sin(a), 0.0), dy = v(Math.sin(a), Math.cos(a), 0.0);
        // (the checker: the 50V's chute and its bracket floated 0.15 px above the rim and in front of it; it sits as the
        // 200V's does, x 2.8 and 0.1 px over the rim's top (was x0 + 0.8, yr + 0.6: 0.5 px further back and higher))
        double[] cc = v(Math.min(x0 + 0.8, 2.8), Math.min(yr + 0.1, 13.6), 8.0);
        m.add(obox(cc, dx, dy, v(0, 0, 1), 2.4, 0.15, 1.6, "steel"));
        // (the checker: the lips' ends and undersides lay in the pan's and z-fought them; the lips are 0.1 px shorter at
        // each end and stand 0.1 px higher)
        for (int s : new int[]{-1, 1})
            m.add(obox(v(cc[0] + 0.55 * dy[0], cc[1] + 0.55 * dy[1], 8.0 + s * 1.65), dx, dy, v(0, 0, 1), 2.3, 0.6, 0.12, "darksteel"));
        m.add(box(0.4, cc[1] - 2.2, 6.9, 1.0, cc[1] + 1.0, 9.1, "darksteel"));                                // chute bracket
        // heating element terminals on the front of the jacket and the conduit to the z- floor terminal
        for (double z : new double[]{z0 + 2.5, z1 - 2.5}) {
            m.add(prism('x', yb + 2.6, z, 0.55, x0 - 0.9, x0, "ceramic", 6));
            m.add(prism('x', yb + 2.6, z, 0.3, x0 - 1.2, x0 - 0.9, "copper", 6));
        }
        m.add(box(x0 - 0.7, yb + 1.6, z0 + 1.6, x0, yb + 2.0, z1 - 1.6, "darksteel"));                       // bus bar strap
        // drive: gearbox at the z+ end on B's shaft, on a pedestal; motor standing on it
        double gx0 = sxb - 2.25, gx1 = sxb + 2.25, gz0 = z1 + 0.1, gz1 = 15.3;
        m.add(box(gx0 + 0.5, 0, gz0 + 0.4, gx1 - 0.5, yb + 0.5, gz1 - 0.5, "darksteel", "nsewu"));
        m.add(box(gx0, yb + 0.5, gz0, gx1, sy + 2.25, gz1, "castiron"));
        // (the checker: the gearbox stood 0.1 px off the trough's end wall; a mounting plate fills the gap, the
        // jacket's height)
        m.add(box(gx0 + 0.25, yb + 0.5, z1, gx1 - 0.25, yf - 0.6, gz0, "castiron", "ewud"));
        m.add(box(gx0 - 0.15, sy + 2.25, gz0 - 0.1, gx1 + 0.15, sy + 2.6, gz1 + 0.1, "paint"));
        double mr = t.mr, mz = Math.min((gz0 + gz1) / 2, 15.85 - mr - 0.35), mx = sxb + 0.25;
        m.add(prism('y', mx, mz, mr, sy + 2.6, 15.0, "steel", 10));
        m.add(prism('y', mx, mz, mr - 0.6, 15.0, 15.7, "darksteel", 10));                                   // fan cowl
        for (double y : t.fins) m.add(prism('y', mx, mz, mr + 0.35, y - 0.2, y + 0.2, "darksteel", 10));
        // (the checker: the 50V's band top lay in the end rim's top plane, which it touches, and z-fought it; the band
        // is 0.1 px lower)
        m.add(prism('y', mx, mz, mr + 0.3, sy + 2.8, sy + 3.4, "paint", 10));
        // motor terminal box (x- side) and cable down the pedestal to the z+ floor terminal
        m.add(box(mx - mr - 0.9, sy + 3.0, mz - 0.8, mx - mr + 0.2, sy + 4.4, mz + 0.8, "steel"));
        m.add(box(gx0 - 0.6, 0.5, mz - 0.35, gx0, sy + 3.0, mz + 0.35, "darksteel"));
        // oil outlet: froth overflow launder on the back rim, down pipe, valve, outlet stub through the back face
        double lz0 = 4.5, lz1 = 7.5, lc = (lz0 + lz1) / 2;
        // (the checker: the launder stood 0.25 px clear of the back rim and the whole outlet with it floated; it reaches
        // the rim, from x1 - 0.1)
        m.add(box(x1 - 0.1, yf, lz0, 15.2, yf + 0.5, lz1, "darksteel"));
        m.add(box(x1 - 0.1, yf + 0.5, lz0, 15.2, yf + 1.2, lz0 + 0.3, "darksteel"));
        m.add(box(x1 - 0.1, yf + 0.5, lz1 - 0.3, 15.2, yf + 1.2, lz1, "darksteel"));
        m.add(box(x1 - 0.1, yf + 0.5, lz0 + 0.3, 15.2, yf + 0.6, lz1 - 0.3, "oil", "u"));
        double px = 14.5;
        m.add(prism('y', px, lc, 0.55, 3.0, yf, "darksteel", 6, false, false));
        m.add(box(px - 0.8, 5.0, lc - 0.8, px + 0.8, 6.4, lc + 0.8, "castiron"));                           // valve body
        m.add(prism('z', px, 5.7, 0.25, lc + 0.8, lc + 1.6, "darksteel", 6, false, false));
        m.addAll(handwheel(px, 5.7, lc + 1.7, 0.9, 'z'));
        m.add(prism('y', px, lc, 0.7, 2.4, 3.0, "darksteel", 6, true, false));                              // elbow
        m.add(prism('x', 2.7, lc, 0.55, px, 15.9, "darksteel", 6, false, true));
        m.add(prism('x', 2.7, lc, 0.95, 15.3, 15.8, "steel", 8));                                           // outlet flange
        // nameplate on the front of the jacket
        // (the checker: its back, on the jacket, z-fought an element terminal's insulator end there; dropped, unseen)
        m.add(box(x0 - 0.1, yb + 2.4, 7.0, x0, yb + 3.4, 9.5, "brass", "nswud"));
        // power: floor terminals on the two z faces
        m.addAll(turned(terminalPlate(), 1));
        m.addAll(turned(terminalPlate(), 3));
        // (the checker: both plates stood clear of the skid, 1.7 px and more; a floor conduit runs from each to the
        // nearest skid rail, the z+ one taking the motor's cable down the pedestal)
        m.add(box(6.8, 0, 0.85, 8.0, 0.6, z0 + 0.5, "darksteel", "ewu"));
        m.add(box(6.8, 0, z1 - 0.5, 8.0, 0.6, 15.15, "darksteel", "ewu"));
        return m;
    }

    public static List<NodeModel> models() {
        List<NodeModel> out = new ArrayList<>();
        for (Tier t : TIERS) {
            NodeModel n = new NodeModel("tarsandseparator" + t.name, t.paint);
            n.part("main", body(t));
            n.part("rot1", rotor(t, 0));
            n.part("rot2", rotor(t, 1));
            n.origin("rot1", t.sx[0], t.sy, 8.0).origin("rot2", t.sx[1], t.sy, 8.0);
            out.add(n);
        }
        return out;
    }
}
