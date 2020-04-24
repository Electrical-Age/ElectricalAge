package mods.eln.api.recipe;

import mods.eln.api.Misc;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;

public class CompressorRecipeList implements IELNMachineRecipeList {
	private static RecipesList recipes = (RecipesList) Misc.getRecipeList("compressorRecipes");

	/**
	 * Adds a new recipe to the compressor
	 * @param input an ItemStack representing the input of the recipe
	 * @param output an ItemStack[] representing the outputs of the recipe
	 * @param energy a double representing the energy cost of the recipe in J.
	 */
	public void addRecipe(ItemStack input, ItemStack[] output, double energy){
		if(input == null || output == null || energy < 0){
			throw new IllegalArgumentException("Invalid Arguments");
		}
		if(output.length > 4){
			throw new IllegalArgumentException("Too many recipe outputs: " + output.length);
		}
		if(energy == 0){
			energy = 0.001;
		}
		Recipe recipe = new Recipe(input, output, energy);
		recipes.addRecipe(recipe);
	}
	
	public void addRecipe(ItemStack input, ItemStack output, double energy){
		if(input == null || output == null || energy < 0){
			throw new IllegalArgumentException("Invalid Inputs");
		}
		if(energy == 0){
			energy = 0.001;
		}
		Recipe recipe = new Recipe(input, output, energy);
		recipes.addRecipe(recipe);
	}

    public void addRecipe(Recipe recipe) {
        //Maintain existing checks
        addRecipe(recipe.input, recipe.output, recipe.energy);
    }

    /**
	 * Removes a recipe from the compressor
	 * @param input an ItemStack representing the input of the recipe
	 */
	public void removeRecipe(ItemStack input){
		if(input == null){
			System.out.println("Unable to remove recipe: invalid input ItemStack");
			return;
		}
		Recipe r = recipes.getRecipe(input);
		recipes.getRecipes().remove(r);
	}
	
	/**
	 * Removes recipes from the compressor by their output ItemStacks
	 * @param output the ItemStack who's recipes will be removed
	 */
	public void removeRecipeByOutput(ItemStack output){
		if (output == null){
			System.out.println("Unable to remove recipe: invalid output ItemStack");
			return; 
		}
		ArrayList<Recipe> r = recipes.getRecipeFromOutput(output);
		for (Recipe recipe : r){
			recipes.getRecipes().remove(recipe);
		}
	}
	
	/**
	 * Returns all recipes from the compressor
	 * @return an ArrayList<Recipe> of all the machine's recipes
	 */
	public ArrayList<Recipe> getRecipes(){
		return recipes.getRecipes();
	}
}
