package mods.eln.api;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

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
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Add a fuel to the gasoline fuels list (turbine and fuel generator).
     * @param name The name of the fuel in the fuel registry
     * @param heatingValue The energy for 1L of the fuel
     * @return true if the addition succeeded, false otherwise.
     */
    public static boolean addGasolineFuel(String name, Double heatingValue) {
        try {
            Class<?> FuelRegistry = Class.forName("mods.eln.fluid.FuelRegistry");
            Field gasolineList = FuelRegistry.getDeclaredField("gasolineList");
            Utilities.makeModifiable(gasolineList);
			String[] gasolineArray = ((String[]) gasolineList.get(null));
			String[] newArray = Arrays.copyOf(gasolineArray, gasolineArray.length + 1);
			newArray[newArray.length - 1] = name;
			gasolineList.set(null, newArray);

            Field gasolineFuels = FuelRegistry.getDeclaredField("gasolineFuels");
            Utilities.makeModifiable(gasolineFuels);
            Map<String, Double> gasolineMap = (Map<String, Double>) gasolineFuels.get(null);
            gasolineMap.put(name, heatingValue);
            return true;
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Add a fuel to the gas fuels list (gas turbine).
     * @param name The name of the fuel in the fuel registry
     * @param heatingValue The energy of the fuel in MJ per cubic meter
     * @return true if the addition succeeded, false otherwise.
     */
    public static boolean addGasFuel(String name, Double heatingValue) {
        try {
            Class<?> FuelRegistry = Class.forName("mods.eln.fluid.FuelRegistry");
            Field gasList = FuelRegistry.getDeclaredField("gasList");
            Utilities.makeModifiable(gasList);
            String[] gasArray = (String[]) gasList.get(null);
            String[] newList = Arrays.copyOf(gasArray, gasArray.length + 1);
            newList[newList.length - 1] = name;
            gasList.set(null, newList);

            Field gasFuels = FuelRegistry.getDeclaredField("gasFuels");
            Utilities.makeModifiable(gasFuels);
            Map<String, Double> gasMap = (Map<String, Double>) gasFuels.get(null);
            gasMap.put(name, heatingValue);
            gasFuels.set(null, gasMap);
            return true;
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * Add a fuel to the diesels list.
     * @param name         The name of the fuel in the fuel registry
     * @param heatingValue Energy for 1L of the fuel
     * @return true if the addition succeeded, false otherwise.
     */
    public static boolean addDieselFuel(String name, Double heatingValue) {
        try {
            Class<?> FuelRegistry = Class.forName("mods.eln.fluid.FuelRegistry");
            Field dieselFuels = FuelRegistry.getDeclaredField("dieselFuels");
            Utilities.makeModifiable(dieselFuels);
            Map<String, Double> dieselMap = (Map<String, Double>) dieselFuels.get(null);
            dieselMap.put(name, heatingValue);
            dieselFuels.set(null, dieselMap);

            Field dieselList = FuelRegistry.getDeclaredField("dieselList");
            Utilities.makeModifiable(dieselList);
            String[] dieselArray = (String[]) dieselList.get(null);
            String[] newList = Arrays.copyOf(dieselArray, dieselArray.length + 1);
            newList[newList.length - 1] = name;
            dieselList.set(null, newList);
            return true;
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return false;
    }

}
