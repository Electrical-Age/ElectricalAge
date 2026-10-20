package mods.eln.node.voxel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What the cable and pipe routers read of the world, per block: loaded or not, the block state, and for a voxel node
 * block its used voxels (or its parts' boxes) and the boxes left of its host block. Live (the level, server thread) or a
 * snapshot of a box of blocks, taken on the server thread and read on the route-preview thread (Baughn: the preview was
 * slow; the router ran on the server thread for every change of aim).
 */
public interface RouteView {
    boolean loaded(BlockPos p);

    BlockState state(BlockPos p);

    /** The voxel node block's data at p, or null if p holds none. */
    Voxels voxels(BlockPos p);

    /** For block shapes (Mimic): the level, or nothing (a snapshot: decorative blocks' shapes follow their state). */
    BlockGetter shapes();

    /** A voxel node block: used voxels if it has a node (else null), its parts' boxes if not, the host block's boxes. */
    record Voxels(BitSet used, List<int[]> partBoxes, List<int[]> hostBoxes) {
        boolean usedIn(int x0, int y0, int z0, int x1, int y1, int z1) {
            if (used != null) {
                for (int x = Math.max(0, x0); x < Math.min(16, x1); x++)
                    for (int y = Math.max(0, y0); y < Math.min(16, y1); y++)
                        for (int z = Math.max(0, z0); z < Math.min(16, z1); z++) if (used.get(VoxelNode.voxel(x, y, z))) return true;
                return false;
            }
            for (int[] b : partBoxes)
                if (b[0] < x1 && x0 < b[3] && b[1] < y1 && y0 < b[4] && b[2] < z1 && z0 < b[5]) return true;
            return false;
        }
    }

    static Voxels voxelsOf(VoxelNodeEntity e) {
        if (e.getNode() instanceof VoxelNode n) return new Voxels(n.usedCopy(), List.of(), List.copyOf(e.hostBoxes()));
        List<int[]> boxes = new java.util.ArrayList<>();
        for (VoxelPart part : e.parts()) boxes.addAll(part.boxes());
        return new Voxels(null, boxes, List.copyOf(e.hostBoxes()));
    }

    static RouteView live(Level level) {
        return new RouteView() {
            @Override
            public boolean loaded(BlockPos p) {
                return level.isLoaded(p);
            }

            @Override
            public BlockState state(BlockPos p) {
                return level.getBlockState(p);
            }

            @Override
            public Voxels voxels(BlockPos p) {
                return level.getBlockEntity(p) instanceof VoxelNodeEntity e ? voxelsOf(e) : null;
            }

            @Override
            public BlockGetter shapes() {
                return level;
            }
        };
    }

    /**
     * A copy of the blocks in [x0, x1] x [y0, y1] x [z0, z1] (block coordinates, inclusive): their states (straight from
     * the loaded chunks' sections) and the voxel node blocks' data. Outside the box, or in a chunk not loaded: not
     * loaded (in the way). Server thread.
     */
    static RouteView snapshot(Level level, int x0, int y0, int z0, int x1, int y1, int z1) {
        int nx = x1 - x0 + 1, ny = y1 - y0 + 1, nz = z1 - z0 + 1;
        BlockState[] states = new BlockState[nx * ny * nz];
        Map<Long, Voxels> voxels = new HashMap<>();
        for (int cx = x0 >> 4; cx <= x1 >> 4; cx++)
            for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
                if (!(level.getChunkSource().getChunkNow(cx, cz) instanceof LevelChunk chunk)) continue;
                for (int x = Math.max(x0, cx << 4); x <= Math.min(x1, (cx << 4) + 15); x++)
                    for (int z = Math.max(z0, cz << 4); z <= Math.min(z1, (cz << 4) + 15); z++)
                        for (int y = y0; y <= y1; y++) {
                            BlockState s = y < level.getMinY() || y > level.getMaxY() ? Blocks.AIR.defaultBlockState()
                                : chunk.getBlockState(new BlockPos(x, y, z));
                            states[((x - x0) * ny + (y - y0)) * nz + (z - z0)] = s;
                        }
                for (var be : chunk.getBlockEntities().values()) {
                    BlockPos p = be.getBlockPos();
                    if (be instanceof VoxelNodeEntity e && p.getX() >= x0 && p.getX() <= x1 && p.getY() >= y0 && p.getY() <= y1
                        && p.getZ() >= z0 && p.getZ() <= z1) voxels.put(p.asLong(), voxelsOf(e));
                }
            }
        return new RouteView() {
            int index(BlockPos p) {
                int x = p.getX() - x0, y = p.getY() - y0, z = p.getZ() - z0;
                if (x < 0 || y < 0 || z < 0 || x >= nx || y >= ny || z >= nz) return -1;
                return (x * ny + y) * nz + z;
            }

            @Override
            public boolean loaded(BlockPos p) {
                int i = index(p);
                return i >= 0 && states[i] != null;
            }

            @Override
            public BlockState state(BlockPos p) {
                int i = index(p);
                return i < 0 || states[i] == null ? Blocks.BEDROCK.defaultBlockState() : states[i];
            }

            @Override
            public Voxels voxels(BlockPos p) {
                return voxels.get(p.asLong());
            }

            @Override
            public BlockGetter shapes() {
                return EmptyBlockGetter.INSTANCE;
            }
        };
    }
}
