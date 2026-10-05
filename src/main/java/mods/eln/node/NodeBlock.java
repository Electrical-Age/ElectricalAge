package mods.eln.node;


import mods.eln.compat.BlockMeta;
import mods.eln.compat.WorldCompat;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.lang.reflect.InvocationTargetException;

public abstract class NodeBlock extends Block {//BlockContainer
    public int blockItemNbr;
    Class tileEntityClass;

    public NodeBlock(Material material, Class tileEntityClass, int blockItemNbr) {
        super(material);
        setTranslationKey("NodeBlock");
        this.tileEntityClass = tileEntityClass;
        useNeighborBrightness = true;
        this.blockItemNbr = blockItemNbr;
        setHardness(1.0f);
        setResistance(1.0f);
    }

    // "meta" block state (porting guide rule 2): TransparentNode keeps its group/opacity bits here
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
    public float getBlockHardness(IBlockState state, World par1World, BlockPos pos) {

        return 1.0f;
    }


    @Override
    public int getWeakPower(IBlockState state, IBlockAccess block, BlockPos pos, EnumFacing side) {
        NodeBlockEntity entity = (NodeBlockEntity) block.getTileEntity(pos);
        return entity.isProvidingWeakPower(Direction.fromIntMinecraftSide(side.getIndex()));
    }

    @Override
    public boolean canConnectRedstone(IBlockState state, IBlockAccess block, BlockPos pos, EnumFacing side) {
        NodeBlockEntity entity = (NodeBlockEntity) block.getTileEntity(pos);
        return entity.canConnectRedstone(Direction.XN);
    }

    @Override
    public boolean canProvidePower(IBlockState state) {

        return super.canProvidePower(state);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return true;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    /** 1.7.10 render type -1: no block model, everything is drawn by the TESR. TODO(1.12 WP5): particle texture. */
    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        final TileEntity entity = world.getTileEntity(pos);
        if (entity == null || !(entity instanceof NodeBlockEntity)) return 0;
        NodeBlockEntity tileEntity = (NodeBlockEntity) entity;
        return tileEntity.getLightValue();
    }


    //client server
    public boolean onBlockPlacedBy(World world, int x, int y, int z, Direction front, EntityLivingBase entityLiving, int metadata) {

        NodeBlockEntity tileEntity = (NodeBlockEntity) WorldCompat.getTileEntity(world, x, y, z);

        tileEntity.onBlockPlacedBy(front, entityLiving, metadata);
        return true;
    }

    //server
    @Override
    public void onBlockAdded(World par1World, BlockPos pos, IBlockState state) {
        if (par1World.isRemote == false) {
            NodeBlockEntity entity = (NodeBlockEntity) par1World.getTileEntity(pos);
            entity.onBlockAdded();
        }
    }


    //server
    @Override
    public void breakBlock(World par1World, BlockPos pos, IBlockState state) {

        //if(par1World.isRemote == false)
        {
            NodeBlockEntity entity = (NodeBlockEntity) par1World.getTileEntity(pos);
            entity.onBreakBlock();
            super.breakBlock(par1World, pos, state);
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block b, BlockPos fromPos) {
        if (Utils.isRemote(world) == false) {
            NodeBlockEntity entity = (NodeBlockEntity) world.getTileEntity(pos);
            entity.onNeighborBlockChange();
        }
    }


    @Override
    public int damageDropped(IBlockState state) {
        return getMetaFromState(state);
    }

    /** 1.7.10 Block.getDamageValue (pick block damage); default = damageDropped(meta). */
    public int getDamageValue(World world, int x, int y, int z) {
        return WorldCompat.getMeta(world, x, y, z);
    }

    /** Pick block: 1.7.10 built it from getDamageValue(world, x, y, z). */
    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(Item.getItemFromBlock(this), 1, getDamageValue(world, pos.getX(), pos.getY(), pos.getZ()));
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> subItems) {
        for (int ix = 0; ix < blockItemNbr; ix++) {
            subItems.add(new ItemStack(this, 1, ix));
        }
    }

    //client server
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer entityPlayer, EnumHand hand, EnumFacing side, float vx, float vy, float vz) {
        // 1.7.10 had one hand; EA code reads the main hand, so only react once (main hand).
        if (hand != EnumHand.MAIN_HAND) return false;
        NodeBlockEntity entity = (NodeBlockEntity) world.getTileEntity(pos);
//    	entityPlayer.openGui( Eln.instance, 0,world,x ,y, z);
        return entity.onBlockActivated(entityPlayer, Direction.fromIntMinecraftSide(side.getIndex()), vx, vy, vz);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World var1, IBlockState state) {
        try {
            return (TileEntity) tileEntityClass.getConstructor().newInstance();
        } catch (InstantiationException e) {

            e.printStackTrace();
        } catch (IllegalAccessException e) {

            e.printStackTrace();
        } catch (IllegalArgumentException e) {

            e.printStackTrace();
        } catch (InvocationTargetException e) {

            e.printStackTrace();
        } catch (NoSuchMethodException e) {

            e.printStackTrace();
        } catch (SecurityException e) {

            e.printStackTrace();
        }
        while (true) ;
    }


}




