package mods.eln.oil;

/**
 * The shape of one generated oil lake (design v2 §3), a pure function of its spec, so worldgen (each chunk's carving,
 * on worker threads) and the lake's record (made on the main thread, OilReservoirs.ensure) agree without talking.
 * <p>
 * An ellipse with a noise-warped shore, oil to baseY (the full surface), deeper towards the middle with a noisy floor;
 * islands (dry, a block above the oil) and pillars (rock to the ceiling); a beach ring of dry floor around the oil;
 * a cavern ceiling 3-8 blocks above the oil.
 */
public record LakeShape(long key, int cx, int cz, int baseY, int rx, int rz, int seed) {
    /** What a column is. */
    public enum Kind { OUTSIDE, BEACH, ISLAND, PILLAR, OIL }

    static float hash(int x, int y, int z, int seed) {
        int h = x * 0x8da6b343 ^ y * 0xd8163841 ^ z * 0xcb1ab31f ^ seed * 0x9e3779b9;
        h ^= h >>> 16;
        h *= 0x7feb352d;
        h ^= h >>> 15;
        h *= 0x846ca68b;
        h ^= h >>> 16;
        return (h >>> 8) / 16777216f;
    }

    static float noise(float x, float z, int seed) {
        int ix = (int) Math.floor(x), iz = (int) Math.floor(z);
        float fx = x - ix, fz = z - iz;
        fx = fx * fx * (3 - 2 * fx);
        fz = fz * fz * (3 - 2 * fz);
        float a = hash(ix, 0, iz, seed), b = hash(ix + 1, 0, iz, seed), c = hash(ix, 0, iz + 1, seed), d = hash(ix + 1, 0, iz + 1, seed);
        return a + (b - a) * fx + (c - a) * fz + (a - b - c + d) * fx * fz;
    }

    /** Normalised distance from the centre with the noisy shore: < 1 is the lake, < 1.18 the cavern (beach). */
    public float dist(int x, int z) {
        float dx = (x + 0.5f - cx) / rx, dz = (z + 0.5f - cz) / rz;
        float d = (float) Math.sqrt(dx * dx + dz * dz);
        float warp = 1 + 0.35f * (noise(x / 18f, z / 18f, seed) - 0.5f) * 2 + 0.12f * (noise(x / 6f, z / 6f, seed ^ 0x51) - 0.5f) * 2;
        return d / warp;
    }

    public Kind kind(int x, int z) {
        float d = dist(x, z);
        if (d >= 1.18f) return Kind.OUTSIDE;
        if (d >= 1f) return Kind.BEACH;
        float isle = noise(x / 11f, z / 11f, seed ^ 0x15E);
        if (isle > 0.86f) return Kind.PILLAR;
        if (isle > 0.78f || depth(x, z) <= 0) return Kind.ISLAND;
        return Kind.OIL;
    }

    /** Oil depth in blocks at a column (cells baseY - depth .. baseY - 1). */
    public int depth(int x, int z) {
        float d = dist(x, z);
        float centre = Math.max(0, 1 - d);
        return Math.round(0.6f + 5.5f * centre * (0.55f + 0.45f * noise(x / 9f, z / 9f, seed ^ 0xDE9)));
    }

    /** The cavern ceiling: the first solid block above the oil/beach. */
    public int ceiling(int x, int z) {
        float d = dist(x, z);
        float dome = Math.max(0, 1.18f - d) / 1.18f;
        return baseY + 2 + Math.round(6 * dome * (0.5f + 0.5f * noise(x / 14f, z / 14f, seed ^ 0xCE1)));
    }

    public int minX() {
        return cx - (int) Math.ceil(rx * 1.6) - 2;
    }

    public int minZ() {
        return cz - (int) Math.ceil(rz * 1.6) - 2;
    }

    public int sizeX() {
        return 2 * ((int) Math.ceil(rx * 1.6) + 2);
    }

    public int sizeZ() {
        return 2 * ((int) Math.ceil(rz * 1.6) + 2);
    }

    /** The lake record's columns (OilReservoirs.create): bottom cell and mask per column over the box (MIN_VALUE: none). */
    public int[][] columns() {
        int sx = sizeX(), sz = sizeZ();
        int[] floor = new int[sx * sz], mask = new int[sx * sz];
        for (int i = 0; i < sx; i++)
            for (int k = 0; k < sz; k++) {
                int x = minX() + i, z = minZ() + k, idx = i * sz + k;
                if (kind(x, z) != Kind.OIL) {
                    floor[idx] = Integer.MIN_VALUE;
                    continue;
                }
                int d = Math.min(31, depth(x, z));
                floor[idx] = baseY - d;
                mask[idx] = (1 << d) - 1;
            }
        return new int[][]{floor, mask};
    }
}
