package mods.eln.robotics.motion;

/**
 * One move of a servo axis: from a position and velocity at a start tick toward a target, with a speed and an
 * acceleration limit (a trapezoidal profile, or a triangle when the move is too short to reach full speed; a start
 * velocity against the target first brakes to rest). Positions are metres along the axis, time in ticks. A pure
 * function of time, so server and clients evaluate the same motion from the same few numbers (no position traffic).
 */
public record Segment(long startTick, double from, double v0, double to, double vmax, double accel) {
    public Segment {
        if (!(vmax > 0) || !(accel > 0)) throw new IllegalArgumentException("vmax and accel must be positive");
    }

    /** At rest at p from tick t. */
    public static Segment rest(long t, double p) {
        return new Segment(t, p, 0, p, 1, 1);
    }

    /** Position after dt ticks (dt may be fractional: a render frame). */
    public double position(double dt) {
        return state(dt)[0];
    }

    /** Velocity (m/tick) after dt ticks. */
    public double velocity(double dt) {
        return state(dt)[1];
    }

    /** Has it arrived (at rest on the target) after dt ticks? */
    public boolean arrived(double dt) {
        return dt >= duration();
    }

    /** Ticks from the start until it is at rest on the target. */
    public double duration() {
        return plan().total;
    }

    /** The phases: an optional brake to rest (against the target), then accelerate, cruise, decelerate. */
    private record Plan(double brakeT, double brakeEnd, double dir, double v1, double accT, double cruiseT, double decT,
                        double total, double origin) {
    }

    private Plan plan() {
        double a = accel, p = from, v = v0;
        double brakeT = 0;
        double d = to - p;
        double dir = Math.signum(d);
        // moving away from the target (or too fast to stop before it): brake to rest first
        double stopDist = v * v / (2 * a);
        if (v != 0 && (Math.signum(v) != dir || Math.abs(d) < stopDist - 1e-12)) {
            brakeT = Math.abs(v) / a;
            p = p + Math.signum(v) * stopDist;
            v = 0;
            d = to - p;
            dir = Math.signum(d);
        }
        double dist = Math.abs(d);
        if (dist < 1e-12) return new Plan(brakeT, p, 0, 0, 0, 0, 0, brakeT, p);
        double u = Math.abs(v);   // speed toward the target at the start of the move
        // change speed from u to v1 (up, or down when the limit fell: a sagging supply), cruise, then brake to rest
        double v1 = vmax;
        double dAdj = Math.abs(v1 * v1 - u * u) / (2 * a), dDec = v1 * v1 / (2 * a);
        double accT, cruiseT, decT;
        if (dAdj + dDec <= dist) {
            accT = Math.abs(v1 - u) / a;
            decT = v1 / a;
            cruiseT = (dist - dAdj - dDec) / v1;
        } else {   // too short to reach the limit (only when speeding up): a triangle
            v1 = Math.sqrt((2 * a * dist + u * u) / 2);
            accT = Math.max(0, (v1 - u) / a);
            decT = v1 / a;
            cruiseT = 0;
        }
        return new Plan(brakeT, p, dir, v1, accT, cruiseT, decT, brakeT + accT + cruiseT + decT, p);
    }

    private double[] state(double dt) {
        if (dt <= 0) return new double[]{from, v0};
        Plan pl = plan();
        double a = accel;
        if (dt < pl.brakeT) {   // braking from v0
            double s = Math.signum(v0);
            double v = v0 - s * a * dt;
            return new double[]{from + v0 * dt - s * a * dt * dt / 2, v};
        }
        double t = dt - pl.brakeT;
        double p = pl.origin, dir = pl.dir;
        if (dir == 0) return new double[]{p, 0};
        double u = pl.brakeT > 0 ? 0 : Math.abs(v0);
        double sa = pl.v1 >= u ? a : -a;   // speeding up, or slowing to a lower limit
        if (t < pl.accT) return new double[]{p + dir * (u * t + sa * t * t / 2), dir * (u + sa * t)};
        p += dir * (u * pl.accT + sa * pl.accT * pl.accT / 2);
        t -= pl.accT;
        if (t < pl.cruiseT) return new double[]{p + dir * pl.v1 * t, dir * pl.v1};
        p += dir * pl.v1 * pl.cruiseT;
        t -= pl.cruiseT;
        if (t < pl.decT) return new double[]{p + dir * (pl.v1 * t - a * t * t / 2), dir * (pl.v1 - a * t)};
        return new double[]{to, 0};
    }

    /** A new move from where this one is at tick t (same limits unless given), toward a new target. */
    public Segment retarget(long t, double target, double vmax, double accel) {
        double dt = t - startTick;
        double[] s = state(dt);
        return new Segment(t, s[0], s[1], target, vmax, accel);
    }
}
