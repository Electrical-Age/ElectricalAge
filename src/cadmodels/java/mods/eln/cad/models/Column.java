package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.oriented;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;

/**
 * Fractionating column (base + stackable segment) and flare stack (mods.eln.oil.Column*, FlareStack*; was
 * tools/models/column.py, design in docs/models/column.md). One part "main" each. Front XN (x = 0).
 * <ul>
 * <li>columnbase: reboiler: cast-iron heat jacket with a heater band, copper thermal pads at the floor centre of all 4
 * sides (thermal cables, LRDU Down), fire porthole and level gauge on the front, crude inlet (z+) and residue drain (z-)
 * nozzles at face centre height, column shell and top half-flange.</li>
 * <li>columnsegment: full-height lagged shell, half-flanges top and bottom (a joint reads as one bolted 1 px flange),
 * draw-off nozzle and valve on the front, temperature dial (z-), manway (z+), ladder with cage on the back.</li>
 * <li>flarestack: base plate, knockout drum on saddles, inlet elbow from the front face centre, slim red/white stack,
 * guy-wire collar and wires, hollow flare tip with wind shield and pilot line; opening at the top centre.</li>
 * </ul>
 * Stacking: a joint is two 0.5 px half-flanges (base/segment top + next segment bottom); ladder rails, rungs (every
 * 2 px) and cage hoops (every 8 px) repeat so they continue across blocks. (The Python's --stack mode, a preview of a
 * stacked tower through elncheck.py, is not ported.)
 */
public final class Column {
    private Column() {
    }

    static int[] all(int k) {
        return k == 0 ? null : new int[]{k, k, k};
    }


    /** Block centre (x and z). */
    static final double C = 8.0;
    /** Sides of every column-shell cylinder (same phase everywhere: flat faces towards +-x, +-z). */
    static final int N = 12;
    /** Column shell radius (base top and segment: must match), flange outer radius, flange bolt circle. */
    static final double SR = 5.0, FR = 6.0, NUT_R = 5.6;
    /** Shell apothem: the flat front face sits at x = C - APO. */
    static final double APO = SR * Math.cos(Math.PI / N);

    // ---------- helpers ----------

    /** Turned QUARTER x 90 deg about the vertical axis through the block centre; quarter 1 takes the XN face to ZN. */
    static Mesh rotY(Mesh mesh, int quarter) {
        return mesh.mapped(p -> {
            for (int i = 0; i < quarter % 4; i++) {
                double dx = p[0] - C, dz = p[2] - C;
                p[0] = C - dz;
                p[2] = C + dx;
            }
        }, false);
    }

    /** Thin n-sided bar from point a to point b (guy wires): side walls only. */
    static Mesh rod(double[] a, double[] b, double r, String mat, int n) {
        double[] d = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
        double l = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
        for (int i = 0; i < 3; i++) d[i] /= l;
        double[] h = Math.abs(d[1]) < 0.9 ? new double[]{0, 1, 0} : new double[]{1, 0, 0};
        double[] u = {h[1] * d[2] - h[2] * d[1], h[2] * d[0] - h[0] * d[2], h[0] * d[1] - h[1] * d[0]};
        double ul = Math.sqrt(u[0] * u[0] + u[1] * u[1] + u[2] * u[2]);
        for (int i = 0; i < 3; i++) u[i] /= ul;
        double[] v = {d[1] * u[2] - d[2] * u[1], d[2] * u[0] - d[0] * u[2], d[0] * u[1] - d[1] * u[0]};
        Mesh m = new Mesh(mat);
        for (int k = 0; k < n; k++) {
            double[] o0 = off(u, v, r, k, n), o1 = off(u, v, r, k + 1, n);
            double[][] q = {add(a, o0), add(a, o1), add(b, o1), add(b, o0)};
            double[] mid = {(o0[0] + o1[0]) / 2, (o0[1] + o1[1]) / 2, (o0[2] + o1[2]) / 2};
            m.poly(oriented(q, mid));
        }
        return m;
    }

    static Mesh rod(double[] a, double[] b, double r, String mat) {
        return rod(a, b, r, mat, 4);
    }

    static double[] off(double[] u, double[] v, double r, int k, int n) {
        double c = Math.cos(2 * Math.PI * k / n + Math.PI / n), s = Math.sin(2 * Math.PI * k / n + Math.PI / n);
        return new double[]{r * (c * u[0] + s * v[0]), r * (c * u[1] + s * v[1]), r * (c * u[2] + s * v[2])};
    }

