package mods.eln.sixnode.lampsocket;


import mods.eln.compat.BlockMeta;
import mods.eln.misc.Coordonate;
import mods.eln.sixnode.lampsocket.LightBlockEntity.LightBlockObserver;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Random;

/** Invisible air-like block that carries a lamp's light level in its META (0..15) state (porting guide rule 2). */
public class LightBlock extends BlockContainer {

    public LightBlock() {
        super(Material.AIR);
        setLightOpacity(0);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, BlockMeta.META);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(BlockMeta.META, meta & 15);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(BlockMeta.META);
    }

    @Override
    @Nullable
    public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d start, Vec3d end) {
        return null;
    }

    @Override
    @Nullable
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public boolean canCollideCheck(IBlockState state, boolean hitIfLiquid) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE; // 1.7.10: -1
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.AIR;
    }

    @Override
    public int quantityDropped(Random par1Random) {
        return 0;
    }

    @Override
    public boolean isReplaceable(IBlockAccess access, BlockPos pos) {
        return true;
    }

    @Override
    public int getLightValue(IBlockState state) {
        return state.getValue(BlockMeta.META);
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(BlockMeta.META);
    }

    @Override
    public TileEntity createNewTileEntity(World arg0, int arg1) {
        return new LightBlockEntity();
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        Coordonate coord = new Coordonate(pos.getX(), pos.getY(), pos.getZ(), world);
        for (LightBlockObserver o : LightBlockEntity.observers) {
            o.lightBlockDestructor(coord);
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public int getLightOpacity(IBlockState state) {
        return 0;
    }
}
