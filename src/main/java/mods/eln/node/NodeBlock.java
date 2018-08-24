package mods.eln.node;

import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public abstract class NodeBlock extends Block {//BlockContainer
    public int blockItemNbr;
    Class tileEntityClass;

    public NodeBlock(Material material, Class tileEntityClass, int blockItemNbr) {
        super(material);
        setRegistryName("NodeBlock");
        this.tileEntityClass = tileEntityClass;
        useNeighborBrightness = true;
        this.blockItemNbr = blockItemNbr;
        setHardness(1.0f);
        setResistance(1.0f);
    }


    public float getBlockHardness(World par1World, int par2, int par3, int par4) {

        return 1.0f;
    }



    public int isProvidingWeakPower(IBlockAccess block, BlockPos pos, int side) {
        NodeBlockEntity entity = (NodeBlockEntity) block.getTileEntity(pos);
        return entity.isProvidingWeakPower(Direction.fromIntMinecraftSide(side));
    }


    public boolean canConnectRedstone(IBlockAccess block, BlockPos pos, int side) {
        NodeBlockEntity entity = (NodeBlockEntity) block.getTileEntity(pos);
        return entity.canConnectRedstone(Direction.XN);
    }


    public boolean canProvidePower() {
        //NOT SURE
        return super.canProvidePower(this.getDefaultState());
    }


    public boolean isOpaqueCube() {
        return true;
    }


    public boolean renderAsNormalBlock() {
        return false;
    }


    public int getRenderType() {
        return -1;
    }

    public int getLightValue(IBlockAccess world, BlockPos pos) {
        final TileEntity entity = world.getTileEntity(pos);
        if (entity == null || !(entity instanceof NodeBlockEntity)) return 0;
        NodeBlockEntity tileEntity = (NodeBlockEntity) entity;
        return tileEntity.getLightValue();
    }


    //client server
    public boolean onBlockPlacedBy(World world, BlockPos pos, Direction front, EntityLivingBase entityLiving, int metadata) {

        NodeBlockEntity tileEntity = (NodeBlockEntity) world.getTileEntity(pos);

        tileEntity.onBlockPlacedBy(front, entityLiving, metadata);
        return true;
    }

    //server   
    public void onBlockAdded(World par1World, BlockPos pos) {
        if (par1World.isRemote == false) {
            NodeBlockEntity entity = (NodeBlockEntity) par1World.getTileEntity(pos);
            entity.onBlockAdded();
        }
    }


    //server
    public void breakBlock(World par1World, BlockPos pos, Block par5, int par6) {

        //if(par1World.isRemote == false)
        {
            NodeBlockEntity entity = (NodeBlockEntity) par1World.getTileEntity(pos);
            entity.onBreakBlock();
            super.breakBlock(par1World, pos, par5.getStateFromMeta(par6));
        }
    }


    public void onNeighborBlockChange(World world, BlockPos pos, Block b) {
        if (!Utils.isRemote(world)) {
            NodeBlockEntity entity = (NodeBlockEntity) world.getTileEntity(pos);
            entity.onNeighborBlockChange();
        }
    }



    public int damageDropped(int metadata) {
        return metadata;
    }

    //@SideOnly(Side.CLIENT)
    public void getSubBlocks(int par1, CreativeTabs tab, List subItems) {
        for (int ix = 0; ix < blockItemNbr; ix++) {
            subItems.add(new ItemStack(this, 1, ix));
        }
    }

    //client server
    public boolean onBlockActivated(World world, BlockPos pos, EntityPlayer entityPlayer, int side, float vx, float vy, float vz) {
        NodeBlockEntity entity = (NodeBlockEntity) world.getTileEntity(pos);
//    	entityPlayer.openGui( Eln.instance, 0,world,x ,y, z);
        return entity.onBlockActivated(entityPlayer, Direction.fromIntMinecraftSide(side), vx, vy, vz);
    }


    public boolean hasTileEntity(int metadata) {
        return true;
    }


    public TileEntity createTileEntity(World var1, int meta) {
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




