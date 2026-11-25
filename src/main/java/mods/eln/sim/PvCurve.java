package mods.eln.sim;

/**
 * A solar panel's current-voltage curve: the single-diode model without series or shunt resistance (Baughn
 * 2026-11-25, option B+ of board #79):
 * <pre>
 *     I(U) = Iph - I0 (exp(U / a) - 1)        Iph = Isc(T) x light,   a = Voc / 22 at the reference temperature
 * </pre>
 * Calibrated so that the open-circuit voltage of a {@link #VOC_RATED_C} panel in full light is {@code ratedU} (the old
 * constant-power panels' Umax: strings built for it stay under their cables' limits, Baughn 2026-11-25: "B, at 10 C;
 * we've got some plants in the cold"), and the maximum power at the reference temperature ({@link #REF_C}, a panel on a
 * hot noon) is {@code ratedP}. The best voltage there is then about 0.77 x ratedU.
 * <p>
 * Temperature (silicon): Voc falls {@link #VOC_PER_K} per kelvin and Isc rises {@link #ISC_PER_K}; the diode's
 * thermal voltage scales with absolute temperature. Below 0 V a bypass diode carries the extra current of a series
 * string (about {@link #BYPASS_OHMS} ohm), so the curve stays monotone everywhere: one operating point on any load
 * line.
 * <p>
 * {@link #at(double, double)} fixes light and temperature (once per slow tick); {@link #current(double)} and
 * {@link #slope(double)} are then cheap enough for the 50 pre-step iterations of each electrical step.
 */
public final class PvCurve {
    /** Panel temperature (C) at which the rated power holds: a 20 C noon in full sun, the panel ~25 K over the air. */
    public static final double REF_C = 45.0;
    /** Panel temperature (C) at which the open-circuit voltage is the rating. */
    public static final double VOC_RATED_C = 10.0;
    /** Open-circuit voltage over the diode's thermal voltage at REF_C (silicon-like: 20-25). */
    public static final double VOC_OVER_A = 22.0;
    public static final double VOC_PER_K = -0.0035, ISC_PER_K = 0.0005;
    public static final double BYPASS_OHMS = 0.05;
    private static final double K0 = 273.15;

    /** Vmp / Voc and Imp / Isc of the ideal diode curve with Voc / a = VOC_OVER_A (light 1). */
    static final double VMP_OVER_VOC, IMP_OVER_ISC;

    static {
        // maximise x (1 - (e^{kx} - 1)/(e^k - 1)) over x = U/Voc, k = VOC_OVER_A: golden-section, once
        double k = VOC_OVER_A, lo = 0.5, hi = 1.0;
        double g = (Math.sqrt(5) - 1) / 2;
        for (int i = 0; i < 200; i++) {
            double m1 = hi - g * (hi - lo), m2 = lo + g * (hi - lo);
            if (pu(m1, k) < pu(m2, k)) lo = m1; else hi = m2;
        }
        double x = (lo + hi) / 2;
        VMP_OVER_VOC = x;
        IMP_OVER_ISC = pu(x, k) / x;
    }

    private static double pu(double x, double k) {
        return x * (1 - Math.expm1(k * x) / Math.expm1(k));
    }

    public final double ratedU, ratedP;
    /** At REF_C and light 1. */
    public final double vocRef, iscRef;

    // set by at(): the curve in force
    private double iph, i0, a, voc;

    public PvCurve(double ratedU, double ratedP) {
        this.ratedU = ratedU;
        this.ratedP = ratedP;
        this.vocRef = ratedU / (1 + VOC_PER_K * (VOC_RATED_C - REF_C));
        this.iscRef = ratedP / (vocRef * VMP_OVER_VOC) / IMP_OVER_ISC;
        at(1.0, REF_C);
    }

    /** Fix light (0..1, may be a little over) and panel temperature (C). Returns this. */
    public PvCurve at(double light, double panelC) {
        double dT = panelC - REF_C;
        double vocFull = Math.max(vocRef * (1 + VOC_PER_K * dT), 1e-3 * vocRef);
        double isc = Math.max(iscRef * (1 + ISC_PER_K * dT), 0);
        this.a = vocRef / VOC_OVER_A * (panelC + K0) / (REF_C + K0);
        this.i0 = isc / Math.expm1(vocFull / this.a);
        this.iph = isc * Math.max(light, 0);
        this.voc = this.iph <= 0 ? 0 : this.a * Math.log1p(this.iph / this.i0);
        return this;
    }

    /** Photocurrent (= short-circuit current) of the curve in force. */
    public double iph() {
        return this.iph;
    }

    /** Open-circuit voltage of the curve in force (0 in the dark). */
    public double voc() {
        return this.voc;
    }

    /** Current out of the panel's + terminal at terminal voltage u. Monotone falling in u. */
    public double current(double u) {
        if (u < 0) return this.iph - u / BYPASS_OHMS;
        return this.iph - this.i0 * Math.expm1(u / this.a);
    }

    /** dI/dU (always negative). */
    public double slope(double u) {
        if (u < 0) return -1 / BYPASS_OHMS;
        return -this.i0 / this.a * Math.exp(u / this.a);
    }

    /**
     * The operating point on the load line I = (u - uth) / rth: the u where the panel's current equals the current
     * the network takes. Safeguarded Newton from {@code guess} inside [min(uth, 0)-ish, voc]. Requires uth < voc and
     * rth > 0; returns NaN only if the inputs are not finite.
     */
    public double solve(double uth, double rth, double guess) {
        if (!(Double.isFinite(uth) && Double.isFinite(rth)) || rth <= 0) return Double.NaN;
        // f(u) = (u - uth)/rth - current(u): increasing; f(voc) >= 0 when uth <= voc
        double hi = this.voc;
        double lo = Math.min(uth, 0);
        if (lo >= hi) return hi;
        // below 0 the bypass line can make f(lo) > 0 only if lo is the root side; extend down until f(lo) <= 0
        double flo = f(lo, uth, rth);
        int guard = 0;
        while (flo > 0 && guard++ < 60) {
            lo = lo * 2 - 1;
            flo = f(lo, uth, rth);
        }
        double u = Double.isFinite(guess) ? Math.min(Math.max(guess, lo), hi) : 0.5 * (lo + hi);
        for (int it = 0; it < 40; it++) {
            double fu = f(u, uth, rth);
            if (fu > 0) hi = u; else lo = u;
            double d = 1 / rth - slope(u);
            double next = u - fu / d;
            if (!(next > lo && next < hi)) next = 0.5 * (lo + hi);   // Newton left the bracket: bisect
            if (Math.abs(next - u) <= 1e-12 * Math.max(1, Math.abs(next)) || hi - lo <= 1e-13 * Math.max(1, hi)) return next;
            u = next;
        }
        return u;
    }

    private double f(double u, double uth, double rth) {
        return (u - uth) / rth - current(u);
    }

    /** The maximum power point of the curve in force: {voltage, power}. For readouts, not per iteration. */
    public double[] maxPowerPoint() {
        if (this.voc <= 0) return new double[]{0, 0};
        double lo = 0, hi = this.voc, g = (Math.sqrt(5) - 1) / 2;
        for (int i = 0; i < 80; i++) {
            double m1 = hi - g * (hi - lo), m2 = lo + g * (hi - lo);
            if (m1 * current(m1) < m2 * current(m2)) lo = m1; else hi = m2;
        }
        double u = (lo + hi) / 2;
        return new double[]{u, u * current(u)};
    }
}
