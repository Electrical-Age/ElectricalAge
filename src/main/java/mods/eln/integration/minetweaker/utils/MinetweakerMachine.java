package mods.eln.integration.minetweaker.utils;

import java.util.ArrayList;

import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import mods.eln.Eln;
import mods.eln.misc.Recipe;
import mods.eln.misc.RecipesList;

import net.minecraft.item.ItemStack;

public class MinetweakerMachine {

	public static MinetweakerMachine MACERATOR = new MinetweakerMachine(Eln.instance.maceratorRecipes, "Adding Macerator Recipe", "Removing Macerator Recipe");
	public static MinetweakerMachine COMPRESSOR = new MinetweakerMachine(Eln.instance.compressorRecipes, "Adding Compressor Recipe", "Removing Compressor Recipe");
	public static MinetweakerMachine MAGNETIZER = new MinetweakerMachine(Eln.instance.magnetizerRecipes, "Adding Magnetizer Recipe", "Removing Magnetizer Recipe");
	public static MinetweakerMachine PLATEMACHINE = new MinetweakerMachine(Eln.instance.plateMachineRecipes, "Adding Plate Machine Recipe", "Removing Plate Machine Recipe");

	public RecipesList recipes;
	public String removeDesc;
	public String addDesc;

	public MinetweakerMachine(RecipesList recipes, String addDesc, String removeDesc) {
		this.recipes = recipes;
		this.addDesc = addDesc;
		this.removeDesc = removeDesc;
	}

	public void addRecipe(IIngredient input, double energy, IItemStack[] output)
	{
		if (input == null || output == null || energy < 0)
			return;
		if (output.length > 4) {
			throw new IllegalArgumentException("Too much outputs");
		}

		ArrayList<ItemStack> outs = new ArrayList<ItemStack>();
		for (IIngredient i : output) {
			ItemStack stack = MineTweakerMC.getItemStack(i);
			if (stack != null) {
				outs.add(stack);
			}
		}

		Recipe recipe = new Recipe(MineTweakerMC.getItemStack(input), outs.toArray(new ItemStack[] {}), energy);

		MineTweakerAPI.apply(new AddRecipe(this, recipe));
	}

	public void removeRecipe(IIngredient input)
	{
		MineTweakerAPI.apply(new RemoveRecipe(this, input));
	}

}
