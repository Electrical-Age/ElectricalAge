package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.oriented;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;
import static mods.eln.cad.models.Mc.v;

/**
 * Shared pieces of the oil-chain and shaft device models (was tools/models/oilkit.py): oriented boxes, quarter turns
 * about the block's vertical centre line, mirrors, the floor terminals (electric, signal, thermal), bolt rings,
 * handwheels and dials. px, corner origin, front XN (x = 0).
 */
public final class OilKit {
    private OilKit() {
    }

    /** The tier and device paints (the "paint" material's palette entry per model). */
    public static final String PAINT_50V = "paint_green", PAINT_200V = "paint_blue", PAINT_BOILER = "paint_boiler";

    /** Oriented box: centre c, orthonormal axes ax ay az, half sizes hx hy hz. */
    public static Mesh obox(double[] c, double[] ax, double[] ay, double[] az, double hx, double hy, double hz, String mat) {
        Mesh m = new Mesh(mat);
        double[][] axes = {ax, ay, az};
        int[][] uv = {{1, 2}, {0, 2}, {0, 1}};
        for (int axis = 0; axis < 3; axis++)
            for (int s = -1; s <= 1; s += 2) {
                double[][] quad = new double[4][];
                int[][] ab = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
                for (int q = 0; q < 4; q++) {
                    double[] sg = new double[3];
                    sg[axis] = s;
                    sg[uv[axis][0]] = ab[q][0];
                    sg[uv[axis][1]] = ab[q][1];
                    double[] p = new double[3];
                    for (int i = 0; i < 3; i++) p[i] = c[i] + sg[0] * hx * ax[i] + sg[1] * hy * ay[i] + sg[2] * hz * az[i];
                    quad[q] = p;
                }
                double[] hint = {s * axes[axis][0], s * axes[axis][1], s * axes[axis][2]};
                m.poly(oriented(quad, hint));
            }
        m.closed = m.convex = true;
        return m;
    }

    /** k quarter turns about the block's vertical centre line: (x, z) -> (z, 16 - x) a turn (x = 0 face to z = 16 at k = 1). */
    public static List<Mesh> turned(List<Mesh> meshes, int k) {
        List<Mesh> out = new ArrayList<>();
        for (Mesh m : meshes)
            out.add(m.mapped(p -> {
                for (int t = 0; t < ((k % 4) + 4) % 4; t++) {
                    double x = p[0];
                    p[0] = p[2];
                    p[2] = 16 - x;
                }
            }, false));
        return out;
    }

    /** Mirrored across x = cx (the winding reversed back to outward). */
    public static List<Mesh> mirroredX(List<Mesh> meshes, double cx) {
        List<Mesh> out = new ArrayList<>();
        for (Mesh m : meshes) out.add(m.mapped(p -> p[0] = 2 * cx - p[0], true));
        return out;
    }

    /** Electric floor terminal on the x = 0 face: steel plate recessed 0.1 px, 7 px wide, floor to 4 px, three copper studs, tier stripe, rubber edges. */
    public static List<Mesh> terminalPlate(String paint) {
        List<Mesh> ms = new ArrayList<>(List.of(box(0.1, 0, 4.5, 0.85, 4.0, 11.5, "steel", "nsewu"), box(0.1, 4.0, 4.5, 0.85, 4.5, 11.5, paint, "nsewu"),
            box(0.1, 0.5, 4.25, 0.85, 3.5, 4.5, "rubber", "nweu"), box(0.1, 0.5, 11.5, 0.85, 3.5, 11.75, "rubber", "sweu")));
        for (double z : new double[]{5.75, 8.0, 10.25}) ms.add(box(0.0, 1.75, z - 0.5, 0.1, 2.75, z + 0.5, "copper", "nsewu"));
        return ms;
    }

    public static List<Mesh> terminalPlate() {
        return terminalPlate("paint");
    }

    /** Signal terminal on the x = 0 face: a narrow plate, 3 px wide, floor to 4 px, one small copper stud. */
    public static List<Mesh> signalPlate() {
        return new ArrayList<>(List.of(box(0.1, 0, 6.5, 0.85, 3.5, 9.5, "steel", "nsewu"), box(0.1, 3.5, 6.5, 0.85, 3.9, 9.5, "darksteel", "nsewu"),
            box(0.0, 1.9, 7.6, 0.1, 2.6, 8.4, "copper", "nsewu")));
    }

