package mods.eln.reservoir;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;

/**
 * One lake (oil or water): its amount and its level->area table, not its geometry (RESERVOIR-PLAN-2.md: EA's dense
 * per-column record didn't scale to Watershed's lakes). The amount lives here; the blocks are a view: every cell
 * below the surface is full, the one at the surface shows 1-8 eighths, and the surface height (eighths) is a pure
 * function of the amount and the area table, so the whole lake rises and falls together.
 * <p>
 * area[y - minY] = cells of the cavity at block level y, now: the basin's estimate for chunks not yet swept, the true
 * count for swept ones (Reservoirs.sweep calibrates), less the dammed cells (a dam keeps the amount, so the surface
 * rises a hair: Baughn). 1 block = 1000 mB, 1/8 = 125.
 */
public final class Reservoir {
    final int id;
    final String kind, basinType;
    final CompoundTag basinSpec;
    int minY;
    int[] area;
    long amount;
    /** The surface (eighths) the blocks were last swept to (-1: never). */
    int shown = -1;
    /** Chunks whose cells are known (ChunkCells), so their estimate has been replaced by the truth. */
    final LongOpenHashSet calibrated;
    /** The basin (from basinType + basinSpec), resolved on first use. */
    Basin basin;

    public static final Codec<Reservoir> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("id").forGetter(r -> r.id),
        Codec.STRING.fieldOf("kind").forGetter(r -> r.kind),
        Codec.STRING.fieldOf("basin").forGetter(r -> r.basinType),
        CompoundTag.CODEC.fieldOf("spec").forGetter(r -> r.basinSpec),
        Codec.INT.fieldOf("min_y").forGetter(r -> r.minY),
        Codec.INT_STREAM.xmap(s -> s.toArray(), Arrays::stream).fieldOf("area").forGetter(r -> r.area),
        Codec.LONG.fieldOf("amount").forGetter(r -> r.amount),
        Codec.INT.optionalFieldOf("shown", -1).forGetter(r -> r.shown),
        Codec.LONG_STREAM.xmap(s -> new LongOpenHashSet(s.toArray()), s -> Arrays.stream(s.toLongArray()))
            .optionalFieldOf("calibrated", new LongOpenHashSet()).forGetter(r -> r.calibrated)
    ).apply(i, Reservoir::new));

    Reservoir(int id, String kind, String basinType, CompoundTag basinSpec, int minY, int[] area, long amount, int shown,
              LongOpenHashSet calibrated) {
        this.id = id;
        this.kind = kind;
        this.basinType = basinType;
        this.basinSpec = basinSpec;
        this.minY = minY;
        this.area = area;
        this.amount = amount;
        this.shown = shown;
        this.calibrated = calibrated;
    }

    public int id() {
        return id;
    }

    public String kind() {
        return kind;
    }

    public long amount() {
        return amount;
    }

    public CompoundTag spec() {
        return basinSpec;
    }

    /** Cells at block level y (the area table). */
    public int areaAt(int y) {
        int i = y - minY;
        return i < 0 || i >= area.length ? 0 : area[i];
    }

    /** Add d cells at level y (calibration, dams), growing the table as needed. */
    void addArea(int y, int d) {
        if (d == 0) return;
        if (area.length == 0) {
            minY = y;
            area = new int[1];
        }
        if (y < minY) {
            int[] a = new int[area.length + (minY - y)];
            System.arraycopy(area, 0, a, minY - y, area.length);
            area = a;
            minY = y;
        } else if (y >= minY + area.length) area = Arrays.copyOf(area, y - minY + 1);
        area[y - minY] = Math.max(0, area[y - minY] + d);
    }

    /** The volume (mB) below a surface at height h (eighths of a block). */
    public long volumeAt(int h) {
        long eighths = 0;
        for (int k = 0; k < area.length; k++) {
            int filled = h - (minY + k) * 8;
            if (filled <= 0) break;
            eighths += (long) area[k] * Math.min(8, filled);
        }
        return eighths * 125;
    }

    int lowestEighth() {
        for (int k = 0; k < area.length; k++) if (area[k] > 0) return (minY + k) * 8;
        return minY * 8;
    }

    int highestEighth() {
        for (int k = area.length - 1; k >= 0; k--) if (area[k] > 0) return (minY + k + 1) * 8;
        return minY * 8;
    }

    /** The full volume (mB): the most the lake holds. */
    public long capacity() {
        return volumeAt(highestEighth());
    }

    /**
     * The surface height (eighths) for the current amount: the lowest h that holds it, so the surface drops a step only
     * once a whole 1/8 layer has gone (AdventurAgent: two buckets lowered a full lake).
     */
    public int surface() {
        int lo = lowestEighth(), hi = highestEighth();
        if (amount <= 0) return lo;
        while (lo < hi) {
            int mid = Math.floorDiv(lo + hi, 2);
            if (volumeAt(mid) >= amount) hi = mid;
            else lo = mid + 1;
        }
        return lo;
    }

    /** The level a cell at y shows at surface h: 0 none, 1-8 eighths. */
    public static int cellLevel(int y, int h) {
        return Math.max(0, Math.min(8, h - y * 8));
    }

    @Override
    public String toString() {
        return "reservoir #" + id + " (" + kind + ", " + basinType + "): " + amount / 1000 + " of " + capacity() / 1000 + " m3, surface "
            + surface() / 8.0;
    }
}
