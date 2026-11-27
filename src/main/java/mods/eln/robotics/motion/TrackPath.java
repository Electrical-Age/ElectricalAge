package mods.eln.robotics.motion;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * A track of straight legs along block axes, each corner rounded into a quarter circle (Baughn 2026-11-27, track B:
 * curved drive-owned tracks; arcs rather than Bezier pieces, so lengths and curvature are exact). s is arc length (m)
 * along the carriage's own path (Baughn: integer distances aren't needed on a curved track), so it runs at one speed
 * through the corners. Legs start and end on block-aligned points, so the stops (where a carriage's floor sits on a
 * whole block: stops()) are the whole-block offsets along each leg's straight part. One straight leg is the same path as
 * StraightPath, with a stop every metre.
 */
public final class TrackPath implements Path {
    /** One leg: its direction and length (blocks, corner to corner). */
    public record Leg(Direction dir, int blocks) {
    }

    /** A piece: a straight from a along u for len, or a quarter arc (radius r) turning from u to v. */
    private record Piece(double s0, double len, Vec3 a, Vec3 u, Vec3 v, double r, boolean arc) {
        Vec3 at(double t) {
            if (!arc) return a.add(u.scale(t));
            double phi = t / r;   // (the arc starts at a, its centre is a + v r)
            return a.add(v.scale(r * (1 - Math.cos(phi)))).add(u.scale(r * Math.sin(phi)));
        }

        Vec3 tangent(double t) {
            if (!arc) return u;
            double phi = t / r;
            return u.scale(Math.cos(phi)).add(v.scale(Math.sin(phi)));
        }
    }

    private final Vec3 origin;
    private final List<Leg> legs;
    private final double[] radii;
    private final List<Piece> pieces = new ArrayList<>();
    private final double[] stops;
    private final double length;

    /**
     * From origin (the carriage's reference point at s = 0, block-aligned) along legs; radii[i] rounds the corner between
     * leg i and leg i+1 (each leg must turn a right angle from the last and be long enough for its corners' radii).
     */
    public TrackPath(Vec3 origin, List<Leg> legs, double[] radii) {
        if (legs.isEmpty()) throw new IllegalArgumentException("no legs");
        if (radii.length != legs.size() - 1) throw new IllegalArgumentException("one radius per corner");
        this.origin = origin;
        this.legs = List.copyOf(legs);
        this.radii = radii.clone();
        List<Double> st = new ArrayList<>();
        Vec3 corner = origin;
        double s = 0;
        for (int i = 0; i < legs.size(); i++) {
            Leg g = legs.get(i);
            Vec3 u = vec(g.dir());
            double rin = i == 0 ? 0 : radii[i - 1], rout = i == legs.size() - 1 ? 0 : radii[i];
            if (i > 0 && Math.abs(vec(legs.get(i - 1).dir()).dot(u)) > 1e-9)
                throw new IllegalArgumentException("leg " + i + " doesn't turn a right angle");
            if (rin + rout > g.blocks() + 1e-9 || rin < 0 || rout < 0) throw new IllegalArgumentException("leg " + i + " too short for its corners");
            // the straight part, from rin to blocks - rout along the leg
            double straight = g.blocks() - rin - rout;
            pieces.add(new Piece(s, straight, corner.add(u.scale(rin)), u, Vec3.ZERO, 0, false));
            for (int k = (int) Math.ceil(rin - 1e-9); k <= g.blocks() - rout + 1e-9; k++) {
                double sk = s + (k - rin);
                if (st.isEmpty() || sk > st.get(st.size() - 1) + 1e-9) st.add(sk);
            }
            s += straight;
            Vec3 next = corner.add(u.scale(g.blocks()));
            if (i < legs.size() - 1) {   // the arc round the corner into the next leg
                Vec3 v = vec(legs.get(i + 1).dir());
                double len = rout * Math.PI / 2;
                if (rout > 0) pieces.add(new Piece(s, len, next.subtract(u.scale(rout)), u, v, rout, true));
                s += len;
            }
            corner = next;
        }
        this.length = s;
        this.stops = st.stream().mapToDouble(Double::doubleValue).toArray();
    }

    static Vec3 vec(Direction d) {
        return new Vec3(d.getStepX(), d.getStepY(), d.getStepZ());
    }

    public List<Leg> legs() {
        return legs;
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

    /** The stops (m along the path, ascending): every whole-block place on a leg's straight part, and both ends. */
    public double[] stops() {
        return stops.clone();
    }

    @Override
    public boolean axisAligned() {
        return legs.size() == 1;
    }
}
