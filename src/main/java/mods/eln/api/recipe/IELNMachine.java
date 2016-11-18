package mods.eln.api.recipe;

import java.util.ArrayList;

import net.minecraft.item.ItemStack;
import mods.eln.misc.RecipesList;

/**
 * Interface for ELN machines to use in the API
 * @author bloxgate
 *
 */
public interface IELNMachine {

	public static RecipesList recipeList = null;
	
	public void addRecipe(ItemStack input, ItemStack[] output, double energy) throws IllegalArgumentException;
	
	public void addRecipe(ItemStack input, ItemStack output, double energy) throws IllegalArgumentException;
	
	public void removeRecipe(ItemStack input);
	
	public void removeRecipeByOutput(ItemStack output);
	
	public ArrayList<Recipe> getRecipes();
	
	
	
}
