package mods.eln.sim;

/**
 * Maximum power point trackers for a buck converter's ratio (output over input voltage), fed once per electrical step
 * with the input voltage and current measured at the last solve. A higher ratio pulls the input voltage down (the
 * input sits near the output voltage over the ratio). Neither reads the panel's curve: they find the peak the way real
 * controllers do (Baughn 2026-11-25, board #79).
 * <ul>
 *   <li>{@link #perturbAndObserve()}: every second, a step (2 %, doubling to 8 % while the power keeps rising); keep
 *       the direction while the power rises. It hunts
 *       round the peak and follows changing light slowly (the cheap tier).</li>
 *   <li>{@link #incrementalConductance()}: every third step, compares dI/dV with -I/V (zero at the peak) and steps
 *       in proportion to how far off it is (the good tier).</li>
 * </ul>
 * While a limit of the converter (output voltage, input power) holds the ratio down, the tracker follows the ratio in
 * use (no wind-up), nudging just above it to notice when the limit lifts. With nothing flowing it starts from 80 % of the open input voltage.
 */
public final class MpptTracker {
    private final boolean incremental;
    private final double period;
    private double clock;
    private double vPrev = Double.NaN, iPrev, pPrev = Double.NaN;
    private int direction = -1;   // -1: lower the input voltage (raise the ratio); +1: raise it

    private MpptTracker(boolean incremental, double period) {
        this.incremental = incremental;
        this.period = period;
    }

    public static MpptTracker perturbAndObserve() {
        return new MpptTracker(false, 1.0);
    }

    /** Every third step: the panel and the converter settle on each other over a step or two after a change, and
     *  differences taken sooner read that settling instead of the curve (it hunted on a resistive output). */
    public static MpptTracker incrementalConductance() {
        return new MpptTracker(true, 0.15);
    }


    /**
     * The next ratio. {@code uIn}/{@code iIn}: input voltage and current; {@code uOut}: output voltage; {@code ratio}:
     * the ratio set; {@code ratioUsed}: the ratio the converter used; {@code limited}: the output limit applied;
     * {@code flowing}: power flowed; {@code dt}: time since the last call. {@code limited}: a limit of the converter
     * (output voltage or input power) set the ratio; the tracker then follows the ratio used.
     */
    public double next(double uIn, double iIn, double uOut, double ratio, double ratioUsed, boolean limited,
                       boolean flowing, double dt) {
        double p = uIn * iIn;
        if (!flowing || !(uIn > 0)) {
            this.turned = false;   // (a fresh start: P&O may stride again)
            this.poStep = 0.02;
            this.vPrev = Double.NaN;
            this.pPrev = Double.NaN;
            this.clock = 0;
            return uIn > 0 && uOut > 0 ? clamp(uOut / (0.8 * uIn)) : ratio;
        }
        if (limited) {
            this.poStep = 0.02;   // (no stride when the limit lifts)
            this.vPrev = Double.NaN;
            this.pPrev = Double.NaN;
            return clamp(ratioUsed * 1.005);
        }
        this.clock += dt;
        if (this.clock < this.period) return ratio;
        this.clock = 0;
        double next;
        if (this.incremental) next = incremental(uIn, iIn, ratio);
        else next = observe(p, ratio);
        this.vPrev = uIn;
        this.iPrev = iIn;
        this.pPrev = p;
        return clamp(next);
    }

    private double poStep = 0.02;
    private boolean turned;

    private double observe(double p, double ratio) {
        // a 2 % step, doubled (to 8 %) while the power keeps rising after a start, 2 % from the first turn on: from a far start
        // (the input read before it settled) it gets there in seconds, then hunts in small steps
        if (Double.isFinite(this.pPrev) && p < this.pPrev) {
            this.direction = -this.direction;
            this.poStep = 0.02;
            this.turned = true;
        } else if (Double.isFinite(this.pPrev) && !this.turned) {
            this.poStep = Math.min(0.08, this.poStep * 2);   // (only on the way there: past the first turn it hunts at 2 %)
        }
        if (ratio >= 1.0 && this.direction < 0) {   // at the buck's end stop (input = output): turn back
            this.direction = 1;
            this.poStep = 0.02;
        }
        return ratio * (1 - this.poStep * this.direction);   // direction +1 (raise the voltage): lower ratio
    }

    private double incremental(double v, double i, double ratio) {
        if (!Double.isFinite(this.vPrev)) return ratio * (1 - 0.01 * this.direction);   // a first step to measure from
        double dv = v - this.vPrev, di = i - this.iPrev;
        double g;   // ~ dP/dV / V: positive below the peak (raise the voltage), negative above
        if (Math.abs(dv) < 1e-6 * Math.max(1, v)) {
            if (Math.abs(di) < 1e-9 * Math.max(1, i)) {   // nothing moved: dither, turning back at the end stop
                if (ratio >= 1.0 && this.direction < 0) this.direction = 1;
                return ratio * (1 - 0.001 * this.direction);
            }
            g = di > 0 ? 1e-3 : -1e-3;
        } else {
            g = di / dv + i / v;
        }
        double rel = Math.abs(g) / Math.max(i / v, 1e-12);   // 0 at the peak, ~1 far from it
        double step = Math.min(0.03, Math.max(0.0005, 0.02 * rel));
        this.direction = g > 0 ? 1 : -1;
        // at the end stop and wanting more: the peak is out of reach (the battery above it) or the readings are stale;
        // a dither's step back keeps it measuring without leaving the best reachable point
        if (ratio >= 1.0 && this.direction < 0) return ratio * (1 - 0.001);
        return ratio * (1 - step * this.direction);
    }

    private static double clamp(double r) {
        return Math.min(1.0, Math.max(1e-3, r));
    }
}
