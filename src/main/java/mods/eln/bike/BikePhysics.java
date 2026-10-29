package mods.eln.bike;

/**
 * The electric bike's riding rules, apart from Minecraft (unit-tested; ElectricBike applies them each tick on the side
 * that moves it). Baughn 2026-10-28: about an average horse on battery, a little faster than running without; speed is
 * the rider's input averaged over 2.5 s, so it rolls up and coasts down like a bicycle; half-block steps and stairs
 * ride, a full block stops you; a charged jump clears a little over one block. The tunables are EA config
 * ([bike] in eln-synced.toml, ElnConfig: the server's values reach the rider's client, which does the moving); the
 * values here are the defaults.
 */
public final class BikePhysics {
    /** Ticks of input averaged into the speed (2.5 s: Baughn's 2-3 s). */
    public static volatile int AVERAGE_TICKS = 50;
    /** Top speeds in blocks per tick: on battery 9.5 b/s (an average horse); pedalling 6.6 b/s (sprinting is 5.6). */
    public static volatile double VMAX_POWERED = 0.475, VMAX_PEDALLING = 0.33;
    /** Backing up (S held from a standstill): walking it backwards, 1 b/s. */
    public static final double REVERSE = 0.05;
    /** A living entity's fall: gravity per tick, then drag. */
    public static final double GRAVITY = 0.08, AIR_DRAG = 0.98;
    /** Half slabs, stairs, snow layers ride; a full block doesn't. */
    public static volatile float STEP_HEIGHT = 0.6f;
    /** Running into something at more than 3 b/s throws you off (Baughn: a full-block step; walls too, by default). */
    public static volatile double CRASH_SPEED = 0.15;
    /** Upward speed of a full-charge jump: peaks a little over one block (BikePhysicsTest computes it). */
    public static volatile double JUMP_VY = 0.43;
    /** How fast the bike swings round to where the rider looks, degrees per tick. */
    public static volatile float TURN_PER_TICK = 12;
    /** ...and at the powered top speed (Baughn: steering slows with speed); in between, linear in speed. */
    public static volatile float TURN_AT_TOP = 5;
    /** The motor at full speed, W (a Portable Battery's 100 W discharge: ~7 min on one, ~20 on a pack). */
    public static volatile double MOTOR_WATTS = 100;

    private float[] inputs = new float[AVERAGE_TICKS];
    private int next;
    private double sum;

    /** Add this tick's input (W = 1, S = -1, nothing = 0) and return the average over the last AVERAGE_TICKS. */
    public double input(float in) {
        if (inputs.length != AVERAGE_TICKS) {   // (the window was reconfigured)
            inputs = new float[AVERAGE_TICKS];
            reset();
        }
        in = Math.max(-1, Math.min(1, in));
        sum += in - inputs[next];
        inputs[next] = in;
        next = (next + 1) % inputs.length;
        if (next == 0) {   // (no drift from float sums)
            sum = 0;
            for (float f : inputs) sum += f;
        }
        return average();
    }

    public double average() {
        return sum / inputs.length;
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

    /** The upward speed whose jump peaks at height h (blocks): jumpPeak inverted, for the config's jump height. */
    public static double velocityForPeak(double h) {
        double lo = 0, hi = 3;
        for (int i = 0; i < 60; i++) {
            double mid = (lo + hi) / 2;
            if (jumpPeak(mid) < h) lo = mid;
            else hi = mid;
        }
        return (lo + hi) / 2;
    }

    /** The heading after a tick of turning toward target (degrees), at a standstill's rate. */
    public static float turn(float yaw, float target) {
        return turn(yaw, target, 0);
    }

    /** How fast it can turn at speed v (b/t), degrees per tick: TURN_PER_TICK standing, TURN_AT_TOP at top speed and above. */
    public static float turnRate(double v) {
        float f = (float) Math.min(1, Math.abs(v) / VMAX_POWERED);
        return TURN_PER_TICK + (TURN_AT_TOP - TURN_PER_TICK) * f;
    }

    /** The heading after a tick of turning toward target at speed v (b/t). */
    public static float turn(float yaw, float target, double v) {
        float d = wrap(target - yaw), r = turnRate(v);
        return yaw + Math.max(-r, Math.min(r, d));
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
