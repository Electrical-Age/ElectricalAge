package mods.eln.oil;

import com.mojang.serialization.Codec;
import mods.eln.reservoir.ArrayBasin;
import mods.eln.reservoir.Basin;
import mods.eln.reservoir.Reservoir;
import mods.eln.reservoir.ReservoirData;
import mods.eln.reservoir.Reservoirs;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Small oil ponds at the surface over half the oil lakes (Baughn 2026-10-28: lakes were too hard to find), each its own
 * tiny reservoir ("oil-pond:" + the lake's key), like the vanilla lava lakes. One path for new worlds and old (retrogen):
 * once a lake's record exists and the chunks over its middle are loaded, the pond is dug, rimmed with tar sand and
 * filled, once. A lake with no flat, dry, natural spot over its middle (a player's build, a tree, water) gets none. The
 * lakes that are done are kept in the saved data eln:oil_ponds.
 */
public final class OilPonds {
    private OilPonds() {
    }

    static final String LAKE = "oil:", POND = "oil-pond:";
    static final int CHECK_TICKS = 40;

    public enum Result { PLACED, NO_POND, NO_SPOT, NOT_LOADED }

    /** The fixed 50% roll for a lake. */
    public static boolean rolls(String lakeKey) {
        long h = lakeKey.hashCode() * 0x9E3779B97F4A7C15L;
        return ((h ^ (h >>> 29)) & 0x100) != 0;
    }

    static final class Done extends SavedData {
        final Set<String> lakes;

        Done(List<String> lakes) {
            this.lakes = new HashSet<>(lakes);
        }

        static final Codec<Done> CODEC = Codec.STRING.listOf().xmap(Done::new, d -> new ArrayList<>(d.lakes)).fieldOf("lakes").codec();
    }

    static final SavedDataType<Done> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath("eln", "oil_ponds"),
        () -> new Done(List.of()), Done.CODEC);

    /** Main thread, from OilReservoirs.tick: every 2 s, try the lakes not done yet. */
    static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % CHECK_TICKS != 0) return;
        for (ServerLevel level : e.getServer().getAllLevels()) {
            ReservoirData data = ReservoirData.get(level);
            Done done = level.getDataStorage().computeIfAbsent(TYPE);
            for (String key : List.copyOf(data.keys())) {
                if (!key.startsWith(LAKE) || done.lakes.contains(key)) continue;
                Reservoir lake = data.byKey(key);
                if (lake == null) continue;
                Result r = tryLake(level, key, lake);
                if (r == Result.NOT_LOADED) continue;
                done.lakes.add(key);
                done.setDirty();
                if (r == Result.NO_SPOT) mods.eln.Eln.LOG.info("Oil lake {}: no place for its pond", key);
            }
        }
    }

    static Result tryLake(ServerLevel level, String key, Reservoir lake) {
        if (!rolls(key)) return Result.NO_POND;
        var spec = lake.spec();
        int minX = spec.getIntOr("min_x", 0), minZ = spec.getIntOr("min_z", 0);
        int sizeX = spec.getIntOr("size_x", 0), sizeZ = spec.getIntOr("size_z", 0);
        return place(level, key, minX + sizeX / 2, minZ + sizeZ / 2, Math.max(6, Math.min(sizeX, sizeZ) / 3), key.hashCode());
    }

    /**
     * Digs a pond near (cx, cz), within reach blocks, for the lake key: tries spots in a seeded order and takes the first
     * that is flat, dry and natural. NOT_LOADED if the chunks there aren't all loaded yet.
     */
    public static Result place(ServerLevel level, String lakeKey, int cx, int cz, int reach, int seed) {
        var rnd = new java.util.Random(seed);
        int rx = 3 + rnd.nextInt(4), rz = 3 + rnd.nextInt(4), depth = 2 + rnd.nextInt(2);
        int pad = Math.max(rx, rz) + 2;
        for (int x = cx - reach - pad; x <= cx + reach + pad; x += 16)
            for (int z = cz - reach - pad; z <= cz + reach + pad; z += 16)
                if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) return Result.NOT_LOADED;
        if (level.getChunkSource().getChunkNow((cx + reach + pad) >> 4, (cz + reach + pad) >> 4) == null) return Result.NOT_LOADED;
        List<int[]> spots = new ArrayList<>();
        for (int dx = -reach; dx <= reach; dx += 3)
            for (int dz = -reach; dz <= reach; dz += 3) spots.add(new int[]{cx + dx, cz + dz});
        spots.sort(java.util.Comparator.comparingInt(s -> (s[0] - cx) * (s[0] - cx) + (s[1] - cz) * (s[1] - cz)));
        // the pond's own size first, then smaller ones (savanna and forest leave little room between trees)
        int[][] sizes = {{rx, rz}, {Math.max(3, rx - 2), Math.max(3, rz - 2)}, {3, 3}};
        for (int[] size : sizes) {
            for (int[] s : spots) {
                Pond p = Pond.at(level, s[0], s[1], size[0], size[1], depth, seed);
                if (p == null) continue;
                p.dig(level);
                int[] bottom = new int[p.sizeX * p.sizeZ], height = new int[bottom.length];
                for (int i = 0; i < bottom.length; i++) {
                    bottom[i] = p.depth[i] > 0 ? p.top - p.depth[i] + 1 : Basin.NONE;
                    height[i] = Math.max(0, p.depth[i]);
                }
                int[][] a = ArrayBasin.area(bottom, height);
                Reservoir r = ReservoirData.get(level).create(level, POND + lakeKey.substring(LAKE.length()), OilReservoirs.KIND,
                    ArrayBasin.TYPE, ArrayBasin.spec(p.minX, p.minZ, p.sizeX, p.sizeZ, bottom, height), a[0][0], a[1], Long.MAX_VALUE);
                Reservoirs.refresh(level, r);
                mods.eln.Eln.LOG.info("Oil pond #{} over lake {} at {} {} {}: {} buckets", r.id(), lakeKey, s[0], p.top, s[1], r.capacity() / 1000);
                return Result.PLACED;
            }
        }
        return Result.NO_SPOT;
    }

    /** A pond's footprint: columns of oil (depth > 0) and of rim (-1) in a box; the oil's top cell is at top, level with
     *  the lowest ground around it (as vanilla's lakes). */
    static final class Pond {
        int minX, minZ, sizeX, sizeZ, top;
        int[] depth, ground;

        /** Ground under (x, z): the top block that isn't air, a plant or snow; MIN if water or not natural ground. */
        static int ground(ServerLevel level, int x, int z) {
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, y, z);
            for (int i = 0; i < 6; i++, p.move(0, -1, 0)) {
                BlockState s = level.getBlockState(p);
                if (!s.getFluidState().isEmpty()) return Integer.MIN_VALUE;
                if (s.isAir() || cover(s)) continue;
                return naturalGround(s) ? p.getY() : Integer.MIN_VALUE;
            }
            return Integer.MIN_VALUE;
        }

        /** Things that may stand on the ground where a pond goes: plants, snow (not trees). */
        static boolean cover(BlockState s) {
            return s.getFluidState().isEmpty() && (s.canBeReplaced() || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS))
                || s.is(Blocks.SNOW);
        }

        static boolean naturalGround(BlockState s) {
            return s.is(BlockTags.DIRT) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.PODZOL) || s.is(Blocks.MYCELIUM) || s.is(BlockTags.SAND)
                || s.is(Blocks.GRAVEL) || s.is(Blocks.CLAY) || s.is(Blocks.SNOW_BLOCK) || s.is(BlockTags.BASE_STONE_OVERWORLD)
                || s.is(BlockTags.TERRACOTTA) || s.is(Blocks.SANDSTONE) || s.is(Blocks.RED_SANDSTONE) || s.is(Blocks.MUD)
                || s.is(OilWorldgen.TAR_SAND.get());
        }

        /** An ellipse with a wobbly edge: 0 at the middle, 1 at the edge. */
        static float dist(int dx, int dz, int rx, int rz, int seed) {
            float d = (float) Math.sqrt((dx * dx) / (float) (rx * rx) + (dz * dz) / (float) (rz * rz));
            return d * (0.85f + 0.3f * LakeShape.hash(dx, 0, dz, seed));
        }

        /** The pond at (cx, cz), or null if the ground there isn't flat (within 2), dry and natural. */
        static Pond at(ServerLevel level, int cx, int cz, int rx, int rz, int maxDepth, int seed) {
            Pond p = new Pond();
            p.minX = cx - rx - 2;
            p.minZ = cz - rz - 2;
            p.sizeX = 2 * rx + 5;
            p.sizeZ = 2 * rz + 5;
            p.depth = new int[p.sizeX * p.sizeZ];
            p.ground = new int[p.depth.length];
            int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
            for (int i = 0; i < p.sizeX; i++)
                for (int k = 0; k < p.sizeZ; k++) {
                    int dx = p.minX + i - cx, dz = p.minZ + k - cz;
                    float d = dist(dx, dz, rx, rz, seed);
                    if (d > 1.45f) continue;
                    int g = ground(level, p.minX + i, p.minZ + k);
                    if (g == Integer.MIN_VALUE) return null;
                    p.ground[i * p.sizeZ + k] = g;
                    lo = Math.min(lo, g);
                    hi = Math.max(hi, g);
                    p.depth[i * p.sizeZ + k] = d <= 1f ? Math.max(1, Math.round(maxDepth * (1 - d * d) + 0.4f)) : -1;
                }
            if (hi - lo > 2) return null;
            // the oil's surface: level with the lowest ground in and around it
            p.top = lo;
            return p;
        }

        /** Clears the pond's cells and the ground above them, seals its floor and sides, tops its rim with tar sand. */
        void dig(ServerLevel level) {
            BlockState air = Blocks.AIR.defaultBlockState(), tar = OilWorldgen.TAR_SAND.get().defaultBlockState();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int i = 0; i < sizeX; i++)
                for (int k = 0; k < sizeZ; k++) {
                    int d = depth[i * sizeZ + k], g = ground[i * sizeZ + k], x = minX + i, z = minZ + k;
                    if (d == 0) continue;
                    if (d < 0) {
                        // rim: its top block becomes tar sand (the bank, level with the ground), plants on it go
                        level.setBlock(p.set(x, g, z), tar, 2);
                        for (int y = g + 1; y <= g + 2; y++)
                            if (cover(level.getBlockState(p.set(x, y, z)))) level.setBlock(p, air, 2);
                        continue;
                    }
                    for (int y = top - d + 1; y <= g + 2; y++) {
                        BlockState s = level.getBlockState(p.set(x, y, z));
                        if (y <= top || cover(s) || naturalGround(s)) level.setBlock(p, air, 2);
                    }
                    // a sealed floor and sides: no hole into a cave, no oil running off
                    BlockState under = level.getBlockState(p.set(x, top - d, z));
                    if (under.isAir() || !under.getFluidState().isEmpty()) level.setBlock(p, tar, 2);
                    for (var dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                        int ni = i + dir.getStepX(), nk = k + dir.getStepZ();
                        boolean inPond = ni >= 0 && nk >= 0 && ni < sizeX && nk < sizeZ && depth[ni * sizeZ + nk] > 0;
                        if (inPond) continue;
                        for (int y = top - d + 1; y <= top; y++) {
                            BlockState s = level.getBlockState(p.set(x + dir.getStepX(), y, z + dir.getStepZ()));
                            if (s.isAir() || !s.getFluidState().isEmpty() || cover(s)) level.setBlock(p, tar, 2);
                        }
                    }
                }
            for (int cx = minX >> 4; cx <= (minX + sizeX - 1) >> 4; cx++)
                for (int cz = minZ >> 4; cz <= (minZ + sizeZ - 1) >> 4; cz++)
                    Heightmap.primeHeightmaps(level.getChunk(cx, cz), java.util.EnumSet.of(Heightmap.Types.MOTION_BLOCKING,
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Heightmap.Types.WORLD_SURFACE, Heightmap.Types.OCEAN_FLOOR));
        }
    }
}
