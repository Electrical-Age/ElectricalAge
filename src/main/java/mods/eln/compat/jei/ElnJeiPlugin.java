package mods.eln.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;

/**
 * JEI (loaded only when JEI is installed): the cable-kit recipes, which are custom recipes JEI can't list itself, as
 * examples in the crafting category (Baughn: they weren't in JEI). The grid takes whole stacks all the same.
 */
@JeiPlugin
public class ElnJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath("eln", "jei");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(RecipeTypes.CRAFTING, mods.eln.node.voxel.CableKitRecipes.examples());
    }
}
