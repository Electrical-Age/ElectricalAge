package mods.eln.oil;

import mods.eln.reservoir.Reservoir;
import mods.eln.reservoir.ReservoirData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.function.LongPredicate;

/**
 * Where the oil lakes are, without loading or generating a chunk (todo #470: oil survey maps). The lakes are a
 * structure set with random-spread placement (data/eln/worldgen/structure_set/oil_lakes.json): each grid cell of
 * `spacing` chunks has one start chunk, a pure function of the world seed, and the lake's centre is that chunk's
 * middle block (OilLakeStructure.findGenerationPoint). So the lakes in a ring around a point are a short loop.
 * <p>
 * A survey picks uniformly among the lakes in a ring (not the nearest: players cluster at spawn and would all get the
 * same lake), skipping lakes known to be mostly pumped out.
 */
public final class OilSurvey {
    private OilSurvey() {
    }

    public static final Identifier LAKES = Identifier.fromNamespaceAndPath("eln", "oil_lakes");
    /** A lake with less than this part of its oil left isn't surveyed. */
    public static final double WORTH = 0.25;

    /** The start chunk of grid cell (gx, gz), packed as ChunkPos.pack does (x low, z high). */
    public interface Cells {
        long start(int gx, int gz);
    }

    public static long pack(int cx, int cz) {
        return (cx & 0xffffffffL) | ((cz & 0xffffffffL) << 32);
    }

    public static int cx(long p) {
        return (int) p;
    }

    public static int cz(long p) {
        return (int) (p >> 32);
    }

    /** A lake's centre block (x or z) from its start chunk coordinate. */
    public static int middle(int chunk) {
        return (chunk << 4) + 8;
    }

    /** The start chunks whose lake centre is minR..maxR blocks (inclusive) from (ox, oz), in a fixed order. */
    public static List<Long> candidates(int spacing, Cells cells, int ox, int oz, int minR, int maxR) {
        List<Long> out = new ArrayList<>();
        int gx0 = Math.floorDiv((ox - maxR) >> 4, spacing), gx1 = Math.floorDiv((ox + maxR) >> 4, spacing);
        int gz0 = Math.floorDiv((oz - maxR) >> 4, spacing), gz1 = Math.floorDiv((oz + maxR) >> 4, spacing);
        long lo = (long) minR * minR, hi = (long) maxR * maxR;
        for (int gx = gx0; gx <= gx1; gx++)
            for (int gz = gz0; gz <= gz1; gz++) {
                long p = cells.start(gx, gz);
                long dx = middle(cx(p)) - ox, dz = middle(cz(p)) - oz;
                long d2 = dx * dx + dz * dz;
                if (d2 >= lo && d2 <= hi) out.add(p);
            }
        return out;
    }

    /**
     * One lake's start chunk, uniformly among those in the ring that pass `ok` (asked of as few as it takes: lakes
     * are drawn in random order until one passes); when the ring has none, the ring is widened (inner edge dropped,
     * outer doubled, twice). nextInt(n) gives 0..n-1. Returns Long.MIN_VALUE for none.
     */
    public static long pick(int spacing, Cells cells, int ox, int oz, int minR, int maxR, LongPredicate ok, IntUnaryOperator nextInt) {
        java.util.Set<Long> failed = new java.util.HashSet<>();
        for (int widen = 0; widen < 3; widen++) {
            List<Long> c = candidates(spacing, cells, ox, oz, widen == 0 ? minR : 0, maxR << widen);
            c.removeAll(failed);
            while (!c.isEmpty()) {
                long p = c.remove(nextInt.applyAsInt(c.size()));
                if (ok.test(p)) return p;
                failed.add(p);
            }
        }
        return Long.MIN_VALUE;
    }

    /** The lake structure set of this level's generator, or null (no oil lakes in this dimension, or a datapack made
     *  their placement something other than random-spread). */
    public static StructureSet lakes(ServerLevel level) {
        for (Holder<StructureSet> h : level.getChunkSource().getGeneratorState().possibleStructureSets())
            if (h.unwrapKey().map(k -> k.identifier().equals(LAKES)).orElse(false)
                && h.value().placement() instanceof RandomSpreadStructurePlacement) return h.value();
        return null;
    }

    /**
     * Is there, or will there be, a lake at this start chunk? False where the chunk was generated without one (a
     * world older than the lakes, or a datapack changed the spacing since) or the structure can't generate there.
     * Vanilla's check, as /locate uses it: reads the saved chunk's structure starts from the region file (cached), or
     * for a chunk not yet generated asks the structure for its generation point; no chunk is loaded or generated.
     */
    public static boolean present(ServerLevel level, StructureSet set, long p) {
        ChunkPos pos = new ChunkPos(cx(p), cz(p));
        for (StructureSet.StructureSelectionEntry e : set.structures())
            if (level.structureManager().checkStructurePresence(pos, e.structure().value(), set.placement(), false)
                != net.minecraft.world.level.levelgen.structure.StructureCheckResult.START_NOT_PRESENT) return true;
        return false;
    }

    /** A lake's record key from its start chunk (as OilLakeStructure makes LakeShape.key). */
    public static String key(long p) {
        return "oil:" + (((long) cx(p) << 32) ^ (cz(p) & 0xffffffffL));
    }

    /** False for a lake whose record says it is mostly pumped out; true for one never generated (no record yet). */
    public static boolean worth(ServerLevel level, long p) {
        Reservoir r = ReservoirData.get(level).byKey(key(p));
        return r == null || r.amount() >= r.capacity() * WORTH;
    }

    /** The centre (x, z; y 0) of a lake minR..maxR blocks from origin, picked at random among those that exist (or
     *  will, once generated) and aren't pumped out; null for none. No chunk is loaded or generated. */
    public static BlockPos pick(ServerLevel level, BlockPos origin, int minR, int maxR, RandomSource rnd) {
        StructureSet set = lakes(level);
        return set == null ? null : pick(level, set, origin, minR, maxR, rnd, q -> worth(level, q) && present(level, set, q));
    }

    /** As pick, for a given structure set and filter (tests: no lake can generate in the flat test world). */
    public static BlockPos pick(ServerLevel level, StructureSet set, BlockPos origin, int minR, int maxR, RandomSource rnd, LongPredicate ok) {
        RandomSpreadStructurePlacement pl = (RandomSpreadStructurePlacement) set.placement();
        long seed = level.getChunkSource().getGeneratorState().getLevelSeed();
        Cells cells = (gx, gz) -> {
            ChunkPos c = pl.getPotentialStructureChunk(seed, gx * pl.spacing(), gz * pl.spacing());
            return pack(c.x(), c.z());
        };
        long p = pick(pl.spacing(), cells, origin.getX(), origin.getZ(), minR, maxR, ok, rnd::nextInt);
        return p == Long.MIN_VALUE ? null : new BlockPos(middle(cx(p)), 0, middle(cz(p)));
    }
}
