package mods.eln.datagen;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.item.crafting.Recipe;

/** Datagen: the EA crafting and smelting recipes (ElnRecipes) as recipe + advancement JSON. */
public class ElnRecipeProvider extends RecipeProvider {
    public ElnRecipeProvider(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
        super(recipes, advancements);
    }

    @Override
    protected void buildRecipes() {
        // the bootstrap may run more than once: start from scratch
        ElnRecipesBase.problems.clear();
        ElnRecipesBase.fixes.clear();
        ElnRecipesBase.skipped.clear();
        ElnRecipesBase.nameUse.clear();
        ElnRecipesBase.smelted.clear();
        ElnRecipesBase.craftingAttempted = ElnRecipesBase.craftingRegistered = ElnRecipesBase.smeltingAdded = 0;
        ElnRecipesBase.items = this.items;
        ElnRecipesBase.out = this.output;
        ElnRecipesBase.hasItem = this::has;
        ElnRecipesBase.hasTag = this::has;
        ElnRecipes.registerAll();
        mods.eln.Eln.LOG.info("Electrical Age recipes: crafting {}/{}, smelting {}; 1.7.10 pattern fixes {}; skipped {}",
            ElnRecipesBase.craftingRegistered, ElnRecipesBase.craftingAttempted, ElnRecipesBase.smeltingAdded,
            ElnRecipesBase.fixes.size(), ElnRecipesBase.skipped);
        for (String f : ElnRecipesBase.fixes) mods.eln.Eln.LOG.info("recipe fix: {}", f);
        if (!ElnRecipesBase.problems.isEmpty())
            throw new IllegalStateException("Electrical Age: " + ElnRecipesBase.problems.size() + " recipe problem(s):\n  "
                + String.join("\n  ", ElnRecipesBase.problems));
    }
}
