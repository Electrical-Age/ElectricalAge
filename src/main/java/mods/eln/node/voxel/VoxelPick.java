package mods.eln.node.voxel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * What the crosshair is on in a voxel block: the one pick for Jade, the outline, breaking and its tool, clicks and
 * middle-click, on both sides (Baughn 2026-11-28: each had its own, Jade named one cable while the outline showed
 * another). Pure: boxes in, a slot or part index out.
 * <p>
 * It follows the line of sight, not the point the crosshair's ray stopped at: that point is on the block's outline,
 * which stands 2 voxels proud of every part, so seen at an angle it is on one cable's outline while the crosshair is
 * drawn over the next cable. From the hit point on, through the block (up to the imitated block, which hides what is
 * behind it):
 * <ol>
 * <li>the box the line enters first: a pipe sensor device's aim boxes, a part's own boxes, a device's boxes. Entered
 * together: a sensor device (over the pipe at its port), a fitting (over the pipe it sits on), a pipe or cable, a
 * junction, a device (a model box can reach across a cable);</li>
 * <li>if it enters none (it only passed through the outline's 2 extra voxels): the part or device whose own box is
 * nearest the line, of those whose outline it passed through.</li>
 * </ol>
 * A junction stands for its arm nearest the place the line met it. Ties go by rank, then by place (the lower first
 * box, the lower slot), never by the order in the block's list: the server and the client then agree, and the client
 * is not sent part ids. With no line (a point worked out by code, not seen by a player) the same rules for the point.
 */
public final class VoxelPick {
    private VoxelPick() {
    }

    /** How near a box's face counts as in it (voxels): a hit on the face, rounded. */
    static final double ON = 0.05;
    /** How far from a box its outline reaches (voxels): VoxelNodeBlock.outlineOf grows it by 2. */
    static final double REACH = 2 + ON;
    private static final double TIE = 1e-7;

    /**
     * A block's contents as the pick sees them (voxels): the parts; per slot the device's boxes (none for an empty
     * slot) and, for a pipe sensor device, where the crosshair means it rather than its pipe (none for other devices);
     * what is left of the imitated block.
     */
    public record Scene(List<VoxelPart> parts, List<List<int[]>> devices, List<List<int[]>> sensors, List<int[]> host) {
        /** The parts alone. */
        public static Scene of(List<VoxelPart> parts) {
            return new Scene(parts, List.of(), List.of(), List.of());
        }
    }

    public static final VoxelNode.Aimed NONE = new VoxelNode.Aimed(-1, -1);

    /** pick for a viewer (a player, the camera's entity) and the place its crosshair's ray stopped, in the world. */
    public static VoxelNode.Aimed pick(Scene s, BlockPos pos, net.minecraft.world.entity.Entity viewer, Vec3 hit) {
        Vec3 o = Vec3.atLowerCornerOf(pos);
        return pick(s, viewer == null ? null : viewer.getEyePosition().subtract(o), viewer == null ? null : viewer.getLookAngle(),
            hit == null ? null : hit.subtract(o));
    }

    /** pick for a block-local point (0..1) alone: a point worked out by code, with no line of sight. */
    public static VoxelNode.Aimed at(Scene s, double x, double y, double z) {
        return pick(s, (Vec3) null, null, new Vec3(x, y, z));
    }

    /**
     * What is aimed at: block-local, in blocks. eye and look: the viewer's eye and the way it looks; hit: where its ray
     * stopped on the block. Any may be null. With an eye and a hit the line runs from the eye through the hit (a
     * server's idea of the look lags the client's; the hit is the client's own), else along look; it starts at the hit,
     * or without one where it enters the block. With no line: the point hit. Nothing there: both -1.
     */
    public static VoxelNode.Aimed pick(Scene s, Vec3 eye, Vec3 look, Vec3 hit) {
        Vec3 from = hit != null ? hit : eye;
        if (from == null) return NONE;
        Vec3 dir = null;
        if (eye != null && hit != null && eye.distanceToSqr(hit) > 1e-8) dir = hit.subtract(eye).normalize();
        else if (look != null && look.lengthSqr() > 1e-12) dir = look.normalize();
        double[] a = {from.x * 16, from.y * 16, from.z * 16}, b = a.clone();
        if (dir != null) {
            // (to where it leaves the block: at most its diagonal away, or from an eye outside as far as its far side)
            double far = hit != null ? 28 : Math.sqrt(sq(a[0] - 8) + sq(a[1] - 8) + sq(a[2] - 8)) + 14;
            b = new double[]{a[0] + dir.x * far, a[1] + dir.y * far, a[2] + dir.z * far};
            int[] block = {0, 0, 0, 16, 16, 16};
            double[] in = span(a, b, block, ON);
            if (in == null) {
                if (hit == null) return NONE;
                b = a.clone();   // (a hit outside the block: the point)
            } else {
                double[] a0 = a, b0 = b;
                a = along(a0, b0, in[0]);
                b = along(a0, b0, in[1]);
            }
            // the imitated block hides what is behind it
            double cut = 1;
            for (int[] h : s.host()) {
                double[] t = span(a, b, h, 0);
                if (t != null) cut = Math.min(cut, t[0]);
            }
            if (cut < 1) b = along(a, b, cut);
        }
        return pick(s, a, b, true);
    }

    private static final int DEVICE = 4;

    /** A part's rank where two are entered together or are equally near: a fitting (a valve, a sensor: its box takes
     *  in the pipe it sits on) before a pipe or cable, a junction (its box takes in its arms' ends) after them. */
    private static int rank(VoxelPart p) {
        return p instanceof JunctionPart ? 3 : p instanceof PipePart || p instanceof CablePart ? 2 : 1;
    }

    private static VoxelNode.Aimed pick(Scene s, double[] a, double[] b, boolean junctions) {
        List<VoxelPart> parts = s.parts();
        Best best = new Best();
        for (int f = 0; f < s.sensors().size(); f++)
            for (int[] box : s.sensors().get(f)) best.enter(a, b, box, 0, new int[]{f}, f, -1);
        for (int i = 0; i < parts.size(); i++) {
            if (!junctions && parts.get(i) instanceof JunctionPart) continue;
            List<int[]> boxes = parts.get(i).boxes();
            for (int[] box : boxes) best.enter(a, b, box, rank(parts.get(i)), boxes.get(0), -1, i);
        }
        for (int f = 0; f < s.devices().size(); f++)
            for (int[] box : s.devices().get(f)) best.enter(a, b, box, DEVICE, new int[]{f}, f, -1);
        if (!best.found()) {
            for (int i = 0; i < parts.size(); i++) {
                if (!junctions && parts.get(i) instanceof JunctionPart) continue;
                List<int[]> boxes = parts.get(i).boxes();
                for (int[] box : boxes) best.near(a, b, box, rank(parts.get(i)), boxes.get(0), -1, i);
            }
            for (int f = 0; f < s.devices().size(); f++)
                for (int[] box : s.devices().get(f)) best.near(a, b, box, DEVICE, new int[]{f}, f, -1);
        }
        if (!best.found()) return NONE;
        if (best.part >= 0 && parts.get(best.part) instanceof JunctionPart j) {
            // (a junction is never aimed at on its own: its arm is; it goes when its arms do)
            double[] p = along(a, b, best.at);
            int arm = armNear(parts, j, p[0], p[1], p[2]);
            return arm >= 0 ? new VoxelNode.Aimed(-1, arm) : pick(s, a, b, false);
        }
        return new VoxelNode.Aimed(best.element, best.part);
    }

    /** The best candidate so far: the least value (where the line enters it, or how far it is), then rank, then key. */
    private static final class Best {
        double value = Double.MAX_VALUE, at;
        int rank, element = -1, part = -1;
        int[] key;

        boolean found() {
            return key != null;
        }

        /** A box the line a-b enters: by where along the line. */
        void enter(double[] a, double[] b, int[] box, int rank, int[] key, int element, int part) {
            double[] t = span(a, b, box, ON);
            if (t != null) offer(t[0], t[0], rank, key, element, part);
        }

        /** A box whose outline the line a-b passes through: by its own distance from the line. */
        void near(double[] a, double[] b, int[] box, int rank, int[] key, int element, int part) {
            if (span(a, b, box, REACH) == null) return;
            double[] d = distance(a, b, box);
            offer(d[0], d[1], rank, key, element, part);
        }

        private void offer(double v, double at, int rank, int[] key, int element, int part) {
            if (found() && v > value + TIE) return;
            if (found() && v >= value - TIE) {
                if (rank > this.rank) return;
                if (rank == this.rank && java.util.Arrays.compare(key, this.key) >= 0) return;
            }
            value = Math.min(v, found() ? value : v);
            this.at = at;
            this.rank = rank;
            this.key = key;
            this.element = element;
            this.part = part;
        }
    }

    /**
     * The pipe ending at junction j whose run leaves nearest the point (block-local voxels: nearest by the pipe's point
     * next to that end), or -1. Equally near: the lower first box.
     */
    static int armNear(List<VoxelPart> parts, JunctionPart j, double x, double y, double z) {
        int best = -1;
        double bd = Double.MAX_VALUE;
        for (int i = 0; i < parts.size(); i++)
            if (parts.get(i) instanceof PipePart pipe && VoxelNode.endsAt(pipe, j.x, j.y, j.z)) {
                int[] pts = pipe.points;
                int k = pts[0] == j.x && pts[1] == j.y && pts[2] == j.z ? 3 : pts.length - 6;   // (the point next to its end)
                double d = sq(pts[k] - x) + sq(pts[k + 1] - y) + sq(pts[k + 2] - z);
                if (d < bd - TIE || d <= bd + TIE && best >= 0
                    && java.util.Arrays.compare(pipe.boxes().get(0), parts.get(best).boxes().get(0)) < 0) {
                    bd = Math.min(d, bd);
                    best = i;
                }
            }
        return best;
    }

    private static double sq(double v) {
        return v * v;
    }

    private static double[] along(double[] a, double[] b, double t) {
        return new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    /** The part of the line a-b (0 at a, 1 at b) inside box grown by grow, as {enters, leaves}; null if none. */
    static double[] span(double[] a, double[] b, int[] box, double grow) {
        double t0 = 0, t1 = 1;
        for (int i = 0; i < 3; i++) {
            double lo = box[i] - grow, hi = box[i + 3] + grow, d = b[i] - a[i];
            if (Math.abs(d) < 1e-12) {
                if (a[i] < lo || a[i] > hi) return null;
                continue;
            }
            double u = (lo - a[i]) / d, v = (hi - a[i]) / d;
            t0 = Math.max(t0, Math.min(u, v));
            t1 = Math.min(t1, Math.max(u, v));
            if (t0 > t1) return null;
        }
        return new double[]{t0, t1};
    }

    /**
     * How far the line a-b passes from box, as {distance, where along the line (0 at a, 1 at b)}. Exact: between the
     * places where the line crosses one of the box's planes the squared distance is a quadratic.
     */
    static double[] distance(double[] a, double[] b, int[] box) {
        double[] cuts = new double[8];
        int n = 0;
        cuts[n++] = 0;
        cuts[n++] = 1;
        for (int i = 0; i < 3; i++) {
            double d = b[i] - a[i];
            if (Math.abs(d) < 1e-12) continue;
            for (int k = 0; k < 2; k++) {
                double t = (box[i + 3 * k] - a[i]) / d;
                if (t > 0 && t < 1) cuts[n++] = t;
            }
        }
        java.util.Arrays.sort(cuts, 0, n);
        double best = Double.MAX_VALUE, at = 0;
        for (int c = 0; c + 1 < n; c++) {
            double from = cuts[c], to = cuts[c + 1], mid = (from + to) / 2;
            // outside the box along some axes over this stretch: the distance to those faces
            double qa = 0, qb = 0;
            for (int i = 0; i < 3; i++) {
                double d = b[i] - a[i], m = a[i] + d * mid;
                double face = m < box[i] ? box[i] : m > box[i + 3] ? box[i + 3] : Double.NaN;
                if (Double.isNaN(face)) continue;
                qa += d * d;
                qb += d * (a[i] - face);
            }
            double t = qa < 1e-18 ? from : Math.max(from, Math.min(to, -qb / qa));
            double v = 0;
            for (int i = 0; i < 3; i++) {
                double x = a[i] + (b[i] - a[i]) * t;
                v += sq(x < box[i] ? box[i] - x : x > box[i + 3] ? x - box[i + 3] : 0);
            }
            if (v < best) {
                best = v;
                at = t;
            }
        }
        return new double[]{Math.sqrt(best), at};
    }
}
