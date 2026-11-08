package mods.eln.anim;

import mods.eln.misc.Obj3D;

/**
 * A part that turns: an angle (degrees, kept in [0, 360)) advanced at degPerSecond times a speed (usually 0..1, or
 * signed for either direction), the speed following its target through a {@link Smooth} so it spins up and runs down.
 * Step it in the render's refresh(dt) (frame time: it stops while the game is paused), read it in draw().
 */
public final class Spin {
    public final double degPerSecond;
    public final Smooth speed;
    double deg;

    /** Turning degPerSecond at speed 1; the speed eases with time constant tau (0: at once). */
    public Spin(double degPerSecond, double tau) {
        this.degPerSecond = degPerSecond;
        speed = new Smooth(tau);
    }

    /** Start at a random angle (so a row of the same device doesn't turn in step). */
    public Spin randomPhase() {
        deg = Math.random() * 360;
        return this;
    }

    public Spin speed(double target) {
        speed.target(target);
        return this;
    }

    public void step(double dt) {
        speed.step(dt);
        deg = wrap(deg + degPerSecond * speed.get() * dt);
    }

    public float deg() {
        return (float) deg;
    }

    public Spin set(double degrees) {
        deg = wrap(degrees);
        return this;
    }

    /** The part turned by the angle about its origin (its .txt) and the axis. */
    public void draw(Obj3D.Obj3DPart part, float ax, float ay, float az) {
        if (part != null) part.draw(deg(), ax, ay, az);
    }

    static double wrap(double d) {
        d %= 360;
        return d < 0 ? d + 360 : d;
    }
}
