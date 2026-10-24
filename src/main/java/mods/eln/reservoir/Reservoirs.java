package mods.eln.reservoir;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiFunction;

/**
 * Keeping reservoirs' blocks in step with their records (RESERVOIR-PLAN-2.md): take/give change an amount; when its
 * surface moves, its loaded chunks are queued and swept a few per tick; a chunk that loads is swept too, and loaded
 * chunks every 10 s. The first sweep of a chunk asks the basin for its cells and corrects the area table by the
 * difference from the estimate. A cell holding something else is a dam: it leaves the area (the amount stays) and
 * fills again when the block goes (Baughn: for oil too).
 * <p>
 * Nothing here imports the rest of EA: kinds and basins are registered (oil: mods.eln.oil.Oil; water: this package,
 * ReservoirWater; Watershed's lakes: its own basin), so the package can become a small library later (Baughn).
 */
public final class Reservoirs {
    private Reservoirs() {
    }

    /** Chunk sweeps per tick. */
    public static int sweepsPerTick = 4;
    /** Ticks between sweeps of every loaded reservoir chunk. */
    static final int RESWEEP = 200;

    static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, "eln");
    /** A chunk's reservoir cells (ChunkCells), saved with the chunk. */
    public static final java.util.function.Supplier<AttachmentType<ChunkCells.All>> CELLS = ATTACHMENTS.register("reservoir_cells",
        () -> AttachmentType.builder(ChunkCells.All::new).serialize(ChunkCells.All.CODEC, a -> !a.isEmpty()).build());

    static final Map<String, ReservoirKind> KINDS = new HashMap<>();
    static final Map<String, BiFunction<ServerLevel, CompoundTag, Basin>> BASINS = new HashMap<>();

    /** A listener told of every take and give (a reservoir, the mB, + for give): Watershed's lakes keep their storage. */
    public interface Listener {
        void moved(ServerLevel level, Reservoir r, long mB);
    }

    static final java.util.List<Listener> LISTENERS = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void listen(Listener l) {
        LISTENERS.add(l);
    }

    public static void registerKind(String id, ReservoirKind k) {
        KINDS.put(id, k);
    }

    public static ReservoirKind kind(String id) {
        return KINDS.get(id);
    }

    public static void registerBasin(String type, BiFunction<ServerLevel, CompoundTag, Basin> f) {
        BASINS.put(type, f);
    }

    /** A basin of a registered type (tests, wrappers). */
    public static Basin basinOf(ServerLevel level, String type, CompoundTag spec) {
        return basin(level, type, spec);
    }

    static Basin basin(ServerLevel level, String type, CompoundTag spec) {
        var f = BASINS.get(type);
        return f == null ? null : f.apply(level, spec);
    }

    private record Job(int reservoir, int cx, int cz) {
    }

    private static final Map<ServerLevel, ArrayDeque<Job>> QUEUE = new WeakHashMap<>();
    private static final Map<ServerLevel, java.util.Set<Job>> QUEUED = new WeakHashMap<>();

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        registerBasin(ArrayBasin.TYPE, (l, t) -> new ArrayBasin(t));
        ReservoirWater.register(modBus);
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, Reservoirs::tick);
        NeoForge.EVENT_BUS.addListener(ChunkEvent.Load.class, Reservoirs::chunkLoad);
    }

    private static synchronized void enqueue(ServerLevel level, Job j) {
        if (QUEUED.computeIfAbsent(level, l -> new java.util.HashSet<>()).add(j))
            QUEUE.computeIfAbsent(level, l -> new ArrayDeque<>()).add(j);
    }

    private static synchronized Job poll(ServerLevel level) {
        ArrayDeque<Job> q = QUEUE.get(level);
        Job j = q == null ? null : q.poll();
        if (j != null) QUEUED.get(level).remove(j);
        return j;
    }

    /** Queue every loaded chunk of the reservoir for a sweep. */
    public static void refresh(ServerLevel level, Reservoir r) {
        if (r.basin == null) return;
        for (long k : r.basin.chunks())
            if (level.getChunkSource().getChunkNow(ChunkPos.getX(k), ChunkPos.getZ(k)) != null)
                enqueue(level, new Job(r.id, ChunkPos.getX(k), ChunkPos.getZ(k)));
    }

    /** Take up to mB (simulate: only say how much); returns what was (or would be) taken. */
    public static long take(ServerLevel level, Reservoir r, long mB, boolean simulate) {
        long t = Math.max(0, Math.min(mB, r.amount));
        if (!simulate && t > 0) {
            r.amount -= t;
            moved(level, r);
            for (Listener l : LISTENERS) l.moved(level, r, -t);
        }
        return t;
    }

    /** Give up to mB back (up to the capacity); returns what was (or would be) taken in. */
    public static long give(ServerLevel level, Reservoir r, long mB, boolean simulate) {
        long g = Math.max(0, Math.min(mB, r.capacity() - r.amount));
        if (!simulate && g > 0) {
            r.amount += g;
            moved(level, r);
            for (Listener l : LISTENERS) l.moved(level, r, g);
        }
        return g;
    }

    /** Set the amount (the owner's simulation: Watershed), no listeners told. */
    public static void setAmount(ServerLevel level, Reservoir r, long mB) {
        long a = Math.max(0, mB);
        if (a == r.amount) return;
        r.amount = a;
        moved(level, r);
    }

    /** Set the amount without sweeping or telling anyone (tests). */
    public static void setAmountQuiet(Reservoir r, long mB) {
        r.amount = Math.max(0, mB);
    }

    private static void moved(ServerLevel level, Reservoir r) {
        ReservoirData.get(level).changed();
        if (r.surface() != r.shown) refresh(level, r);
    }

    /** Take 1000 mB for a bucket from the reservoir whose cell is at pos, if any: true if it held enough, or if the
     *  block isn't a reservoir cell (vanilla behaviour); false: the lake is (nearly) empty, no bucket. */
    public static boolean bucket(ServerLevel level, BlockPos pos) {
        Reservoir r = ReservoirData.get(level).at(level, pos);
        if (r == null) return true;
        if (take(level, r, 1000, true) < 1000) return false;
        take(level, r, 1000, false);
        return true;
    }

    static void chunkLoad(ChunkEvent.Load e) {
        if (!(e.getLevel() instanceof ServerLevel level) || !level.getServer().isSameThread()) return;
        ChunkPos cp = e.getChunk().getPos();
        for (Reservoir r : ReservoirData.get(level).inChunk(cp.x(), cp.z())) enqueue(level, new Job(r.id, cp.x(), cp.z()));
    }

    static long sweeps, sweepNanos;

    public static String debug() {
        return sweeps + " sweeps, " + (sweeps == 0 ? 0 : sweepNanos / 1000 / sweeps) + " us each";
    }

    static void tick(ServerTickEvent.Post e) {
        try {
            for (ServerLevel level : e.getServer().getAllLevels()) {
                if (level.getGameTime() % RESWEEP == 0) {
                    var data = level.getDataStorage().get(ReservoirData.TYPE);
                    if (data != null) for (Reservoir r : data.all()) refresh(level, r);
                }
                for (int n = 0; n < sweepsPerTick; n++) {
                    Job j = poll(level);
                    if (j == null) break;
                    Reservoir r = ReservoirData.get(level).byId(j.reservoir());
                    if (r == null) continue;
                    long t0 = System.nanoTime();
                    sweep(level, r, j.cx(), j.cz());
                    sweepNanos += System.nanoTime() - t0;
                    sweeps++;
                }
            }
        } catch (Throwable t) {
            org.slf4j.LoggerFactory.getLogger("eln").error("Reservoirs: tick failed", t);
        }
    }

    /** First sweep of a chunk: its cells from the basin, and the area table corrected from the estimate to the truth. */
    static ChunkCells calibrate(ServerLevel level, Reservoir r, LevelChunk chunk) {
        ChunkCells c = new ChunkCells();
        r.basin.cells(level, chunk, c.bottom, c.top);
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int k = 0; k < 256; k++)
            if (c.bottom[k] != Basin.NONE && c.top[k] > c.bottom[k]) {
                lo = Math.min(lo, c.bottom[k]);
                hi = Math.max(hi, c.top[k]);
            }
        int cx = chunk.getPos().x(), cz = chunk.getPos().z();
        int[] est = r.basin.estimate(cx, cz, r.minY, r.area.length);
        if (est != null) for (int k = 0; k < est.length; k++) r.addArea(r.minY + k, -est[k]);
        for (int k = 0; k < 256; k++)
            if (c.bottom[k] != Basin.NONE) for (int y = c.bottom[k]; y < c.top[k]; y++) r.addArea(y, 1);
        r.calibrated.add(ChunkPos.pack(cx, cz));
        return c;
    }

    /** Bring one chunk of a reservoir in line with its record (public for tests). */
    public static void sweep(ServerLevel level, Reservoir r, int cx, int cz) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
        ReservoirKind kind = KINDS.get(r.kind);
        if (chunk == null || kind == null || r.basin == null) return;
        ChunkCells.All all = chunk.getData(CELLS);
        ChunkCells c = all.byId.get(r.id);
        ReservoirData data = ReservoirData.get(level);
        boolean reshape = false;
        if (c == null) {
            if (r.calibrated.contains(ChunkPos.pack(cx, cz))) return;   // (no cells here)
            c = calibrate(level, r, chunk);
            all.byId.put(r.id, c);
            chunk.setData(CELLS, all);
            chunk.markUnsaved();
            reshape = true;
        }
        int h = r.surface();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        boolean changed = false;
        for (int k = 0; k < 256; k++) {
            if (c.bottom[k] == Basin.NONE) continue;
            int x = (cx << 4) + (k >> 4), z = (cz << 4) + (k & 15);
            for (int y = c.bottom[k]; y < c.top[k]; y++) {
                p.set(x, y, z);
                BlockState cur = chunk.getBlockState(p);
                int lvl = Reservoir.cellLevel(y, h);
                long key = p.asLong();
                boolean dam = c.dams.contains(key);
                BlockState want;
                if (kind.ours(cur) || kind.fillable(cur)) {
                    want = lvl == 0 ? (kind.ours(cur) ? air : cur) : kind.matches(cur, lvl) ? cur : kind.cell(lvl);
                } else if (kind.waterlogs() && ReservoirKind.waterloggable(cur)) {
                    // part of the lake: holds water where the lake reaches it (Baughn: waterloggable blocks waterlog)
                    want = cur.setValue(BlockStateProperties.WATERLOGGED, lvl >= 4);
                } else {
                    long poured = kind.poured(cur);
                    if (poured > 0) {
                        r.amount = Math.min(r.capacity(), r.amount + poured);
                        reshape = true;
                        want = lvl == 0 ? air : kind.cell(lvl);
                    } else {
                        if (!dam) {
                            c.dams.add(key);
                            r.addArea(y, -1);
                            reshape = true;
                        }
                        continue;
                    }
                }
                if (dam) {
                    c.dams.remove(key);
                    r.addArea(y, 1);
                    reshape = true;
                }
                if (want != cur) {
                    level.setBlock(p, want, 2 | 16);
                    changed = true;
                }
            }
        }
        if (reshape) {
            chunk.markUnsaved();
            data.changed();
            if (r.surface() != h) refresh(level, r);
        }
        if (changed) chunk.markUnsaved();
        if (r.shown != h) {
            r.shown = h;
            data.changed();
        }
    }
}
