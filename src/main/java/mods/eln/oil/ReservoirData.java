package mods.eln.oil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import mods.eln.Eln;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The oil lakes of a level (Reservoir), saved with it, with a chunk index for lookups. */
public final class ReservoirData extends SavedData {
    public static final Codec<ReservoirData> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("next_id").forGetter(d -> d.nextId),
        Reservoir.CODEC.listOf().fieldOf("reservoirs").forGetter(d -> new ArrayList<>(d.byId.values()))
    ).apply(i, ReservoirData::new));

    public static final SavedDataType<ReservoirData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(Eln.MODID, "oil_reservoirs"), ReservoirData::new, CODEC);

    private int nextId;
    private final Map<Integer, Reservoir> byId = new LinkedHashMap<>();
    private final Long2ObjectOpenHashMap<List<Reservoir>> byChunk = new Long2ObjectOpenHashMap<>();

    public ReservoirData() {
        this(1, List.of());
    }

    private ReservoirData(int nextId, List<Reservoir> list) {
        this.nextId = nextId;
        for (Reservoir r : list) add(r);
    }

    public static ReservoirData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private void add(Reservoir r) {
        byId.put(r.id, r);
        for (int cx = r.minX >> 4; cx <= (r.minX + r.sizeX - 1) >> 4; cx++)
            for (int cz = r.minZ >> 4; cz <= (r.minZ + r.sizeZ - 1) >> 4; cz++)
                byChunk.computeIfAbsent(ChunkPos.pack(cx, cz), k -> new ArrayList<>(1)).add(r);
    }

    /** A new lake (worldgen, tests): columns' bottom cells and masks over a box; amount in mB. */
    public Reservoir create(int minX, int minZ, int sizeX, int sizeZ, int[] floor, int[] mask, long amount) {
        Reservoir r = new Reservoir(nextId++, minX, minZ, sizeX, sizeZ, floor, mask, 0, -1);
        r.amount = Math.max(0, Math.min(amount, r.capacity()));
        add(r);
        setDirty();
        return r;
    }

    public void remove(Reservoir r) {
        byId.remove(r.id);
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
        List<Reservoir> l = byChunk.get(ChunkPos.pack(cx, cz));
        return l == null ? List.of() : l;
    }

    /** The lake the block is a cell of, or null. */
    public Reservoir at(BlockPos pos) {
        for (Reservoir r : inChunk(pos.getX() >> 4, pos.getZ() >> 4))
            if (r.contains(pos.getX(), pos.getY(), pos.getZ())) return r;
        return null;
    }

    void changed() {
        setDirty();
    }
}
