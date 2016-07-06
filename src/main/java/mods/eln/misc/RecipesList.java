package mods.eln.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import mods.eln.Eln;
import mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceProcess;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraftforge.oredict.OreDictionary;
//import mods.eln.electricalfurnace.ElectricalFurnaceProcess;

public class RecipesList {

	public static final ArrayList<RecipesList> listOfList = new ArrayList<RecipesList>();
	
	private ArrayList<Recipe> recipeList = new ArrayList<Recipe>();
	private ArrayList<ItemStack> machineList = new ArrayList<ItemStack>();

	public RecipesList() {
		listOfList.add(this);
	}

	public ArrayList<Recipe> getRecipes(){
		return recipeList;
	}

	public ArrayList<ItemStack> getMachines(){
		return machineList;
	}

	public void addRecipe(Recipe recipe) {
		recipeList.add(recipe);
		recipe.setMachineList(machineList);
	}
	
	public void addRecipe(String input, ItemStack[] output, double energy) {
		addRecipe(input, 1, output, energy);
	}
	
	public void addRecipe(String input, ItemStack output, double energy) {
		addRecipe(input, 1, output, energy);
	}
	
	public void addRecipe(String input, int inputQuant, ItemStack[] output, double energy) {
		ArrayList<ItemStack> ores = OreDictionary.getOres(input);
		
		for (ItemStack inputStack : ores) {
			inputStack.stackSize = inputQuant;
			addRecipe(new Recipe(inputStack, output, energy));
		}
	}
	
	public void addRecipe(String input, int inputQuant, ItemStack output, double energy) {
		ArrayList<ItemStack> ores = OreDictionary.getOres(input);
		
		for (ItemStack inputStack : ores) {
			inputStack.stackSize = inputQuant;
			addRecipe(new Recipe(inputStack, output, energy));
		}
	}
	
	public void addRecipe(String input, int inputQuant, String[] output, double energy) {
		int[] quants = new int[output.length];
		for (int i = 0; i < output.length; i++) {
			quants[i] = 1;
		}
		addRecipe(input, inputQuant, output, quants, energy);
	}
	
	public void addRecipe(String input, int inputQuant, String[] output, int[] outputQuant, double energy) {
		ArrayList<ItemStack> ores = OreDictionary.getOres(input);
		ItemStack[] drops = new ItemStack[output.length];
		
		for (int i = 0; i < output.length; i++) {
			ArrayList<ItemStack> matches = OreDictionary.getOres(output[i]);
			if (matches.size() > 0) {
				drops[i] = matches.get(0);
				drops[i].stackSize = outputQuant[i];
			}
		}
		
		for (ItemStack inputStack : ores) {
			inputStack.stackSize = inputQuant;
			addRecipe(new Recipe(inputStack, drops, energy));
		}
	}
	
	public void addRecipe(String input, int inputQuant, String output, double energy) {
		addRecipe(input, inputQuant, output, 1, energy);
	}
	
	public void addRecipe(String input, int inputQuant, String output, int outputQuant, double energy) {
		ArrayList<ItemStack> drops = OreDictionary.getOres(output);
		ItemStack drop = null;
		if (drops.size() > 0) {
			drop = drops.get(0);
			drop.stackSize = outputQuant;
		}
		addRecipe(input, inputQuant, drop, energy);
	}
	
	public void addMachine(ItemStack machine) {
		machineList.add(machine);
	}

	public Recipe getRecipe(ItemStack input) {
		for(Recipe r : recipeList) {
			if(r.canBeCraftedBy(input)) return r;
		}
		return null;
	}

	public ArrayList<Recipe> getRecipeFromOutput(ItemStack output) {
		ArrayList<Recipe> list = new ArrayList<Recipe>();
		for(Recipe r : recipeList) {
			for(ItemStack stack : r.getOutputCopy()) {
				if(Utils.areSame(stack, output)) {
					list.add(r);
					break;
				}
			}
		}
		return list;
	}

	public static ArrayList<Recipe> getGlobalRecipeWithOutput(ItemStack output) {
		output = output.copy();
		output.stackSize = 1;
		ArrayList<Recipe> list = new ArrayList<Recipe>();
		for(RecipesList recipesList : listOfList) {
			list.addAll(recipesList.getRecipeFromOutput(output));
		}
		
		FurnaceRecipes furnaceRecipes = FurnaceRecipes.smelting();

		{
			Iterator it = furnaceRecipes.getSmeltingList().entrySet().iterator();
		    while (it.hasNext()) {
		    	try {
			        Map.Entry pairs = (Map.Entry)it.next();
					Recipe recipe; // List<Integer>, ItemStack
					ItemStack stack = (ItemStack)pairs.getValue();
					ItemStack li = (ItemStack)pairs.getKey();
					if(Utils.areSame(output,stack)) {
						list.add(recipe = new Recipe(li.copy(), output, ElectricalFurnaceProcess.energyNeededPerSmelt));
						recipe.setMachineList(Eln.instance.furnaceList);
					}					
				} catch (Exception e) {
					// TODO: handle exception
				}
		    }
		}

		return list;
	}

	public static ArrayList<Recipe> getGlobalRecipeWithInput(ItemStack input) {
		input = input.copy();
		input.stackSize = 64;
		ArrayList<Recipe> list = new ArrayList<Recipe>();
		for(RecipesList recipesList : listOfList) {
			Recipe r = recipesList.getRecipe(input);
			if(r != null)
				list.add(r);
		}
		
		FurnaceRecipes furnaceRecipes = FurnaceRecipes.smelting();
		ItemStack smeltResult = furnaceRecipes.getSmeltingResult(input);
		Recipe smeltRecipe;
		if(smeltResult != null) {
			try {
				ItemStack input1 = input.copy();
				input1.stackSize = 1;
				list.add(smeltRecipe = new Recipe(input1, smeltResult, ElectricalFurnaceProcess.energyNeededPerSmelt));
				smeltRecipe.machineList.addAll(Eln.instance.furnaceList);				
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		
		return list;
	}
}
/*		FurnaceRecipes.smelting().addSmelting(in.itemID, in.getItemDamage(),
				findItemStack("Copper ingot"), 0);*/
