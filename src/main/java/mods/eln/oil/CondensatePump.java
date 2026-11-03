package mods.eln.oil;

/**
 * A condenser's condensate pump curve (Baughn 2026-11-03: a centrifugal curve; "a steam turbine running at nominal
 * pressure should work on its own, while an 'overclocked' one would need a pipe pump"). Flow against a back-pressure dp
 * (Pa over the vessel) is Q = speed x Qmax x sqrt(1 - dp / H0): flat near free flow, steep near the shut-off head H0.
 * Sized from its turbine's rated inlet pressure and its condensate at full power: against RATED_OVER_NOMINAL times that
 * pressure (the boiler a governed turbine runs from) it passes RATED_MARGIN times the condensate, and it shuts off at
 * SHUTOFF_OVER_NOMINAL times it. Speed scales
 * the flow only; the shut-off head stays (pending Baughn: affinity laws would cut a slow turbine's head to nothing).
 */
public final class CondensatePump {
    /**
     * Shut-off head over the turbine's rated inlet pressure; the back-pressure (over that rating) at which it still passes
     * RATED_MARGIN times the condensate. A governed turbine throttles, so its boiler sits well above its rating (a 10 bar
     * turbine's at ~14 bar, AdventurAgent on live): a turbine at its rating returns its own condensate while its boiler
     * stays under ~1.5x, and an "overclocked" boiler past ~1.8x needs an inline pump (Baughn).
     */
    public static final double SHUTOFF_OVER_NOMINAL = 2.0, RATED_OVER_NOMINAL = 1.5, RATED_MARGIN = 1.5;
    /** Shut-off head (Pa over the vessel), the rated back-pressure (Pa) and free flow (kg/s at full speed, no head). */
    public final double shutoffPa, ratedPa, freeKgPerS;

    public CondensatePump(double shutoffPa, double ratedPa, double freeKgPerS) {
        this.shutoffPa = shutoffPa;
        this.ratedPa = ratedPa;
        this.freeKgPerS = freeKgPerS;
    }

    /** The pump for a turbine rated at nominalPa (its boiler's pressure) making condensateKgPerS at full power. */
    public static CondensatePump forTurbine(double nominalPa, double condensateKgPerS) {
        double h0 = SHUTOFF_OVER_NOMINAL * nominalPa, rated = RATED_OVER_NOMINAL * nominalPa;
        return new CondensatePump(h0, rated, RATED_MARGIN * condensateKgPerS / Math.sqrt(1 - rated / h0));
    }

    /** Flow at full speed against dp (Pa), as a share of the free flow: 1 at no head, 0 at the shut-off and past it. */
    public double share(double dp) {
        return Math.sqrt(1 - x(dp));
    }

    /** Flow at full speed against dp (kg/s). */
    public double kgPerS(double dp) {
        return freeKgPerS * share(dp);
    }

    double x(double dp) {
        return Math.max(0, Math.min(1, dp / shutoffPa));
    }

    /** The steepest the solver's line may be, as a share of Qmax / H0 (the tangent's slope runs to infinity at H0). */
    static final double MAX_LINE_SHARE = 32;

    /**
     * The solver's line for this step (its edges are linear: Q = g (boost - dp)): the curve's tangent at dp, so it meets
     * the curve where the pump runs now with its slope, its slope capped at MAX_LINE_SHARE (and at least the free-flow
     * tangent's, 0.5, for a dp below 0). Returns the conductance as a share of Qmax / H0; boost() goes with it.
     * <p>
     * The pump is linearised before each step, on the back-pressure the step starts from. A chord from free flow (less
     * steep than the curve) made each step overshoot the last near the shut-off, against a feed that passes little: the
     * pipe to the boiler then flipped between two pressures every step (Baughn 2026-11-03: 10.5..12.5 bar). On the
     * tangent the error of a step is of second order, and only within 1/(2 x MAX_LINE_SHARE)^2 of the shut-off (0.03 %)
     * can it still overshoot.
     */
    public double lineShare(double dp) {
        double u = Math.sqrt(1 - x(dp));
        return u <= 0.5 / MAX_LINE_SHARE ? MAX_LINE_SHARE : Math.max(0.5, 0.5 / u);
    }

    /**
     * The line's boost (Pa) at dp: Q = lineShare x Qmax / H0 x (boost - dp) meets the curve at dp. At and past the
     * shut-off, the line through it: it pushes back, and the check valve shuts.
     */
    public double boost(double dp) {
        double q = share(dp);
        return q <= 0 ? shutoffPa : dp + q * shutoffPa / lineShare(dp);
    }
}
