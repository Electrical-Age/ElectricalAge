package mods.eln.api;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.item.ItemStack;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

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

    /**
     * Add a fuel to the gasoline fuels list (turbine and fuel generator).
     * Must be called before Eln starts preinit.
     * @param name The name of the fuel in the fuel registry
     * @return true if the addition succeeded, false otherwise.
     */
	public static boolean addGasolineFuel(String name){
        try {
            Class<?> FuelRegistry = Class.forName("mods.eln.misc.FuelRegistryKt");
            Field gasolineList = FuelRegistry.getDeclaredField("gasolineList");
            Utilities.makeModifiable(gasolineList);
			String[] gasolineArray = ((String[]) gasolineList.get(null));
			String[] newArray = Arrays.copyOf(gasolineArray, gasolineArray.length + 1);
			newArray[newArray.length - 1] = name;
			gasolineList.set(null, newArray);
            return true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Add a fuel to the gas fuels list (gas turbine).
     * Must be called before Eln starts preinit.
     * @param name The name of the fuel in the fuel registry
     * @return true if the addition succeeded, false otherwise.
     */
    public static boolean addGasFuel(String name){
        try {
            Class<?> FuelRegistry = Class.forName("mods.eln.misc.FuelRegistryKt");
            Field gasList = FuelRegistry.getDeclaredField("gasList");
            Utilities.makeModifiable(gasList);
            String[] gasArray = (String[]) gasList.get(null);
            String[] newList = Arrays.copyOf(gasArray, gasArray.length + 1);
            newList[newList.length - 1] = name;
            gasList.set(null, newList);
            return true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }


    public static boolean addDieselFuel(String name) throws NotImplementedException {
        throw new NotImplementedException(); //Diesel fuels aren't used yet
    }
}
