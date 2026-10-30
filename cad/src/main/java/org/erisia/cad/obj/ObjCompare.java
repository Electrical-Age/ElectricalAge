package org.erisia.cad.obj;

import org.erisia.cad.Face;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Do two face soups draw the same surface (a model ported from another engine: different triangulation, same look)?
 * Per material the same area, and the middle of every triangle of each lies on a triangle of the other facing the same
 * way, with the same texture coordinates there (unless the face is at 45 degrees, where planar mapping may pick either
 * axis).
 */
public final class ObjCompare {
    private ObjCompare() {
    }

    /** A face's material key for the comparison. */
    @FunctionalInterface
    public interface Key {
        String of(Face f);
    }

    /** The differences (empty: the same), at most max of them. */
    public static List<String> differences(List<Face> a, List<Face> b, Key key, int max) {
        return differences(a, b, key, max, true);
    }

    /** ... comparing texture coordinates or not (a model moved to another atlas layout). */
    public static List<String> differences(List<Face> a, List<Face> b, Key key, int max, boolean uvs) {
        List<String> out = new ArrayList<>();
        Map<String, double[]> area = new HashMap<>();
        for (Face f : a) area.computeIfAbsent(key.of(f), k -> new double[2])[0] += f.area();
        for (Face f : b) area.computeIfAbsent(key.of(f), k -> new double[2])[1] += f.area();
        for (var e : area.entrySet())
            if (Math.abs(e.getValue()[0] - e.getValue()[1]) > 1e-3 * Math.max(1, e.getValue()[0]))
                out.add(e.getKey() + ": area " + e.getValue()[0] + " vs " + e.getValue()[1]);
        covered(a, b, key, "first", out, max, uvs);
        covered(b, a, key, "second", out, max, uvs);
        return out.size() > max ? out.subList(0, max) : out;
    }

    static void covered(List<Face> from, List<Face> on, Key key, String which, List<String> out, int max, boolean uvs) {
        for (Face f : from) {
            if (out.size() > max) return;
            double[] d = org.erisia.cad.Geom2.distinct(f);
            if (d.length < 9) continue;
            int n = d.length / 3;
            double[] c = new double[3];
            for (int i = 0; i < n; i++) for (int k = 0; k < 3; k++) c[k] += d[3 * i + k] / n;
            boolean hit = false;
            for (Face g : on) {
                if (!key.of(f).equals(key.of(g)) || f.nx * g.nx + f.ny * g.ny + f.nz * g.nz < 1 - 1e-4) continue;
                if (Math.abs(g.nx * c[0] + g.ny * c[1] + g.nz * c[2] - g.plane()) > 1e-3 || !org.erisia.cad.Geom2.inside(g, c)) continue;
                double[] ua = org.erisia.cad.Geom2.uvAt(f, c), ub = org.erisia.cad.Geom2.uvAt(g, c);
                if (uvs && ua != null && ub != null && !PlanarUv.ambiguous(f.nx, f.ny, f.nz) && (Math.abs(ua[0] - ub[0]) > 2e-3 || Math.abs(ua[1] - ub[1]) > 2e-3)) continue;
                hit = true;
                break;
            }
            if (!hit) out.add("the " + which + "'s " + f + " isn't drawn by the other");
        }
    }
}
