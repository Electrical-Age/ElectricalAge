package mods.eln.lampsocket;

import java.util.ArrayList;
import java.util.Random;

import cpw.mods.fml.common.FMLCommonHandler;

import mods.eln.lampsocket.LightBlockEntity.LightBlockObserver;
import mods.eln.misc.Coordonate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class LightBlock extends BlockContainer {
	

	public LightBlock(int par1) {
		super(par1, Material.air);
		// TODO Auto-generated constructor stub
	}
	public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 start, Vec3 end)
    {
		return null;
    }
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World par1World, int par2, int par3, int par4)
    {
    	return null;
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
	public int idDropped(int par1, Random par2Random, int par3) {
		// TODO Auto-generated method stub
		return 0;
	}
    public int quantityDropped(Random par1Random)
    {
        return 0;
    }
    @Override
    public boolean isBlockReplaceable(World world, int x, int y, int z) {
    	return true;
    }
    
    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
    	// TODO Auto-generated method stub
    	/*if(FMLCommonHandler.instance().getEffectiveSide().isClient()){
    		System.out.println("Light at " + x +":"+ y +":" + z +" " + FMLCommonHandler.instance().getEffectiveSide().toString() + " get light " + world.getBlockMetadata(x, y, z));
    	}*/
    //	System.out.println("Light at " + x +":"+ y +":" + z +" " + FMLCommonHandler.instance().getEffectiveSide().toString() + " get light " + world.getBlockMetadata(x, y, z));
    	return world.getBlockMetadata(x, y, z);
    	
    	//return ((LightBlockEntity)world.getBlockTileEntity(x, y, z)).getClientLight();
    }
	@Override
	public TileEntity createNewTileEntity(World world) {
		// TODO Auto-generated method stub
		return new LightBlockEntity();
	}
	
	@Override
	public void breakBlock(World world, int x, int y, int z,
			int par5, int par6) {
		Coordonate coord = new Coordonate(x, y, z, world);
		for(LightBlockObserver o : LightBlockEntity.observers)
		{
			o.lightBlockDestructor(coord);
		}
	/*	world.updateLightByType(EnumSkyBlock.Block,x,y,z);
		if(world.isRemote){
			
		}*/
		super.breakBlock(world, x, y, z, par5, par6);
	}
}
