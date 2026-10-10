package mods.eln.compat;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * 1.7.10-style x,y,z world access over 1.12's BlockPos/IBlockState API (porting guide rule 2).
 * Each method documents the 1.7.10 World method it replaces. Keep semantics here, not at call sites.
 *
 * "meta" means {@code block.getMetaFromState(state)}; EA blocks that used metadata carry a
 * {@code PropertyInteger META 0..15} with identity get/setMetaFromState, so it keeps its 1.7.10 meaning.
 */
public final class WorldCompat {
    private WorldCompat() {
    }

    public static BlockPos pos(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    // ---- reads (IBlockAccess, so they also work on ChunkCache / client worlds) ----

    /** 1.7.10 {@code world.getBlock(x, y, z)}. */
    public static Block getBlock(IBlockAccess world, int x, int y, int z) {
        return world.getBlockState(new BlockPos(x, y, z)).getBlock();
    }

    /** 1.7.10 {@code world.getBlockMetadata(x, y, z)}. */
    public static int getMeta(IBlockAccess world, int x, int y, int z) {
        IBlockState state = world.getBlockState(new BlockPos(x, y, z));
        return state.getBlock().getMetaFromState(state);
    }

    /** 1.7.10 {@code world.getTileEntity(x, y, z)}. */
    public static TileEntity getTileEntity(IBlockAccess world, int x, int y, int z) {
        return world.getTileEntity(new BlockPos(x, y, z));
    }

    /** 1.7.10 {@code world.isAirBlock(x, y, z)}. */
    public static boolean isAir(IBlockAccess world, int x, int y, int z) {
        return world.isAirBlock(new BlockPos(x, y, z));
    }

    /** 1.7.10 {@code world.blockExists(x, y, z)} (= chunk loaded). */
    public static boolean blockExists(World world, int x, int y, int z) {
        return world.isBlockLoaded(new BlockPos(x, y, z));
    }

    /** 1.7.10 {@code world.getChunkFromBlockCoords(x, z)}. */
    public static Chunk getChunkFromBlockCoords(World world, int x, int z) {
        return world.getChunk(new BlockPos(x, 0, z));
    }

    // ---- writes ----

    /** 1.7.10 {@code world.setBlock(x, y, z, block)} (flags 3). Uses the block's default state. */
    public static boolean setBlock(World world, int x, int y, int z, Block block) {
        return world.setBlockState(new BlockPos(x, y, z), block.getDefaultState(), 3);
    }

    /** 1.7.10 {@code world.setBlock(x, y, z, block, meta, flags)}. Flags have the same meaning in 1.12. */
    @SuppressWarnings("deprecation")
    public static boolean setBlock(World world, int x, int y, int z, Block block, int meta, int flags) {
        return world.setBlockState(new BlockPos(x, y, z), block.getStateFromMeta(meta), flags);
    }

    /** 1.7.10 {@code world.setBlockMetadataWithNotify(x, y, z, meta, flags)}: same block, new meta. */
    @SuppressWarnings("deprecation")
    public static boolean setMeta(World world, int x, int y, int z, int meta, int flags) {
        BlockPos pos = new BlockPos(x, y, z);
        Block block = world.getBlockState(pos).getBlock();
        return world.setBlockState(pos, block.getStateFromMeta(meta), flags);
    }

    /** 1.7.10 {@code world.setBlockToAir(x, y, z)}. */
    public static boolean setBlockToAir(World world, int x, int y, int z) {
        return world.setBlockToAir(new BlockPos(x, y, z));
    }

    // ---- updates / notifications ----

    /** 1.7.10 {@code world.markBlockForUpdate(x, y, z)}: resend the block (and its TE description) to clients. */
    public static void markBlockForUpdate(World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
    }

    /**
     * 1.7.10 {@code world.notifyBlockChange(x, y, z, block)} / {@code notifyBlocksOfNeighborChange(x, y, z, block)}.
     * Observers did not exist in 1.7.10; we let them see the change (updateObservers = true, as vanilla setBlockState does).
     */
    public static void notifyNeighbours(World world, int x, int y, int z, Block block) {
        world.notifyNeighborsOfStateChange(new BlockPos(x, y, z), block, true);
    }

    /** 1.7.10 {@code world.markBlockRangeForRenderUpdate(x1, y1, z1, x2, y2, z2)} exists unchanged in 1.12. */
    public static void markBlockRangeForRenderUpdate(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
        world.markBlockRangeForRenderUpdate(x1, y1, z1, x2, y2, z2);
    }

    // ---- redstone ----

    /**
     * 1.7.10 {@code world.getIndirectPowerLevelTo(x, y, z, side)}: power the block at x,y,z emits towards
     * {@code side} (Forge side index 0..5 = DOWN, UP, NORTH, SOUTH, WEST, EAST, same order as EnumFacing).
     */
    public static int getIndirectPowerLevelTo(World world, int x, int y, int z, int side) {
        return world.getRedstonePower(new BlockPos(x, y, z), EnumFacing.byIndex(side));
    }

    /** 1.7.10 {@code world.getStrongestIndirectPower(x, y, z)}. */
    public static int getStrongestIndirectPower(World world, int x, int y, int z) {
        return world.getRedstonePowerFromNeighbors(new BlockPos(x, y, z));
    }

    /** 1.7.10 {@code world.isBlockIndirectlyGettingPowered(x, y, z)}. */
    public static boolean isBlockIndirectlyGettingPowered(World world, int x, int y, int z) {
        return world.isBlockPowered(new BlockPos(x, y, z));
    }

    // ---- light ----

    /** 1.7.10 {@code world.getBlockLightValue(x, y, z)} (combined light, checking neighbours of non-full blocks). */
    public static int getBlockLightValue(World world, int x, int y, int z) {
        return world.getLightFromNeighbors(new BlockPos(x, y, z));
    }

    /** 1.7.10 {@code world.getSavedLightValue(type, x, y, z)}. */
    public static int getSavedLightValue(World world, EnumSkyBlock type, int x, int y, int z) {
        return world.getLightFor(type, new BlockPos(x, y, z));
    }

    /** 1.7.10 {@code world.setLightValue(type, x, y, z, value)}. */
    public static void setLightValue(World world, EnumSkyBlock type, int x, int y, int z, int value) {
        world.setLightFor(type, new BlockPos(x, y, z), value);
    }

    /** 1.7.10 {@code world.updateLightByType(type, x, y, z)}. */
    public static boolean updateLightByType(World world, EnumSkyBlock type, int x, int y, int z) {
        return world.checkLightFor(type, new BlockPos(x, y, z));
    }

    /** 1.7.10 {@code world.func_147451_t(x, y, z)} (updateAllLightTypes). */
    public static boolean updateAllLightTypes(World world, int x, int y, int z) {
        return world.checkLight(new BlockPos(x, y, z));
    }

    /** 1.7.10 {@code world.getBlockLightOpacity(x, y, z)}. */
    public static int getBlockLightOpacity(World world, int x, int y, int z) {
        return world.getBlockLightOpacity(new BlockPos(x, y, z));
    }

    // ---- tile entity position helpers (TileEntity.xCoord etc. from outside a TE subclass) ----

    public static int x(TileEntity te) {
        return te.getPos().getX();
    }

    public static int y(TileEntity te) {
        return te.getPos().getY();
    }

    public static int z(TileEntity te) {
        return te.getPos().getZ();
    }
}
