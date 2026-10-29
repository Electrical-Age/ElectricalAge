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
        if (CablePart.TIERS[tier].thermal()) return Identifier.fromNamespaceAndPath("eln", "block/cable/thermal");
        return Identifier.fromNamespaceAndPath("eln", CablePart.TIERS[tier].height() > 1 && CablePart.TIERS[tier].name().startsWith("Very")
            ? "block/cable/cablevhv" : "block/cable/cable");
    }

    public static List<Quad> of(List<VoxelPart> parts) {
        return of(parts, null);
    }

    /** ... with lugs where floor cables end at a terminal plate (lugs: conductor height px per horizontal face, or null). */
    public static List<Quad> of(List<VoxelPart> parts, float[] lugs) {
        Builder b = new Builder();
        for (VoxelPart part : parts) {
            if (part instanceof ValvePart v) {
                // a gate valve: a body round the pipe, a stem, and a round handwheel wider than the pipe (Baughn: they
                // looked like buttons); the wheel red when shut, green when open
                b.sprite = PIPE[0];
                b.color(0.45f, 0.45f, 0.5f);
                int h = v.bore / 2 + 1, r = v.wheel(), s = v.reach();
                double[] c = {v.x, v.y, v.z};
                int[] o = others(v.axis);
                b.tube(v.axis, c[o[0]], c[o[1]], h, c[v.axis] - h, c[v.axis] + h);
                int stem = v.axis == 1 ? 0 : 1;   // (the stem's axis: up, or along x for a vertical pipe)
                int[] so = others(stem);
                b.tube(stem, c[so[0]], c[so[1]], 0.75, c[stem] + h - 0.5, c[stem] + s);
                // (the control and throttle valves: red shut, through yellow, to green open)
                float op = (float) (v.mode == ValvePart.ON_OFF ? (v.open ? 1 : 0) : v.opening());
                b.color(0.8f - 0.5f * op, 0.2f + 0.55f * op, 0.15f + 0.15f * op);
                double plane = c[stem] + s;
                if (v.mode == ValvePart.PRESSURE) {
                    // the pressure valve: a dome actuator (brass relieving, steel reducing) and a nub on the body's
                    // downstream side showing its flow (Baughn 2026-10-29)
                    double[] lo = new double[3], hi = new double[3];
                    lo[stem] = plane;
                    hi[stem] = plane + 1.5;
                    lo[so[0]] = c[so[0]] - 2;
                    hi[so[0]] = c[so[0]] + 2;
                    lo[so[1]] = c[so[1]] - 2;
                    hi[so[1]] = c[so[1]] + 2;
                    if (v.pmode == ValvePart.RELIEF) b.color(0.75f, 0.6f, 0.25f);
                    else b.color(0.6f, 0.62f, 0.68f);
                    b.boxD(lo, hi);
                    double[] nlo = new double[3], nhi = new double[3];
                    double tip = c[v.axis] + v.flow * (h + 1);
                    nlo[v.axis] = Math.min(c[v.axis] + v.flow * h, tip);
                    nhi[v.axis] = Math.max(c[v.axis] + v.flow * h, tip);
                    nlo[so[0] == v.axis ? so[1] : so[0]] = c[so[0] == v.axis ? so[1] : so[0]] - 0.75;
                    nhi[so[0] == v.axis ? so[1] : so[0]] = c[so[0] == v.axis ? so[1] : so[0]] + 0.75;
                    nlo[stem] = c[stem] + h - 1.5;
                    nhi[stem] = c[stem] + h;
                    b.color(0.85f, 0.85f, 0.2f);
                    b.boxD(nlo, nhi);
                    continue;
                }
                if (v.mode == ValvePart.CONTROL) {
                    // an actuator: a square box on the stem, coloured by the opening (its signal: a cable laid into the block)
                    double[] lo = new double[3], hi = new double[3];
                    lo[stem] = plane;
                    hi[stem] = plane + 1;
                    lo[so[0]] = c[so[0]] - 2;
                    hi[so[0]] = c[so[0]] + 2;
                    lo[so[1]] = c[so[1]] - 2;
                    hi[so[1]] = c[so[1]] + 2;
                    b.boxD(lo, hi);
                    continue;
                }

                // the rim: a ring (AdventurAgent: 16 cubes read as beads); two spokes across
                b.ring(stem, c[so[0]], c[so[1]], r - 1.1, r, plane, plane + 1, 16);
                for (int a = 0; a < 2; a++) {
                    double[] lo = new double[3], hi = new double[3];
                    lo[stem] = plane + 0.2;
                    hi[stem] = plane + 0.8;
                    lo[so[a]] = c[so[a]] - r + 0.5;
                    hi[so[a]] = c[so[a]] + r - 0.5;
                    lo[so[1 - a]] = c[so[1 - a]] - 0.4;
                    hi[so[1 - a]] = c[so[1 - a]] + 0.4;
                    b.boxD(lo, hi);
                }
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
            } else if (part instanceof PipeSensorPart ps) {
                // (Baughn 2026-10-27) a stem (steel for pressure, copper for temperature) and a dial whose face goes green
                // to red with the reading's share of full scale
                b.sprite = PIPE[0];
                int[] bx = ps.boxes().get(0);
                int ax = ps.outlet / 2 == 0 ? 1 : ps.outlet / 2 == 1 ? 2 : 0;
                double[] c = {(bx[0] + bx[3]) / 2.0, (bx[1] + bx[4]) / 2.0, (bx[2] + bx[5]) / 2.0};
                int[] o = others(ax);
                double lo = bx[ax], hi = bx[ax + 3];
                boolean up = ps.outlet % 2 == 1;
                if (ps.sensing == PipeSensorPart.PRESSURE) b.color(0.7f, 0.7f, 0.72f);
                else if (ps.sensing == PipeSensorPart.TEMPERATURE) b.color(0.8f, 0.5f, 0.3f);
                else b.color(0.35f, 0.55f, 0.85f);   // (steam quality: blue)
                b.tube(ax, c[o[0]], c[o[1]], 0.6, up ? lo : hi - 2, up ? lo + 2 : hi);
                float f = (float) Math.max(0, Math.min(1, ps.shown));
                b.color(0.2f + 0.7f * f, 0.75f - 0.55f * f, 0.15f);
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
                // (black insulation, Baughn; burnt: scorched brown)
                if (c.burnt != null) b.color(0.35f, 0.22f, 0.14f);
                else b.color(0.22f, 0.22f, 0.24f);
                cable(b, c);
                if (lugs != null && c.face == 0 && !c.isKnuckle())
                    for (CablePart.Port port : c.cablePorts())
                        if (port.face() >= 2 && lugs[port.face() - 2] > 0) lug(b, c, port, lugs[port.face() - 2]);
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
            boolean knuckle = c.isKnuckle();
            double top = c.isKnuckle() || CablePart.plane(c.face) == 0 ? bx[n + 3] : bx[n];
            b.uvs = (q, fa) -> {
                float[] uv = new float[8];
                for (int j = 0; j < 4; j++) {
                    double[] v = q[j];
                    double u, vv = v[along] / 16;
                    double depth = Math.abs(top - v[n]);
                    if (knuckle) u = 0.25 + (v[w] - cw) / 32;   // (a knuckle: just the strip)
                    else if (fa == w) u = 0.25 + Math.signum(v[w] - cw) * (half / 32 + depth / 16);
                    else u = 0.25 + (v[w] - cw) / 32;
                    if (fa == along && !knuckle) vv = 0.5 - depth / 16;
                    uv[2 * j] = (float) clamp01(u);
                    uv[2 * j + 1] = (float) clamp01(vv);
                }
                return uv;
            };
            b.box(bx);
            b.uvs = null;
        }
    }

    /**
     * A cable lug where a floor cable ends at a device's terminal plate (as six-node cables draw them, CableRender.drawKnuckles):
     * the cable turns up against the face, a copper crimp barrel tops it and a copper palm lies on the plate's middle
     * conductor, H px above the floor.
     */
    static void lug(Builder b, CablePart c, CablePart.Port port, float H) {
        int along = port.face() >= 4 ? 0 : 2, across = 2 - along;
        double edge = along == 0 ? port.x() : port.z(), in = edge == 0 ? 1 : -1, mid = across == 0 ? port.x() : port.z();
        double w2 = c.tierInfo().width() / 2.0, h = c.tierInfo().height();
        b.color(0.22f, 0.22f, 0.24f);
        lugBox(b, along, across, 0, H - 0.1, mid, w2 + 0.05, edge + in * 0.25, edge + in * (0.25 + h));
        b.sprite = PIPE[0];
        b.color(0.77f, 0.43f, 0.27f);   // (dull copper: a lug you can touch isn't bare metal, Baughn)
        lugBox(b, along, across, H - 0.1, H + 0.6, mid, w2 + 0.15, edge + in * 0.15, edge + in * (0.35 + h));
        lugBox(b, along, across, H - 0.85, H + 0.85, mid, Math.max(0.75, w2 - 0.25), edge, edge + in * 0.25);
        b.sprite = cableSprite(c.tier);
        b.color(0.22f, 0.22f, 0.24f);
    }

    static void lugBox(Builder b, int along, int across, double y0, double y1, double mid, double half, double a0, double a1) {
        double[] lo = new double[3], hi = new double[3];
        lo[1] = y0;
        hi[1] = y1;
        lo[across] = mid - half;
        hi[across] = mid + half;
        lo[along] = Math.min(a0, a1);
        hi[along] = Math.max(a0, a1);
        b.boxD(lo, hi);
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

        /** A box in fractional voxels (not culled against the block's faces). */
        void boxD(double[] lo, double[] hi) {
            double[][] c = new double[8][];
            for (int i = 0; i < 8; i++) c[i] = new double[]{(i & 1) == 0 ? lo[0] : hi[0], (i & 2) == 0 ? lo[1] : hi[1], (i & 4) == 0 ? lo[2] : hi[2]};
            face(c[0], c[4], c[6], c[2], -1, 0, 0, -1);
            face(c[1], c[3], c[7], c[5], 1, 0, 0, -1);
            face(c[0], c[1], c[5], c[4], 0, -1, 0, -1);
            face(c[2], c[6], c[7], c[3], 0, 1, 0, -1);
            face(c[0], c[2], c[3], c[1], 0, 0, -1, -1);
            face(c[4], c[5], c[7], c[6], 0, 0, 1, -1);
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

        /** A flat ring round axis, centred at (c1, c2), radii rIn..rOut, from a0 to a1, in n segments. */
        void ring(int axis, double c1, double c2, double rIn, double rOut, double a0, double a1, int n) {
            int[] o = others(axis);
            java.util.function.BiFunction<Double, double[], double[]> at = (rr, aa) -> {
                double[] v = new double[3];
                v[o[0]] = c1 + rr * Math.cos(aa[0]);
                v[o[1]] = c2 + rr * Math.sin(aa[0]);
                v[axis] = aa[1];
                return v;
            };
            for (int k = 0; k < n; k++) {
                double t0 = 2 * Math.PI * k / n, t1 = 2 * Math.PI * (k + 1) / n, tm = (t0 + t1) / 2;
                double[] i00 = at.apply(rIn, new double[]{t0, a0}), i10 = at.apply(rIn, new double[]{t1, a0});
                double[] i01 = at.apply(rIn, new double[]{t0, a1}), i11 = at.apply(rIn, new double[]{t1, a1});
                double[] o00 = at.apply(rOut, new double[]{t0, a0}), o10 = at.apply(rOut, new double[]{t1, a0});
                double[] o01 = at.apply(rOut, new double[]{t0, a1}), o11 = at.apply(rOut, new double[]{t1, a1});
                double[] out = new double[3], down = new double[3];
                out[o[0]] = Math.cos(tm);
                out[o[1]] = Math.sin(tm);
                down[axis] = -1;
                float[] uv = {k / (float) n, 0, (k + 1) / (float) n, 0, (k + 1) / (float) n, 1, k / (float) n, 1};
                add(wind(o00, o10, o11, o01, out[0], out[1], out[2]), uv, -1);
                add(wind(i00, i10, i11, i01, -out[0], -out[1], -out[2]), uv, -1);
                add(wind(i00, i10, o10, o00, down[0], down[1], down[2]), uv, -1);
                add(wind(i01, i11, o11, o01, -down[0], -down[1], -down[2]), uv, -1);
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
