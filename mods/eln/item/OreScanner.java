package mods.eln.item;

import java.util.List;

import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.misc.Utils;
import mods.eln.sim.IVoltageWatchdogDescriptor;
import mods.eln.sim.IVoltageWatchdogDescriptorForInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class OreScanner extends GenericItemUsingDamageDescriptorUpgrade implements IVoltageWatchdogDescriptorForInventory{

	public OreScanner(
			String name,
			double nominalVoltage,double maximalVoltage,
			int operationRadius,double operationEnergy

			
			) {
		super(name);
		this.radius = operationRadius;
		this.OperationEnergy = operationEnergy;
		this.nominalVoltage = nominalVoltage;
		this.maximalVoltage = maximalVoltage;
		// TODO Auto-generated constructor stub
	}

	
	public double nominalVoltage,maximalVoltage;
	public double OperationEnergy;
	public int radius;
	
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		
		list.add("Nominal :");
		list.add(Utils.plotVolt("    voltage",nominalVoltage));
		list.add(Utils.plotEnergy("Energy per operation",OperationEnergy));
		list.add("Scann area " + (radius*2 +1)*(radius*2 +1) + " blocks");

	}



	@Override
	public double getBreakPropPerVoltOverflow() {
		// TODO Auto-generated method stub
		return 1/maximalVoltage/0.2;
	}


	@Override
	public double getUmax() {
		// TODO Auto-generated method stub
		return maximalVoltage;
	}


	@Override
	public double getUmin() {
		// TODO Auto-generated method stub
		return -0.1 * maximalVoltage;
	}
}
