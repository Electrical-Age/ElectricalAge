package mods.eln.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.item.ItemStack;

public class RecipesList {
	public static ArrayList<RecipesList> listOfList = new ArrayList<RecipesList>();
	
	private ArrayList<Recipe> recipeList = new ArrayList<Recipe>();
	private ArrayList<ItemStack> machineList = new ArrayList<ItemStack>();

	public RecipesList() {

		listOfList.add(this);
	}

	public void addRecipe(Recipe recipe)
	{
		recipeList.add(recipe);
		recipe.setMachineList(machineList);
	}
	
	public void addMachine(ItemStack machine)
	{
		machineList.add(machine);
	}
	
	
	public Recipe getRecipe(ItemStack input)
	{
		for(Recipe r : recipeList)
		{
			if(r.canBeCraftedBy(input)) return r;
		}
		return null;
	}
	public ArrayList<Recipe> getRecipeFromOutput(ItemStack output)
	{
		ArrayList<Recipe> list = new ArrayList<Recipe>();
		for(Recipe r : recipeList)
		{
			for(ItemStack stack : r.getOutputCopy()){
				if(stack.itemID == output.itemID && stack.getItemDamage() == output.getItemDamage()){
					list.add(r);
					break;
				}
			}
			
		}
		return list;
	}
		
	
	public static ArrayList<Recipe> getGlobalRecipeWithOutput(ItemStack output)
	{
		output = output.copy();
		output.stackSize = 1;
		ArrayList<Recipe> list = new ArrayList<Recipe>();
		for(RecipesList recipesList : listOfList){
			list.addAll(recipesList.getRecipeFromOutput(output));
		}
		return list;
	}
	public static ArrayList<Recipe> getGlobalRecipeWithInput(ItemStack input)
	{
		input = input.copy();
		input.stackSize = 64;
		ArrayList<Recipe> list = new ArrayList<Recipe>();
		for(RecipesList recipesList : listOfList){
			Recipe r = recipesList.getRecipe(input);
			if(r != null)
				list.add(r);
		}
		return list;
	}
}
