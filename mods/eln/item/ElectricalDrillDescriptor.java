package mods.eln.item;

import java.util.List;

import mods.eln.misc.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class ElectricalDrillDescriptor extends GenericItemUsingDamageDescriptorUpgrade {

	public ElectricalDrillDescriptor(
			String name,
			double operationTime,double operationEnergy
			) {
		super(name);
		this.OperationEnergy = operationEnergy;
		this.operationTime = operationTime;
		nominalPower = operationEnergy / operationTime;
	}

	
	public double nominalPower;
	public double operationTime,OperationEnergy;
	
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		
		super.addInformation(itemStack, entityPlayer, list, par4);
		
		list.add("Nominal :");
		list.add(Utils.plotPower("    Power :",nominalPower));
		list.add(Utils.plotTime("    Time per Operation :",operationTime));
		list.add(Utils.plotEnergy("Energy per Operation :",OperationEnergy));
	}
	
	

}
