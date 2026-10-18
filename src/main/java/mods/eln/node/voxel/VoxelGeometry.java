package mods.eln.node.voxel;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * What a voxel node's parts look like, as textured quads (notes/cables/PLAN.md step 8: baked into the chunk mesh, not
 * drawn every frame): round pipes with fittings and flanges, valves, adapters, and cables as boxes in their six-node
 * texture. Pure geometry, no client classes; VoxelModel (client) bakes it.
 */
public final class VoxelGeometry {
    private VoxelGeometry() {
    }

    /**
     * One quad: its texture (block-atlas sprite id), colour (ARGB, multiplied in), corners (block-local 0..1, x y z each,
     * wound counter-clockwise seen from outside), UVs (0..1 of the sprite, u v each), and the block face it lies on
     * (3D value; -1 if inside the block), for culling.
     */
    public record Quad(Identifier sprite, int argb, float[] xyz, float[] uv, int face) {
    }

    public static final Identifier[] PIPE = {Identifier.fromNamespaceAndPath("eln", "block/pipe_iron"),
        Identifier.fromNamespaceAndPath("eln", "block/pipe_alloy")};
    static final int SIDES = 12;

    /** The cable texture of a tier (the six-node cable's, copied onto the block atlas: textures/block/cable/). */
    public static Identifier cableSprite(int tier) {
        return Identifier.fromNamespaceAndPath("eln", CablePart.TIERS[tier].height() > 1 && CablePart.TIERS[tier].name().startsWith("Very")
            ? "block/cable/cablevhv" : "block/cable/cable");
    }

    public static List<Quad> of(List<VoxelPart> parts) {
        Builder b = new Builder();
        for (VoxelPart part : parts) {
            if (part instanceof ValvePart v) {
                b.sprite = PIPE[0];
                var bs = v.boxes();
                b.color(0.45f, 0.45f, 0.5f);
                b.box(bs.get(0));
                // the handwheel: red when shut, green when open
                if (v.open) b.color(0.3f, 0.75f, 0.3f);
                else b.color(0.8f, 0.2f, 0.15f);
                int[] s = bs.get(1);
                b.box(s);
                if (v.axis == 1) b.box(new int[]{s[3] - 1, s[1] - 2, s[2] - 2, s[3], s[4] + 2, s[5] + 2});
                else b.box(new int[]{s[0] - 2, s[4] - 1, s[2] - 2, s[3] + 2, s[4], s[5] + 2});
            } else if (part instanceof SafetyValvePart sv) {
                b.sprite = PIPE[0];
                b.color(0.8f, 0.6f, 0.3f);   // (brass body, a red cap)
                int[] bx = sv.boxes().get(0);
                int ax = sv.outlet / 2 == 0 ? 1 : sv.outlet / 2 == 1 ? 2 : 0;
                double[] c = {(bx[0] + bx[3]) / 2.0, (bx[1] + bx[4]) / 2.0, (bx[2] + bx[5]) / 2.0};
                int[] o = others(ax);
                double lo = bx[ax], hi = bx[ax + 3];
                b.tube(ax, c[o[0]], c[o[1]], 1.4, lo, hi);
                b.color(0.75f, 0.15f, 0.12f);
                boolean up = sv.outlet % 2 == 1;
                b.tube(ax, c[o[0]], c[o[1]], 2.0, up ? hi - 1 : lo, up ? hi : lo + 1);
            } else if (part instanceof AdapterPart a) {
                b.sprite = PIPE[0];
                b.color(0.75f, 0.55f, 0.3f);   // (brass)
                b.box(a.boxes().get(0));
            } else if (part instanceof PipePart pipe) {
                b.sprite = PIPE[pipe.tier];
                if (pipe.burst) b.color(0.55f, 0.35f, 0.25f);   // (burst: scorched)
                else b.color(1, 1, 1);
                pipe(b, pipe);
            } else if (part instanceof CablePart c) {
                b.sprite = cableSprite(c.tier);
                if (c.burnt != null) b.color(0.25f, 0.2f, 0.18f);
                else b.color(1, 1, 1);
                cable(b, c);
            }
        }
        return b.out;
    }

