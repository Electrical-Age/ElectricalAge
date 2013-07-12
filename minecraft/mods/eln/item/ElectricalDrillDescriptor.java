package mods.eln.item;

import java.util.List;

import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.misc.Utils;
import mods.eln.sim.IVoltageWatchdogDescriptor;
import mods.eln.sim.IVoltageWatchdogDescriptorForInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class ElectricalDrillDescriptor extends GenericItemUsingDamageDescriptor implements IVoltageWatchdogDescriptorForInventory{

	public ElectricalDrillDescriptor(
			String name,
			double nominalVoltage,double maximalVoltage,
			double operationTime,double operationEnergy
			) {
		super(name);
		this.OperationEnergy = operationEnergy;
		this.operationTime = operationTime;
		this.nominalVoltage = nominalVoltage;
		this.maximalVoltage = maximalVoltage;
		nominalPower = operationEnergy / operationTime;
	}

	
	public double nominalPower;
	public double nominalVoltage,maximalVoltage;
	public double operationTime,OperationEnergy;
	
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		
		list.add("Nominal :");
		list.add(Utils.plotVolt("    voltage",nominalVoltage));
		list.add(Utils.plotPower("    power",nominalPower));
		list.add(Utils.plotTime("    time per operation",operationTime));
		list.add(Utils.plotEnergy("Energy per operation",OperationEnergy));
	}
	
	
	public double getRp(double additionnalEnergy)
	{
		double power = (OperationEnergy + additionnalEnergy) / operationTime;
		return nominalVoltage * nominalVoltage / power;
	}


	@Override
	public double getUmax() {
		// TODO Auto-generated method stub
		return maximalVoltage;
	}


	@Override
	public double getUmin() {
		// TODO Auto-generated method stub
		return maximalVoltage * -0.1;
	}


	@Override
	public double getBreakPropPerVoltOverflow() {
		// TODO Auto-generated method stub
		return 1/maximalVoltage/0.2;
	}
}
