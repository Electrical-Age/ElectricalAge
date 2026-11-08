package mods.eln.anim;

import mods.eln.gl.GL11;
import mods.eln.misc.Obj3D;

/**
 * A part between two poses: a value from 0 (off, closed, released) to 1 (on, open, pressed) that eases toward its target,
 * either smoothly ({@link #smooth}) or on a damped spring that may overshoot and settle ({@link #spring}: a lever that
 * snaps, a meter needle, a door). Draw helpers turn or slide a part by it.
 */
public final class Travel {
    final Smooth smooth;
    final mods.eln.misc.PhysicalInterpolator spring;

    private Travel(Smooth smooth, mods.eln.misc.PhysicalInterpolator spring) {
        this.smooth = smooth;
        this.spring = spring;
    }

    public static Travel smooth(double tau) {
        return new Travel(new Smooth(tau), null);
    }

    /** misc.PhysicalInterpolator's spring (preTao, accPerSPerError, slowPerS, rebound), as the 1.12 devices used. */
    public static Travel spring(float preTao, float accPerSPerError, float slowPerS, float rebound) {
        return new Travel(null, new mods.eln.misc.PhysicalInterpolator(preTao, accPerSPerError, slowPerS, rebound));
    }

    public Travel target(double t) {
        if (smooth != null) smooth.target(t);
        else spring.setTarget((float) t);
        return this;
    }

    public Travel to(boolean on) {
        return target(on ? 1 : 0);
    }

    public double target() {
        return smooth != null ? smooth.target() : spring.getTarget();
    }

    public void step(double dt) {
        if (smooth != null) smooth.step(dt);
        else spring.step((float) dt);
    }

    public float get() {
        return (float) (smooth != null ? smooth.get() : spring.get());
    }

    /** Jump to v (the target unchanged; a meter's published position). */
    public Travel set(double v) {
        if (smooth != null) smooth.set(v);
        else spring.setPos((float) v);
        return this;
    }

    /** Jump to the target (a device seen for the first time shows its state, not a swing to it). */
    public Travel snap() {
        if (smooth != null) smooth.snap();
        else spring.setPos(spring.getTarget());
        return this;
    }

    /** a at 0, b at 1. */
    public float lerp(float a, float b) {
        return a + (b - a) * get();
    }

    /** The part turned about its origin from offDeg (at 0) to onDeg (at 1). */
    public void rotate(Obj3D.Obj3DPart part, float offDeg, float onDeg, float ax, float ay, float az) {
        if (part != null) part.draw(lerp(offDeg, onDeg), ax, ay, az);
    }

    /** The part slid by get() times (dx, dy, dz) (block units). */
    public void slide(Obj3D.Obj3DPart part, float dx, float dy, float dz) {
        if (part == null) return;
        float f = get();
        GL11.glPushMatrix();
        GL11.glTranslatef(dx * f, dy * f, dz * f);
        part.draw();
        GL11.glPopMatrix();
    }
}