    /** Thermal floor contact on the x = 0 face: heat-tinted copper pad 8 px wide, recessed 0.1 px, cast-iron backing, four bolt heads. */
    public static List<Mesh> heatPad(double depth, double top) {
        List<Mesh> ms = new ArrayList<>(List.of(box(0.1, 0, 4.0, depth, top, 12.0, "hotcopper"), box(depth, 0, 3.5, depth + 0.5, top + 0.5, 12.5, "castiron")));
        for (double[] yz : new double[][]{{top - 0.9, 4.9}, {top - 0.9, 11.1}, {0.9, 4.9}, {0.9, 11.1}})
            ms.add(prism('x', yz[0], yz[1], 0.35, 0.0, 0.1, "brass", 6, true, false));
        return ms;
    }

    public static List<Mesh> heatPad() {
        return heatPad(1.0, 4.0);
    }

    public static List<Mesh> boltRing(char axis, double c1, double c2, double r, double a0, double a1, int count, String mat, double rb, double phase) {
        List<Mesh> ms = new ArrayList<>();
        for (int k = 0; k < count; k++) {
            double a = phase + 2 * Math.PI * (k + 0.5) / count;
            ms.add(prism(axis, c1 + r * Math.cos(a), c2 + r * Math.sin(a), rb, a0, a1, mat, 6, false, true));
        }
        return ms;
    }

    public static List<Mesh> boltRing(char axis, double c1, double c2, double r, double a0, double a1, int count) {
        return boltRing(axis, c1, c2, r, a0, a1, count, "brass", 0.3, 0);
    }

    /** Red valve handwheel (rim and two spokes) at (x, y, z), turning about 'y' or 'z'. */
    public static List<Mesh> handwheel(double x, double y, double z, double r, char axis) {
        List<Mesh> ms = new ArrayList<>();
        if (axis == 'y') {
            ms.add(tube('y', x, z, r - 0.3, r, y - 0.15, y + 0.15, "red", 6));
            ms.add(box(x - r + 0.2, y - 0.1, z - 0.12, x + r - 0.2, y + 0.1, z + 0.12, "red"));
            ms.add(box(x - 0.12, y - 0.1, z - r + 0.2, x + 0.12, y + 0.1, z + r - 0.2, "red"));
            ms.add(prism('y', x, z, 0.3, y - 0.2, y + 0.3, "darksteel", 6));
        } else {
            ms.add(tube('z', x, y, r - 0.3, r, z - 0.15, z + 0.15, "red", 6));
            ms.add(box(x - r + 0.2, y - 0.12, z - 0.1, x + r - 0.2, y + 0.12, z + 0.1, "red"));
            ms.add(box(x - 0.12, y - r + 0.2, z - 0.1, x + 0.12, y + r - 0.2, z + 0.1, "red"));
            ms.add(prism('z', x, y, 0.3, z - 0.2, z + 0.3, "darksteel", 6));
        }
        return ms;
    }

    public static List<Mesh> handwheel(double x, double y, double z) {
        return handwheel(x, y, z, 1.2, 'y');
    }

    /**
     * Round gauge: brass case, glass face, darksteel needle; facing "xn" (out of the front), "zn" or "zp". (the checker:
     * the glass was 0.05 px thick, its face on the z-fight limit with the case's; it is 0.1 px thick and the needle 0.05 px
     * further out)
     */
    public static List<Mesh> dial(double x, double y, double z, double r, String facing, double depth) {
        if (facing.equals("xn"))
            return new ArrayList<>(List.of(prism('x', y, z, r, x, x + depth, "brass", 10), prism('x', y, z, r - 0.25, x - 0.1, x, "glass", 10, true, false),
                obox(v(x - 0.15, y + 0.25 * r, z + 0.2 * r), v(1, 0, 0), v(0, 0.8, 0.6), v(0, -0.6, 0.8), 0.04, 0.5 * r, 0.08, "darksteel")));
        int s = facing.equals("zn") ? -1 : 1;
        double a0 = s > 0 ? z - depth : z, a1 = s > 0 ? z : z + depth;
        Mesh glass = s > 0 ? prism('z', x, y, r - 0.25, a1, a1 + 0.1, "glass", 10, false, true) : prism('z', x, y, r - 0.25, a0 - 0.1, a0, "glass", 10, true, false);
        return new ArrayList<>(List.of(prism('z', x, y, r, a0, a1, "brass", 10), glass,
            obox(v(x - 0.3 * r, y + 0.22 * r, s > 0 ? a1 + 0.15 : a0 - 0.15), v(0.6, 0.8, 0), v(-0.8, 0.6, 0), v(0, 0, 1), 0.08, 0.5 * r, 0.04, "darksteel")));
    }

    public static List<Mesh> dial(double x, double y, double z, double r) {
        return dial(x, y, z, r, "xn", 0.5);
    }
}
