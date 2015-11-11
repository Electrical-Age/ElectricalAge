package mods.eln.integration.minetweaker.machines;

import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import mods.eln.integration.minetweaker.utils.MinetweakerMachine;

@ZenClass("mods.electricalage.Magnetizer")
public class Magnetizer {
	
	@ZenMethod
	public static void addRecipe(IIngredient input, double energy, IItemStack[] output)
	{
		MinetweakerMachine.MAGNETIZER.addRecipe(input, energy, output);
	}

	@ZenMethod
	public static void removeRecipe(IIngredient input)
	{
		MinetweakerMachine.MAGNETIZER.removeRecipe(input);
	}
	
}
