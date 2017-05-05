package mods.eln.integration.minetweaker.machines;

import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import mods.eln.integration.minetweaker.utils.MinetweakerMachine;

@ZenClass("mods.electricalage.Compressor")
public class Compressor {

	@ZenMethod
	public static void addRecipe(IIngredient input, double energy, IItemStack[] output)
	{
		MinetweakerMachine.COMPRESSOR.addRecipe(input, energy, output);
	}

	@ZenMethod
	public static void removeRecipe(IIngredient input)
	{
		MinetweakerMachine.COMPRESSOR.removeRecipe(input);
	}
}
