package mods.eln.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * The 1.7.10-style world calls EA code is written against (x, y, z ints, metadata), on 26.x. Same method names as the
 * 1.12 port's WorldCompat, so device code ports unchanged. Light is computed by the 26.x light engine from block
 * states, so the old manual light updates are no-ops.
 */
public final class WorldCompat {
    private WorldCompat() {
    }

    public static BlockPos pos(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    public static Block getBlock(BlockGetter world, int x, int y, int z) {
        return world.getBlockState(pos(x, y, z)).getBlock();
    }

    /** The block's BlockMeta.META value, 0 for blocks without it. */
    public static int getMeta(BlockGetter world, int x, int y, int z) {
        BlockState s = world.getBlockState(pos(x, y, z));
        return s.hasProperty(BlockMeta.META) ? s.getValue(BlockMeta.META) : 0;
    }

    public static BlockEntity getTileEntity(BlockGetter world, int x, int y, int z) {
        return world.getBlockEntity(pos(x, y, z));
    }

    /** Same as getTileEntity (26.x name). */
    public static BlockEntity getBlockEntity(BlockGetter world, int x, int y, int z) {
        return getTileEntity(world, x, y, z);
    }

    public static boolean isAir(BlockGetter world, int x, int y, int z) {
        return world.getBlockState(pos(x, y, z)).isAir();
    }

    /** 1.7.10 blockExists: the chunk is loaded. */
    public static boolean blockExists(Level world, int x, int y, int z) {
        return world.isLoaded(pos(x, y, z));
    }

    public static LevelChunk getChunkFromBlockCoords(Level world, int x, int z) {
        return world.getChunkAt(pos(x, 0, z));
    }

    public static boolean setBlock(Level world, int x, int y, int z, Block block) {
        return world.setBlockAndUpdate(pos(x, y, z), block.defaultBlockState());
    }

    public static boolean setBlock(Level world, int x, int y, int z, Block block, int meta, int flags) {
        BlockState s = block.defaultBlockState();
        if (s.hasProperty(BlockMeta.META)) s = s.setValue(BlockMeta.META, meta & 15);
        return world.setBlock(pos(x, y, z), s, flags);
    }

    public static boolean setMeta(Level world, int x, int y, int z, int meta, int flags) {
        BlockState s = world.getBlockState(pos(x, y, z));
        if (!s.hasProperty(BlockMeta.META)) return false;
        return world.setBlock(pos(x, y, z), s.setValue(BlockMeta.META, meta & 15), flags == 0 ? 2 : flags);
    }

    public static boolean setBlockToAir(Level world, int x, int y, int z) {
        return world.setBlockAndUpdate(pos(x, y, z), Blocks.AIR.defaultBlockState());
    }

    public static void markBlockForUpdate(Level world, int x, int y, int z) {
        BlockState s = world.getBlockState(pos(x, y, z));
        world.sendBlockUpdated(pos(x, y, z), s, s, 3);
    }

    public static void notifyNeighbours(Level world, int x, int y, int z, Block block) {
        world.updateNeighborsAt(pos(x, y, z), block);
    }

    public static void markBlockRangeForRenderUpdate(Level world, int x1, int y1, int z1, int x2, int y2, int z2) {
        // client render invalidation: done by block updates on 26.x
    }

    /** Signal the block at x,y,z emits towards side (Forge/vanilla 3D index 0..5 = DOWN, UP, NORTH, SOUTH, WEST, EAST). */
    public static int getIndirectPowerLevelTo(Level world, int x, int y, int z, int side) {
        return world.getSignal(pos(x, y, z), net.minecraft.core.Direction.from3DDataValue(side));
    }

    public static int getStrongestIndirectPower(Level world, int x, int y, int z) {
        return world.getBestNeighborSignal(pos(x, y, z));
    }

    public static boolean isBlockIndirectlyGettingPowered(Level world, int x, int y, int z) {
        return world.hasNeighborSignal(pos(x, y, z));
    }

    /** Combined (sky, block) light at the position. */
    public static int getBlockLightValue(Level world, int x, int y, int z) {
        return world.getMaxLocalRawBrightness(pos(x, y, z));
    }

    public static int getSavedLightValue(Level world, LightLayer type, int x, int y, int z) {
        return world.getBrightness(type, pos(x, y, z));
    }

    public static void setLightValue(Level world, LightLayer type, int x, int y, int z, int value) {
        // 26.x: light comes from block states (light emission); nothing to set
    }

    public static boolean updateLightByType(Level world, LightLayer type, int x, int y, int z) {
        return false; // the light engine updates on block changes
    }

    public static boolean updateAllLightTypes(Level world, int x, int y, int z) {
        return false;
    }

    public static int getBlockLightOpacity(Level world, int x, int y, int z) {
        return world.getBlockState(pos(x, y, z)).getLightDampening();
    }

    public static int x(BlockEntity te) {
        return te.getBlockPos().getX();
    }

    public static int y(BlockEntity te) {
        return te.getBlockPos().getY();
    }

    public static int z(BlockEntity te) {
        return te.getBlockPos().getZ();
    }
}
