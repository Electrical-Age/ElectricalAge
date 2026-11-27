package mods.eln.robotics.motion;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * A track of straight legs between corner points, each corner rounded into a circular arc (Baughn 2026-11-27, track B:
 * curved drive-owned tracks, laid with a track kit by clicking the corners; arcs rather than Bezier pieces, so lengths
 * and curvature are exact). s is arc length (m) along the carriage's own path (Baughn: integer distances aren't needed on
 * a curved track), so it runs at one speed through the corners. Stops are locked to the block grid (Baughn
 * 1575949076032266462): the kit's points snap to block centres, and a stop is either end or a point the track runs
 * straight through (a corner that doesn't turn); a round cabin's footprint is an axis-aligned square at any heading, so
 * a car stopped there covers whole blocks and the floor ghost works. One leg is the same path as StraightPath, with a
 * stop every whole metre (elevators and level runs, as before).
 */
public final class TrackPath implements Path {
    /** One leg along a block axis: its direction and length (blocks, corner to corner). */
    public record Leg(Direction dir, int blocks) {
    }

    /** A piece: a straight from a along u for len, or an arc (radius r) from a, starting along u and bending towards n. */
    private record Piece(double s0, double len, Vec3 a, Vec3 u, Vec3 n, double r, boolean arc) {
        Vec3 at(double t) {
            if (!arc) return a.add(u.scale(t));
            double phi = t / r;   // (the arc's centre is a + n r)
            return a.add(n.scale(r * (1 - Math.cos(phi)))).add(u.scale(r * Math.sin(phi)));
        }

        Vec3 tangent(double t) {
            if (!arc) return u;
            double phi = t / r;
            return u.scale(Math.cos(phi)).add(n.scale(Math.sin(phi)));
        }
    }

    private final List<Vec3> corners;
    private final double[] radii;
    private final List<Piece> pieces = new ArrayList<>();
    private final double[] stops;
    private final double length;

    /** From origin along legs on block axes; radii[i] rounds the corner between leg i and leg i+1. */
    public TrackPath(Vec3 origin, List<Leg> legs, double[] radii) {
        this(cornersOf(origin, legs), radii);
    }

    static List<Vec3> cornersOf(Vec3 origin, List<Leg> legs) {
        List<Vec3> c = new ArrayList<>();
        c.add(origin);
        Vec3 p = origin;
        for (Leg g : legs) c.add(p = p.add(vec(g.dir()).scale(g.blocks())));
        return c;
    }

    /**
     * Through corner points c0..cn (c0 the carriage's reference point at s = 0); radii[i] rounds the corner at c(i+1).
     * Each leg must be long enough for its corners' arcs (an arc turning by angle th takes r tan(th/2) off either leg);
     * a straight-through corner (no turn) takes none, and a turn back on itself is refused.
     */
    public TrackPath(List<Vec3> corners, double[] radii) {
        int legs = corners.size() - 1;
        if (legs < 1) throw new IllegalArgumentException("no legs");
        if (radii.length != legs - 1) throw new IllegalArgumentException("one radius per corner");
        this.corners = List.copyOf(corners);
        this.radii = radii.clone();
        double[] cut = new double[legs + 1];   // (how much of a leg the arc at its end corner takes; cut[0], cut[legs] 0)
        Vec3[] dirs = new Vec3[legs];
        double[] lens = new double[legs];
        for (int i = 0; i < legs; i++) {
            Vec3 d = corners.get(i + 1).subtract(corners.get(i));
            lens[i] = d.length();
            if (lens[i] < 1e-9) throw new IllegalArgumentException("leg " + i + " has no length");
            dirs[i] = d.scale(1 / lens[i]);
        }
        for (int i = 0; i + 1 < legs; i++) {
            if (radii[i] < 0) throw new IllegalArgumentException("negative radius");
            double c = Math.max(-1, Math.min(1, dirs[i].dot(dirs[i + 1])));
            if (c < -1 + 1e-9) throw new IllegalArgumentException("corner " + i + " turns back on itself");
            cut[i + 1] = radii[i] * Math.tan(Math.acos(c) / 2);
        }
        List<Double> st = new ArrayList<>();
        double s = 0;
        for (int i = 0; i < legs; i++) {
            double rin = i == 0 ? 0 : cut[i], rout = cut[i + 1];
            if (rin + rout > lens[i] + 1e-9) throw new IllegalArgumentException("leg " + i + " too short for its corners");
            double straight = Math.max(0, lens[i] - rin - rout);
            pieces.add(new Piece(s, straight, corners.get(i).add(dirs[i].scale(rin)), dirs[i], Vec3.ZERO, 0, false));
            if (legs == 1) for (int k = 0; k <= lens[0] + 1e-9; k++) st.add((double) k);   // (one leg: every whole metre)
            else if (i == 0) st.add(0.0);
            if (legs > 1 && rout <= 1e-12) st.add(s + straight);   // (a corner that doesn't turn, or the far end)
            else if (legs == 1 && Math.abs(lens[0] - Math.rint(lens[0])) > 1e-9) st.add(lens[0]);
            s += straight;
            if (i + 1 < legs && rout > 1e-12) {   // the arc round corner i+1
                Vec3 u = dirs[i], v = dirs[i + 1];
                double th = Math.acos(Math.max(-1, Math.min(1, u.dot(v))));
                Vec3 n = v.subtract(u.scale(u.dot(v))).normalize();
                double len = radii[i] * th;
                pieces.add(new Piece(s, len, corners.get(i + 1).subtract(u.scale(rout)), u, n, radii[i], true));
                s += len;
            }
        }
        this.length = s;
        this.stops = st.stream().mapToDouble(Double::doubleValue).toArray();
    }

    static Vec3 vec(Direction d) {
        return new Vec3(d.getStepX(), d.getStepY(), d.getStepZ());
    }

    /** Its corner points (c0 = where s = 0). */
    public List<Vec3> corners() {
        return corners;
    }

    public double radius(int corner) {
        return radii[corner];
    }

    @Override
    public double length() {
        return length;
    }

    private Piece piece(double s) {
        int lo = 0, hi = pieces.size() - 1;
        while (lo < hi) {   // (the last piece starting at or before s)
            int mid = (lo + hi + 1) >>> 1;
            if (pieces.get(mid).s0() <= s) lo = mid;
            else hi = mid - 1;
        }
        return pieces.get(lo);
    }

    /** Off either end it goes on straight along the end legs (so a carriage overrunning a little has a place). */
    @Override
    public Vec3 at(double s) {
        Piece p = piece(s);
        if (p.arc() && s > p.s0() + p.len()) p = piece(p.s0() + p.len() + 1e-12);
        double t = s - p.s0();
        if (p.arc() && (t < 0 || t > p.len())) t = Math.max(0, Math.min(p.len(), t));
        return p.at(t);
    }

    @Override
    public Vec3 tangent(double s) {
        Piece p = piece(s);
        return p.tangent(Math.max(0, Math.min(p.len(), s - p.s0())));
    }

    /** The radius of the tightest corner between s0 and s1 (m; infinite on straights). */
    public double minRadius(double s0, double s1) {
        double lo = Math.min(s0, s1), hi = Math.max(s0, s1), r = Double.POSITIVE_INFINITY;
        for (Piece p : pieces)
            if (p.arc() && p.s0() < hi && p.s0() + p.len() > lo) r = Math.min(r, p.r());
        return r;
    }

    /** The stops (m along the path, ascending): both ends and every point it runs straight through; one leg: each metre. */
    public double[] stops() {
        return stops.clone();
    }

    /** One straight run along a block axis (stops on it included): a walled cabin may ride it. */
    @Override
    public boolean axisAligned() {
        Vec3 d = corners.get(corners.size() - 1).subtract(corners.get(0));
        int axes = (Math.abs(d.x) > 1e-9 ? 1 : 0) + (Math.abs(d.y) > 1e-9 ? 1 : 0) + (Math.abs(d.z) > 1e-9 ? 1 : 0);
        if (axes != 1) return false;
        Vec3 u = d.normalize();
        for (Vec3 c : corners) {
            Vec3 off = c.subtract(corners.get(0));
            if (off.subtract(u.scale(off.dot(u))).lengthSqr() > 1e-12) return false;
        }
        return true;
    }
}
