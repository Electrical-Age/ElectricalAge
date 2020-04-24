package mods.eln.integration.minetweaker.utils;

import minetweaker.api.item.IIngredient;
import minetweaker.api.minecraft.MineTweakerMC;
import mods.eln.api.recipe.Recipe;

import java.util.ArrayList;


public class RemoveRecipe extends BasicUndoableAction{
	IIngredient input;
	ArrayList<Recipe> toRemove;
	
	public RemoveRecipe(MinetweakerMachine machine, IIngredient input) {
		super(machine);
		this.input = input;
        this.toRemove = new ArrayList<>();
    }
	
	@Override
	public void apply() {
        for (Recipe recipe : machine.recipes.getRecipes()) {
            if (input.matches(MineTweakerMC.getIItemStack(recipe.input))) {
                toRemove.add(recipe);
            }
        }
        for (Recipe recipe : toRemove) {
            machine.recipes.removeRecipe(recipe.input);
        }
    }
	
	@Override
	public void undo() {
        for (Recipe recipe : toRemove) {
            machine.recipes.addRecipe(recipe);
        }
        toRemove = new ArrayList<>();
    }

}
