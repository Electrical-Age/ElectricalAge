package mods.eln.transparentnode.electricalmachine;

import mods.eln.misc.Recipe;
import mods.eln.misc.RecipesList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The 230V compressor's recipes (the rubber chain, 2026-11-07): the compressors' shared list, plus "8 dandelions ->
 * 250 mB latex" (Baughn: a simple recipe for now; robotic dandelion farming later), which only the 230V has, as only it
 * has a latex outlet. The latex goes to its tank (CompressorPortsElement); the recipe's item output is empty.
 */
public class PressRecipes extends RecipesList {
    final RecipesList base;
    public static final int DANDELIONS = 8;
    public static final double DANDELION_JOULES = 4000;
    private Recipe dandelions;

    public PressRecipes(RecipesList base) {
        this.base = base;
    }

    /** The dandelion recipe (made on first use: items exist by then). */
    public Recipe dandelions() {
        if (dandelions == null) dandelions = new Recipe(new ItemStack(Items.DANDELION, DANDELIONS), new ItemStack[0], DANDELION_JOULES);
        return dandelions;
    }

    @Override
    public Recipe getRecipe(ItemStack input) {
        if (dandelions().canBeCraftedBy(input)) return dandelions;
        return base.getRecipe(input);
    }

    @Override
    public boolean accepts(ItemStack input) {
        return dandelions().canBeCraftedBy(input) || base.accepts(input);
    }

    @Override
    public void addMachine(ItemStack machine) {
        super.addMachine(machine);
        base.addMachine(machine);   // (it's a compressor too: JEI's compressor page lists it)
    }
}
