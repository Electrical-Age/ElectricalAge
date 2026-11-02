package mods.eln.robotics.motion;

/**
 * A servo drive's electrics (one 200 V tier, Baughn): its top speed follows the line voltage, so a sagging supply makes
 * the axis slow and a dead one stalls it; its draw is idle losses plus the mechanical power it delivers over its
 * efficiency. Pure numbers; the device turns the draw into a smoothly varying load (never rewiring the circuit).
 */
public record Drive(double nominalVolts, double vmaxNominal, double accel, double idleWatts, double efficiency) {
    /** Below this share of nominal voltage the motor can't hold and stalls. */
    public static final double STALL_SHARE = 0.3;
    /** Above nominal it runs at most this much faster. */
    public static final double OVERSPEED = 1.1;

    /** Top speed (m/s) at this line voltage: 0 below the stall share. */
    public double vmaxAt(double volts) {
        double k = volts / nominalVolts;
        if (k < STALL_SHARE) return 0;
        return vmaxNominal * Math.min(OVERSPEED, k);
    }

    /** Electrical draw (W) moving at v (m/s) against a force f (N, positive when the motor does work: lifting). */
    public double watts(double v, double f) {
        double mech = f * v;
        return idleWatts + (mech > 0 ? mech / efficiency : 0);
    }

    /** The load's resistance (ohm) drawing p watts at u volts. */
    public static double resistance(double u, double p) {
        return Math.max(1e-3, u * u / Math.max(1e-6, p));
    }
}
