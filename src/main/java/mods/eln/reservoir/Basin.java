package mods.eln.reservoir;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * A reservoir's natural cavity, per chunk (plan: notes/hydro/RESERVOIR-PLAN-2.md). The record never stores or walks
 * per-column geometry: the cavity of a chunk is asked for once, the first time the chunk is swept, and kept with the
 * chunk (ChunkCells). Until then the record's level->area table holds the basin's estimate for that chunk.
 */
public interface Basin {
    int NONE = Short.MIN_VALUE;

    /** The chunks (ChunkPos.pack) the cavity may reach into. */
    long[] chunks();

    /**
     * The chunk's cavity as generated: per local column (index (x & 15) * 16 + (z & 15)), its lowest cell (bottom,
     * inclusive) and top (exclusive), or NONE. Called once per chunk, the first time it is swept.
     */
    void cells(ServerLevel level, LevelChunk chunk, short[] bottom, short[] top);

    /** What the record's area table already counts for this chunk, per block level from minY (null: nothing). */
    int[] estimate(int cx, int cz, int minY, int height);
}
