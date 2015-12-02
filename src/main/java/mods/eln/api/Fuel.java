package mods.eln.api;

import mods.eln.misc.Utils;
import net.minecraft.item.ItemStack;

public class Fuel {
	/**
	 * Gives the energy equivalent of a fuel in J. Returns 0 if the item cannot be burned.
	 * @param fuel The ItemStack of fuel to determine the energy equivalent of.
	 * @return fuel's energy equivalent in J.
	 */
	public static double getEnergyEquivalent(ItemStack fuel){
		return Utils.getItemEnergie(fuel);
	}
}
