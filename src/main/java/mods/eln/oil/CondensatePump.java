package mods.eln.oil;

/**
 * A condenser's condensate pump curve (Baughn 2026-11-03: a centrifugal curve; "a steam turbine running at nominal
 * pressure should work on its own, while an 'overclocked' one would need a pipe pump"). Flow against a back-pressure dp
 * (Pa over the vessel) is Q = speed x Qmax x sqrt(1 - dp / H0): flat near free flow, steep near the shut-off head H0.
 * Sized from its turbine's rated inlet pressure (the boiler it feeds) and its condensate at full power: against that
 * pressure it passes RATED_MARGIN times the condensate, and it shuts off at SHUTOFF_OVER_NOMINAL times it. Speed scales
 * the flow only; the shut-off head stays (pending Baughn: affinity laws would cut a slow turbine's head to nothing).
 */
public final class CondensatePump {
    /** Shut-off head over the turbine's rated inlet pressure, and the flow against that pressure over its condensate. */
    public static final double SHUTOFF_OVER_NOMINAL = 1.3, RATED_MARGIN = 1.5;
    /** Shut-off head (Pa over the vessel), the rated back-pressure (Pa) and free flow (kg/s at full speed, no head). */
    public final double shutoffPa, ratedPa, freeKgPerS;

    public CondensatePump(double shutoffPa, double ratedPa, double freeKgPerS) {
        this.shutoffPa = shutoffPa;
        this.ratedPa = ratedPa;
        this.freeKgPerS = freeKgPerS;
    }

    /** The pump for a turbine rated at nominalPa (its boiler's pressure) making condensateKgPerS at full power. */
    public static CondensatePump forTurbine(double nominalPa, double condensateKgPerS) {
        double h0 = SHUTOFF_OVER_NOMINAL * nominalPa;
        return new CondensatePump(h0, nominalPa, RATED_MARGIN * condensateKgPerS / Math.sqrt(1 - nominalPa / h0));
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