    static double[] add(double[] a, double[] b) {
        return new double[]{a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    /** 0.5 px half-flange at y0..y0 + 0.5 with 8 bolt nuts on its free side (above if nutsUp, else below). Two halves of
     *  neighbouring blocks meet at the block boundary and read as one bolted 1 px flange. */
    static void halfFlange(List<Mesh> m, double y0, boolean nutsUp) {
        m.add(tube('y', C, C, SR - 0.1, FR, y0, y0 + 0.5, "steel", N, Double.NaN, false));
        double ny0 = nutsUp ? y0 + 0.5 : y0 - 0.3, ny1 = nutsUp ? y0 + 0.8 : y0;
        for (int k = 0; k < 8; k++) {
            double a = 2 * Math.PI * (k + 0.5) / 8;
            m.add(prism('y', C + NUT_R * Math.cos(a), C + NUT_R * Math.sin(a), 0.3, ny0, ny1, "darksteel", 6, !nutsUp, nutsUp));
        }
    }

    /** Ladder with a safety cage on the back (x+) of the shell, between y0 and y1. Rungs every 2 px (y = 1, 3, ...) and
     *  cage hoops every 8 px (y = 4, 12) so the ladder continues across stacked blocks. */
    static void ladder(List<Mesh> m, double y0, double y1, double cageFrom) {
        double xr0 = 13.6, xr1 = 14.0;
        // (the checker: on the base the rails' tops at y 16 lay in the half-flange's top plane; they are left open at the
        // block's top as at its bottom: the rails go on into the segment above)
        for (double z0 : new double[]{6.25, 9.35}) {
            m.add(box(xr0, y0, z0, xr1, y1, z0 + 0.4, "darksteel", (y0 > 0 ? "d" : "") + (y1 < 16 ? "u" : "") + "nsew"));
            for (double ys : new double[]{2.0, 10.0})                                   // stand-offs to the shell
                if (y0 <= ys && ys <= y1 - 1) m.add(box(C + APO - 0.2, ys, z0 + 0.05, xr0, ys + 0.4, z0 + 0.35, "darksteel"));
        }
        for (int k = 0; k < 8; k++) {
            double y = 1 + 2 * k;
            if (y0 + 0.5 <= y && y <= y1 - 0.5) m.add(box(xr0 + 0.05, y, 6.65, xr1 - 0.05, y + 0.35, 9.35, "steel", "ewud"));
        }
        // (the checker: the cage stood clear of the ladder; a tab on each hoop's front end turns in to the rail, x 13.6..14.0,
        // z 5.2..6.25 and 9.75..10.8, a small visible bracket)
        for (double y : new double[]{4.0, 12.0})                                        // cage hoops
            if (y >= Math.max(y0, cageFrom)) {
                m.add(box(xr1, y, 5.2, 15.5, y + 0.4, 5.6, "steel"));
                m.add(box(xr1, y, 10.4, 15.5, y + 0.4, 10.8, "steel"));
                m.add(box(15.5, y, 5.2, 15.9, y + 0.4, 10.8, "steel"));
                m.add(box(xr0, y, 5.2, xr1, y + 0.4, 6.25, "steel"));
                m.add(box(xr0, y, 9.75, xr1, y + 0.4, 10.8, "steel"));
            }
        double cy0 = Math.max(y0, cageFrom);
        // (the checker: the side bars' faces lay 0.05 px inside the hoops' sides; they are 0.2 px thick in z, was 0.3:
        // 5.3..5.5 and 10.5..10.7)
        for (double z : new double[]{5.25, 7.0, 9.0, 10.75}) {                           // cage bars
            boolean mid = 6 < z && z < 10;
            double x0 = mid ? 15.55 : 14.6, x1 = mid ? 15.85 : 14.9;
            double z0 = mid ? z - 0.15 : z < 6 ? 5.3 : 10.5, z1 = mid ? z + 0.15 : z < 6 ? 5.5 : 10.7;
            m.add(box(x0, cy0, z0, x1, y1, z1, "darksteel", cy0 > 0 ? "nsewud" : "nsew"));
        }
    }

    /** nb bolt heads on a flange face normal to x, circle radius r round (cy, cz). */
    static void boltsOnFaceX(List<Mesh> m, double x0, double x1, double cy, double cz, double r, int nb, double br, double phase) {
        for (int k = 0; k < nb; k++) {
            double a = 2 * Math.PI * (k + phase) / nb;
            m.add(prism('x', cy + r * Math.cos(a), cz + r * Math.sin(a), br, x0, x1, "brass", 6, true, false));
        }
    }

    /** Pipe nozzle along z to the side face (side +1: z+, -1: z-), flange flush with the face (recessed 0.1 px) at face
     *  centre height, where a pipe from another mod would meet it. */
    static void nozzleZ(List<Mesh> m, int side, double cy, boolean valve) {
        double face = side > 0 ? 16.0 : 0.0;
        double fl0 = Math.min(face - side * 0.6, face - side * 0.1), fl1 = Math.max(face - side * 0.6, face - side * 0.1);
        double p0 = Math.min(face - side * 0.6, C + side * 4.0), p1 = Math.max(face - side * 0.6, C + side * 4.0);
        m.add(prism('z', C, cy, 1.1, p0, p1, "steel", 8, false, false));
        m.add(prism('z', C, cy, 1.8, fl0, fl1, "steel", 10));
        m.add(prism('z', C, cy, 0.7, Math.min(fl0, face), Math.max(fl1, face), "soot", 8, side < 0, side > 0));   // bore, at the face
        for (int k = 0; k < 4; k++) {
            double a = 2 * Math.PI * (k + 0.5) / 4;
            double b0 = Math.min(face - side * 0.1, face), b1 = Math.max(face - side * 0.1, face);
            m.add(prism('z', C + 1.35 * Math.cos(a), cy + 1.35 * Math.sin(a), 0.25, b0, b1, "brass", 6, side < 0, side > 0));
        }
        if (valve) {   // gate valve with a red handwheel on top
            double vz = face - side * 2.2;
            // (the checker: the valve body's outer face lay 0.02 px inside the base's heat band; it is 0.1 px shorter there)
            m.add(box(C - 1.3, cy - 1.3, Math.min(vz + side * 0.6, vz - side * 0.7), C + 1.3, cy + 1.3, Math.max(vz + side * 0.6, vz - side * 0.7),
                "darksteel"));
            m.add(prism('y', C, vz, 0.25, cy + 1.3, cy + 2.9, "steel", 6, false, true));
            m.add(tube('y', C, vz, 0.8, 1.2, cy + 2.7, cy + 3.05, "paint", 10));
            m.add(box(C - 0.8, cy + 2.75, vz - 0.12, C + 0.8, cy + 3.0, vz + 0.12, "paint", "nsud"));
        }
    }

    // ---------- the models ----------

    static List<Mesh> columnBase() {
        List<Mesh> m = new ArrayList<>();
        m.add(box(1.0, 0, 1.0, 15.0, 0.75, 15.0, "darksteel"));                          // plinth
        m.add(prism('y', C, C, 6.5, 0.75, 7.5, "castiron", N, false, false));             // heat jacket / reboiler
        m.add(tube('y', C, C, 6.4, 6.75, 6.6, 7.5, "paint", N, Double.NaN, false));       // heat band (top of jacket)
        m.add(prism('y', C, C, 6.5, 7.5, 10.0, "steel", N, SR, false, false, Double.NaN));   // shoulder
        m.add(prism('y', C, C, SR, 10.0, 15.9, "lagging", N, false, false));              // column shell
        m.add(prism('y', C, C, SR - 0.1, 15.5, 16.0, "darksteel", N, false, true));       // head
        m.add(tube('y', C, C, SR - 0.1, SR + 0.25, 12.5, 12.9, "darksteel", N, Double.NaN, false));   // lagging strap
        halfFlange(m, 15.5, false);
        // thermal pads: copper plate flush with each side face (floor centre, 7 px wide), heat bus into the jacket
        List<Mesh> pad = new ArrayList<>();
        pad.add(box(0.1, 0, 4.5, 0.7, 3.5, 11.5, "copper"));
        pad.add(box(0.7, 0.75, 6.0, 2.2, 3.0, 10.0, "copper"));
        for (double[] yz : new double[][]{{0.6, 5.1}, {0.6, 10.9}, {2.9, 5.1}, {2.9, 10.9}})
            pad.add(prism('x', yz[0], yz[1], 0.35, 0.0, 0.1, "brass", 4, true, false));
        pad.add(box(0.1, 3.5, 4.3, 0.8, 3.85, 11.7, "darksteel"));                         // pad clamp bar
        pad.add(box(0.0, 1.75, 7.5, 0.1, 2.75, 8.5, "brass", "nsudw"));                   // terminal stud (README: cable terminals)
        for (int q = 0; q < 4; q++) for (Mesh mesh : pad) m.add(rotY(mesh, q));
        // front: fire porthole on the jacket, level gauge on the shell
        double jx = C - 6.5 * Math.cos(Math.PI / N);
        m.add(prism('x', 5.1, C, 1.15, jx - 0.5, jx + 0.2, "brass", 10));
        m.add(prism('x', 5.1, C, 0.8, jx - 0.6, jx - 0.5, "fire", 10, true, false));
        for (int k = 0; k < 4; k++) {
            double a = 2 * Math.PI * (k + 0.5) / 4;
            m.add(prism('x', 5.1 + 0.98 * Math.cos(a), C + 0.98 * Math.sin(a), 0.15, jx - 0.65, jx - 0.5, "darksteel", 4, true, false));
        }
        double sx = C - APO;
        m.add(box(sx - 1.2, 10.4, 7.35, sx + 0.1, 11.1, 8.65, "brass"));
        m.add(box(sx - 1.2, 14.4, 7.35, sx + 0.1, 15.1, 8.65, "brass"));
        m.add(prism('y', sx - 0.6, C, 0.4, 11.1, 14.4, "sightglass", 6, false, false));
        // (the checker: the guard rods met the cocks only along an edge; they moved 0.2 px in, onto the cocks' ends, z 7.35..7.55
        // and 8.45..8.65)
        m.add(box(sx - 1.4, 11.1, 7.35, sx - 0.6, 14.4, 7.55, "darksteel"));
        m.add(box(sx - 1.4, 11.1, 8.45, sx - 0.6, 14.4, 8.65, "darksteel"));
        // crude inlet (z+) and residue drain with valve (z-), both at face centre height
        nozzleZ(m, +1, 8.0, false);
        nozzleZ(m, -1, 8.0, true);
        // ladder starts on the jacket top (back)
        ladder(m, 7.5, 16.0, 11.0);
        return m;
    }

    static List<Mesh> columnSegment() {
        List<Mesh> m = new ArrayList<>();
        m.add(prism('y', C, C, SR, 0.0, 15.9, "lagging", N, false, false));
        m.add(prism('y', C, C, SR - 0.1, 15.5, 16.0, "darksteel", N, false, true));       // top head (hidden when stacked)
        halfFlange(m, 0.0, true);
        halfFlange(m, 15.5, false);
        for (double y : new double[]{4.5, 12.5})                                           // lagging straps
            m.add(tube('y', C, C, SR - 0.1, SR + 0.25, y, y + 0.4, "darksteel", N, Double.NaN, false));
        m.add(tube('y', C, C, SR - 0.1, SR + 0.2, 13.4, 14.4, "paint", N, Double.NaN, false));   // painted band
        // tray draw-off nozzle on the front (face centre), gate valve with handwheel
        double sx = C - APO;
        m.add(prism('x', 8.0, C, 1.7, sx - 0.4, sx + 0.3, "steel", 10));                   // reinforcing pad
        m.add(prism('x', 8.0, C, 1.1, 0.6, sx - 0.4, "steel", 8, false, false));
        m.add(prism('x', 8.0, C, 1.8, 0.1, 0.6, "steel", 10));
        m.add(prism('x', 8.0, C, 0.7, 0.0, 0.6, "soot", 8, true, false));
        boltsOnFaceX(m, 0.0, 0.1, 8.0, C, 1.35, 4, 0.25, 0.5);
        double vx = 1.9;
        m.add(box(vx - 0.7, 6.7, 6.7, vx + 0.7, 9.3, 9.3, "darksteel"));
        m.add(prism('y', vx, C, 0.25, 9.3, 10.9, "steel", 6, false, true));
        m.add(tube('y', vx, C, 0.8, 1.2, 10.7, 11.05, "paint", 10));
        m.add(box(vx - 0.12, 10.75, C - 0.8, vx + 0.12, 11.0, C + 0.8, "paint", "ewud"));
        m.add(box(vx - 0.8, 10.75, C - 0.12, vx + 0.8, 11.0, C + 0.12, "paint", "nsud"));
        // temperature dial on the z- flank
        double sz = C - APO;
        m.add(prism('z', 8.0, C, 0.25, sz - 1.0, sz + 0.2, "steel", 6, false, false));
        m.add(prism('z', 8.0, C, 1.0, sz - 1.6, sz - 1.0, "brass", 10));
        m.add(prism('z', 8.0, C, 0.8, sz - 1.7, sz - 1.6, "white", 10, true, false));
        m.add(box(C - 0.15, 7.9, sz - 1.8, C + 0.65, 8.1, sz - 1.7, "darksteel"));         // needle
        // manway on the z+ flank
        double mz = C + APO;
        m.add(prism('z', C, 9.0, 1.6, mz - 0.3, mz + 0.6, "steel", 10, false, false));
        m.add(prism('z', C, 9.0, 2.0, mz + 0.6, mz + 1.1, "steel", 10));
        for (int k = 0; k < 6; k++) {
            double a = 2 * Math.PI * (k + 0.5) / 6;
            m.add(prism('z', C + 1.7 * Math.cos(a), 9.0 + 1.7 * Math.sin(a), 0.2, mz + 1.1, mz + 1.3, "brass", 6, false, true));
        }
        m.add(box(C - 0.6, 9.0 - 0.2, mz + 1.1, C + 0.6, 9.0 + 0.2, mz + 1.5, "darksteel"));   // handle
        ladder(m, 0.0, 16.0, 0.0);
        return m;
    }

    static List<Mesh> flareStack() {
        List<Mesh> m = new ArrayList<>();
        m.add(box(0.1, 0, 0.1, 15.9, 0.6, 15.9, "steel"));                                 // base plate
        // (the checker: the bolt's underside lay in the anchor post's; the bolt's, on the plate, is dropped)
        for (double[] xz : new double[][]{{1.3, 1.3}, {14.7, 1.3}, {1.3, 14.7}, {14.7, 14.7}}) {   // anchor bolts / wire anchors
            double x = xz[0], z = xz[1];
            m.add(prism('y', x, z, 0.45, 0.6, 0.9, "brass", 6, false, true));
            m.add(box(x - 0.3, 0.6, z - 0.3, x + 0.3, 1.4, z + 0.3, "darksteel"));
        }
        // knockout drum on two saddles, along x
        double dy = 3.7, dr = 2.7;
        for (double x : new double[]{4.5, 10.5}) m.add(box(x, 0.6, 5.6, x + 1.0, 1.8, 10.4, "darksteel"));
        m.add(prism('x', dy, C, dr, 3.5, 12.5, "steel", N, false, false));
        m.add(prism('x', dy, C, dr - 1.0, 2.8, 3.5, "steel", N, dr, true, false, Double.NaN));     // dished heads
        m.add(prism('x', dy, C, dr, 12.5, 13.2, "steel", N, dr - 1.0, false, true, Double.NaN));
        for (double x : new double[]{5.6, 10.0}) m.add(tube('x', dy, C, dr - 0.1, dr + 0.25, x, x + 0.4, "paint", N, Double.NaN, false));
        m.add(box(7.3, 0.6, 7.3, 8.7, dy - dr + 0.3, 8.7, "darksteel"));                    // drain leg
        // inlet: flange at the front face centre, elbow down into the drum
        m.add(prism('x', 8.0, C, 1.8, 0.1, 0.6, "steel", 10));
        m.add(prism('x', 8.0, C, 0.7, 0.0, 0.6, "soot", 8, true, false));
        boltsOnFaceX(m, 0.0, 0.1, 8.0, C, 1.35, 4, 0.25, 0.5);
        m.add(prism('x', 8.0, C, 0.9, 0.6, 3.6, "steel", 8, false, false));
        m.add(box(3.4, 7.0, 7.0, 5.4, 9.0, 9.0, "castiron"));                               // elbow
        m.add(prism('y', 4.4, C, 0.9, dy + dr - 0.6, 7.0, "steel", 8, false, false));
        // stack: plain lower part, red/white bands, guy collar
        double sr = 1.4;
        m.add(prism('y', C, C, sr + 0.6, dy + dr - 0.4, dy + dr + 0.4, "steel", 10));      // nozzle boss on the drum
        m.add(prism('y', C, C, sr, dy + dr + 0.4, 9.5, "steel", 10, false, false));
        double[] bands = {9.5, 10.5, 11.5, 12.5};
        for (int k = 0; k < 4; k++) m.add(prism('y', C, C, sr, bands[k], bands[k] + 1.0, k % 2 == 0 ? "paint" : "white", 10, false, false));
        m.add(tube('y', C, C, sr - 0.1, sr + 0.5, 8.6, 9.2, "darksteel", 10, Double.NaN, false));   // guy collar
        // (the checker: the lugs stood 0.1 px off the collar; they sit 0.5 px further in, at sr + 0.5, half set into the collar
        // (at sr + 0.6 they held 49 %); the wires start with them)
        double[][] lugs = new double[4][];
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + k * Math.PI / 2;
            double lx = C + (sr + 0.5) * Math.cos(a), lz = C + (sr + 0.5) * Math.sin(a);
            m.add(box(lx - 0.3, 8.4, lz - 0.3, lx + 0.3, 9.4, lz + 0.3, "darksteel"));
            lugs[k] = new double[]{lx, 8.9, lz};
        }
        double[][] anchors = {{14.7, 14.7}, {1.3, 14.7}, {1.3, 1.3}, {14.7, 1.3}};
        // (the checker: a slanted 0.24 px wire can't hold half of either end piece's outline seen along an axis, however it
        // is set into them; the wires are left free)
        for (int k = 0; k < 4; k++)
            m.add(rod(lugs[k], new double[]{anchors[k][0], 1.4, anchors[k][1]}, 0.12, "darksteel").free("a guy wire, slanted, from a collar lug to an anchor post"));
        // flare tip: flared cone, hollow barrel (opening at the top centre), soot floor, wind shield on struts
        m.add(prism('y', C, C, sr, 13.5, 14.5, "castiron", 10, 2.1, false, false, Double.NaN));
        m.add(tube('y', C, C, 1.75, 2.1, 14.5, 16.0, "castiron", 10));
        m.add(prism('y', C, C, 1.8, 14.8, 14.9, "soot", 10, false, true));
        m.add(tube('y', C, C, 2.6, 2.85, 14.6, 15.8, "steel", 12));
        for (int k = 0; k < 3; k++) {
            double a = 2 * Math.PI * k / 3 + 0.3;
            m.add(rod(new double[]{C + 2.0 * Math.cos(a), 14.9, C + 2.0 * Math.sin(a)}, new double[]{C + 2.7 * Math.cos(a), 14.9, C + 2.7 * Math.sin(a)},
                0.15, "darksteel"));
        }
        // pilot line: igniter box on the plate (z+), brass line up beside the stack, pilot head over the rim
        double pz = 11.4;
        m.add(box(6.8, 0.6, 11.9, 9.2, 3.0, 13.6, "darksteel"));
        m.add(box(6.6, 3.0, 11.7, 9.4, 3.3, 13.8, "paint"));
        m.add(prism('y', C, 12.75, 0.35, 3.3, 3.9, "brass", 6));
        m.add(rod(new double[]{C, 3.6, 12.75}, new double[]{C, 4.6, pz}, 0.22, "brass", 6));
        m.add(prism('y', C, pz, 0.22, 4.6, 15.3, "brass", 6, false, false));
        m.add(box(C - 0.3, 15.1, 10.2, C + 0.3, 15.7, pz + 0.3, "brass"));                 // pilot head
        // (the checker: the clips' sides lay 0.04 px inside the line's and the lower clip's underside in the boss's; the clips
        // are 0.6 px wide, was 0.3, and the lower one is 0.1 px higher, y 6.1)
        for (double y : new double[]{6.1, 11.0})                                            // line clips
            m.add(box(C - 0.3, y, sr + C - 0.1, C + 0.3, y + 0.3, pz, "darksteel"));
        return m;
    }

    static NodeModel node(String name, List<Mesh> main) {
        // "paint" is signal red (heat band, handwheels, flare stack bands): the palette's red
        NodeModel n = new NodeModel(name, "red");
        n.part("main", main);
        return n;
    }

    public static List<NodeModel> models() {
        return List.of(node("columnbase", columnBase()), node("columnsegment", columnSegment()), node("flarestack", flareStack()));
    }
}
