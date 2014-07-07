package mods.eln.sixnode.electricalwatch;

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
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.BrushDescriptor;
import mods.eln.item.ElectricalDrillDescriptor;
import mods.eln.item.EntitySensorFilterDescriptor;
import mods.eln.item.MiningPipeDescriptor;
import mods.eln.item.electricalitem.BatteryItem;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import mods.eln.transparentnode.autominer.AutoMinerContainer;

public class ElectricalWatchSlowProcess implements IProcess, INBTTReady {
	ElectricalWatchElement element;
	
	public ElectricalWatchSlowProcess(ElectricalWatchElement element) {
		this.element = element;
	}
	
	
	boolean upToDate = false;
	long oldDate = 1379;
	@Override
	public void process(double time) {
		ItemStack batteryStack = element.inventory.getStackInSlot(ElectricalWatchContainer.batteryId);
		BatteryItem battery = (BatteryItem) BatteryItem.getDescriptor(batteryStack);
		double energy;
		if(battery == null || (energy = battery.getEnergy(batteryStack)) < element.descriptor.powerConsumtion*time*4){
			if(upToDate == true){
				upToDate = false;
				oldDate = element.sixNode.coordonate.world().getWorldTime();
				battery.setEnergy(batteryStack, 0);
				element.needPublish();
			}
		}else{
			if(upToDate == false){
				upToDate = true;
				element.needPublish();
			}
			battery.setEnergy(batteryStack, energy-element.descriptor.powerConsumtion*time);
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		upToDate = nbt.getBoolean(str + "upToDate");
		oldDate = nbt.getLong(str + "oldDate");
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setBoolean(str + "upToDate",upToDate);
		nbt.setLong(str + "oldDate",oldDate);
	}
}
