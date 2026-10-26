package mods.eln.misc;

import mods.eln.Eln;
import net.minecraft.world.item.ItemStack;

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
        return fallback != null && !Utils.isEmpty(input) ? fallback.apply(input) : null;
    }

    /** For an input no recipe takes: a recipe made for it, or null (the macerator's ore loot: OreLootRecipe.of). */
    public java.util.function.Function<ItemStack, Recipe> fallback;
    /** Where fallback can't tell (a client without the server's loot tables): may it take this input? */
    public java.util.function.Predicate<ItemStack> fallbackMaybe;

    /** May a machine of this list take the input (its input slot's filter)? */
    public boolean accepts(ItemStack input) {
        return getRecipe(input) != null || fallbackMaybe != null && fallbackMaybe.test(input);
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

        // 26.x: smelting recipes live in the server's recipe manager
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (net.minecraft.world.item.crafting.RecipeHolder<?> h : server.getRecipeManager().getRecipes()) {
                if (!(h.value() instanceof net.minecraft.world.item.crafting.SmeltingRecipe r)) continue;
                for (net.minecraft.core.Holder<net.minecraft.world.item.Item> in : r.input().items().toList()) {
                    ItemStack li = new ItemStack(in);
                    ItemStack stack = Utils.smeltResult(server.overworld(), li);
                    if (Utils.areSame(output, stack)) {
                        Recipe recipe;
                        list.add(recipe = new Recipe(li, output, electricalFurnaceEnergyPerSmelt));
                        recipe.setMachineList(Eln.furnaceList);
                    }
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

        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        ItemStack smeltResult = server == null ? ItemStack.EMPTY : Utils.smeltResult(server.overworld(), input);
        Recipe smeltRecipe;
        if (!smeltResult.isEmpty()) {
            try {
                ItemStack input1 = input.copy();
                input1.setCount(1);
                list.add(smeltRecipe = new Recipe(input1, smeltResult, electricalFurnaceEnergyPerSmelt));
                smeltRecipe.machineList.addAll(Eln.furnaceList);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }

        return list;
    }
}
/*		FurnaceRecipes.instance().addSmelting(in.itemID, in.getMetadata(),
                findItemStack("Copper ingot"), 0);*/
