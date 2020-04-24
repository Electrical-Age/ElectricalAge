package mods.eln.integration.minetweaker.utils;

import mods.eln.api.recipe.Recipe;

public class AddRecipe extends BasicUndoableAction {
	Recipe recipe;

	public AddRecipe(MinetweakerMachine machine, Recipe recipe) {
		super(machine);
		this.recipe = recipe;
	}

	@Override
	public void apply() {
		machine.recipes.addRecipe(recipe);
	}

	@Override
	public void undo() {
        machine.recipes.removeRecipe(recipe.input);
    }

}
