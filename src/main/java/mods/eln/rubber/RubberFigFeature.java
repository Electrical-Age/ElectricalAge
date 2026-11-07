package mods.eln.rubber;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The rubber fig, Ficus elastica (Baughn 2026-11-07: taller than Hevea, with aerial roots, and far more recognisable): a
 * tall trunk with buttress fins at its foot that splits into three or four leaders leaning out, a broad umbrella crown of
 * big glossy leaves, and aerial roots hanging from the branches; some reach the ground and stand as pillar trunks.
 * Everything stays within 8 blocks of the trunk (a feature may write into the chunks round its own).
 */
public record RubberFigFeature() implements Feature {
    public static final MapCodec<RubberFigFeature> CODEC = MapCodec.unit(RubberFigFeature::new);

    @Override
    public MapCodec<RubberFigFeature> codec() {
        return CODEC;
    }

    /** Reach from the trunk, any direction (blocks). */
    static final int REACH = 8;

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource r, BlockPos origin) {
        if (!soil(level, origin.below())) return refuse("no dirt under " + origin + ": " + level.getBlockState(origin.below()));
        int height = 10 + r.nextInt(4);           // the central leader's top
        int split = 5 + r.nextInt(2);             // where the leaders part
        for (int y = 0; y <= height; y++)
            if (!free(level, origin.above(y))) return refuse("trunk blocked at " + origin.above(y) + ": " + level.getBlockState(origin.above(y)));
        Map<BlockPos, BlockState> logs = new HashMap<>();
        Set<BlockPos> leaves = new LinkedHashSet<>();
        BlockState log = RubberTrees.LOG.get().defaultBlockState(), wood = RubberTrees.WOOD.get().defaultBlockState();

        // the trunk and the central leader
        for (int y = 0; y <= height; y++) logs.put(origin.above(y), log);
        // buttresses: low fins on three or four sides, a block or two out, sunk to the ground
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (r.nextInt(4) == 0) continue;
            int len = 1 + r.nextInt(2);
            for (int k = 1; k <= len; k++) {
                BlockPos p = origin.relative(d, k);
                int tall = k == 1 ? 1 + r.nextInt(2) : 0;
                for (int y = 0; y <= tall; y++) logs.put(p.above(y), wood);
                for (int y = -1; y >= -2 && free(level, p.above(y)); y--) logs.put(p.above(y), wood);   // (down to the ground)
            }
        }
        // the leaders: lean out from the split, each ending in a crown lobe
        int leaders = 3 + r.nextInt(2);
        double a0 = r.nextDouble() * Math.PI * 2;
        java.util.List<BlockPos> tips = new java.util.ArrayList<>();
        java.util.List<BlockPos> limbs = new java.util.ArrayList<>();
        for (int i = 0; i < leaders; i++) {
            double a = a0 + i * Math.PI * 2 / leaders + (r.nextDouble() - 0.5) * 0.6;
            double dx = Math.cos(a), dz = Math.sin(a);
            int y0 = split + r.nextInt(2), len = 4 + r.nextInt(2);
            BlockPos last = origin.above(y0);
            for (int k = 1; k <= len; k++) {
                double out = k * 0.85;
                BlockPos p = origin.offset((int) Math.round(dx * out), y0 + k, (int) Math.round(dz * out));
                // (a limb is bark all round where it bends: wood; logs only where it runs straight up)
                boolean straight = p.getX() == last.getX() && p.getZ() == last.getZ();
                logs.put(p, straight ? log : wood);
                if (p.getX() != last.getX() && p.getZ() != last.getZ())   // (keep the limb joined face to face)
                    logs.put(new BlockPos(p.getX(), p.getY(), last.getZ()), wood);
                limbs.add(p);
                last = p;
            }
            tips.add(last);
        }
        tips.add(origin.above(height));
        // the crown: a broad, flattened lobe round each tip
        for (BlockPos t : tips) {
            double rx = 3.0 + r.nextDouble() * 0.8;
            for (int dy = -1; dy <= 2; dy++) {
                double rr = dy <= 0 ? rx : rx - dy * 0.9;
                int ir = (int) Math.ceil(rr);
                for (int dx = -ir; dx <= ir; dx++)
                    for (int dz = -ir; dz <= ir; dz++) {
                        double d2 = dx * dx + dz * dz;
                        if (d2 > rr * rr || (d2 > (rr - 0.7) * (rr - 0.7) && r.nextInt(3) == 0)) continue;   // (a ragged rim)
                        BlockPos p = t.offset(dx, dy, dz);
                        if (!logs.containsKey(p) && within(origin, p)) leaves.add(p);
                    }
            }
        }
        // aerial roots: from under the limbs and the crown's underside, hanging down; many reach the ground, and some of
        // those have thickened into pillar trunks (AdventurAgent: 2-3 short ones per tree read as too few)
        Map<BlockPos, Boolean> roots = new HashMap<>();
        java.util.List<BlockPos> sources = new java.util.ArrayList<>(limbs);
        for (BlockPos l : leaves)
            if (!leaves.contains(l.below()) && !logs.containsKey(l.below()) && r.nextInt(6) == 0) sources.add(l);
        int drops = 9 + r.nextInt(6), pillars = 0;
        for (int n = 0; n < drops && !sources.isEmpty(); n++) {
            BlockPos s = sources.remove(r.nextInt(sources.size())).below();
            if (Math.abs(s.getX() - origin.getX()) + Math.abs(s.getZ() - origin.getZ()) < 2 || roots.containsKey(s)) continue;
            int ground = 0;
            while (ground < 20 && free(level, s.below(ground)) && !logs.containsKey(s.below(ground)) && !leaves.contains(s.below(ground)))
                ground++;
            boolean reaches = ground < 20 && soil(level, s.below(ground));
            if (reaches && pillars < 2 && r.nextInt(3) == 0) {
                for (int k = 0; k < ground; k++) logs.put(s.below(k), wood);   // a pillar trunk
                pillars++;
            } else {
                int len = reaches && r.nextBoolean() ? ground : Math.min(ground - 1, 3 + r.nextInt(ground / 2 + 2));
                for (int k = 0; k < len; k++) roots.put(s.below(k), true);
            }
        }

        for (var e : logs.entrySet()) if (free(level, e.getKey())) level.setBlock(e.getKey(), e.getValue(), 19);
        Map<BlockPos, Integer> dist = distances(logs.keySet(), leaves);
        BlockState leaf = RubberTrees.LEAVES.get().defaultBlockState();
        for (BlockPos p : leaves)
            if (free(level, p) && dist.containsKey(p)) level.setBlock(p, leaf.setValue(LeavesBlock.DISTANCE, dist.get(p)), 19);
        BlockState root = RubberTrees.AERIAL_ROOT.get().defaultBlockState();
        for (BlockPos p : roots.keySet()) if (level.getBlockState(p).isAir()) level.setBlock(p, root, 19);
        return true;
    }

    /** Why the last tree didn't grow (for tests and /elnlab). */
    public static String lastRefusal = "";

    static boolean refuse(String why) {
        lastRefusal = why;
        return false;
    }

    /** Ground a sapling would grow on (grass and dirt are no longer one tag). */
    static boolean soil(WorldGenLevel level, BlockPos p) {
        return RubberTrees.SAPLING.get().defaultBlockState().canSurvive(level, p.above());
    }

    static boolean within(BlockPos origin, BlockPos p) {
        return Math.abs(p.getX() - origin.getX()) <= REACH && Math.abs(p.getZ() - origin.getZ()) <= REACH;
    }

    static boolean free(WorldGenLevel level, BlockPos p) {
        return TreeFeature.validTreePos(level, p) || level.getBlockState(p).is(RubberTrees.SAPLING.get());
    }

    /** Each leaf's distance (1..6) to the nearest log, through leaves; leaves farther off are left out. */
    static Map<BlockPos, Integer> distances(Set<BlockPos> logs, Set<BlockPos> leaves) {
        Map<BlockPos, Integer> d = new HashMap<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        for (BlockPos l : logs)
            for (Direction dir : Direction.values()) {
                BlockPos n = l.relative(dir);
                if (leaves.contains(n) && !d.containsKey(n)) {
                    d.put(n, 1);
                    q.add(n);
                }
            }
        while (!q.isEmpty()) {
            BlockPos p = q.poll();
            int k = d.get(p);
            if (k >= 6) continue;
            for (Direction dir : Direction.values()) {
                BlockPos n = p.relative(dir);
                if (leaves.contains(n) && !d.containsKey(n)) {
                    d.put(n, k + 1);
                    q.add(n);
                }
            }
        }
        return d;
    }
}
