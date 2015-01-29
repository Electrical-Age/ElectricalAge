package mods.eln.item.electricalinterface;

import net.minecraft.item.ItemStack;

public interface IItemEnergyBattery {

	//double putEnergy(ItemStack stack,double energy,double time);
	void setEnergy(ItemStack stack, double value);
	double getEnergy(ItemStack stack);
	double getEnergyMax(ItemStack stack);
	
	double getChargePower(ItemStack stack);
	double getDischagePower(ItemStack stack);
	
	int getPriority(ItemStack stack);
	
	void electricalItemUpdate(ItemStack stack, double time);
}
