package mods.eln.electricalentitysensor;


import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.BrushDescriptor;
import mods.eln.item.EntitySensorFilterDescriptor;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;

public class ElectricalEntitySensorSlowProcess implements IProcess {
	ElectricalEntitySensorElement element;
	public ElectricalEntitySensorSlowProcess(ElectricalEntitySensorElement element) {
		this.element = element;
	}
	double timeCounter = 0;
	static final double refreshPeriode = 0.2;

	@Override
	public void process(double time) {
		// TODO Auto-generated method stub
		timeCounter += time;
		if(timeCounter > refreshPeriode)
		{
			timeCounter -= refreshPeriode;
			Coordonate coord = element.sixNode.coordonate;
			ItemStack filterStack = element.inventory.getStackInSlot(ElectricalEntitySensorContainer.filterId);
			
			Class filterClass = EntityLivingBase.class;
			if(filterStack != null){
				GenericItemUsingDamageDescriptor gen = EntitySensorFilterDescriptor.getDescriptor(filterStack);
				if(gen != null && gen instanceof EntitySensorFilterDescriptor){
					EntitySensorFilterDescriptor filter = (EntitySensorFilterDescriptor) gen;
					filterClass = filter.entityClass;
				}
			}
			World world = coord.world();
			double rayMax = element.descriptor.maxRange;
			AxisAlignedBB bb = coord.getAxisAlignedBB((int) rayMax);
			List list = world.getEntitiesWithinAABB(filterClass, bb);
			double output = 0;
			for(Object o : list){
				Entity e = (Entity)o;
				double weight = 1;
				ArrayList<Block> blockList = Utils.traceRay(world,coord.x+0.5,coord.y+0.5,coord.z+0.5,e.posX,e.posY+e.getEyeHeight(),e.posZ);
				
				boolean view = true;
				for(Block b : blockList){
					if(b.isOpaqueCube()){
						view = false;
						break;
					}
				}
				if(view){
					double distance = Utils.getLength(coord.x+0.5,coord.y+0.5,coord.z+0.5,e.posX,e.posY+e.getEyeHeight(),e.posZ);
					if(distance < rayMax){
						output += weight*(rayMax-distance)/rayMax;
					}
				}
			}
			//System.out.println(output);
			element.outputGateProcess.setOutputNormalized(output);
		}

	}

}
