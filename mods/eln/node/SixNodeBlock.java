package mods.eln.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import mods.eln.Eln;
import mods.eln.item.SixNodeCacheItem;
import mods.eln.misc.Direction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
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
import net.minecraft.world.chunk.Chunk;

public class SixNodeBlock extends NodeBlock{
	//public static ArrayList<Integer> repertoriedItemStackId = new ArrayList<Integer>();
	
	public SixNodeBlock (int id, Material material,Class tileEntityClass) {
		super(id, material, tileEntityClass, 0);
	}

    public AxisAlignedBB getCollisionBoundingBoxFromPool(World par1World, int par2, int par3, int par4)
    {
        if(nodeHasCache(par1World, par2, par3, par4) || hasVolume(par1World, par2, par3, par4))
        	return super.getCollisionBoundingBoxFromPool(par1World, par2, par3, par4);
        else
        	return null;
    }

    public boolean hasVolume(World world, int x, int y, int z) {
		SixNodeEntity entity = getEntity(world, x, y, z);
		if(entity == null) return false;
		return entity.hasVolume(world,x,y,z);
	
	}

	@Override
    public float getBlockHardness(World world, int x, int y, int z) {
    	return 0.3f;
    }
	
	@Override
	public int getDamageValue(World world, int x, int y, int z) {
		// TODO Auto-generated method stub
		return ((SixNodeEntity) world.getBlockTileEntity(x, y, z)).getDamageValue( world,  x,  y,  z);
	}
    
    SixNodeEntity getEntity(World world, int x, int y, int z)
    {
    	SixNodeEntity tileEntity = (SixNodeEntity) world.getBlockTileEntity(x, y, z);
		if(tileEntity != null)
			return tileEntity;
		System.out.println("ASSERTSixNodeEntity getEntity() null");
		return null;
		   	
    }
	//@SideOnly(Side.CLIENT)
	public void getSubBlocks(int par1, CreativeTabs tab, List subItems) {
		/*for (Integer id : repertoriedItemStackId) {
			subItems.add(new ItemStack(this, 1, id));
		}*/
		Eln.sixNodeItem.getSubItems(par1, tab, subItems);
	}
    public void registerIcons(IconRegister par1IconRegister)
    {
    	
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
/*
	@Override
	public int getLightOpacity(World world, int x, int y, int z) {
		// TODO Auto-generated method stub
		return 255;
	}*/

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
    	return false;
    }
    @Override
    public boolean canPlaceBlockOnSide(World par1World, int par2, int par3, int par4, int par5)
    {
    	return true;/*
    	if(par1World.isRemote) return true;
		SixNodeEntity tileEntity = (SixNodeEntity) par1World.getBlockTileEntity(par2, par3, par4);
		if(tileEntity == null || (tileEntity instanceof SixNodeEntity) == false) return true;
		Direction direction = Direction.fromIntMinecraftSide(par5);
		SixNode node = (SixNode) tileEntity.getNode();
		if(node == null) return true;
		if(node.getSideEnable(direction))return false;

        return true;*/
    }


    @Override
    public boolean canPlaceBlockAt(World par1World, int par2, int par3, int par4)
    {
    	System.out.println("CACATOTAL1");
        while(true);//DON'tCALL THAT XD
    }
    
    

    @Override
    public boolean onBlockPlacedBy(World world, int x, int y, int z, Direction direction,EntityLivingBase entityLiving,int metadata)
    {

		return true;
	}
    
