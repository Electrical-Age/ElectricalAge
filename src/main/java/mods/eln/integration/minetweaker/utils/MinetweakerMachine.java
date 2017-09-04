package mods.eln.integration.minetweaker.utils;

import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import mods.eln.api.recipe.*;
import net.minecraft.item.ItemStack;

public class MinetweakerMachine {

    public static MinetweakerMachine MACERATOR = new MinetweakerMachine(new MaceratorRecipeList(), "Adding Macerator Recipe", "Removing Macerator Recipe");
    public static MinetweakerMachine COMPRESSOR = new MinetweakerMachine(new CompressorRecipeList(), "Adding Compressor Recipe", "Removing Compressor Recipe");
    public static MinetweakerMachine MAGNETIZER = new MinetweakerMachine(new MagnetizerRecipeList(), "Adding Magnetizer Recipe", "Removing Magnetizer Recipe");
    public static MinetweakerMachine PLATEMACHINE = new MinetweakerMachine(new PlateMachineRecipeList(), "Adding Plate Machine Recipe", "Removing Plate Machine Recipe");

    public IELNMachineRecipeList recipes;
    public String removeDesc;
	public String addDesc;

    public MinetweakerMachine(IELNMachineRecipeList recipeList, String addDesc, String removeDesc) {
        this.recipes = recipeList;
        this.addDesc = addDesc;
		this.removeDesc = removeDesc;
	}

	public void addRecipe(IIngredient input, double energy, IItemStack[] output)
	{
        ItemStack inStack = MineTweakerMC.getItemStack(input);
        ItemStack[] outStacks = new ItemStack[output.length];
        for (int i = 0; i < output.length; i++) {
            outStacks[i] = MineTweakerMC.getItemStack(output[i]);
        }

        Recipe recipe = new Recipe(inStack, outStacks, energy);

		MineTweakerAPI.apply(new AddRecipe(this, recipe));
	}

	public void removeRecipe(IIngredient input)
	{
		MineTweakerAPI.apply(new RemoveRecipe(this, input));
	}

}
