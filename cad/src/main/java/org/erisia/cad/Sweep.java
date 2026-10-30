package org.erisia.cad;

import java.util.ArrayList;
import java.util.List;

/**
 * A round section swept along a polyline: one cylinder per straight piece, each run on into the bends by extend x r so
 * the pieces meet without a gap (a fitting ring over the bend hides the joint). Pieces along a frame axis are made
 * exactly on it; others turned onto their direction.
 */
public final class Sweep {
    private Sweep() {
    }

    /** The pieces along points (x y z each, px), radius r, 12 sides. */
    public static List<Frustum> along(double[] points, double r, double extend) {
        int n = points.length / 3;
        List<Frustum> out = new ArrayList<>(Math.max(0, n - 1));
        for (int i = 1; i < n; i++) {
            double[] a = {points[3 * i - 3], points[3 * i - 2], points[3 * i - 1]}, c = {points[3 * i], points[3 * i + 1], points[3 * i + 2]};
            boolean aBend = i > 1, cBend = i < n - 1;
            int moved = 0, axis = -1;
            for (int k = 0; k < 3; k++)
                if (a[k] != c[k]) {
                    moved++;
                    axis = k;
                }
            if (moved == 1) {
                double lo = Math.min(a[axis], c[axis]), hi = Math.max(a[axis], c[axis]);
                if (a[axis] < c[axis]) {
                    if (aBend) lo -= r * extend;
                    if (cBend) hi += r * extend;
                } else {
                    if (cBend) lo -= r * extend;
                    if (aBend) hi += r * extend;
                }
                int[] o = Frustum.others(axis);
                out.add(Frustum.cylinder(axis, a[o[0]], a[o[1]], r, lo, hi));
                continue;
            }
            // a slanted piece: a cylinder along z from 0 to its length, turned and moved onto it
            double dx = c[0] - a[0], dy = c[1] - a[1], dz = c[2] - a[2], len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1e-9) continue;
            double yaw = Math.atan2(dx, dz), pitch = Math.atan2(-dy, Math.sqrt(dx * dx + dz * dz));
            Frame f = Frame.translation(a[0], a[1], a[2]).then(Frame.rotY(yaw)).then(Frame.rotX(pitch));
            Frustum p = Frustum.cylinder(2, 0, 0, r, aBend ? -r * extend : 0, len + (cBend ? r * extend : 0));
            p.frame = f;
            out.add(p);
        }
        return out;
    }
}
