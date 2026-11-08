package mods.eln.anim;

/**
 * A machine's progress through its current job (0..1) on the client: the server publishes it with a rate only now and
 * then (on power changes, a new item, a finished one), so between publishes it runs on at that rate, held at 1.
 * {@link #shown} also follows forward through a short {@link Smooth} (a publish that corrects it doesn't jump), and drops
 * straight back when a new job starts: the next piece goes in.
 */
public final class Progress {
    double state, perSecond;
    final Smooth shown = new Smooth(0.15);

    /** A publish: the state and how fast it goes (per second). */
    public void set(double state, double perSecond) {
        if (state < this.state - 0.2 || state < 0.02 && this.state > 0.5) shown.set(Math.max(0, state));   // (a new job)
        this.state = Math.max(0, Math.min(1, state));
        this.perSecond = perSecond;
    }

    public void step(double dt) {
        state = Math.min(1, state + perSecond * dt);
        shown.target(state);
        if (shown.get() > state) shown.set(state);
        shown.step(dt);
    }

    /** The extrapolated state. */
    public double state() {
        return state;
    }

    /** What to draw: the state, eased forward. */
    public float shown() {
        return (float) shown.get();
    }
}
