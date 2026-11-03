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

    /**
     * The solver's line for this step (its edges are linear: Q = g (boost - dp)): the chord from free flow through the
     * curve at dp, so it meets the curve where the pump runs now and its slope stays bounded (Qmax / 2H0 .. Qmax / H0)
     * up to the shut-off, where it turns into the line through it (past it the check valve shuts). Returns the
     * conductance as a share of Qmax / H0 (0.5 .. 1); boost() is H0 over that.
     */
    public double chordShare(double dp) {
        double x = x(dp);
        return x < 1e-6 ? 0.5 + x / 8 : (1 - Math.sqrt(1 - x)) / x;
    }

    /** The chord's boost (Pa) at dp: Q = chordShare x Qmax / H0 x (boost - dp). */
    public double boost(double dp) {
        return shutoffPa / chordShare(dp);
    }
}
