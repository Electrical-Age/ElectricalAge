package mods.eln.cad.models;

import org.erisia.cad.Frustum;
import org.erisia.cad.Mesh;
import org.erisia.cad.Solid;
import org.erisia.cad.Sweep;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.OilKit.boltRing;

/**
 * The 1x1x2 Condenser Coil (refinery M2): an upright iron drum on four legs, a copper cooling coil wound round it, a
 * copper heat plate strapped to it on each thermal face. Drawn by its render from its lower block, full height: 0..16 px
 * across, 0..32 px up, front XN (x = 0), as CondenserCoilElement's ports and faces:
 * <ul>
 * <li>vapour in: a flange on top (8, 32, 8);</li>
 * <li>gas off: a flange on the front of the upper block, high (y 28, z 8);</li>
 * <li>condensate out: a flange underneath (8, 0, 8) on the drum's drain, and one on the front of the lower block, low
 * (y 4, z 8);</li>
 * <li>thermal: a heat plate on the back and both flanks of each block (y 9.5, y 24.5).</li>
 * </ul>
 */
public final class CondenserCoil {
    private CondenserCoil() {
    }

    /** The drum's axis (x, z), radius, bottom and top; the coil's radius, the tube's, its turns and span. */
    static final double C = 8, R = 5, Y0 = 3.5, Y1 = 29, RC = 5.5, RT = 0.6, TURNS = 4, H0 = 7, H1 = 26;
    static final int N = 16;

    /** A side flange on the front (x 0) at (y, z): flange, reducer, bolts and a stub into the drum. */
    static List<Mesh> frontFlange(double y, double z) {
        List<Mesh> m = new ArrayList<>();
        m.add(prism('x', y, z, 2.75, 0.0, 1.0, "steel", 12));
        m.add(prism('x', y, z, 2.0, 1.0, 1.4, "steel", 12, false, true));
        m.addAll(boltRing('x', y, z, 2.35, 1.0, 1.2, 6, "brass", 0.2, 0));
        m.add(prism('x', y, z, 1.2, 1.4, C - R + 0.8, "steel", 10, false, false));
        return m;
    }

    static List<Solid> main() {
        List<Solid> m = new ArrayList<>();
        // the drum, its heads and a foot band; four legs on the diagonals up to the band
        m.add(prism('y', C, C, R, Y0, Y1, "steel", N, false, false));
        m.add(prism('y', C, C, R - 1.2, Y0 - 1.0, Y0, "steel", N, R, true, false, Double.NaN));
        m.add(prism('y', C, C, R, Y1, Y1 + 1.0, "steel", N, R - 1.2, false, true, Double.NaN));
        m.add(prism('y', C, C, R + 0.6, 3.6, 4.6, "darksteel", N, false, false));
        for (double[] q : new double[][]{{4.4, 4.4}, {11.6, 4.4}, {4.4, 11.6}, {11.6, 11.6}})
            m.add(box(q[0] - 0.5, 0, q[1] - 0.5, q[0] + 0.5, 4.4, q[1] + 0.5, "darksteel"));
        // the cooling coil: a copper tube wound round the drum, in at the top and out at the bottom through the shell
        int per = 12;
        int n = (int) (TURNS * per);
        double[] pts = new double[3 * (n + 3)];
        pts[0] = C + R - 1.0;
        pts[1] = H1;
        pts[2] = C;
        for (int i = 0; i <= n; i++) {
            double a = 2 * Math.PI * i / per, y = H1 - (H1 - H0) * i / n;
            pts[3 + 3 * i] = C + RC * Math.cos(a);
            pts[4 + 3 * i] = y;
            pts[5 + 3 * i] = C + RC * Math.sin(a);
        }
        double aEnd = 2 * Math.PI * n / per;
        pts[3 * (n + 2)] = C + (R - 1.0) * Math.cos(aEnd);
        pts[3 * (n + 2) + 1] = H0;
        pts[3 * (n + 2) + 2] = C + (R - 1.0) * Math.sin(aEnd);
        for (Frustum f : Sweep.along(pts, RT, 0.3)) m.add(f.material("copper"));
        // the heat plates on the back and flanks of each block, each strapped to the drum
        for (double y : new double[]{9.5, 24.5}) {
            m.add(box(14.9, y - 2.5, 5.0, 15.9, y + 2.5, 11.0, "hotcopper"));
            m.add(box(12.5, y - 1.0, 7.0, 15.0, y + 1.0, 9.0, "hotcopper"));
            m.add(box(5.0, y - 2.5, 0.1, 11.0, y + 2.5, 1.1, "hotcopper"));
            m.add(box(7.0, y - 1.0, 1.0, 9.0, y + 1.0, 3.5, "hotcopper"));
            m.add(box(5.0, y - 2.5, 14.9, 11.0, y + 2.5, 15.9, "hotcopper"));
            m.add(box(7.0, y - 1.0, 12.5, 9.0, y + 1.0, 15.0, "hotcopper"));
        }
        // the vapour inlet on top: a neck off the head, the flange at the top face
        m.add(prism('y', C, C, 1.4, Y1 + 0.6, 31.0, "steel", 12, false, false));
        m.add(prism('y', C, C, 2.75, 31.0, 32.0, "steel", 12));
        m.addAll(boltRing('y', C, C, 2.35, 30.8, 31.0, 6, "brass", 0.2, 0));
        // the gas offtake (front, high) and the condensate outlets (front, low; underneath, on the drain)
        m.addAll(frontFlange(28.0, C));
        m.addAll(frontFlange(4.0, C));
        m.add(prism('y', C, C, 1.2, 1.0, Y0 - 0.6, "steel", 10, false, false));
        m.add(prism('y', C, C, 2.75, 0.0, 1.0, "steel", 12));
        return m;
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("condensercoil", OilKit.PAINT_BOILER);
        n.part("main", main());
        return List.of(n);
    }
}
