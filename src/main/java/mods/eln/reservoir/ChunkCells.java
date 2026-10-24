package mods.eln.reservoir;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A chunk's cells of each reservoir it holds (a chunk data attachment): per local column the cavity's bottom and top
 * (Basin.cells, fixed when the chunk was first swept) and the cells now dammed (a foreign block in them: they leave the
 * lake's area until the block goes, then refill).
 */
public final class ChunkCells {
    final short[] bottom = new short[256], top = new short[256];
    final LongOpenHashSet dams = new LongOpenHashSet();

    ChunkCells() {
        Arrays.fill(bottom, (short) Basin.NONE);
    }

    /** Is (x, y, z) a cell of the cavity (dammed or not)? */
    boolean contains(int x, int y, int z) {
        int i = (x & 15) * 16 + (z & 15);
        return bottom[i] != Basin.NONE && y >= bottom[i] && y < top[i];
    }

    /** All reservoirs' cells in one chunk, by reservoir id. */
    public static final class All {
        final Int2ObjectOpenHashMap<ChunkCells> byId = new Int2ObjectOpenHashMap<>();

        record Entry(int id, List<Integer> bottom, List<Integer> top, List<Long> dams) {
        }

        static final Codec<Entry> ENTRY = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("id").forGetter(Entry::id),
            Codec.INT.listOf().fieldOf("bottom").forGetter(Entry::bottom),
            Codec.INT.listOf().fieldOf("top").forGetter(Entry::top),
            Codec.LONG.listOf().optionalFieldOf("dams", List.of()).forGetter(Entry::dams)
        ).apply(i, Entry::new));

        public static final MapCodec<All> CODEC = ENTRY.listOf().fieldOf("reservoirs").xmap(All::of, All::entries);

        static All of(List<Entry> es) {
            All a = new All();
            for (Entry e : es) {
                ChunkCells c = new ChunkCells();
                for (int k = 0; k < 256 && k < e.bottom().size(); k++) {
                    c.bottom[k] = (short) (int) e.bottom().get(k);
                    c.top[k] = (short) (int) e.top().get(k);
                }
                c.dams.addAll(e.dams());
                a.byId.put(e.id(), c);
            }
            return a;
        }

        List<Entry> entries() {
            List<Entry> out = new ArrayList<>();
            for (var en : byId.int2ObjectEntrySet()) {
                ChunkCells c = en.getValue();
                List<Integer> b = new ArrayList<>(256), t = new ArrayList<>(256);
                for (int k = 0; k < 256; k++) {
                    b.add((int) c.bottom[k]);
                    t.add((int) c.top[k]);
                }
                out.add(new Entry(en.getIntKey(), b, t, new ArrayList<>(c.dams)));
            }
            return out;
        }

        public boolean isEmpty() {
            return byId.isEmpty();
        }
    }
}