    /*
	@Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer entityPlayer, int minecraftSide, float vx, float vy, float vz)
    {
		SixNodeEntity tileEntity = (SixNodeEntity) world.getBlockTileEntity(x, y, z);
		 
		return tileEntity.onBlockActivated(entityPlayer, Direction.fromIntMinecraftSide(minecraftSide),vx,vy,vz);
    }
	*/
    @Override
    public boolean removeBlockByPlayer(World world, EntityPlayer entityPlayer, int x, int y, int z) 
    {
    	if(world.isRemote) return false;
    	
    	SixNodeEntity tileEntity = (SixNodeEntity) world.getBlockTileEntity(x, y, z);
    	 	
		MovingObjectPosition MOP = collisionRayTrace(world,x,y,z,entityPlayer);
		if(MOP == null) return false;
		
		
		SixNode sixNode = (SixNode) tileEntity.getNode();
		if(sixNode.sixNodeCacheMapId >= 0)
		{
			if(SixNodeCacheItem.map[sixNode.sixNodeCacheMapId] != null)
			{
				ItemStack stack = SixNodeCacheItem.map[sixNode.sixNodeCacheMapId].newItemStack(1);
				sixNode.dropItem(stack);
			}
			sixNode.sixNodeCacheMapId = -1;
			Chunk chunk = world.getChunkFromBlockCoords(x, z);
			chunk.generateHeightMap();
			chunk.updateSkylight();
			chunk.generateSkylightMap();
			world.updateAllLightTypes(x,y,z);
			sixNode.setNeedPublish(true);
			return false;
		}
		if(false == sixNode.deleteSubBlock(Direction.fromIntMinecraftSide(MOP.sideHit))) return false;

		if(sixNode.getIfSideRemain()) return true;
		
        return super.removeBlockByPlayer(world, entityPlayer, x, y, z);
    }
    @Override
    public void breakBlock(World world, int x, int y, int z, int par5, int par6)
    {    	
    	
    	if(world.isRemote == false)
    	{
    		SixNodeEntity tileEntity = (SixNodeEntity) world.getBlockTileEntity(x, y, z);
    		SixNode sixNode = (SixNode) tileEntity.getNode();
    		for(Direction direction : Direction.values())
        	{
    			if(sixNode.getSideEnable(direction))
    			{
    				sixNode.deleteSubBlock(direction); 	
    			}
        	}
    	}
    	super.breakBlock(world, x, y, z, par5, par6);
    }
    
    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, int par5)
    {
    	SixNodeEntity tileEntity = (SixNodeEntity) world.getBlockTileEntity(x, y, z);
   		SixNode sixNode = (SixNode) tileEntity.getNode();
 
		
		for(Direction direction : Direction.values())
    	{
			if(sixNode.getSideEnable(direction))
			{
	    		if(! getIfOtherBlockIsSolid(world, x, y, z,direction))
	    		{
	    			sixNode.deleteSubBlock(direction); 
	    		} 	
			}
    	}
		
		if(! sixNode.getIfSideRemain())
		{
			world.setBlock(x, y, z, 0); //caca1.5.1
		}
		else
		{
			super.onNeighborBlockChange(world, x, y, z, par5);
		}
    }
   
    
    
    double w = 0.0;

    boolean[] booltemp = new boolean[6];
    @Override
    public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 start, Vec3 end)
    {
    	if(nodeHasCache(world, x, y, z)) return super.collisionRayTrace(world, x, y, z, start, end);
    	SixNodeEntity tileEntity = (SixNodeEntity) world.getBlockTileEntity(x, y, z);

    	if(world.isRemote)
    	{
    		booltemp[0] = tileEntity.getSyncronizedSideEnable(Direction.XN);
    		booltemp[1] = tileEntity.getSyncronizedSideEnable(Direction.XP);
    		booltemp[2] = tileEntity.getSyncronizedSideEnable(Direction.YN);
    		booltemp[3] = tileEntity.getSyncronizedSideEnable(Direction.YP);
    		booltemp[4] = tileEntity.getSyncronizedSideEnable(Direction.ZN);
    		booltemp[5] = tileEntity.getSyncronizedSideEnable(Direction.ZP);
        	SixNodeEntity entity= getEntity(world, x, y, z);
        	if(entity != null)
        	{
        		SixNodeElementRender element = entity.elementRenderList[Direction.YN.getInt()];
        		if(element != null && element.sixNodeDescriptor.hasVolume())
        			return new MovingObjectPosition(x, y, z, Direction.YN.toSideValue(), Vec3.createVectorHelper(0.5,0.5,0.5));

        	}

    	}
    	else
    	{
    		SixNode sixNode = (SixNode) tileEntity.getNode();
    		booltemp[0] = sixNode.getSideEnable(Direction.XN);
    		booltemp[1] = sixNode.getSideEnable(Direction.XP);
    		booltemp[2] = sixNode.getSideEnable(Direction.YN);
    		booltemp[3] = sixNode.getSideEnable(Direction.YP);
    		booltemp[4] = sixNode.getSideEnable(Direction.ZN);
    		booltemp[5] = sixNode.getSideEnable(Direction.ZP);
        	SixNodeEntity entity= getEntity(world, x, y, z);
        	if(entity != null)
        	{
        		NodeBase node = entity.getNode();
        		if(node != null && node instanceof SixNode)
        		{
        			SixNodeElement element = ((SixNode)node).sideElementList[Direction.YN.getInt()];
        			if(element != null && element.sixNodeElementDescriptor.hasVolume())
        				return new MovingObjectPosition(x, y, z, Direction.YN.toSideValue(), Vec3.createVectorHelper(0.5,0.5,0.5));
        		}
        	}

    	}
    	//XN
    	
    	if(isIn(x,end.xCoord,start.xCoord) &&  booltemp[0])
    	{
    		double hitX,hitY,hitZ,ratio;
    		ratio = (x-start.xCoord)/(end.xCoord-start.xCoord);
    		if(ratio <= 1.1)
    		{
    			hitX =  start.xCoord + ratio * (end.xCoord-start.xCoord); hitY =  start.yCoord + ratio * (end.yCoord-start.yCoord); hitZ = start.zCoord + ratio * (end.zCoord-start.zCoord);
    			if(isIn(hitY,y+w,y+1-w) && isIn(hitZ,z+w,z+1-w)) 
    				return new MovingObjectPosition(x, y, z, Direction.XN.toSideValue(), Vec3.createVectorHelper(hitX,hitY,hitZ));
    		}
    	}
    	//XP
    	if(isIn(x+1,start.xCoord,end.xCoord) && booltemp[1])
    	{
    		double hitX,hitY,hitZ,ratio;
    		ratio = (x+1-start.xCoord)/(end.xCoord-start.xCoord);
    		if(ratio <= 1.1)
    		{
    			hitX =  start.xCoord + ratio * (end.xCoord-start.xCoord); hitY =  start.yCoord + ratio * (end.yCoord-start.yCoord); hitZ = start.zCoord + ratio * (end.zCoord-start.zCoord);
    			if(isIn(hitY,y+w,y+1-w) && isIn(hitZ,z+w,z+1-w)) 
    				return new MovingObjectPosition(x, y, z, Direction.XP.toSideValue(), Vec3.createVectorHelper(hitX,hitY,hitZ));
    		}
    	}
    	//YN
    	if(isIn(y,end.yCoord,start.yCoord) && booltemp[2])
    	{
    		double hitX,hitY,hitZ,ratio;
    		ratio = (y-start.yCoord)/(end.yCoord-start.yCoord);
    		if(ratio <= 1.1)
    		{
    			hitX =  start.xCoord + ratio * (end.xCoord-start.xCoord); hitY =  start.yCoord + ratio * (end.yCoord-start.yCoord); hitZ = start.zCoord + ratio * (end.zCoord-start.zCoord);
    			if(isIn(hitX,x+w,x+1-w) && isIn(hitZ,z+w,z+1-w)) 
    				return new MovingObjectPosition(x, y, z, Direction.YN.toSideValue(), Vec3.createVectorHelper(hitX,hitY,hitZ));
    		}
    		
    	}
    	//YP
    	if(isIn(y+1,start.yCoord,end.yCoord) && booltemp[3])
    	{
    		double hitX,hitY,hitZ,ratio; 
    		ratio = (y+1-start.yCoord)/(end.yCoord-start.yCoord);
    		if(ratio <= 1.1)
    		{
    			hitX =  start.xCoord + ratio * (end.xCoord-start.xCoord); hitY =  start.yCoord + ratio * (end.yCoord-start.yCoord); hitZ = start.zCoord + ratio * (end.zCoord-start.zCoord);
    			if(isIn(hitX,x+w,x+1-w) && isIn(hitZ,z+w,z+1-w)) 
    				return new MovingObjectPosition(x, y, z, Direction.YP.toSideValue(), Vec3.createVectorHelper(hitX,hitY,hitZ));
    		}
    	}
    	//ZN
    	if(isIn(z,end.zCoord,start.zCoord)  && booltemp[4])
    	{
    		double hitX,hitY,hitZ,ratio;
    		ratio = (z-start.zCoord)/(end.zCoord-start.zCoord);
    		if(ratio <= 1.1)
    		{
    			hitX =  start.xCoord + ratio * (end.xCoord-start.xCoord); hitY =  start.yCoord + ratio * (end.yCoord-start.yCoord); hitZ = start.zCoord + ratio * (end.zCoord-start.zCoord);
    			if(isIn(hitY,y+w,y+1-w) && isIn(hitX,x+w,x+1-w)) 
    				return new MovingObjectPosition(x, y, z, Direction.ZN.toSideValue(), Vec3.createVectorHelper(hitX,hitY,hitZ));
    		}
    	}
    	//ZP
    	if(isIn(z+1,start.zCoord,end.zCoord)  && booltemp[5])
    	{
    		double hitX,hitY,hitZ,ratio;
    		ratio = (z+1-start.zCoord)/(end.zCoord-start.zCoord);
    		if(ratio <= 1.1)
    		{
    			hitX =  start.xCoord + ratio * (end.xCoord-start.xCoord); hitY =  start.yCoord + ratio * (end.yCoord-start.yCoord); hitZ = start.zCoord + ratio * (end.zCoord-start.zCoord);
    			if(isIn(hitY,y+w,y+1-w) && isIn(hitX,x+w,x+1-w)) 
    				return new MovingObjectPosition(x, y, z, Direction.ZP.toSideValue(), Vec3.createVectorHelper(hitX,hitY,hitZ));
    		}
    	}

    	return null;
    }

	public static boolean isIn(double value,double min,double max)
	{
		if(value >= min && value <= max) return true;
		return false;
	}
	
    public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z,EntityPlayer entityLiving)
    {
    	
    	//double distanceMax = (double)Minecraft.getMinecraft().playerController.getBlockReachDistance();
    	double distanceMax = 5.0;
    	Vec3 start = Vec3.fakePool.getVecFromPool(entityLiving.posX, entityLiving.posY, entityLiving.posZ);
    	
		//Vec3 start = entityLiving.getPosition(0.5f);
		if(!world.isRemote) start.yCoord += 1.62;
		Vec3 var5 = entityLiving.getLook(0.5f);
		Vec3 end = start.addVector(var5.xCoord * distanceMax, var5.yCoord * distanceMax, var5.zCoord * distanceMax);
	//Vec3 startVect,endVect;
    	//entityLiving.EntityPlayerMP
    	return collisionRayTrace(world,x,y,z,start,end);
    }
    
	public boolean getIfOtherBlockIsSolid(World world,int x, int y, int z,Direction direction) {
		
		
 		int[]vect = new int[3];
		vect[0] = x;
		vect[1] = y;
		vect[2] = z;
		direction.applyTo(vect, 1);    
		
		// TODO Auto-generated method stub
		int blockId = world.getBlockId(vect[0],vect[1],vect[2]);
		if(blockId == 0) return false;
		Block block = Block.blocksList[blockId];
		if(block == null) return false;
		if(block.isOpaqueCube()) return true;
		
		return false;
	}
	
	public boolean nodeHasCache(World world, int x, int y, int z) 
    {
    	if(world.isRemote)
    	{
    		TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
    		if(tileEntity != null)
    			return ((SixNodeEntity)tileEntity).sixNodeCacheMapId >= 0;
			else
				System.out.println("ASSERT B public boolean nodeHasCache(World world, int x, int y, int z) ");
    		 
    	}
    	else
    	{   	
	    	SixNodeEntity tileEntity = (SixNodeEntity) world.getBlockTileEntity(x, y, z);
			SixNode sixNode = (SixNode) tileEntity.getNode();
			if(sixNode != null)
				return sixNode.sixNodeCacheMapId >= 0;
			else
				System.out.println("ASSERT A public boolean nodeHasCache(World world, int x, int y, int z) ");
    	}
    	return false;
	}
	
	
	@Override
	public int getLightOpacity(World world, int x, int y, int z) {
		if(nodeHasCache(world, x, y, z))
			return 255;
		else
			return 0;
	}
}
