package mods.eln.node;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Random;

import org.lwjgl.Sys;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;


import mods.eln.CommonProxy;
import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public abstract class NodeBlock extends BlockContainer{//BlockContainer
	public int blockItemNbr;
	Class tileEntityClass;
	public NodeBlock ( Material material,Class tileEntityClass,int blockItemNbr) {
		super(material);
		setBlockName("NodeBlock");
		this.tileEntityClass = tileEntityClass;
		useNeighborBrightness = true;
		this.blockItemNbr = blockItemNbr;
		setHardness(1.0f);
		setResistance(1.0f);
	}
	
	@Override
	public float getBlockHardness(World par1World, int par2, int par3, int par4) {
		// TODO Auto-generated method stub
		return 1.0f;
	}
	

	
	@Override
    public int isProvidingWeakPower(IBlockAccess block, int x, int y, int z, int side)
    {
		NodeBlockEntity entity = (NodeBlockEntity) block.getTileEntity(x, y, z);
    	return entity.isProvidingWeakPower(Direction.fromIntMinecraftSide(side));
    }
	
	@Override
    public boolean canConnectRedstone(IBlockAccess block, int x, int y, int z, int side)
    {
		NodeBlockEntity entity = (NodeBlockEntity) block.getTileEntity(x, y, z);
    	return entity.canConnectRedstone(Direction.XN);
    }
	 
    @Override
	public boolean canProvidePower() {
		// TODO Auto-generated method stub
		return super.canProvidePower();
	}
	
	@Override
	public boolean isOpaqueCube() {
	  return true;
	}
	@Override
	public boolean renderAsNormalBlock() {
	  return false;
	}
	@Override
	public int getRenderType() {
	  return -1;
	}
/*
	@Override  //caca1.5.1
	public String getTextureFile () {
		return CommonProxy.BLOCK_PNG;
	}*/
	
    public int getLightValue(IBlockAccess world, int x, int y, int z) 
    {
    	NodeBlockEntity tileEntity = (NodeBlockEntity) world.getTileEntity(x, y, z);
    	return tileEntity.getLightValue();
    }
	
	
    public void func_85105_g(World world, int x, int y, int z, int metadata)
    {
    	Utils.println("CACATOTAL3");
        while(true);//DON'tCALL THAT XD	
    }
	
    public void onBlockPlacedBy(World par1World, int x, int y, int z, EntityLiving entityLiving)
    {
    	Utils.println("CACATOTAL2");
    	while(true);
    }   
    
    //client server
    public boolean onBlockPlacedBy(World world, int x, int y, int z, Direction front,EntityLivingBase entityLiving,int metadata)
    {

    	NodeBlockEntity tileEntity = (NodeBlockEntity) world.getTileEntity(x, y, z);

		tileEntity.onBlockPlacedBy(front,entityLiving,metadata);
		return true;
	}
    
    //server   
    public void onBlockAdded(World par1World, int x, int y, int z)
    {
    	if(par1World.isRemote == false)
    	{
    		NodeBlockEntity entity = (NodeBlockEntity) par1World.getTileEntity(x, y, z);
    		entity.onBlockAdded();
    	}   	
    }
    

    
    //server
    public void breakBlock(World par1World, int x, int y, int z, Block par5, int par6)
    {
    	
    	//if(par1World.isRemote == false)
    	{
    		NodeBlockEntity entity = (NodeBlockEntity) par1World.getTileEntity(x, y, z);
	    	entity.onBreakBlock();
	        super.breakBlock(par1World, x, y, z, par5, par6);
    	}
    }
    @Override
    public void onNeighborChange(IBlockAccess world, int x, int y, int z,
    		int tileX, int tileY, int tileZ) {
    	if(Utils.isRemote(world) == false)
    	{
    		NodeBlockEntity entity = (NodeBlockEntity) world.getTileEntity(x, y, z);
	    	entity.onNeighborBlockChange();
    	}
    }

   
	

	
	@Override
	public int damageDropped (int metadata) {
		return metadata;
	}
	//@SideOnly(Side.CLIENT)
	public void getSubBlocks(int par1, CreativeTabs tab, List subItems) {
		for (int ix = 0; ix < blockItemNbr; ix++) {
			subItems.add(new ItemStack(this, 1, ix));
		}
	}

   //client server
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer entityPlayer, int side, float vx, float vy, float vz)
    {   
    	NodeBlockEntity entity = (NodeBlockEntity) world.getTileEntity(x, y, z);
//    	entityPlayer.openGui( Eln.instance, 0,world,x ,y, z);
    	return entity.onBlockActivated( entityPlayer,  Direction.fromIntMinecraftSide(side),  vx,  vy,  vz);
    }

	@Override
	public TileEntity createNewTileEntity(World var1,int a) {
		// TODO Auto-generated method stub

		try {
			return (TileEntity) tileEntityClass.getConstructor().newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while(true);
	}


    
}




