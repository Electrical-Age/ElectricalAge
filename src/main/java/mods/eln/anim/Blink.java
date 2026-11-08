package mods.eln.anim;

/**
 * Something that blinks on frame time (stepped in refresh(dt), so it holds while the game is paused): a fixed period
 * and duty, or random on and off spells (a beacon, busy status LEDs).
 */
public final class Blink {
    final double onMin, onMax, offMin, offMax;
    double left;
    boolean on;

    private Blink(double onMin, double onMax, double offMin, double offMax) {
        this.onMin = onMin;
        this.onMax = onMax;
        this.offMin = offMin;
        this.offMax = offMax;
        left = offMin + (offMax - offMin) * Math.random();
    }

    /** On for duty of every period seconds. */
    public static Blink every(double period, double duty) {
        return new Blink(period * duty, period * duty, period * (1 - duty), period * (1 - duty));
    }

    /** On for onMin..onMax, then off for offMin..offMax seconds, each spell random. */
    public static Blink random(double onMin, double onMax, double offMin, double offMax) {
        return new Blink(onMin, onMax, offMin, offMax);
    }

    /** Advance by dt; returns whether it's on. */
    public boolean step(double dt) {
        left -= dt;
        int guard = 0;
        while (left <= 0 && guard++ < 1000) {
            on = !on;
            double lo = on ? onMin : offMin, hi = on ? onMax : offMax;
            left += Math.max(1e-3, lo + (hi - lo) * Math.random());
        }
        return on;
    }

    public boolean on() {
        return on;
    }
}
