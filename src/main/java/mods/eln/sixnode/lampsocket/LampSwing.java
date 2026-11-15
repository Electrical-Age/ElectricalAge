package mods.eln.sixnode.lampsocket;

/**
 * The swing of a hanging lamp (client only, no game classes: see LampSwingTest).
 * <p>
 * Outdoors the wind pushes it every frame, in all weather, so it never rests and the renderer draws it every frame.
 * Indoors only something passing pushes it; it swings, the swing dies out, and below {@link #REST_ANGLE} it is put
 * exactly at rest. A lamp at rest draws the same from frame to frame, so the static bake (gl/Bake) moves its chain and
 * head into the chunk mesh; the next push takes it back to the renderer.
 */
public final class LampSwing {
    /**
     * Outdoors = sky light at the lamp's block of at least this: open sky (15) or right next to it (14, e.g. under
     * the one-block arm of a lamp post). A hanging lamp always has its ceiling block above it, so "can see the sky
     * straight up" would call every hanging lamp indoors.
     */
    public static final int OUTDOOR_SKY_LIGHT = 14;
    /** Seconds between two looks at the sky light (a roof built or removed is noticed within this, plus up to 25 %). */
    public static final float SKY_CHECK_SECONDS = 1f;
    /**
     * Indoors: a swing whose amplitude has fallen below this (degrees over the whole chain; the head then moves less
     * than a tenth of a pixel) is put at rest.
     */
    public static final float REST_ANGLE = 0.1f;

    float vy = 0, py = 0;
    float vz = 0, pz = 0;
    float weatherAlphaZ = 0, weatherAlphaY = 0;

    /** Something moving past (an arrow: factor 1, a living thing: factor 4). One that stands still pushes nothing. */
    public void push(double motionX, double motionZ, float factor, float dt) {
        vz += motionX * factor * dt;
        vy += motionZ * factor * dt;
    }

    /** The wind of one frame. weather: 0.1 clear, 0.55 rain, 1 thunder (as 1.12). Call only for a lamp outdoors. */
    public void wind(float weather, float dt) {
        weatherAlphaY += (0.4 - Math.random()) * dt * Math.PI / 0.2 * weather;
        weatherAlphaZ += (0.4 - Math.random()) * dt * Math.PI / 0.2 * weather;
        if (weatherAlphaY > 2 * Math.PI)
            weatherAlphaY -= 2 * Math.PI;
        if (weatherAlphaZ > 2 * Math.PI)
            weatherAlphaZ -= 2 * Math.PI;
        vy += Math.random() * Math.sin(weatherAlphaY) * weather * weather * dt * 3;
        vz += Math.random() * Math.cos(weatherAlphaY) * weather * weather * dt * 3;

        vy += 0.4 * dt * weather * Math.signum(vy) * Math.random();
        vz += 0.4 * dt * weather * Math.signum(vz) * Math.random();
    }

    /** One frame of the pendulum. Indoors (windless) a swing that has died out ends at exactly 0. */
    public void step(float dt, boolean outdoors) {
        vy -= py / 10 * dt;
        vy *= (1 - 0.2 * dt);
        py += vy;

        vz -= pz / 10 * dt;
        vz *= (1 - 0.2 * dt);
        pz += vz;

        // amplitude^2 = angle^2 + speed^2 * 10 / dt (the speed is per frame, the pull angle / 10 * dt): the same swing
        // at any frame rate, and not fooled by the moment the lamp passes through the middle
        if (!outdoors && dt > 0 && py * py + pz * pz + (vy * vy + vz * vz) * 10 / dt < REST_ANGLE * REST_ANGLE) {
            vy = py = vz = pz = 0f;
        }
    }

    /** The widest of the two swing angles, degrees over the whole chain. */
    public float angle() {
        return Math.max(Math.abs(py), Math.abs(pz));
    }

    public boolean atRest() {
        return vy == 0 && py == 0 && vz == 0 && pz == 0;
    }
}
