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

    /**
     * A pick and where the line of sight met what it picked (block-local, blocks): where it entered its box, or came
     * nearest it; with no line (or nothing picked), the point hit. Placement takes the place on the part from this, not
     * from the hit: the hit is on the outline of the first thing the ray touched, which may be another part's, far along
     * (a pipe crossing over a floor cable seen low: the pick is the cable, the hit on the pipe's outline).
     */
    public record Met(VoxelNode.Aimed what, double x, double y, double z) {
    }

    /** How far an eye may be from the hit and still be believed (blocks): well past any reach. */
    static final double EYE_REACH = 8;

    /**
     * pick in the world: eye, the viewer's eye as it was when its crosshair's ray was traced (a client's: at that
     * frame's partial tick, or the line swings while the player moves), null for none; hit, where the ray stopped.
     */
    public static VoxelNode.Aimed pick(Scene s, BlockPos pos, Vec3 eye, Vec3 hit) {
        return meet(s, pos, eye, hit).what();
    }

    /** pick in the world, with where the line met what it picked (Met; block-local). */
    public static Met meet(Scene s, BlockPos pos, Vec3 eye, Vec3 hit) {
        Vec3 o = Vec3.atLowerCornerOf(pos);
        double[][] l = line(s, eye == null ? null : eye.subtract(o), null, hit == null ? null : hit.subtract(o));
        return l == null ? new Met(NONE, 0, 0, 0) : pick(s, l[0], l[1], true, true);
    }

    /** pick for a block-local point (0..1) alone: a point worked out by code, with no line of sight. */
    public static VoxelNode.Aimed at(Scene s, double x, double y, double z) {
        return pick(s, (Vec3) null, null, new Vec3(x, y, z));
    }

    /** The part or device whose own box holds the block-local point (0..1): at without its second pass, so nothing
     *  nearby counts. */
    public static VoxelNode.Aimed holding(Scene s, double x, double y, double z) {
        double[] a = {x * 16, y * 16, z * 16};
        return pick(s, a, a, true, false).what();
    }

    /**
     * What is aimed at: block-local, in blocks. eye and look: the viewer's eye and the way it looks; hit: where its ray
     * stopped on the block. Any may be null. With an eye and a hit the line runs from the eye through the hit (a
     * server's idea of the look lags the client's; the hit is the client's own), else along look; it starts at the hit,
     * or without one where it enters the block. With no line: the point hit. Nothing there: both -1.
     */
    public static VoxelNode.Aimed pick(Scene s, Vec3 eye, Vec3 look, Vec3 hit) {
        double[][] l = line(s, eye, look, hit);
        return l == null ? NONE : pick(s, l[0], l[1], true, true).what();
    }

    /**
     * Everything in the block that can be aimed at, in the order the line of sight favours them: what pick gives, then
     * the other parts and devices by how far their own boxes are from the line (Jade sends the nearest when a block
     * holds more than it sends). World coordinates, as pick's.
     */
    public static List<VoxelNode.Aimed> ranked(Scene s, BlockPos pos, Vec3 eye, Vec3 hit) {
        Vec3 o = Vec3.atLowerCornerOf(pos);
        double[][] l = line(s, eye == null ? null : eye.subtract(o), null, hit == null ? null : hit.subtract(o));
        VoxelNode.Aimed first = l == null ? NONE : pick(s, l[0], l[1], true, true).what();
        record Far(VoxelNode.Aimed what, double d, int[] key) {
        }
        List<Far> rest = new java.util.ArrayList<>();
        for (int i = 0; i < s.parts().size(); i++) {
            if (s.parts().get(i) instanceof JunctionPart || i == first.part()) continue;
            List<int[]> boxes = s.parts().get(i).boxes();
            rest.add(new Far(new VoxelNode.Aimed(-1, i), far(l, boxes), boxes.get(0)));
        }
        for (int f = 0; f < s.devices().size(); f++)
            if (!s.devices().get(f).isEmpty() && f != first.element()) rest.add(new Far(new VoxelNode.Aimed(f, -1), far(l, s.devices().get(f)), new int[]{f}));
        rest.sort(java.util.Comparator.comparingDouble(Far::d).thenComparing(Far::key, java.util.Arrays::compare));
        List<VoxelNode.Aimed> out = new java.util.ArrayList<>();
        if (first.element() >= 0 || first.part() >= 0) out.add(first);
        for (Far f : rest) out.add(f.what());
        return out;
    }

    private static double far(double[][] l, List<int[]> boxes) {
        double d = l == null ? 0 : Double.MAX_VALUE;
        if (l != null) for (int[] box : boxes) d = Math.min(d, distance(l[0], l[1], box)[0]);
        return d;
    }

    /** The line of sight through the block as {from, to} (voxels; one point twice with no line), or null with neither
     *  an eye nor a hit, or for an eye whose look misses the block. */
    private static double[][] line(Scene s, Vec3 eye, Vec3 look, Vec3 hit) {
        Vec3 from = hit != null ? hit : eye;
        if (from == null) return null;
        Vec3 dir = null;
        if (eye != null && hit != null && eye.distanceToSqr(hit) > 1e-8) {
            if (believable(s, eye, hit)) dir = hit.subtract(eye).normalize();
        } else if (look != null && look.lengthSqr() > 1e-12) dir = look.normalize();
        double[] a = {from.x * 16, from.y * 16, from.z * 16}, b = a.clone();
        if (dir == null) return new double[][]{a, b};
        // (to where it leaves the block: at most its diagonal away, or from an eye outside as far as its far side)
        double far = hit != null ? 28 : Math.sqrt(sq(a[0] - 8) + sq(a[1] - 8) + sq(a[2] - 8)) + 14;
        b = new double[]{a[0] + dir.x * far, a[1] + dir.y * far, a[2] + dir.z * far};
        int[] block = {0, 0, 0, 16, 16, 16};
        double[] in = span(a, b, block, ON);
        if (in == null) {
            if (hit == null) return null;
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
        return new double[][]{a, b};
    }

    /**
     * Could a ray from eye have stopped at hit? Not from farther than EYE_REACH, and not through one of the block's own
     * boxes on the way (the hit is where the ray first touched the outline, and every box is inside the outline: an eye
     * behind or inside what was hit is not where the crosshair was traced from). Else the eye is not believed and the
     * pick is by the point (another mod's fake player, a stale eye, a rider whose place on the server lags).
     */
    private static boolean believable(Scene s, Vec3 eye, Vec3 hit) {
        double d = eye.distanceTo(hit);
        if (d > EYE_REACH) return false;
        double[] a = {eye.x * 16, eye.y * 16, eye.z * 16}, b = new double[3];
        Vec3 before = hit.subtract(hit.subtract(eye).scale(Math.min(1, 0.2 / 16 / d)));   // (0.2 voxels short of the hit)
        b[0] = before.x * 16;
        b[1] = before.y * 16;
        b[2] = before.z * 16;
        for (VoxelPart p : s.parts()) for (int[] box : p.boxes()) if (span(a, b, box, 0) != null) return false;
        for (List<int[]> boxes : s.devices()) for (int[] box : boxes) if (span(a, b, box, 0) != null) return false;
        for (int[] box : s.host()) if (span(a, b, box, 0) != null) return false;
        return true;
    }

    private static final int DEVICE = 4;

    /** A part's rank where two are entered together or are equally near: a fitting (a valve, a sensor: its box takes
     *  in the pipe it sits on) before a pipe or cable, a junction (its box takes in its arms' ends) after them. */
    private static int rank(VoxelPart p) {
        return p instanceof JunctionPart ? 3 : p instanceof PipePart || p instanceof CablePart ? 2 : 1;
    }

    /** near: with the second pass (the nearest of those whose outline the line passes through). */
    private static Met pick(Scene s, double[] a, double[] b, boolean junctions, boolean near) {
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
        if (!best.found() && near) {
            for (int i = 0; i < parts.size(); i++) {
                if (!junctions && parts.get(i) instanceof JunctionPart) continue;
                List<int[]> boxes = parts.get(i).boxes();
                for (int[] box : boxes) best.near(a, b, box, rank(parts.get(i)), boxes.get(0), -1, i);
            }
            for (int f = 0; f < s.devices().size(); f++)
                for (int[] box : s.devices().get(f)) best.near(a, b, box, DEVICE, new int[]{f}, f, -1);
        }
        if (!best.found()) return new Met(NONE, a[0] / 16, a[1] / 16, a[2] / 16);
        double[] p = along(a, b, best.at);
        if (best.part >= 0 && parts.get(best.part) instanceof JunctionPart j) {
            // (a junction is never aimed at on its own: its arm is; it goes when its arms do)
            int arm = armNear(parts, j, p[0], p[1], p[2]);
            return arm >= 0 ? new Met(new VoxelNode.Aimed(-1, arm), p[0] / 16, p[1] / 16, p[2] / 16) : pick(s, a, b, false, near);
        }
        return new Met(new VoxelNode.Aimed(best.element, best.part), p[0] / 16, p[1] / 16, p[2] / 16);
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
     * The pipe ending at junction j that leaves it nearest the point (block-local voxels): by its first piece, from the
     * junction to the pipe's next point (not by that point alone: a long straight arm lost to a riser that bends just
     * over the junction), or -1. Equally near: the lower first box.
     */
    static int armNear(List<VoxelPart> parts, JunctionPart j, double x, double y, double z) {
        int best = -1;
        double bd = Double.MAX_VALUE;
        double[] p = {x, y, z}, c = {j.x, j.y, j.z};
        for (int i = 0; i < parts.size(); i++)
            if (parts.get(i) instanceof PipePart pipe && VoxelNode.endsAt(pipe, j.x, j.y, j.z)) {
                int[] pts = pipe.points;
                int k = pts[0] == j.x && pts[1] == j.y && pts[2] == j.z ? 3 : pts.length - 6;   // (the point next to its end)
                double[] v = {pts[k], pts[k + 1], pts[k + 2]};
                double len = 0, dot = 0;
                for (int n = 0; n < 3; n++) {
                    len += sq(v[n] - c[n]);
                    dot += (p[n] - c[n]) * (v[n] - c[n]);
                }
                double t = len < 1e-12 ? 0 : Math.max(0, Math.min(1, dot / len)), d = 0;
                for (int n = 0; n < 3; n++) d += sq(c[n] + (v[n] - c[n]) * t - p[n]);
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
