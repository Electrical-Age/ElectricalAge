package mods.eln.bike;

/**
 * The bike's shape, in the model's pixels (y down, the ground at 24, the front toward -z), shared by BikeModel (which
 * draws it) and BikeColors (which aims dyes at it), so the dye targets can't drift from what is drawn (AdventurAgent:
 * the saddle's target stopped short of the drawn saddle). A box is {x0, y0, z0, x1, y1, z1}.
 */
public final class BikeGeometry {
    private BikeGeometry() {
    }

    /** Wheel radius, hub height, axles; head tube, bottom bracket, seat post top. */
    public static final float R = 5.5f, HUB_Y = 24 - R, FRONT_Z = -10, REAR_Z = 10;
    public static final float HEAD_Z = -7, HEAD_TOP = 8, HEAD_BOT = 11, BB_Y = 18.5f, BB_Z = 1, SEAT_Z = 3.5f, SEAT_Y = 9.5f;

    /** The saddle: its part's origin, and its two boxes about it (the rear one 0.1 px lower on top and higher underneath:
     *  their tops, 0.05 apart, and their shared underside z-fought; the checker wants 0.05 or more). */
    public static final float SADDLE_OY = SEAT_Y - 1.8f, SADDLE_OZ = SEAT_Z;
    public static final float[][] SADDLE_BOXES = {{-1.5f, -1, -3.5f, 1.5f, 0.5f, 1.5f}, {-2.5f, -0.9f, -0.5f, 2.5f, 0.4f, 2}};
    /** The rack's top, and the trunk on it: body and overhanging lid, about the trunk's origin. */
    public static final float RACK_Y = HUB_Y - R - 2.2f, TRUNK_OZ = REAR_Z - 3.5f;
    public static final float[] TRUNK_BOX = {-3.5f, -4.5f, 0, 3.5f, 0, 7}, LID_BOX = {-3.8f, -5.5f, -0.3f, 3.8f, -4.5f, 7.3f};

    /** The smallest box holding these boxes, moved by (oy, oz). */
    static float[] around(float oy, float oz, float[]... boxes) {
        float[] r = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (float[] b : boxes)
            for (int i = 0; i < 3; i++) {
                float o = i == 1 ? oy : i == 2 ? oz : 0;
                r[i] = Math.min(r[i], b[i] + o);
                r[i + 3] = Math.max(r[i + 3], b[i + 3] + o);
            }
        return r;
    }

    /** The saddle and the trunk as drawn (model px). */
    public static float[] saddle() {
        return around(SADDLE_OY, SADDLE_OZ, SADDLE_BOXES);
    }

    public static float[] trunk() {
        return around(RACK_Y, TRUNK_OZ, TRUNK_BOX, LID_BOX);
    }

    /** The fork's two blades, head to front hub, 1.1 thick at x +-1.3 (model px). */
    public static float[] fork() {
        return new float[]{-1.9f, HEAD_BOT - 0.6f, FRONT_Z - 0.6f, 1.9f, HUB_Y + 0.6f, HEAD_Z - 1 + 0.6f};
    }

    /** The frame: the head and top tube; the down tube, seat tube and bottom bracket; the stays (model px, tubes' bounds). */
    public static float[][] frame() {
        return new float[][]{
            {-1, HEAD_TOP - 1, HEAD_Z - 1.5f, 1, SEAT_Y + 2.3f, SEAT_Z - 0.2f},
            {-2, HEAD_BOT - 0.8f, HEAD_Z - 0.5f, 2, BB_Y + 1, BB_Z + 1},
            {-1.7f, SEAT_Y + 1, BB_Z + 1, 1.7f, HUB_Y + 0.6f, REAR_Z + 0.6f}};
    }
}
