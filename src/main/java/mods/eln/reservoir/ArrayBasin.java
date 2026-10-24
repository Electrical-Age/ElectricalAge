package mods.eln.reservoir;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Arrays;

/**
 * A small cavity given whole (EA's oil lakes, tests): a box of columns, each a bottom cell and a height (cells bottom ..
 * bottom + height - 1). Exact, so its estimate is the truth. Spec: min_x, min_z, size_x, size_z, bottom[], height[].
 */
public final class ArrayBasin implements Basin {
    public static final String TYPE = "eln:array";
    final int minX, minZ, sizeX, sizeZ;
    final int[] bottom, height;

    ArrayBasin(CompoundTag t) {
        minX = t.getIntOr("min_x", 0);
        minZ = t.getIntOr("min_z", 0);
        sizeX = t.getIntOr("size_x", 0);
        sizeZ = t.getIntOr("size_z", 0);
        bottom = t.getIntArray("bottom").orElse(new int[0]);
        height = t.getIntArray("height").orElse(new int[0]);
    }

    /** The spec for a box of columns (bottom: NONE for none). */
    public static CompoundTag spec(int minX, int minZ, int sizeX, int sizeZ, int[] bottom, int[] height) {
        CompoundTag t = new CompoundTag();
        t.putInt("min_x", minX);
        t.putInt("min_z", minZ);
        t.putInt("size_x", sizeX);
        t.putInt("size_z", sizeZ);
        t.putIntArray("bottom", bottom);
        t.putIntArray("height", height);
        return t;
    }

    /** The whole cavity's cells per level: {minY, counts...} for ReservoirData.create. */
    public static int[][] area(int[] bottom, int[] height) {
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int i = 0; i < bottom.length; i++)
            if (bottom[i] != NONE && height[i] > 0) {
                lo = Math.min(lo, bottom[i]);
                hi = Math.max(hi, bottom[i] + height[i]);
            }
        if (lo > hi) return new int[][]{{0}, new int[0]};
        int[] a = new int[hi - lo];
        for (int i = 0; i < bottom.length; i++)
            if (bottom[i] != NONE) for (int k = 0; k < height[i]; k++) a[bottom[i] - lo + k]++;
        return new int[][]{{lo}, a};
    }

    @Override
    public long[] chunks() {
        long[] out = new long[((minX + sizeX - 1 >> 4) - (minX >> 4) + 1) * ((minZ + sizeZ - 1 >> 4) - (minZ >> 4) + 1)];
        int n = 0;
        for (int cx = minX >> 4; cx <= (minX + sizeX - 1) >> 4; cx++)
            for (int cz = minZ >> 4; cz <= (minZ + sizeZ - 1) >> 4; cz++) out[n++] = ChunkPos.pack(cx, cz);
        return out;
    }

    @Override
    public void cells(ServerLevel level, LevelChunk chunk, short[] bot, short[] top) {
        int cx = chunk.getPos().x(), cz = chunk.getPos().z();
        for (int x = cx << 4; x < (cx << 4) + 16; x++)
            for (int z = cz << 4; z < (cz << 4) + 16; z++) {
                int dx = x - minX, dz = z - minZ;
                if (dx < 0 || dz < 0 || dx >= sizeX || dz >= sizeZ) continue;
                int i = dx * sizeZ + dz;
                if (bottom[i] == NONE || height[i] <= 0) continue;
                bot[(x & 15) * 16 + (z & 15)] = (short) bottom[i];
                top[(x & 15) * 16 + (z & 15)] = (short) (bottom[i] + height[i]);
            }
    }

    @Override
    public int[] estimate(int cx, int cz, int minY, int h) {
        int[] out = new int[h];
        for (int x = cx << 4; x < (cx << 4) + 16; x++)
            for (int z = cz << 4; z < (cz << 4) + 16; z++) {
                int dx = x - minX, dz = z - minZ;
                if (dx < 0 || dz < 0 || dx >= sizeX || dz >= sizeZ) continue;
                int i = dx * sizeZ + dz;
                if (bottom[i] == NONE) continue;
                for (int k = 0; k < height[i]; k++) {
                    int y = bottom[i] + k - minY;
                    if (y >= 0 && y < h) out[y]++;
                }
            }
        return out;
    }

    @Override
    public String toString() {
        return "box x " + minX + ".." + (minX + sizeX - 1) + ", z " + minZ + ".." + (minZ + sizeZ - 1) + " " + Arrays.stream(height).sum() + " cells";
    }
}
