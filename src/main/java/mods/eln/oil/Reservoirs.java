package mods.eln.oil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Keeping oil lakes' blocks in step with their records (design v2 §2): take/give change a lake's amount; when its
 * surface moves, its loaded chunks are queued and swept a few per tick, and a chunk that loads is swept too (so
 * unloaded parts catch up; a lake's chunks never need to stay loaded). The sweep only writes lake cells that hold
 * reservoir oil or air. Crude oil poured into a cell is taken into the lake (+1000 mB per source block); any other
 * block found in a cell is a dam: that cell leaves the lake.
 */
public final class Reservoirs {
    private Reservoirs() {
    }

    /** Chunk sweeps per tick (each up to 256 columns of a lake). */
    static final int SWEEPS_PER_TICK = 4;

    private record Job(int reservoir, int cx, int cz) {
    }

    private static final Map<ServerLevel, ArrayDeque<Job>> QUEUE = new WeakHashMap<>();

    static void register() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, Reservoirs::tick);
        NeoForge.EVENT_BUS.addListener(ChunkEvent.Load.class, Reservoirs::chunkLoad);
    }

    private static synchronized void enqueue(ServerLevel level, Job j) {
        ArrayDeque<Job> q = QUEUE.computeIfAbsent(level, l -> new ArrayDeque<>());
        if (!q.contains(j)) q.add(j);
    }

    /** Queue every loaded chunk of the lake for a sweep. */
    public static void refresh(ServerLevel level, Reservoir r) {
        for (int cx = r.minX >> 4; cx <= (r.minX + r.sizeX - 1) >> 4; cx++)
            for (int cz = r.minZ >> 4; cz <= (r.minZ + r.sizeZ - 1) >> 4; cz++)
                if (level.getChunkSource().getChunkNow(cx, cz) != null) enqueue(level, new Job(r.id, cx, cz));
    }

    /** Take up to mB from the lake (simulate: only say how much); returns what was (or would be) taken. */
    public static long take(ServerLevel level, Reservoir r, long mB, boolean simulate) {
        long t = Math.min(mB, r.amount);
        if (!simulate && t > 0) {
            r.amount -= t;
            moved(level, r);
        }
        return t;
    }

    /** Give up to mB back to the lake (up to its capacity); returns what was (or would be) taken in. */
    public static long give(ServerLevel level, Reservoir r, long mB, boolean simulate) {
        long g = Math.max(0, Math.min(mB, r.capacity() - r.amount));
        if (!simulate && g > 0) {
            r.amount += g;
            moved(level, r);
        }
        return g;
    }

    private static void moved(ServerLevel level, Reservoir r) {
        ReservoirData.get(level).changed();
        if (r.surface() != r.shown) refresh(level, r);
    }

    static void chunkLoad(ChunkEvent.Load e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        ChunkPos cp = e.getChunk().getPos();
        // (data storage: main thread only; chunk loads come in on the main thread)
        if (!level.getServer().isSameThread()) return;
        for (Reservoir r : ReservoirData.get(level).inChunk(cp.x(), cp.z())) enqueue(level, new Job(r.id, cp.x(), cp.z()));
    }

    static void tick(ServerTickEvent.Post e) {
        for (ServerLevel level : e.getServer().getAllLevels()) {
            // every 10 s re-check loaded lakes: crude poured in, blocks placed into them (dams)
            if (level.getGameTime() % 200 == 0) {
                var data = level.getDataStorage().get(ReservoirData.TYPE);
                if (data != null) for (Reservoir r : data.all()) refresh(level, r);
            }
            ArrayDeque<Job> q;
            synchronized (Reservoirs.class) {
                q = QUEUE.get(level);
            }
            if (q == null) continue;
            for (int n = 0; n < SWEEPS_PER_TICK; n++) {
                Job j;
                synchronized (Reservoirs.class) {
                    j = q.poll();
                }
                if (j == null) break;
                Reservoir r = ReservoirData.get(level).byId(j.reservoir());
                if (r != null) sweep(level, r, j.cx(), j.cz());
            }
        }
    }

    /** Bring one chunk of a lake in line with its record (public for tests). */
    public static void sweep(ServerLevel level, Reservoir r, int cx, int cz) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
        if (chunk == null) return;
        int h = r.surface();
        boolean reshape = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = Math.max(r.minX, cx << 4); x < Math.min(r.minX + r.sizeX, (cx << 4) + 16); x++)
            for (int z = Math.max(r.minZ, cz << 4); z < Math.min(r.minZ + r.sizeZ, (cz << 4) + 16); z++) {
                int i = r.index(x, z);
                if (r.floor[i] == Reservoir.NONE) continue;
                int m = r.mask[i];
                while (m != 0) {
                    int b = Integer.numberOfTrailingZeros(m);
                    m &= m - 1;
                    int y = r.floor[i] + b;
                    p.set(x, y, z);
                    BlockState cur = chunk.getBlockState(p);
                    FluidState fs = cur.getFluidState();
                    boolean ours = cur.is(Oil.RESERVOIR_BLOCK.get());
                    if (!ours && !cur.isAir()) {
                        if (fs.is(Oil.CRUDE.get()) && fs.isSource()) {
                            // crude poured back into the lake: it joins the record
                            r.amount = Math.min(r.capacity(), r.amount + 1000);
                            reshape = true;
                        } else if (!fs.is(Oil.CRUDE_FLOWING.get())) {
                            // a dam: the cell leaves the lake (the oil it held stays in the record)
                            r.block(x, y, z);
                            reshape = true;
                            continue;
                        }
                    }
                    int lvl = Reservoir.cellLevel(y, h);
                    BlockState want = lvl == 0 ? air : Oil.RESERVOIR_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.LEVEL_FLOWING, lvl);
                    if (cur != want) level.setBlock(p, want, 2 | 16);
                }
            }
        if (reshape) {
            r.amount = Math.min(r.amount, r.capacity());
            ReservoirData.get(level).changed();
            if (r.surface() != h) refresh(level, r);
        }
        if (r.shown != h) {
            r.shown = h;
            ReservoirData.get(level).changed();
        }
    }
}
