package mods.eln.node.voxel;

/**
 * The chisel's brush geometry: which voxels of one block a stroke covers. Pure maths (no world), shared by
 * ChiselItem.carve and the client outline so the preview is exactly what a click does. Every box stays inside the
 * block (0..16 on each axis) and is aligned to the brush size across the face.
 */
public final class ChiselBrush {
    private ChiselBrush() {
    }

    /** Brush sizes, in voxels across. */
    public static final int[] SIZES = {1, 2, 4, 8};

    /**
     * Baughn: cube (the size cube round the voxel hit), flat (a 1-voxel-deep size x size layer across the face hit,
     * for reliefs and trims), tunnel (the size x size footprint through the block's full depth along the face normal:
     * the cable hole; put back fills the same column).
     */
    public enum Shape {
        CUBE, FLAT, TUNNEL;

        public String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        /** The shape with this ordinal, or null. */
        public static Shape byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : null;
        }
    }

    public static boolean validSize(int size) {
        for (int s : SIZES) if (s == size) return true;
        return false;
    }

    /** A brush choice a client may ask for: a listed size and a known shape. */
    public static boolean valid(int size, int shape) {
        return validSize(size) && Shape.byId(shape) != null;
    }

    /**
     * The box {x0, y0, z0, x1, y1, z1} (voxels, end exclusive) a stroke of this shape and size covers, given the voxel
     * (vx, vy, vz) hit (0..15 each) and the axis of the face normal (0 = x, 1 = y, 2 = z).
     */
    public static int[] box(Shape shape, int size, int axis, int vx, int vy, int vz) {
        if (!validSize(size)) throw new IllegalArgumentException("brush size " + size);
        if (axis < 0 || axis > 2) throw new IllegalArgumentException("axis " + axis);
        int[] v = {vx, vy, vz}, b = new int[6];
        for (int a = 0; a < 3; a++) {
            int c = Math.clamp(v[a], 0, 15);
            int lo, hi;
            if (a != axis || shape == Shape.CUBE) {
                lo = c / size * size;
                hi = lo + size;
            } else if (shape == Shape.FLAT) {
                lo = c;
                hi = c + 1;
            } else {
                lo = 0;
                hi = 16;
            }
            b[a] = lo;
            b[a + 3] = hi;
        }
        return b;
    }

    /** The number of voxels in a box. */
    public static int volume(int[] b) {
        return (b[3] - b[0]) * (b[4] - b[1]) * (b[5] - b[2]);
    }
}
