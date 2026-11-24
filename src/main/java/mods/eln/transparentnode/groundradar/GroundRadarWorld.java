package mods.eln.transparentnode.groundradar;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Ground Radar's world: blocks as the radar sees them (GroundScan.Material, by tags, cached per block state) in
 * loaded chunks only, and the pieces of the structures (any mod's: the structure registry's starts) over the scan's
 * footprint. Server thread only.
 */
final class GroundRadarWorld implements GroundScan.World {
    private static final Map<BlockState, GroundScan.Material> MATERIALS = new IdentityHashMap<>();
    /** Structure pieces wider than this (blocks) aren't buildings. */
    static final int MAX_PIECE = 48;

    private final ServerLevel level;
    private final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
    private LevelChunk chunk;
    private int chunkX = Integer.MIN_VALUE, chunkZ = Integer.MIN_VALUE;
    /** Structure pieces' boxes by chunk (x, z packed), for the chunks they touch. */
    private final it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<List<BoundingBox>> pieces = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();

    GroundRadarWorld(ServerLevel level, int cx, int cz, int halfWidth) {
        this.level = level;
        collectStructures(cx, cz, halfWidth);
    }

    @Override
    public GroundScan.Material material(int x, int y, int z) {
        if (y < level.getMinY() || y >= level.getMaxY()) return GroundScan.Material.AIR_;
        int qx = x >> 4, qz = z >> 4;
        if (qx != chunkX || qz != chunkZ) {
            chunkX = qx;
            chunkZ = qz;
            chunk = level.getChunkSource().getChunkNow(qx, qz);
        }
        if (chunk == null) return null;
        return of(chunk.getBlockState(at.set(x, y, z)));
    }

    @Override
    public boolean built(int x, int y, int z) {
        if (pieces.isEmpty()) return false;
        List<BoundingBox> l = pieces.get(key(x >> 4, z >> 4));
        if (l == null) return false;
        for (BoundingBox b : l) if (b.isInside(x, y, z)) return true;
        return false;
    }

    private static long key(int qx, int qz) {
        return ((long) qx << 32) ^ (qz & 0xFFFFFFFFL);
    }

    /** The pieces of every structure referenced by a loaded chunk within halfWidth of (cx, cz). */
    private void collectStructures(int cx, int cz, int halfWidth) {
        java.util.Set<Object> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (int qx = (cx - halfWidth) >> 4; qx <= (cx + halfWidth) >> 4; qx++)
            for (int qz = (cz - halfWidth) >> 4; qz <= (cz + halfWidth) >> 4; qz++) {
                LevelChunk c = level.getChunkSource().getChunkNow(qx, qz);
                if (c == null) continue;
                for (var e : c.getAllReferences().entrySet()) {
                    var structure = e.getKey();
                    for (long ref : e.getValue()) {
                        int sx = net.minecraft.world.level.ChunkPos.getX(ref), sz = net.minecraft.world.level.ChunkPos.getZ(ref);
                        LevelChunk sc = level.getChunkSource().getChunkNow(sx, sz);
                        if (sc == null) continue;
                        var start = sc.getStartForStructure(structure);
                        if (start == null || !start.isValid() || !seen.add(start)) continue;
                        for (var piece : start.getPieces()) {
                            BoundingBox b = piece.getBoundingBox();
                            // (a piece as wide as terrain is terrain, not a building: an oil lake's (its oil shows as oil),
                            // a lake's; its box hatched the whole picture over one)
                            if (b.getXSpan() > MAX_PIECE || b.getZSpan() > MAX_PIECE) continue;
                            for (int px = b.minX() >> 4; px <= b.maxX() >> 4; px++)
                                for (int pz = b.minZ() >> 4; pz <= b.maxZ() >> 4; pz++)
                                    pieces.computeIfAbsent(key(px, pz), k -> new ArrayList<>()).add(b);
                        }
                    }
                }
            }
    }

    /** A block as the radar sees it: densities in g/cm3, attenuation per block (wet ground fades the signal most). */
    static GroundScan.Material of(BlockState s) {
        GroundScan.Material m = MATERIALS.get(s);
        if (m == null) {
            m = classify(s);
            MATERIALS.put(s, m);
        }
        return m;
    }

    private static GroundScan.Material classify(BlockState s) {
        if (s.isAir()) return GroundScan.Material.AIR_;
        if (s.getBlock() instanceof mods.eln.oil.ReservoirOilBlock) return new GroundScan.Material(0.85, 0.002, GroundScan.Material.OIL);
        FluidState f = s.getFluidState();
        boolean open = s.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty();
        if (!f.isEmpty() && open) {
            if (f.is(FluidTags.LAVA)) return new GroundScan.Material(3.1, 0.05, GroundScan.Material.LAVA);
            if (f.is(FluidTags.WATER)) return new GroundScan.Material(1.0, 0.02, GroundScan.Material.WATER);
            return new GroundScan.Material(0.9, 0.01, GroundScan.Material.OIL);   // (other mods' fluids: oils, fuels)
        }
        if (open) return GroundScan.Material.AIR_;   // (plants, torches, rails, snow layers)
        if (s.is(net.neoforged.neoforge.common.Tags.Blocks.ORES)) return new GroundScan.Material(4.0, 0.006, GroundScan.Material.SOLID);
        if (s.is(Blocks.BEDROCK)) return new GroundScan.Material(3.3, 0.01, GroundScan.Material.SOLID);
        if (s.is(Blocks.CLAY) || s.is(Blocks.MUD) || s.is(Blocks.PACKED_MUD) || s.is(Blocks.MUDDY_MANGROVE_ROOTS))
            return new GroundScan.Material(1.8, 0.05, GroundScan.Material.SOLID);
        if (s.is(Blocks.DEEPSLATE) || s.is(Blocks.COBBLED_DEEPSLATE) || s.is(Blocks.REINFORCED_DEEPSLATE))
            return new GroundScan.Material(2.9, 0.004, GroundScan.Material.SOLID);
        if (s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.BASE_STONE_NETHER)) return new GroundScan.Material(2.7, 0.004, GroundScan.Material.SOLID);
        if (s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL) || s.is(BlockTags.DIRT)) return new GroundScan.Material(1.7, 0.008, GroundScan.Material.SOLID);
        if (s.is(BlockTags.ICE)) return new GroundScan.Material(0.9, 0.001, GroundScan.Material.SOLID);
        if (s.is(BlockTags.LOGS) || s.is(BlockTags.PLANKS)) return new GroundScan.Material(0.6, 0.01, GroundScan.Material.SOLID);
        if (s.is(Blocks.OBSIDIAN) || s.is(Blocks.CRYING_OBSIDIAN)) return new GroundScan.Material(2.6, 0.004, GroundScan.Material.SOLID);
        if (s.is(net.neoforged.neoforge.common.Tags.Blocks.STORAGE_BLOCKS)) return new GroundScan.Material(4.5, 0.02, GroundScan.Material.SOLID);
        return new GroundScan.Material(2.5, 0.006, GroundScan.Material.SOLID);
    }
}
