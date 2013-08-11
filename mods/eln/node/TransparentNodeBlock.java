package mods.eln.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3Pool;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class TransparentNodeBlock extends NodeBlock{
	
	public TransparentNodeBlock(int id, Material material,
			Class tileEntityClass) {
		super(id, material, tileEntityClass, 0);
		// TODO Auto-generated constructor stub
	}



	
	//@SideOnly(Side.CLIENT)
	public void getSubBlocks(int par1, CreativeTabs tab, List subItems) {
		Eln.transparentNodeItem.getSubItems(par1, tab, subItems);
	}

	@Override
	public boolean isOpaqueCube() {
	  return false;
	}
	@Override
	public boolean renderAsNormalBlock() {
	  return false;
	}
	@Override
	public int getRenderType() {
	  return -1;
	}

    public void registerIcons(IconRegister par1IconRegister)
    {
    	
    }
    @Override
    public int getDamageValue(World world, int x, int y, int z) {
    	// TODO Auto-generated method stub
    	return ((TransparentNodeEntity) world.getBlockTileEntity(x, y, z)).getDamageValue( world,  x,  y,  z);
    }
	@Override
	public int getLightOpacity(World world, int x, int y, int z) {
		return (world.getBlockMetadata(x, y, z) & 3) << 6;
	}
	
	@Override
	public int idDropped(int par1, Random par2Random, int par3) {
		// TODO Auto-generated method stub
		return 0;
	}
    public int quantityDropped(Random par1Random)
    {
        return 0;
    }
	

    @Override
    public boolean canPlaceBlockOnSide(World par1World, int par2, int par3, int par4, int par5)
    {
    	return true;
    }

    
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB par5AxisAlignedBB, List list, Entity entity)
    {
     //   this.setBlockBoundsBasedOnState(world,x, y, z);
      //  super.addCollisionBoxesToList(world, x, y, z, par5AxisAlignedBB, list, entity);
    	TransparentNodeEntity tileEntity = (TransparentNodeEntity) world.getBlockTileEntity(x, y, z);
    	if(tileEntity == null){
    		super.addCollisionBoxesToList(world, x, y, z, par5AxisAlignedBB, list, entity);
    	}
    	else{
    		tileEntity.addCollisionBoxesToList(par5AxisAlignedBB, list);
    	}
        //System.out.println(list);
    }



 
}
