package mods.eln.reservoir;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A level's reservoirs (Reservoir), saved with it, with a chunk index from their basins. */
public final class ReservoirData extends SavedData {
    public static final Codec<ReservoirData> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("next_id").forGetter(d -> d.nextId),
        Reservoir.CODEC.listOf().fieldOf("reservoirs").forGetter(d -> new ArrayList<>(d.byId.values())),
        Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("keys", Map.of()).forGetter(d -> d.byKey)
    ).apply(i, ReservoirData::new));

    public static final SavedDataType<ReservoirData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("eln", "reservoirs"), ReservoirData::new, CODEC);

    private int nextId;
    private final Map<Integer, Reservoir> byId = new LinkedHashMap<>();
    private final Long2ObjectOpenHashMap<List<Reservoir>> byChunk = new Long2ObjectOpenHashMap<>();
    /** Reservoirs by their maker's key (an oil lake's start, "watershed:lake/1295"): each made once. */
    private final Map<String, Integer> byKey = new java.util.HashMap<>();
    private final List<Reservoir> unresolved = new ArrayList<>();

    public ReservoirData() {
        this(1, List.of(), Map.of());
    }

    private ReservoirData(int nextId, List<Reservoir> list, Map<String, Integer> keys) {
        this.nextId = nextId;
        for (Reservoir r : list) byId.put(r.id, r);
        unresolved.addAll(list);
        byKey.putAll(keys);
    }

    public static ReservoirData get(ServerLevel level) {
        ReservoirData d = level.getDataStorage().computeIfAbsent(TYPE);
        d.resolve(level);
        return d;
    }

    /** Basins for reservoirs loaded before their basin type was registered (or just loaded): build them, index them. */
    void resolve(ServerLevel level) {
        if (unresolved.isEmpty()) return;
        for (var it = unresolved.iterator(); it.hasNext(); ) {
            Reservoir r = it.next();
            Basin b = Reservoirs.basin(level, r.basinType, r.basinSpec);
            if (b == null) continue;
            r.basin = b;
            index(r);
            it.remove();
        }
    }

    private void index(Reservoir r) {
        for (long k : r.basin.chunks()) byChunk.computeIfAbsent(k, q -> new ArrayList<>(1)).add(r);
    }

    public Reservoir byKey(String key) {
        Integer id = byKey.get(key);
        return id == null ? null : byId.get(id);
    }

    /**
     * A new reservoir: its kind, its basin (type and spec, Reservoirs.registerBasin), the basin's area table (cells per
     * block level from minY: the estimate until chunks are swept), and its amount (mB, capped at its capacity).
     */
    public Reservoir create(ServerLevel level, String key, String kind, String basinType, CompoundTag spec, int minY, int[] area, long amount) {
        Reservoir r = new Reservoir(nextId++, kind, basinType, spec, minY, area.clone(), 0, -1, new LongOpenHashSet());
        r.amount = Math.max(0, Math.min(amount, r.capacity()));
        r.basin = Reservoirs.basin(level, basinType, spec);
        if (r.basin == null) throw new IllegalArgumentException("no basin type " + basinType);
        byId.put(r.id, r);
        index(r);
        if (key != null) byKey.put(key, r.id);
        setDirty();
        return r;
    }

    /** Remove the reservoirs whose maker's key matches (a maker replacing its lakes: Watershed after a recompute). */
    public int removeKeyed(java.util.function.Predicate<String> which) {
        List<Reservoir> gone = new ArrayList<>();
        byKey.forEach((k, id) -> {
            if (which.test(k) && byId.containsKey(id)) gone.add(byId.get(id));
        });
        for (Reservoir r : gone) remove(r);
        return gone.size();
    }

    public void remove(Reservoir r) {
        byId.remove(r.id);
        byKey.values().removeIf(v -> v == r.id);
        for (var list : byChunk.values()) list.remove(r);
        setDirty();
    }

    public Reservoir byId(int id) {
        return byId.get(id);
    }

    public Collection<Reservoir> all() {
        return byId.values();
    }

    public List<Reservoir> inChunk(int cx, int cz) {
        List<Reservoir> l = byChunk.get(net.minecraft.world.level.ChunkPos.pack(cx, cz));
        return l == null ? List.of() : l;
    }

    /** The reservoir the block is a cell of (in a swept chunk), or null. */
    public Reservoir at(ServerLevel level, BlockPos pos) {
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return null;
        ChunkCells.All all = chunk.getData(Reservoirs.CELLS);
        for (Reservoir r : inChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            ChunkCells c = all.byId.get(r.id);
            if (c != null && c.contains(pos.getX(), pos.getY(), pos.getZ())) return r;
        }
        return null;
    }

    void changed() {
        setDirty();
    }
}
