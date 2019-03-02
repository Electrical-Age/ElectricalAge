package mods.eln.misc;

import mods.eln.Eln;
import mods.eln.init.Recipes;
import mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceProcess;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
//import mods.eln.electricalfurnace.ElectricalFurnaceProcess;

public class RecipesList {

    public static final ArrayList<RecipesList> listOfList = new ArrayList<RecipesList>();

    private ArrayList<Recipe> recipeList = new ArrayList<Recipe>();
    private ArrayList<ItemStack> machineList = new ArrayList<ItemStack>();

    public RecipesList() {
        listOfList.add(this);
    }

    public ArrayList<Recipe> getRecipes() {
        return recipeList;
    }

    public ArrayList<ItemStack> getMachines() {
        return machineList;
    }

    public void addRecipe(Recipe recipe) {
        recipeList.add(recipe);
        recipe.setMachineList(machineList);
    }

    public void addMachine(ItemStack machine) {
        machineList.add(machine);
    }

    public Recipe getRecipe(ItemStack input) {
        for (Recipe r : recipeList) {
            if (r.canBeCraftedBy(input)) return r;
        }
        return null;
    }

    public ArrayList<Recipe> getRecipeFromOutput(ItemStack output) {
        ArrayList<Recipe> list = new ArrayList<Recipe>();
        for (Recipe r : recipeList) {
            for (ItemStack stack : r.getOutputCopy()) {
                if (Utils.areSame(stack, output)) {
                    list.add(r);
                    break;
                }
            }
        }
        return list;
    }

    public static ArrayList<Recipe> getGlobalRecipeWithOutput(ItemStack output) {
        output = output.copy();
        output.setCount(1);
        ArrayList<Recipe> list = new ArrayList<Recipe>();
        for (RecipesList recipesList : listOfList) {
            list.addAll(recipesList.getRecipeFromOutput(output));
        }

        FurnaceRecipes furnaceRecipes = FurnaceRecipes.instance();

        {
            for (Map.Entry<ItemStack, ItemStack> itemStackItemStackEntry : furnaceRecipes.getSmeltingList().entrySet()) {
                Recipe recipe;
                ItemStack stack = (ItemStack) ((Map.Entry) itemStackItemStackEntry).getValue();
                ItemStack li = (ItemStack) ((Map.Entry) itemStackItemStackEntry).getKey();
                if (Utils.areSame(output, stack)) {
                    list.add(recipe = new Recipe(li.copy(), output, ElectricalFurnaceProcess.energyNeededPerSmelt));
                    recipe.setMachineList(Recipes.furnaceList);
                }
            }
        }

        return list;
    }

    public static ArrayList<Recipe> getGlobalRecipeWithInput(ItemStack input) {
        input = input.copy();
        input.setCount(64);
        ArrayList<Recipe> list = new ArrayList<Recipe>();
        for (RecipesList recipesList : listOfList) {
            Recipe r = recipesList.getRecipe(input);
            if (r != null)
                list.add(r);
        }

        FurnaceRecipes furnaceRecipes = FurnaceRecipes.instance();
        ItemStack smeltResult = furnaceRecipes.getSmeltingResult(input);
        Recipe smeltRecipe;
        if (!smeltResult.isEmpty()) {
            ItemStack input1 = input.copy();
            input1.setCount(1);
            list.add(smeltRecipe = new Recipe(input1, smeltResult, ElectricalFurnaceProcess.energyNeededPerSmelt));
            smeltRecipe.machineList.addAll(Recipes.furnaceList);
        }

        return list;
    }
}
