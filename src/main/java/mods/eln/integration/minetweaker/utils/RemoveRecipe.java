package mods.eln.integration.minetweaker.utils;

import java.util.ArrayList;

import minetweaker.api.item.IIngredient;
import minetweaker.api.minecraft.MineTweakerMC;
import mods.eln.Eln;
import mods.eln.misc.Recipe;


public class RemoveRecipe extends BasicUndoableAction{
	IIngredient input;
	ArrayList<Recipe> toRemove;
	
	public RemoveRecipe(MinetweakerMachine machine, IIngredient input) {
		super(machine);
		this.input = input;
	}
	
	@Override
	public void apply() {
		toRemove = new ArrayList<Recipe>();
		for (Recipe r : Eln.instance.maceratorRecipes.getRecipes()){
			if(input.matches(MineTweakerMC.getIItemStack(r.input))){
				toRemove.add(r);
			}
		}
		machine.recipes.getRecipes().removeAll(toRemove);		
	}
	
	@Override
	public void undo() {
		machine.recipes.getRecipes().addAll(toRemove);
	}

}
