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
    /** The world as the router sees it: solid blocks and other devices are in the way, and so are used voxels of
     *  voxel nodes; air, replaceable blocks and liquids are free. */
    static PipeRouter.Space obstacles(Level level) {
        return obstacles(RouteView.live(level));
    }

    /** What's in the way, read through a view of the world (live, or a snapshot for the preview thread). */
    static PipeRouter.Space obstacles(RouteView view) {
        return (x0, y0, z0, x1, y1, z1) -> {
            for (long bx = Math.floorDiv(x0, 16); bx <= Math.floorDiv(x1 - 1, 16); bx++)
                for (long by = Math.floorDiv(y0, 16); by <= Math.floorDiv(y1 - 1, 16); by++)
                    for (long bz = Math.floorDiv(z0, 16); bz <= Math.floorDiv(z1 - 1, 16); bz++) {
                        BlockPos p = new BlockPos((int) bx, (int) by, (int) bz);
                        if (!view.loaded(p)) return true;
                        BlockState s = view.state(p);
                        if (s.isAir() || s.canBeReplaced() || s.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) continue;
                        if (Mimic.allowed(s)) {   // (a stair, fence, slab: only its own voxels are in the way)
                            int lx0 = (int) (x0 - bx * 16), ly0 = (int) (y0 - by * 16), lz0 = (int) (z0 - bz * 16);
                            int lx1 = (int) (x1 - bx * 16), ly1 = (int) (y1 - by * 16), lz1 = (int) (z1 - bz * 16);
                            for (int[] b : Mimic.boxes(s, view.shapes(), p))
                                if (b[0] < lx1 && lx0 < b[3] && b[1] < ly1 && ly0 < b[4] && b[2] < lz1 && lz0 < b[5]) return true;
                            continue;
                        }
                        if (s.getBlock() instanceof VoxelNodeBlock) {
                            RouteView.Voxels v = view.voxels(p);
                            if (v != null) {
                                int lx0 = (int) (x0 - bx * 16), ly0 = (int) (y0 - by * 16), lz0 = (int) (z0 - bz * 16);
                                int lx1 = (int) (x1 - bx * 16), ly1 = (int) (y1 - by * 16), lz1 = (int) (z1 - bz * 16);
                                if (v.usedIn(lx0, ly0, lz0, lx1, ly1, lz1)) return true;
                                continue;
                            }
                        }
                        return true;
                    }
            return false;
        };
    }

    /** The space, but free in the cube of half-side h round a junction point (a new pipe or cable may meet the old one there). */
    static PipeRouter.Space freeAround(PipeRouter.Space s, long[] p, int h) {
        return (x0, y0, z0, x1, y1, z1) -> {
            if (x0 >= p[0] - h && x1 <= p[0] + h && y0 >= p[1] - h && y1 <= p[1] + h && z0 >= p[2] - h && z1 <= p[2] + h) return false;
            // a box reaching into the cube: check only its part outside (axis by axis: enough for lattice steps)
            if (x0 < p[0] + h && x1 > p[0] - h && y0 < p[1] + h && y1 > p[1] - h && z0 < p[2] + h && z1 > p[2] - h) {
                long[][] parts = {{x0, Math.min(x1, p[0] - h)}, {Math.max(x0, p[0] + h), x1}};
                boolean b = false;
                for (long[] xr : parts) if (xr[0] < xr[1]) b |= s.blocked(xr[0], y0, z0, xr[1], y1, z1);
                long cx0 = Math.max(x0, p[0] - h), cx1 = Math.min(x1, p[0] + h);
                long[][] ys = {{y0, Math.min(y1, p[1] - h)}, {Math.max(y0, p[1] + h), y1}};
                for (long[] yr : ys) if (yr[0] < yr[1]) b |= s.blocked(cx0, yr[0], z0, cx1, yr[1], z1);
                long cy0 = Math.max(y0, p[1] - h), cy1 = Math.min(y1, p[1] + h);
                long[][] zs = {{z0, Math.min(z1, p[2] - h)}, {Math.max(z0, p[2] + h), z1}};
                for (long[] zr : zs) if (zr[0] < zr[1]) b |= s.blocked(cx0, cy0, zr[0], cx1, cy1, zr[1]);
                return b;
            }
            return s.blocked(x0, y0, z0, x1, y1, z1);
        };
    }

    boolean loaded(BlockPos p);

    BlockState state(BlockPos p);

    /** The voxel node block's data at p, or null if p holds none. */
    Voxels voxels(BlockPos p);

    /** For block shapes (Mimic): the level, or nothing (a snapshot: decorative blocks' shapes follow their state). */
    BlockGetter shapes();

    /** The parts of the voxel node block at p (a copy of the list; empty if p holds none): the run a route joins (JoinRun). */
    List<VoxelPart> parts(BlockPos p);

    /**
     * The view with the voxels freed (block-local, VoxelNode.voxel) taken out of the used voxels of the voxel block at b:
     * a route may pass through them (JoinRun: the old run near a join point, todo #516).
     */
    static RouteView freeing(RouteView v, BlockPos b, BitSet freed) {
        return new RouteView() {
            @Override
            public boolean loaded(BlockPos p) {
                return v.loaded(p);
            }

            @Override
            public BlockState state(BlockPos p) {
                return v.state(p);
            }

            @Override
            public Voxels voxels(BlockPos p) {
                Voxels x = v.voxels(p);
                if (x == null || x.used() == null || !p.equals(b)) return x;
                BitSet used = (BitSet) x.used().clone();
                used.andNot(freed);
                return new Voxels(used, x.partBoxes(), x.hostBoxes());
            }

            @Override
            public BlockGetter shapes() {
                return v.shapes();
            }

            @Override
            public List<VoxelPart> parts(BlockPos p) {
                return v.parts(p);
            }
        };
    }

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

            @Override
            public List<VoxelPart> parts(BlockPos p) {
                return level.getBlockEntity(p) instanceof VoxelNodeEntity e && e.getNode() instanceof VoxelNode n ? List.copyOf(n.parts) : List.of();
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
        Map<Long, List<VoxelPart>> parts = new HashMap<>();
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
                        && p.getZ() >= z0 && p.getZ() <= z1) {
                        voxels.put(p.asLong(), voxelsOf(e));
                        if (e.getNode() instanceof VoxelNode n) parts.put(p.asLong(), List.copyOf(n.parts));
                    }
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

            @Override
            public List<VoxelPart> parts(BlockPos p) {
                return parts.getOrDefault(p.asLong(), List.of());
            }
        };
    }
}
