package mods.eln.bike;

/**
 * The electric bike's riding rules, apart from Minecraft (unit-tested; ElectricBike applies them each tick on the side
 * that moves it). Baughn 2026-10-28: about an average horse on battery, a little faster than running without; speed is
 * the rider's input averaged over 2.5 s, so it rolls up and coasts down like a bicycle; half-block steps and stairs
 * ride, a full block stops you; a charged jump clears a little over one block.
 */
public final class BikePhysics {
    /** Ticks of input averaged into the speed (2.5 s: Baughn's 2-3 s). */
    public static final int AVERAGE_TICKS = 50;
    /** Top speeds in blocks per tick: on battery 9.5 b/s (an average horse); pedalling 6.6 b/s (sprinting is 5.6). */
    public static final double VMAX_POWERED = 0.475, VMAX_PEDALLING = 0.33;
    /** Backing up (S held from a standstill): walking it backwards, 1 b/s. */
    public static final double REVERSE = 0.05;
    /** A living entity's fall: gravity per tick, then drag. */
    public static final double GRAVITY = 0.08, AIR_DRAG = 0.98;
    /** Half slabs, stairs, snow layers ride; a full block doesn't. */
    public static final float STEP_HEIGHT = 0.6f;
    /** Running into something at more than 3 b/s throws you off (Baughn: a full-block step; walls too, by default). */
    public static final double CRASH_SPEED = 0.15;
    /** Upward speed of a full-charge jump: peaks a little over one block (BikePhysicsTest computes it). */
    public static final double JUMP_VY = 0.43;
    /** How fast the bike swings round to where the rider looks, degrees per tick. */
    public static final float TURN_PER_TICK = 12;
    /** The motor at full speed, W (a Portable Battery's 100 W discharge: ~7 min on one, ~20 on a pack). */
    public static final double MOTOR_WATTS = 100;

    private final float[] inputs = new float[AVERAGE_TICKS];
    private int next;
    private double sum;

    /** Add this tick's input (W = 1, S = -1, nothing = 0) and return the average over the last AVERAGE_TICKS. */
    public double input(float in) {
        in = Math.max(-1, Math.min(1, in));
        sum += in - inputs[next];
        inputs[next] = in;
        next = (next + 1) % AVERAGE_TICKS;
        if (next == 0) {   // (no drift from float sums)
            sum = 0;
            for (float f : inputs) sum += f;
        }
        return average();
    }

    public double average() {
        return sum / AVERAGE_TICKS;
    }

    /** Forget the input (dismounted, crashed). */
    public void reset() {
        java.util.Arrays.fill(inputs, 0);
        sum = 0;
        next = 0;
    }

    /** This tick's speed along the bike's heading (b/t) for this tick's input: the average times the top speed. */
    public double speed(float in, boolean powered) {
        return speedFor(input(in), in, powered);
    }

    /** Speed for an input average a: forward a * vmax; held back past zero, it backs up slowly. */
    public static double speedFor(double a, float in, boolean powered) {
        if (a > 0) return Math.min(1, a) * (powered ? VMAX_POWERED : VMAX_PEDALLING);
        return in < 0 ? -REVERSE * Math.min(1, -a / 0.2) : 0;
    }

    /** Upward speed for a charged jump (scale 0..1 from the jump bar). */
    public static double jumpVelocity(float scale) {
        return JUMP_VY * Math.max(0, Math.min(1, scale));
    }

    /** The height a jump at vy reaches, falling as the bike does (move, then gravity and drag). */
    public static double jumpPeak(double vy) {
        double y = 0, peak = 0;
        for (int i = 0; i < 200 && (vy > 0 || y > 0); i++) {
            y += vy;
            peak = Math.max(peak, y);
            vy = (vy - GRAVITY) * AIR_DRAG;
        }
        return peak;
    }

    /** The heading after a tick of turning toward target (degrees), at most TURN_PER_TICK. */
    public static float turn(float yaw, float target) {
        float d = wrap(target - yaw);
        return yaw + Math.max(-TURN_PER_TICK, Math.min(TURN_PER_TICK, d));
    }

    static float wrap(float deg) {
        deg %= 360;
        if (deg >= 180) deg -= 360;
        if (deg < -180) deg += 360;
        return deg;
    }

    /** The motor's draw at speed v (b/t): MOTOR_WATTS at full speed, falling with the square of speed. */
    public static double motorWatts(double v) {
        double f = Math.min(1, Math.abs(v) / VMAX_POWERED);
        return MOTOR_WATTS * f * f;
    }

    /** Whether running into something at horizontal speed v (b/t) throws the rider off. */
    public static boolean crashes(double v) {
        return Math.abs(v) > CRASH_SPEED;
    }
}
