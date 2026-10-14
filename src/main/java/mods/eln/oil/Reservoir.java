package mods.eln.oil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Arrays;

/**
 * One underground oil lake: its shape and its pooled volume (design v2 §2). The oil amount lives here, not in the
 * blocks; the blocks (ReservoirOilBlock) are a view: every cell below the surface is full, the one at the surface
 * shows 1-8 eighths, and the surface height is a pure function of the amount (water filling a bowl), so the whole lake
 * rises and falls together.
 * <p>
 * Shape: a box of columns; per column the bottom cell's y and a 32-bit mask of which cells above it (bit i = y
 * floor + i) belong to the lake. A dam (any block placed into the lake) clears its bit. 1 block = 1000 mB, 1/8 = 125.
 */
public final class Reservoir {
    public static final int NONE = Integer.MIN_VALUE;

    final int id, minX, minZ, sizeX, sizeZ;
    final int[] floor, mask;
    long amount;
    /** The surface height in eighths of a block that the blocks were last swept to (-1: never). */
    int shown = -1;

    public static final Codec<Reservoir> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("id").forGetter(r -> r.id),
        Codec.INT.fieldOf("min_x").forGetter(r -> r.minX),
        Codec.INT.fieldOf("min_z").forGetter(r -> r.minZ),
        Codec.INT.fieldOf("size_x").forGetter(r -> r.sizeX),
        Codec.INT.fieldOf("size_z").forGetter(r -> r.sizeZ),
        Codec.INT_STREAM.xmap(s -> s.toArray(), Arrays::stream).fieldOf("floor").forGetter(r -> r.floor),
        Codec.INT_STREAM.xmap(s -> s.toArray(), Arrays::stream).fieldOf("mask").forGetter(r -> r.mask),
        Codec.LONG.fieldOf("amount").forGetter(r -> r.amount),
        Codec.INT.optionalFieldOf("shown", -1).forGetter(r -> r.shown)
    ).apply(i, Reservoir::new));

    Reservoir(int id, int minX, int minZ, int sizeX, int sizeZ, int[] floor, int[] mask, long amount, int shown) {
        this.id = id;
        this.minX = minX;
        this.minZ = minZ;
        this.sizeX = sizeX;
        this.sizeZ = sizeZ;
        this.floor = floor;
        this.mask = mask;
        this.amount = amount;
        this.shown = shown;
    }

    public int id() {
        return id;
    }

    public String describeBox() {
        return "x " + minX + ".." + (minX + sizeX - 1) + ", z " + minZ + ".." + (minZ + sizeZ - 1);
    }

    public long amount() {
        return amount;
    }

    int index(int x, int z) {
        int dx = x - minX, dz = z - minZ;
        return dx < 0 || dz < 0 || dx >= sizeX || dz >= sizeZ ? -1 : dx * sizeZ + dz;
    }

    /** Is the block a cell of this lake? */
    public boolean contains(int x, int y, int z) {
        int i = index(x, z);
        if (i < 0 || floor[i] == NONE) return false;
        int b = y - floor[i];
        return b >= 0 && b < 32 && (mask[i] >>> b & 1) != 0;
    }

    /** The volume (mB) below a surface at height h (eighths of a block). */
    public long volumeAt(int h) {
        long eighths = 0;
        for (int i = 0; i < floor.length; i++) {
            if (floor[i] == NONE || mask[i] == 0) continue;
            int m = mask[i];
            int base = floor[i] * 8;
            if (h <= base) continue;
            while (m != 0) {
                int b = Integer.numberOfTrailingZeros(m);
                m &= m - 1;
                int filled = h - (base + b * 8);
                if (filled <= 0) break;
                eighths += Math.min(8, filled);
            }
        }
        return eighths * 125;
    }

    int lowestEighth() {
        int lo = Integer.MAX_VALUE;
        for (int i = 0; i < floor.length; i++)
            if (floor[i] != NONE && mask[i] != 0) lo = Math.min(lo, (floor[i] + Integer.numberOfTrailingZeros(mask[i])) * 8);
        return lo == Integer.MAX_VALUE ? 0 : lo;
    }

    int highestEighth() {
        int hi = Integer.MIN_VALUE;
        for (int i = 0; i < floor.length; i++)
            if (floor[i] != NONE && mask[i] != 0) hi = Math.max(hi, (floor[i] + 32 - Integer.numberOfLeadingZeros(mask[i])) * 8);
        return hi == Integer.MIN_VALUE ? 0 : hi;
    }

    /** The full volume (mB): the most the lake holds. */
    public long capacity() {
        return volumeAt(highestEighth());
    }

    /** The surface height (eighths) for the current amount: the highest h whose volume fits. */
    public int surface() {
        int lo = lowestEighth(), hi = highestEighth();
        if (amount <= 0) return lo;
        while (lo < hi) {
            int mid = (lo + hi + 1) >> 1;   // signed: lakes below y 0 have negative heights
            if (volumeAt(mid) <= amount) lo = mid;
            else hi = mid - 1;
        }
        return lo;
    }

    /** The oil level the cell should show at surface h: 0 none, 1-8 eighths. */
    static int cellLevel(int y, int h) {
        return Math.max(0, Math.min(8, h - y * 8));
    }

    /** Remove a cell (a dam); true if it was one. */
    boolean block(int x, int y, int z) {
        int i = index(x, z);
        if (i < 0 || floor[i] == NONE) return false;
        int b = y - floor[i];
        if (b < 0 || b >= 32 || (mask[i] >>> b & 1) == 0) return false;
        mask[i] &= ~(1 << b);
        return true;
    }
}
