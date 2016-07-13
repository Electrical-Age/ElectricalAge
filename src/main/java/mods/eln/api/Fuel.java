package mods.eln.api;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.item.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Fuel {
	/**
	 * Gives the energy equivalent of a fuel in J. Returns 0 if the item cannot be burned.
	 * @param fuel The ItemStack of fuel to determine the energy equivalent of.
	 * @return fuel's energy equivalent in J.
	 */
	public static double getEnergyEquivalent(ItemStack fuel){
		try {
			Class<?> c = Class.forName("mods.eln.misc.Utils");
			Method energy = c.getDeclaredMethod("getItemEnergie", ItemStack.class);
			return Double.valueOf(energy.invoke(c, fuel).toString());
		}
		catch(ClassNotFoundException e){
			FMLLog.warning("ELN isn't loaded. Someone just tried to use its API.");
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return 0;
	}
}
