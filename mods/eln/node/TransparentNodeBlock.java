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
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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
	
	public TransparentNodeBlock( Material material,
			Class tileEntityClass) {
		super( material, tileEntityClass, 0);
		// TODO Auto-generated constructor stub
	}



	
	//@SideOnly(Side.CLIENT)
	public void getSubBlocks(Item par1, CreativeTabs tab, List subItems) {
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

	
	
    @Override
    public boolean removedByPlayer(World world, EntityPlayer entityPlayer, int x, int y, int z) 
    {  	
		if(!world.isRemote){
			NodeBlockEntity entity = (NodeBlockEntity) world.getTileEntity(x, y, z);
			if(entity != null){
				NodeBase nodeBase = entity.getNode();
				if(nodeBase instanceof TransparentNode){
					TransparentNode t = (TransparentNode) nodeBase;
					t.removedByPlayer = (EntityPlayerMP)entityPlayer;
				}
			}
		}

    	return super.removedByPlayer(world, entityPlayer, x, y, z);

    }
	
    @Override
    public int getDamageValue(World world, int x, int y, int z) {
    	// TODO Auto-generated method stub
    	return ((TransparentNodeEntity) world.getTileEntity(x, y, z)).getDamageValue( world,  x,  y,  z);
    }
    
    

	@Override
	public int getLightOpacity(IBlockAccess world, int x, int y, int z) {
		return (world.getBlockMetadata(x, y, z) & 3) << 6;
	}
	
	
	@Override
	public Item getItemDropped(int p_149650_1_, Random p_149650_2_,
			int p_149650_3_) {
		return null;
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
    	TransparentNodeEntity tileEntity = (TransparentNodeEntity) world.getTileEntity(x, y, z);
    	if(tileEntity == null){
    		super.addCollisionBoxesToList(world, x, y, z, par5AxisAlignedBB, list, entity);
    	}
    	else{
    		tileEntity.addCollisionBoxesToList(par5AxisAlignedBB, list);
    	}
        //Utils.println(list);
    }



 
}
