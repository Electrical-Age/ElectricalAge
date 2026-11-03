package mods.eln.robotics.motion;

/**
 * A servo drive's electrics and what its energy pays for (one 200 V tier, Baughn). Its top speed follows the line
 * voltage, so a sagging supply makes the axis slow and below the stall share it holds; and it never moves further in a
 * tick than the energy it got pays for: moving against the net force (the load less the counterweight) costs the work
 * over the efficiency, moving with it costs nothing (idle losses are the machine's own draw, not counted here). Pure
 * numbers: positions in metres, segment speeds in m/tick.
 * <p>
 * ratedLoadKg is the net load (over the counterweight) it lifts at full speed on its rated power; heavier, it gets
 * no more power and goes slower.
 */
public record Drive(double nominalVolts, double vmaxNominal, double accel, double idleWatts, double efficiency,
                    double ratedLoadKg) {
    /**
     * Below this share of nominal voltage the motor can't hold and stalls; it runs again once the line has been over
     * RESUME_SHARE for RESUME_TICKS (so a line hovering at the stall share doesn't start and stop it every tick).
     */
    public static final double STALL_SHARE = 0.3, RESUME_SHARE = 0.35;
    public static final int RESUME_TICKS = 10;
    /** Above nominal it runs at most this much faster. */
    public static final double OVERSPEED = 1.1;
    /** Gravity, m/s^2. */
    public static final double G = 9.81;

    /** Top speed (m/s) at this share of nominal voltage: 0 below the stall share, at most OVERSPEED x nominal. */
    public double vmaxAt(double voltageRatio) {
        if (!(voltageRatio >= STALL_SHARE)) return 0;
        return vmaxNominal * Math.min(OVERSPEED, voltageRatio);
    }

    /** The most it draws, W: idle plus lifting its rated load at full speed. */
    public double ratedWatts() {
        return idleWatts + G * ratedLoadKg * vmaxNominal / efficiency;
    }

    /**
     * The energy (J) to move dx metres along the axis against forceN (the force it must hold along the axis: positive
     * when a net weight pulls back along it), over the efficiency; 0 when the net weight does the work.
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
     * load, harder than its acceleration limit), and a new segment starts there at t0 + 1 toward the same target, at
     * the speed paid for (continuous in position; the speed steps down). Its top speed is what the supply keeps
     * paying for, sustainJoules a tick (the line's delivery less the idle draw; a tick's shortfall may only be an
     * empty buffer), but at least the speed paid and at most seg's; with neither, it goes on from rest with seg's
     * limits (it stood still, waiting for the energy it asked for).
     */
    public Segment afterPaying(Segment seg, long t0, double forceN, double joules, double sustainJoules) {
        double dt = t0 - seg.startTick();
        double p = seg.position(dt), dx = seg.position(dt + 1) - p;
        double paid = paidMetres(dx, forceN, joules);
        if (paid == dx) return seg;
        double sustain = Math.abs(paidMetres(dx, forceN, Math.max(0, sustainJoules)));
        double vmax = Math.min(seg.vmax(), Math.max(Math.abs(paid), sustain));
        return new Segment(t0 + 1, p + paid, paid, seg.to(), vmax > 0 ? vmax : seg.vmax(), seg.accel());
    }
}
