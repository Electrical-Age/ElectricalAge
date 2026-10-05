package mods.eln.misc;

import mods.eln.Eln;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
//import mods.eln.electricalfurnace.ElectricalFurnaceProcess;

public class RecipesList {
    /** Was ElectricalFurnaceProcess.energyNeededPerSmelt (device); that constant now refers to this one. */
    public static final double electricalFurnaceEnergyPerSmelt = 1000;


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
            Iterator it = furnaceRecipes.getSmeltingList().entrySet().iterator();
            while (it.hasNext()) {
                try {
                    Map.Entry pairs = (Map.Entry) it.next();
                    Recipe recipe; // List<Integer>, ItemStack
                    ItemStack stack = (ItemStack) pairs.getValue();
                    ItemStack li = (ItemStack) pairs.getKey();
                    if (Utils.areSame(output, stack)) {
                        list.add(recipe = new Recipe(li.copy(), output, electricalFurnaceEnergyPerSmelt));
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
            try {
                ItemStack input1 = input.copy();
                input1.setCount(1);
                list.add(smeltRecipe = new Recipe(input1, smeltResult, electricalFurnaceEnergyPerSmelt));
                smeltRecipe.machineList.addAll(Eln.instance.furnaceList);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }

        return list;
    }
}
/*		FurnaceRecipes.instance().addSmelting(in.itemID, in.getMetadata(),
                findItemStack("Copper ingot"), 0);*/
