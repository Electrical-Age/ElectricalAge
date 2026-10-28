package mods.eln.oil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mods.eln.reservoir.ArrayBasin;
import mods.eln.reservoir.Basin;
import mods.eln.reservoir.Reservoir;
import mods.eln.reservoir.ReservoirData;
import mods.eln.reservoir.ReservoirKind;
import mods.eln.reservoir.Reservoirs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Oil lakes on the reservoir package (mods.eln.reservoir): the oil kind, records for generated lakes (an ArrayBasin
 * from LakeShape), and the upgrade of saves from before it (the eln:oil_reservoirs data, dense per-column masks).
 */
public final class OilReservoirs {
    private OilReservoirs() {
    }

    public static final String KIND = "oil";

    static void register() {
        Reservoirs.registerKind(KIND, new ReservoirKind() {
            @Override
            public BlockState cell(int level) {
                return Oil.RESERVOIR_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.LEVEL_FLOWING, Math.max(1, Math.min(8, level)));
            }

            @Override
            public boolean ours(BlockState s) {
                return s.is(Oil.RESERVOIR_BLOCK.get()) || s.is(Oil.CRUDE_BLOCK.get()) && !s.getFluidState().isSource();
            }

            @Override
            public boolean fillable(BlockState s) {
                return s.isAir();
            }

            @Override
            public long poured(BlockState s) {
                return s.is(Oil.CRUDE_BLOCK.get()) && s.getFluidState().isSource() ? 1000 : 0;
            }
        });
    }

    /** A lake from columns given as a bottom cell and a mask of cells above it (as LakeShape and the old record had
     *  them; a mask is taken up to its top bit); amount in mB. */
    public static Reservoir create(ServerLevel level, String key, int minX, int minZ, int sizeX, int sizeZ, int[] floor, int[] mask, long amount) {
        int[] bottom = new int[floor.length], height = new int[floor.length];
        for (int i = 0; i < floor.length; i++) {
            bottom[i] = floor[i] == Integer.MIN_VALUE || mask[i] == 0 ? Basin.NONE : floor[i];
            height[i] = bottom[i] == Basin.NONE ? 0 : 32 - Integer.numberOfLeadingZeros(mask[i]);
        }
        int[][] a = ArrayBasin.area(bottom, height);
        return ReservoirData.get(level).create(level, key, KIND, ArrayBasin.TYPE, ArrayBasin.spec(minX, minZ, sizeX, sizeZ, bottom, height),
            a[0][0], a[1], amount);
    }

    /** Main thread: the record of a generated lake, made once (keyed by the lake's start chunk). */
    static Reservoir make(ServerLevel level, LakeShape shape) {
        String key = "oil:" + shape.key();
        Reservoir r = ReservoirData.get(level).byKey(key);
        if (r != null) return r;
        int[][] cols = shape.columns();
        r = create(level, key, shape.minX(), shape.minZ(), shape.sizeX(), shape.sizeZ(), cols[0], cols[1], Long.MAX_VALUE);
        mods.eln.Eln.LOG.info("Oil lake #{} made at {} {} (base y {}): {} buckets", r.id(), shape.cx(), shape.cz(), shape.baseY(), r.capacity() / 1000);
        return r;
    }

    private static final java.util.concurrent.ConcurrentLinkedQueue<Object[]> PENDING = new java.util.concurrent.ConcurrentLinkedQueue<>();

    /** Worldgen (any thread): make sure the generated lake has its record; made on the main thread, next tick. */
    public static void ensure(ServerLevel level, LakeShape shape) {
        PENDING.add(new Object[]{level, shape});
    }

    /** Main thread, each tick: pending lakes made; old saves upgraded once. */
    static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) {
        try {
            for (ServerLevel level : e.getServer().getAllLevels()) upgrade(level);
            for (Object[] p; (p = PENDING.poll()) != null; ) {
                Reservoir r = make((ServerLevel) p[0], (LakeShape) p[1]);
                Reservoirs.refresh((ServerLevel) p[0], r);
            }
            OilPonds.tick(e);
        } catch (Throwable t) {
            mods.eln.Eln.LOG.error("Oil: reservoir tick failed", t);
        }
    }

    // ---- saves from before the reservoir package: eln:oil_reservoirs (dense floor/mask per lake), upgraded once

    record Old(int id, int minX, int minZ, int sizeX, int sizeZ, int[] floor, int[] mask, long amount) {
    }

    static final Codec<Old> OLD = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("id").forGetter(Old::id),
        Codec.INT.fieldOf("min_x").forGetter(Old::minX),
        Codec.INT.fieldOf("min_z").forGetter(Old::minZ),
        Codec.INT.fieldOf("size_x").forGetter(Old::sizeX),
        Codec.INT.fieldOf("size_z").forGetter(Old::sizeZ),
        Codec.INT_STREAM.xmap(s -> s.toArray(), Arrays::stream).fieldOf("floor").forGetter(Old::floor),
        Codec.INT_STREAM.xmap(s -> s.toArray(), Arrays::stream).fieldOf("mask").forGetter(Old::mask),
        Codec.LONG.fieldOf("amount").forGetter(Old::amount)
    ).apply(i, Old::new));

    /** The old data, read once and emptied (kept as an empty file so the upgrade isn't repeated). */
    static final class OldData extends SavedData {
        List<Old> lakes;
        Map<String, Integer> keys;

        OldData(List<Old> lakes, Map<String, Integer> keys) {
            this.lakes = new java.util.ArrayList<>(lakes);
            this.keys = new java.util.HashMap<>(keys);
        }

        static final Codec<OldData> CODEC = RecordCodecBuilder.create(i -> i.group(
            OLD.listOf().optionalFieldOf("reservoirs", List.of()).forGetter(d -> d.lakes),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("keys", Map.of()).forGetter(d -> d.keys)
        ).apply(i, OldData::new));
    }

    static final SavedDataType<OldData> OLD_TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath("eln", "oil_reservoirs"),
        () -> new OldData(List.of(), Map.of()), OldData.CODEC);

    static final java.util.Set<ServerLevel> UPGRADED = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    static void upgrade(ServerLevel level) {
        if (!UPGRADED.add(level)) return;
        OldData old = level.getDataStorage().get(OLD_TYPE);
        if (old == null || old.lakes.isEmpty()) return;
        Map<Integer, String> keyOf = new java.util.HashMap<>();
        old.keys.forEach((k, v) -> keyOf.put(v, "oil:" + k));
        for (Old o : old.lakes) {
            String key = keyOf.get(o.id());
            if (key != null && ReservoirData.get(level).byKey(key) != null) continue;
            // (dammed cells, cleared bits under the top one, are cells again: the sweep finds the dams and they
            // refill when the block goes, the new rule)
            Reservoir r = create(level, key, o.minX(), o.minZ(), o.sizeX(), o.sizeZ(), o.floor(), o.mask(), o.amount());
            mods.eln.Eln.LOG.info("Oil lake #{} upgraded to reservoir #{}: {} of {} buckets", o.id(), r.id(), r.amount() / 1000, r.capacity() / 1000);
        }
        old.lakes.clear();
        old.keys.clear();
        old.setDirty();
    }
}
