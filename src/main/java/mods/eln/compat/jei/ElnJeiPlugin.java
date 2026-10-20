package mods.eln.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mods.eln.Eln;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * JEI (loaded only when JEI is installed): the cable-kit recipes, which are custom recipes JEI can't list itself, as
 * examples in the crafting category (Baughn: they weren't in JEI; the grid takes whole stacks all the same); and the
 * Tree Resin Collector as a machine making tree resin (ResinCategory).
 */
@JeiPlugin
public class ElnJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath("eln", "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        ItemStack collector = Eln.findItemStack("Tree Resin Collector", 1);
        if (!collector.isEmpty()) registration.addRecipeCategories(new ResinCategory(registration.getJeiHelpers().getGuiHelper(), collector));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(RecipeTypes.CRAFTING, mods.eln.node.voxel.CableKitRecipes.examples());
        ItemStack collector = Eln.findItemStack("Tree Resin Collector", 1), resin = Eln.findItemStack("Tree Resin", 1);
        if (!collector.isEmpty() && !resin.isEmpty())
            registration.addRecipes(ResinCategory.TYPE, List.of(new ResinCategory.Resin(collector, resin)));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        ItemStack collector = Eln.findItemStack("Tree Resin Collector", 1);
        if (!collector.isEmpty()) registration.addCraftingStation(ResinCategory.TYPE, collector);
    }
}
