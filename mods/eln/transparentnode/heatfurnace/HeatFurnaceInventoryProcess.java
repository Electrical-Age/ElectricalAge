package mods.eln.transparentnode.heatfurnace;

import mods.eln.generic.GenericItemUsingDamage;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.Utils;
import mods.eln.server.SaveConfig;
import mods.eln.sim.IProcess;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class HeatFurnaceInventoryProcess implements IProcess , INBTTReady{

	HeatFurnaceElement furnace;
	//double combustibleEnergyMax = 40000*2;
	double combustibleBuffer = 0;
	
	public HeatFurnaceInventoryProcess(HeatFurnaceElement furnace) {
		this.furnace = furnace;
	}
	
	@Override
	public void process(double time) {
		
		ItemStack combustibleStack = furnace.inventory.getStackInSlot(HeatFurnaceContainer.combustibleId);
		ItemStack combustionChamberStack = furnace.inventory.getStackInSlot(HeatFurnaceContainer.combustrionChamberId);
		ItemStack isolatorChamberStack = furnace.inventory.getStackInSlot(HeatFurnaceContainer.isolatorId);
		


		
		
		
		double isolationFactor = 1;
		if(isolatorChamberStack != null)
		{
			ThermalIsolatorElement iso = (ThermalIsolatorElement) ((GenericItemUsingDamage)isolatorChamberStack.getItem()).getDescriptor(isolatorChamberStack);
			isolationFactor = iso.conductionFactor;
		}
		furnace.thermalLoad.setRp(furnace.descriptor.thermal.Rp / isolationFactor);
		
		
		int combustionChamberNbr = 0;
		if(combustionChamberStack != null)
		{
			combustionChamberNbr = combustionChamberStack.stackSize;
		}
		furnace.furnaceProcess.nominalPower = furnace.descriptor.nominalPower + furnace.descriptor.combustionChamberPower * combustionChamberNbr;
		
		if(furnace.getTakeFuel() == true)
		{
			if(! SaveConfig.instance.heatFurnaceFuel){
				combustibleBuffer =  furnace.furnaceProcess.nominalCombustibleEnergy;		
			} 
			else if(combustibleStack != null){
				double itemEnergy = Utils.getItemEnergie(combustibleStack);
				if(itemEnergy != 0)
				{
					if(furnace.furnaceProcess.combustibleEnergy + combustibleBuffer < furnace.furnaceProcess.nominalCombustibleEnergy)
					{
					//	furnace.furnaceProcess.combustibleEnergy += itemEnergy;
						combustibleBuffer += itemEnergy;
						furnace.inventory.decrStackSize(HeatFurnaceContainer.combustibleId, 1);
						if(combustibleStack.getItem() == Items.lava_bucket){
							furnace.inventory.setInventorySlotContents(HeatFurnaceContainer.combustibleId, new ItemStack(Items.bucket));
						}
					}
				}
			}
		}
		

		if(furnace.furnaceProcess.combustibleEnergy + combustibleBuffer < furnace.furnaceProcess.nominalCombustibleEnergy)
		{
			furnace.furnaceProcess.combustibleEnergy += combustibleBuffer;
			combustibleBuffer = 0;
		}
		else
		{
			double delta = furnace.furnaceProcess.nominalCombustibleEnergy - furnace.furnaceProcess.combustibleEnergy;
			furnace.furnaceProcess.combustibleEnergy += delta;
			combustibleBuffer -= delta;
		}


	
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		combustibleBuffer = nbt.getDouble(str + "HFIP" + "combustribleBuffer");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setDouble(str + "HFIP" + "combustribleBuffer", combustibleBuffer);
	}

}
