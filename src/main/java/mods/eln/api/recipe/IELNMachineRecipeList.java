package mods.eln.api.recipe;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;

/**
 * Interface for ELN machines to use in the API
 * @author bloxgate
 *
 */
public interface IELNMachineRecipeList {


    void addRecipe(ItemStack input, ItemStack[] output, double energy) throws IllegalArgumentException;

    void addRecipe(ItemStack input, ItemStack output, double energy) throws IllegalArgumentException;

    void addRecipe(Recipe recipe);

    void removeRecipe(ItemStack input);

    void removeRecipeByOutput(ItemStack output);

    ArrayList<Recipe> getRecipes();


}
