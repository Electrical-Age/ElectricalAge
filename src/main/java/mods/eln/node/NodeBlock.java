package mods.eln.node;

import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public abstract class NodeBlock extends Block {//BlockContainer
    public int blockItemNbr;
    Class tileEntityClass;

    public NodeBlock(Material material, Class tileEntityClass, int blockItemNbr) {
        super(material);
        setUnlocalizedName("NodeBlock");
        this.tileEntityClass = tileEntityClass;
        useNeighborBrightness = true;
        this.blockItemNbr = blockItemNbr;
        setHardness(1.0f);
        setResistance(1.0f);
    }

    @Override
    public float getBlockHardness(IBlockState blockState, World worldIn, BlockPos pos) {
        return 1.0f;
    }

    @Override
    public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        NodeBlockEntity entity = (NodeBlockEntity) blockAccess.getTileEntity(pos);
        return entity.isProvidingWeakPower(Direction.fromFacing(side));
    }

    @Override
    public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        NodeBlockEntity entity = (NodeBlockEntity) world.getTileEntity(pos);
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

    //@Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        //-1
        return this.getRenderType(state);
    }


    public int getLightValue(IBlockAccess world, BlockPos pos) {
        final TileEntity entity = world.getTileEntity(pos);
        if (entity == null || !(entity instanceof NodeBlockEntity)) return 0;
        NodeBlockEntity tileEntity = (NodeBlockEntity) entity;
        return tileEntity.getLightValue();
    }


    //client server
    public boolean onBlockPlacedBy(World world, BlockPos pos, Direction front, EntityLivingBase entityLiving, IBlockState state) {

        NodeBlockEntity tileEntity = (NodeBlockEntity) world.getTileEntity(pos);

        tileEntity.onBlockPlacedBy(front, entityLiving, state);
        return true;
    }

    @SideOnly(Side.SERVER)
    public void onBlockAdded(World par1World, BlockPos pos) {
        if (!par1World.isRemote) {
            NodeBlockEntity entity = (NodeBlockEntity) par1World.getTileEntity(pos);
            entity.onBlockAdded();
        }
    }


    @SideOnly(Side.SERVER)
    public void breakBlock(World par1World, BlockPos pos, Block par5, int par6) {
        if(!par1World.isRemote)
        {
            NodeBlockEntity entity = (NodeBlockEntity) par1World.getTileEntity(pos);
            entity.onBreakBlock();
            super.breakBlock(par1World, pos, par5.getStateFromMeta( par6));
        }
    }

    @Override
    public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor) {
        if (!Utils.isRemote(world)) {
            NodeBlockEntity entity = (NodeBlockEntity) world.getTileEntity(pos);
            entity.onNeighborBlockChange();
        }
    }


    @Override
    public int damageDropped(IBlockState state) {
        return getMetaFromState(state);
    }

    //@SideOnly(Side.CLIENT)
    public void getSubBlocks(int par1, CreativeTabs tab, List subItems) {
        for (int ix = 0; ix < blockItemNbr; ix++) {
            subItems.add(new ItemStack(this, 1, ix));
        }
    }

    //client server
    public boolean onBlockActivated(World world, BlockPos pos, EntityPlayer entityPlayer, EnumFacing side, float vx, float vy, float vz) {
        NodeBlockEntity entity = (NodeBlockEntity) world.getTileEntity(pos);
//    	entityPlayer.openGui( Eln.instance, 0,world,x ,y, z);
        return entity.onBlockActivated(entityPlayer, Direction.fromFacing(side), vx, vy, vz);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return hasTileEntity(getDefaultState());
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