    /** A pipe: a round tube along each straight piece, a fitting ring at each bend, a flange where it meets a face. */
    static void pipe(Builder b, PipePart pipe) {
        double r = pipe.bore() / 2.0;
        int[] p = pipe.points;
        int n = p.length / 3;
        for (int i = 1; i < n; i++) {
            int[] a = {p[3 * i - 3], p[3 * i - 2], p[3 * i - 1]}, c = {p[3 * i], p[3 * i + 1], p[3 * i + 2]};
            int axis = a[0] != c[0] ? 0 : a[1] != c[1] ? 1 : 2;
            double lo = Math.min(a[axis], c[axis]), hi = Math.max(a[axis], c[axis]);
            // run into the bend a little so the tubes meet without a gap
            boolean aBend = i > 1, cBend = i < n - 1;
            if (a[axis] < c[axis]) { if (aBend) lo -= r * 0.7; if (cBend) hi += r * 0.7; }
            else { if (cBend) lo -= r * 0.7; if (aBend) hi += r * 0.7; }
            int[] o = others(axis);
            b.tube(axis, a[o[0]], a[o[1]], r, lo, hi);
        }
        for (int i = 1; i < n - 1; i++) {
            int[] c = {p[3 * i], p[3 * i + 1], p[3 * i + 2]};
            int axis = p[3 * i - 3] != c[0] ? 0 : p[3 * i - 2] != c[1] ? 1 : 2;
            int[] o = others(axis);
            b.tube(axis, c[o[0]], c[o[1]], r + 0.35, c[axis] - r - 0.3, c[axis] + r + 0.3);
        }
        for (int end = 0; end < 2; end++) {
            int i = end == 0 ? 0 : n - 1;
            int x = p[3 * i], y = p[3 * i + 1], z = p[3 * i + 2];
            int face = PipePart.faceOf(x, y, z);
            if (face < 0) continue;
            int axis = face <= 1 ? 1 : face <= 3 ? 2 : 0;
            int[] c = {x, y, z};
            int[] o = others(axis);
            double at = c[axis];
            b.tube(axis, c[o[0]], c[o[1]], r + 0.75, at == 0 ? 0 : 15, at == 0 ? 1 : 16);
        }
    }

    /**
     * A cable: its boxes, textured as six-node cables are (CableRender): the strip of the texture's left half centred
     * on u = 1/4, across the cable on top and running down its sides, v along it.
     */
    static void cable(Builder b, CablePart c) {
        List<int[]> boxes = c.boxes();
        int[] p = c.points;
        for (int i = 0; i < boxes.size(); i++) {
            int along, n;
            if (c.isKnuckle()) {
                along = p[0] != 0 && p[0] != 16 ? 0 : p[1] != 0 && p[1] != 16 ? 1 : 2;
                n = along == 1 ? 0 : 1;
            } else {
                int k = 3 * (i + 1);
                along = p[k] != p[k - 3] ? 0 : p[k + 1] != p[k - 2] ? 1 : 2;
                n = CablePart.normalAxis(c.face);
            }
            int[] bx = boxes.get(i);
            int w = 3 - along - n;
            double cw = (bx[w] + bx[w + 3]) / 2.0, half = (bx[w + 3] - bx[w]) / 2.0;
            double top = c.isKnuckle() || CablePart.plane(c.face) == 0 ? bx[n + 3] : bx[n];
            b.uvs = (q, fa) -> {
                float[] uv = new float[8];
                for (int j = 0; j < 4; j++) {
                    double[] v = q[j];
                    double u, vv = v[along] / 16;
                    double depth = Math.abs(top - v[n]);
                    if (fa == w) u = 0.25 + Math.signum(v[w] - cw) * (half / 32 + depth / 16);
                    else u = 0.25 + (v[w] - cw) / 32;
                    if (fa == along) vv = 0.5 - depth / 16;
                    uv[2 * j] = (float) clamp01(u);
                    uv[2 * j + 1] = (float) clamp01(vv);
                }
                return uv;
            };
            b.box(bx);
            b.uvs = null;
        }
    }

