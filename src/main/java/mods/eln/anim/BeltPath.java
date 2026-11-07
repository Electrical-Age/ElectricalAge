package mods.eln.anim;

/**
 * An open belt round two pulleys in one plane (Baughn 2026-11-07: belts should wrap their wheels; shared code for every
 * belt drive): the two outer tangent runs and the arcs round each pulley. Units are the caller's (EA models use px in
 * the model's frame). The belt runs clockwise (the pulleys' angle decreasing): along the upper run from pulley A to
 * pulley B (as seen with A to B along +x), round B's far side, back along the lower run and round A's far side.
 * {@link #at} gives the point and heading a distance along it, so links spaced evenly and shifted by the pulleys'
 * turned arc ({@code angle * ra}) move with the drive.
 */
public final class BeltPath {
    public final double ax, ay, ar, bx, by, br;
    /** The tangent angles (radians) of the two runs' ends on both pulleys, the straight runs' length, the whole length. */
    final double t1, t2, run, arcB, arcA;
    public final double length;

    public BeltPath(double ax, double ay, double ar, double bx, double by, double br) {
        this.ax = ax;
        this.ay = ay;
        this.ar = ar;
        this.bx = bx;
        this.by = by;
        this.br = br;
        double dx = bx - ax, dy = by - ay, d = Math.hypot(dx, dy);
        if (d <= Math.abs(ar - br)) throw new IllegalArgumentException("pulleys overlap: " + d + " <= |" + ar + " - " + br + "|");
        double base = Math.atan2(dy, dx), off = Math.acos((ar - br) / d);
        t1 = base + off;
        t2 = base - off;
        run = Math.sqrt(d * d - (ar - br) * (ar - br));
        arcB = br * 2 * off;
        arcA = ar * (2 * Math.PI - 2 * off);
        length = 2 * run + arcB + arcA;
    }

    /** {x, y, heading} at distance s along the belt (any s: it wraps); heading is the direction of travel, radians. */
    public double[] at(double s) {
        s = ((s % length) + length) % length;
        if (s < run) {                                                     // upper run, A to B
            double f = s / run;
            double x0 = ax + ar * Math.cos(t1), y0 = ay + ar * Math.sin(t1), x1 = bx + br * Math.cos(t1), y1 = by + br * Math.sin(t1);
            return new double[]{x0 + f * (x1 - x0), y0 + f * (y1 - y0), Math.atan2(y1 - y0, x1 - x0)};
        }
        s -= run;
        if (s < arcB) {                                                    // round B, clockwise from t1 to t2
            double a = t1 - s / br;
            return new double[]{bx + br * Math.cos(a), by + br * Math.sin(a), a - Math.PI / 2};
        }
        s -= arcB;
        if (s < run) {                                                     // lower run, B to A
            double f = s / run;
            double x0 = bx + br * Math.cos(t2), y0 = by + br * Math.sin(t2), x1 = ax + ar * Math.cos(t2), y1 = ay + ar * Math.sin(t2);
            return new double[]{x0 + f * (x1 - x0), y0 + f * (y1 - y0), Math.atan2(y1 - y0, x1 - x0)};
        }
        s -= run;
        double a = t2 - s / ar;                                            // round A, clockwise from t2 to t1 - 2 pi
        return new double[]{ax + ar * Math.cos(a), ay + ar * Math.sin(a), a - Math.PI / 2};
    }
}
