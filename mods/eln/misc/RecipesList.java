package mods.eln.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import mods.eln.Eln;
import mods.eln.electricalfurnace.ElectricalFurnaceProcess;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraftforge.oredict.OreDictionary;

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
				if(Utils.areSame(stack,output)){
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
		FurnaceRecipes furnaceRecipes = FurnaceRecipes.smelting();
		for(Entry<List<Integer>, ItemStack> entry : furnaceRecipes.getMetaSmeltingList().entrySet()){
			Recipe recipe;
			if(Utils.areSame(output,entry.getValue())){
				list.add(recipe = new Recipe(new ItemStack(entry.getKey().get(0),1,entry.getKey().get(1)), output, ElectricalFurnaceProcess.energyNeededPerSmelt));
				recipe.setMachineList(Eln.instance.furnaceList);
			}
		}
		for(Object entry : furnaceRecipes.getSmeltingList().entrySet()){
			Recipe recipe = null;
			Entry<Integer, Object> e = (Entry<Integer, Object>)entry;
			if(((ItemStack)e.getValue()).itemID == output.itemID){
				list.add(recipe = new Recipe(new ItemStack(e.getKey(),1,0), output, ElectricalFurnaceProcess.energyNeededPerSmelt));
				recipe.setMachineList(Eln.instance.furnaceList);
			}
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
		
		FurnaceRecipes furnaceRecipes = FurnaceRecipes.smelting();
		ItemStack smeltResult = furnaceRecipes.getSmeltingResult(input);
		Recipe smeltRecipe;
		if(smeltResult != null) {
			ItemStack input1 = input.copy();
			input1.stackSize = 1;
			list.add(smeltRecipe = new Recipe(input1, smeltResult, ElectricalFurnaceProcess.energyNeededPerSmelt));
			smeltRecipe.machineList.addAll(Eln.instance.furnaceList);
		}
		
		return list;
	}
}
/*		FurnaceRecipes.smelting().addSmelting(in.itemID, in.getItemDamage(),
				findItemStack("Copper ingot"), 0);*/