    static double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }

    static int[] others(int axis) {
        return axis == 0 ? new int[]{1, 2} : axis == 1 ? new int[]{0, 2} : new int[]{0, 1};
    }

    static final class Builder {
        final List<Quad> out = new ArrayList<>();
        Identifier sprite;
        int argb = 0xFFFFFFFF;
        /** UVs for box faces (corners, the face's normal axis), or null for planar. */
        java.util.function.BiFunction<double[][], Integer, float[]> uvs;

        void color(float r, float g, float b) {
            argb = 0xFF000000 | Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255);
        }

        /** A box in voxels, every face out, UVs planar on the face's two other axes (vanilla density). */
        void box(int[] bx) {
            double x0 = bx[0], y0 = bx[1], z0 = bx[2], x1 = bx[3], y1 = bx[4], z1 = bx[5];
            if (x1 <= x0 || y1 <= y0 || z1 <= z0) return;
            double[][] c = new double[8][];
            for (int i = 0; i < 8; i++) c[i] = new double[]{(i & 1) == 0 ? x0 : x1, (i & 2) == 0 ? y0 : y1, (i & 4) == 0 ? z0 : z1};
            face(c[0], c[4], c[6], c[2], -1, 0, 0, x0 == 0 ? 4 : -1);
            face(c[1], c[3], c[7], c[5], 1, 0, 0, x1 == 16 ? 5 : -1);
            face(c[0], c[1], c[5], c[4], 0, -1, 0, y0 == 0 ? 0 : -1);
            face(c[2], c[6], c[7], c[3], 0, 1, 0, y1 == 16 ? 1 : -1);
            face(c[0], c[2], c[3], c[1], 0, 0, -1, z0 == 0 ? 2 : -1);
            face(c[4], c[5], c[7], c[6], 0, 0, 1, z1 == 16 ? 3 : -1);
        }

        private void face(double[] a, double[] b, double[] c, double[] d, int nx, int ny, int nz, int cull) {
            double[][] q = wind(a, b, c, d, nx, ny, nz);
            if (uvs != null) {
                add(q, uvs.apply(q, nx != 0 ? 0 : ny != 0 ? 1 : 2), cull);
                return;
            }
            float[] uv = new float[8];
            for (int i = 0; i < 4; i++) {
                double[] p = q[i];
                double u = nx != 0 ? p[2] : p[0], v = ny != 0 ? p[2] : p[1];
                uv[2 * i] = (float) (u / 16);
                uv[2 * i + 1] = (float) (1 - v / 16);
            }
            add(q, uv, cull);
        }

        /** A capped tube along axis, centred at (c1, c2) on the other two, from a0 to a1 (voxels). */
        void tube(int axis, double c1, double c2, double r, double a0, double a1) {
            int[] o = others(axis);
            double[][] ring0 = new double[SIDES][], ring1 = new double[SIDES][];
            for (int k = 0; k < SIDES; k++) {
                double ang = 2 * Math.PI * (k + 0.5) / SIDES;
                double[] v0 = new double[3], v1 = new double[3];
                v0[o[0]] = v1[o[0]] = c1 + r * Math.cos(ang);
                v0[o[1]] = v1[o[1]] = c2 + r * Math.sin(ang);
                v0[axis] = a0;
                v1[axis] = a1;
                ring0[k] = v0;
                ring1[k] = v1;
            }
            for (int k = 0; k < SIDES; k++) {
                int k1 = (k + 1) % SIDES;
                double ang = 2 * Math.PI * (k + 1) / SIDES;
                double[] nrm = new double[3];
                nrm[o[0]] = Math.cos(ang);
                nrm[o[1]] = Math.sin(ang);
                double[][] q = wind(ring0[k], ring0[k1], ring1[k1], ring1[k], nrm[0], nrm[1], nrm[2]);
                float[] uv = new float[8];
                for (int i = 0; i < 4; i++) {
                    double[] v = q[i];
                    uv[2 * i] = (float) ((k + (v == ring0[k1] || v == ring1[k1] ? 1 : 0)) / (double) SIDES);
                    uv[2 * i + 1] = (float) clamp01(v[axis] / 16);
                }
                add(q, uv, -1);
            }
            double[] cc0 = new double[3], cc1 = new double[3];
            cc0[o[0]] = cc1[o[0]] = c1;
            cc0[o[1]] = cc1[o[1]] = c2;
            cc0[axis] = a0;
            cc1[axis] = a1;
            int f0 = a0 <= 0 ? (axis == 0 ? 4 : axis == 1 ? 0 : 2) : -1, f1 = a1 >= 16 ? (axis == 0 ? 5 : axis == 1 ? 1 : 3) : -1;
            for (int k = 0; k < SIDES; k++) {
                int k1 = (k + 1) % SIDES;
                double[] n0 = new double[3], n1 = new double[3];
                n0[axis] = -1;
                n1[axis] = 1;
                cap(wind(cc0, cc0, ring0[k1], ring0[k], n0[0], n0[1], n0[2]), axis, f0);
                cap(wind(cc1, cc1, ring1[k], ring1[k1], n1[0], n1[1], n1[2]), axis, f1);
            }
        }

        private void cap(double[][] q, int axis, int cull) {
            int[] o = others(axis);
            float[] uv = new float[8];
            for (int i = 0; i < 4; i++) {
                uv[2 * i] = (float) clamp01(q[i][o[0]] / 16);
                uv[2 * i + 1] = (float) clamp01(q[i][o[1]] / 16);
            }
            add(q, uv, cull);
        }

        /** The four corners in an order whose normal (b-a)x(c-a) points along n (a degenerate first edge skipped). */
        static double[][] wind(double[] a, double[] b, double[] c, double[] d, double nx, double ny, double nz) {
            double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2], vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
            if (ux * ux + uy * uy + uz * uz < 1e-12) {
                ux = c[0] - b[0]; uy = c[1] - b[1]; uz = c[2] - b[2];
                vx = d[0] - b[0]; vy = d[1] - b[1]; vz = d[2] - b[2];
            }
            double cx = uy * vz - uz * vy, cy = uz * vx - ux * vz, cz = ux * vy - uy * vx;
            return cx * nx + cy * ny + cz * nz >= 0 ? new double[][]{a, b, c, d} : new double[][]{a, d, c, b};
        }

        void add(double[][] q, float[] uv, int cull) {
            float[] xyz = new float[12];
            for (int i = 0; i < 4; i++)
                for (int a = 0; a < 3; a++) xyz[3 * i + a] = (float) (q[i][a] / 16);
            out.add(new Quad(sprite, argb, xyz, uv, cull));
        }
    }
}
