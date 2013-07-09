package mods.eln.heatfurnace;

import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.generic.GenericItemUsingDamage;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.item.regulator.IRegulatorDescriptor;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import mods.eln.sim.RegulatorType;
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
		// TODO Auto-generated method stub
		ItemStack combustibleStack = furnace.inventory.getStackInSlot(HeatFurnaceContainer.combustibleId);
		ItemStack regulatorStack = furnace.inventory.getStackInSlot(HeatFurnaceContainer.regulatorId);
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
		
		if(combustibleStack != null && furnace.getTakeFuel() == true)
		{
			double itemEnergy = Utils.getItemEnergie(combustibleStack);
			if(itemEnergy != 0)
			{
				if(furnace.furnaceProcess.combustibleEnergy + combustibleBuffer < furnace.furnaceProcess.nominalCombustibleEnergy)
				{
				//	furnace.furnaceProcess.combustibleEnergy += itemEnergy;
					combustibleBuffer += itemEnergy;
					furnace.inventory.decrStackSize(HeatFurnaceContainer.combustibleId, 1);
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
		
		if(regulatorStack != null)
		{
			IRegulatorDescriptor regulator = (IRegulatorDescriptor) Eln.sharedItem.getDescriptor(regulatorStack);
			
			regulator.applyTo(furnace.regulator,500.0,10.0,0.1,0.1);
		//	furnace.regulator.target = 240;
		}
		else
		{
			furnace.regulator.setManuel();
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
