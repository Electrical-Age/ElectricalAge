package mods.eln.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.item.ItemStack;

public class RecipesList {
    private ArrayList<Recipe> recipeList = new ArrayList<Recipe>();

	public RecipesList() {
		// TODO Auto-generated constructor stub
	}

	public void addRecipe(Recipe recipe)
	{
		recipeList.add(recipe);
	}
	
	
	public Recipe getRecipe(ItemStack input)
	{
		for(Recipe r : recipeList)
		{
			if(r.canBeCraftedBy(input)) return r;
		}
		return null;
	}
	
}
