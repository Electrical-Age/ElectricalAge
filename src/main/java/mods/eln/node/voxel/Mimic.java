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
