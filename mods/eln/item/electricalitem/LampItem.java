package mods.eln.item.electricalitem;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.sixnode.lampsocket.LightBlockEntity;

public abstract class LampItem extends GenericItemUsingDamageDescriptor{

	public LampItem(String name) {
		super(name);
		
	}
	
	abstract int getLightState(ItemStack stack);
	abstract int getRange(ItemStack stack);
	abstract int getLight(ItemStack stack);
	
	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int par4,
			boolean par5) {
		if(world.isRemote) return;
		if(getLightState(stack) == 0 ) return;
		int light = getLight(stack);
		if(light == 0) return;
		
		double x = entity.posX,y = entity.posY+1.62,z = entity.posZ;
		
		Vec3 v = entity.getLookVec();
		v.xCoord*=0.25;
		v.yCoord*=0.25;
		v.zCoord*=0.25;
		int range = getRange(stack);
		int rCount = 0;
		for(int idx = 0;idx<range;idx++){
			x+=v.xCoord;
			y+=v.yCoord;
			z+=v.zCoord;
	
			Block block = world.getBlock((int)x, (int)y, (int)z);
			if(block != Blocks.air && block != Eln.instance.lightBlock /*&& Block.blocksList[blockId].isOpaqueCube() == false*/){
				x-=v.xCoord;
				y-=v.yCoord;
				z-=v.zCoord;		
				break;
			}
			rCount++;
		}
		while(rCount > 0){

			Block block = world.getBlock((int)x, (int)y, (int)z);
			if(block == Blocks.air || block == Eln.instance.lightBlock){
				//break;
				LightBlockEntity.addLight(world, (int)x, (int)y, (int)z, light,10);
				break;/*
				x-=v.xCoord*4;
				y-=v.yCoord*4;
				z-=v.zCoord*4;	
				rCount-=4;*/
			}
			x-=v.xCoord;
			y-=v.yCoord;
			z-=v.zCoord;	
			rCount--;
		}

	}

	
	
}
