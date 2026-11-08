package mods.eln.anim;

/**
 * A first-order smoother for animations: the value follows its target with time constant tau, stepped exactly
 * (value += (target - value) * (1 - e^(-dt/tau))), so it never overshoots or rings at any frame rate. (misc.RcInterpolator
 * takes an Euler step, which overshoots once dt passes tau and oscillates without decay at twice it; it stays for the
 * server-side users that save it.) tau 0 follows at once.
 */
public final class Smooth {
    public final double tau;
    double value, target;

    public Smooth(double tau) {
        this(tau, 0);
    }

    public Smooth(double tau, double initial) {
        this.tau = tau;
        value = target = initial;
    }

    public Smooth target(double t) {
        target = t;
        return this;
    }

    public double target() {
        return target;
    }

    /** Advance by dt seconds; returns the value. */
    public double step(double dt) {
        if (tau <= 0 || dt >= 50 * tau) value = target;
        else if (dt > 0) value += (target - value) * (1 - Math.exp(-dt / tau));
        return value;
    }

    public double get() {
        return value;
    }

    /** Jump to v (target unchanged). */
    public Smooth set(double v) {
        value = v;
        return this;
    }

    /** Jump to the target. */
    public Smooth snap() {
        value = target;
        return this;
    }
}
