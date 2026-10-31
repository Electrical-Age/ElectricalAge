package mods.eln.node.voxel;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * A decorative block a voxel block imitates so cables can share its space (notes/cables/OVERLAY.md, design B; Baughn
 * 2026-10-18): stairs, slabs, fences, walls, panes and bars, carpets. Only passive blocks: nothing with a block entity
 * or behaviour of its own (anvils, chests, trapdoors, buttons...), which a voxel block would silently drop.
 */
public final class Mimic {
    private Mimic() {
    }

    /** May a voxel block take this block's place (imitating it)? */
    public static boolean allowed(BlockState s) {
        if (s.isAir() || s.hasBlockEntity() || s.getBlock() instanceof VoxelNodeBlock) return false;
        return s.is(BlockTags.STAIRS) || s.is(BlockTags.SLABS) || s.is(BlockTags.FENCES) || s.is(BlockTags.WALLS)
            || s.is(BlockTags.WOOL_CARPETS) || s.getBlock() instanceof net.minecraft.world.level.block.IronBarsBlock
            || s.getBlock() instanceof net.minecraft.world.level.block.CarpetBlock;
    }

    /** May the chisel carve this block (Mimic.allowed, or a plain full block with no behaviour of its own: stone,
     *  planks, bricks, logs)? */
    public static boolean carvable(BlockState s) {
        if (allowed(s)) return true;
        if (s.isAir() || s.hasBlockEntity()) return false;
        Class<?> c = s.getBlock().getClass();
        return c == net.minecraft.world.level.block.Block.class || c == net.minecraft.world.level.block.RotatedPillarBlock.class;
    }

    /** Why the chisel can't carve this block (for the player), or null if it can. */
    public static String whyNotCarvable(BlockState s) {
        if (carvable(s)) return null;
        if (s.isAir()) return "nothing to carve there";
        String name = s.getBlock().getName().getString();
        if (s.hasBlockEntity()) return "can't carve " + name + ": it holds or does something (only plain blocks)";
        return "can't carve " + name + ": only plain blocks (stone, planks, bricks, logs), stairs, slabs, fences, walls, bars and carpets";
    }

    /** The voxels of boxes (0..16) as a set (VoxelNode.voxel indexing). */
    public static java.util.BitSet voxels(List<int[]> boxes) {
        java.util.BitSet s = new java.util.BitSet(4096);
        for (int[] b : boxes)
            for (int x = b[0]; x < b[3]; x++)
                for (int y = b[1]; y < b[4]; y++)
                    for (int z = b[2]; z < b[5]; z++) s.set(VoxelNode.voxel(x, y, z));
        return s;
    }

    /** A voxel set as boxes, merged greedily (runs along z, then x, then y). */
    public static List<int[]> boxes(java.util.BitSet set) {
        java.util.BitSet left = (java.util.BitSet) set.clone();
        List<int[]> out = new ArrayList<>();
        for (int i = left.nextSetBit(0); i >= 0; i = left.nextSetBit(i + 1)) {
            int x0 = i / 256, y0 = (i / 16) % 16, z0 = i % 16;
            int z1 = z0 + 1;
            while (z1 < 16 && left.get(VoxelNode.voxel(x0, y0, z1))) z1++;
            int x1 = x0 + 1;
            grow:
            while (x1 < 16) {
                for (int z = z0; z < z1; z++) if (!left.get(VoxelNode.voxel(x1, y0, z))) break grow;
                x1++;
            }
            int y1 = y0 + 1;
            growY:
            while (y1 < 16) {
                for (int x = x0; x < x1; x++) for (int z = z0; z < z1; z++) if (!left.get(VoxelNode.voxel(x, y1, z))) break growY;
                y1++;
            }
            for (int x = x0; x < x1; x++) for (int y = y0; y < y1; y++) for (int z = z0; z < z1; z++) left.clear(VoxelNode.voxel(x, y, z));
            out.add(new int[]{x0, y0, z0, x1, y1, z1});
        }
        return out;
    }

    /** The voxels the block takes in its block (its outline and collision, inside the block), as boxes. */
    public static List<int[]> boxes(BlockState s, BlockGetter level, BlockPos pos) {
        List<int[]> out = new ArrayList<>();
        var shape = net.minecraft.world.phys.shapes.Shapes.or(s.getShape(level, pos), s.getCollisionShape(level, pos));
        for (var b : shape.toAabbs()) {
            int[] v = {(int) Math.floor(b.minX * 16 + 1e-6), (int) Math.floor(b.minY * 16 + 1e-6), (int) Math.floor(b.minZ * 16 + 1e-6),
                (int) Math.ceil(b.maxX * 16 - 1e-6), (int) Math.ceil(b.maxY * 16 - 1e-6), (int) Math.ceil(b.maxZ * 16 - 1e-6)};
            for (int a = 0; a < 6; a++) v[a] = Math.max(0, Math.min(16, v[a]));
            if (v[0] < v[3] && v[1] < v[4] && v[2] < v[5]) out.add(v);
        }
        return out;
    }
}
