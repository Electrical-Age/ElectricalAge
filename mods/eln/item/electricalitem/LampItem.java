package mods.eln.item.electricalitem;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
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
		
		for(int yOffset = 0;yOffset < 2;yOffset++){
			double x = entity.posX,y = entity.posY+1.62-yOffset,z = entity.posZ;
		
			Vec3 v = entity.getLookVec();
			
			v.xCoord*=0.25;
			v.yCoord*=0.25;
			v.zCoord*=0.25;
			int range = getRange(stack)+1;
			int rCount = 0;
			for(int idx = 0;idx<range;idx++){
				x+=v.xCoord;
				y+=v.yCoord;
				z+=v.zCoord;
		
				Block block = world.getBlock(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z));
				if(block != Blocks.air && block != Eln.lightBlock /*&& Block.blocksList[blockId].isOpaqueCube() == false*/){
					x-=v.xCoord;
					y-=v.yCoord;
					z-=v.zCoord;		
					break;
				}
				rCount++;
			}
			while(rCount > 0){
	
				Block block = world.getBlock(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z));
				if(block == Blocks.air || block == Eln.lightBlock){
					//break;
					LightBlockEntity.addLight(world, MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z), light,10);
					return;/*
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

	
	
}
