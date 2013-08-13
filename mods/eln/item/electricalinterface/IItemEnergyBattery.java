package mods.eln.item.electricalinterface;

import net.minecraft.item.ItemStack;

public interface IItemEnergyBattery {
	double putEnergy(ItemStack stack,double energy,double time);
}
