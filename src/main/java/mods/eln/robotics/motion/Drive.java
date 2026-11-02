package mods.eln.robotics.motion;

/**
 * A servo drive's electrics and what its energy pays for (one 200 V tier, Baughn). Its top speed follows the line
 * voltage, so a sagging supply makes the axis slow and below the stall share it holds; and it never moves further in a
 * tick than the energy it got pays for: lifting costs the work over the efficiency, lowering costs nothing (idle losses
 * are the machine's own draw, not counted here). Pure numbers: positions in metres, segment speeds in m/tick.
 * <p>
 * ratedLoadKg is the net load (over the counterweight) it lifts at full speed on its rated power; heavier, it gets
 * no more power and goes slower.
 */
public record Drive(double nominalVolts, double vmaxNominal, double accel, double idleWatts, double efficiency,
                    double ratedLoadKg) {
    /** Below this share of nominal voltage the motor can't hold and stalls. */
    public static final double STALL_SHARE = 0.3;
    /** Above nominal it runs at most this much faster. */
    public static final double OVERSPEED = 1.1;
    /** Gravity, m/s^2. */
    public static final double G = 9.81;

    /** Top speed (m/s) at this share of nominal voltage: 0 below the stall share, at most OVERSPEED x nominal. */
    public double vmaxAt(double voltageRatio) {
        if (!(voltageRatio >= STALL_SHARE)) return 0;
        return vmaxNominal * Math.min(OVERSPEED, voltageRatio);
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

    /** The most it draws, W: idle plus lifting its rated load at full speed. */
    public double ratedWatts() {
        return idleWatts + G * ratedLoadKg * vmaxNominal / efficiency;
    }

    /**
     * The energy (J) to move dx metres along the axis against forceN (the force it must hold along the axis: positive
     * when a net weight pulls back along it), over the efficiency; 0 when the load does the work (lowering).
     */
    public double joules(double dx, double forceN) {
        double work = forceN * dx;
        return work > 0 ? work / efficiency : 0;
    }

    /** How far (m, signed as dx) joules pay for of a planned move dx against forceN: all of it when it costs nothing. */
    public double paidMetres(double dx, double forceN, double joules) {
        double need = joules(dx, forceN);
        if (need <= joules * (1 + 1e-9) + 1e-12) return dx;   // (rounding in the buffer is not a shortfall)
        return Math.signum(dx) * Math.max(0, joules) * efficiency / Math.abs(forceN);
    }

    /**
     * The motion after the tick from t0 on seg when the move got joules against forceN. Paid in full: seg itself.
     * Short: the axis went only as far as the joules pay for (a lift whose motor can't hold it is braked by its
     * load, harder than its acceleration limit), and a new segment starts there at t0 + 1 toward the same target,
     * at the speed paid for and no faster (continuous in position; the speed steps down). Paid nothing: it stood
     * still this tick and goes on from rest with seg's limits.
     */
    public Segment afterPaying(Segment seg, long t0, double forceN, double joules) {
        double dt = t0 - seg.startTick();
        double p = seg.position(dt), dx = seg.position(dt + 1) - p;
        double paid = paidMetres(dx, forceN, joules);
        if (paid == dx) return seg;
        double v = Math.abs(paid);
        return new Segment(t0 + 1, p + paid, paid, seg.to(), v > 0 ? Math.min(v, seg.vmax()) : seg.vmax(), seg.accel());
    }
}
