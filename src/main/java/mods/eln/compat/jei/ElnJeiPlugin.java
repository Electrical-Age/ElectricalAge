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

    /** The machines' recipe lists, by key (MachineCategory). */
    static final java.util.Map<String, java.util.function.Supplier<mods.eln.misc.RecipesList>> MACHINES = new java.util.LinkedHashMap<>();
    static final java.util.Map<String, mezz.jei.api.recipe.types.IRecipeType<mods.eln.misc.Recipe>> TYPES = new java.util.LinkedHashMap<>();

    static {
        MACHINES.put("macerator", () -> Eln.maceratorRecipes);
        MACHINES.put("compressor", () -> Eln.compressorRecipes);
        MACHINES.put("plate_machine", () -> Eln.plateMachineRecipes);
        MACHINES.put("magnetiser", () -> Eln.magnetiserRecipes);
        MACHINES.put("separator", () -> Eln.separatorRecipes);
        for (String k : MACHINES.keySet()) TYPES.put(k, mezz.jei.api.recipe.types.IRecipeType.create("eln", k, mods.eln.misc.Recipe.class));
    }

    /**
     * The machines' recipe lists are filled when a server starts (ElnMachineRecipes, after the registries are bound), so a
     * client on a dedicated server had none, and no machine list either: JEI showed no EA machine recipes (Baughn, on
     * Incognito; it worked in single player). JEI runs once the client has joined and its registries are bound: fill them
     * here if they're empty.
     */
    static void ensureRecipes() {
        mods.eln.registry.ElnRegistry.afterRegistration();
        if (Eln.maceratorRecipes.getRecipes().isEmpty()) mods.eln.registry.ElnMachineRecipes.registerAll();
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        ensureRecipes();
        for (var e : MACHINES.entrySet()) {
            var list = e.getValue().get();
            if (list.getMachines().isEmpty() || list.getRecipes().isEmpty()) continue;
            registration.addRecipeCategories(new MachineCategory(registration.getJeiHelpers().getGuiHelper(), TYPES.get(e.getKey()), e.getKey(),
                list.getMachines().get(0)));
        }
        ItemStack collector = Eln.findItemStack("Tree Resin Collector", 1);
        if (!collector.isEmpty()) registration.addRecipeCategories(new ResinCategory(registration.getJeiHelpers().getGuiHelper(), collector));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ensureRecipes();
        registration.addRecipes(RecipeTypes.CRAFTING, mods.eln.node.voxel.CableKitRecipes.examples());
        registration.addRecipes(RecipeTypes.CRAFTING, mods.eln.node.voxel.PipeKitRecipes.examples());
        for (var e : MACHINES.entrySet()) {
            var list = e.getValue().get();
            if (!list.getMachines().isEmpty() && !list.getRecipes().isEmpty()) registration.addRecipes(TYPES.get(e.getKey()), list.getRecipes());
        }
        ItemStack collector = Eln.findItemStack("Tree Resin Collector", 1), resin = Eln.findItemStack("Tree Resin", 1);
        if (!collector.isEmpty() && !resin.isEmpty())
            registration.addRecipes(ResinCategory.TYPE, List.of(new ResinCategory.Resin(collector, resin)));
    }

    /** The bike trunk's panel lies right of the trunk's rectangle: JEI keeps its ingredient list off it (AdventurAgent). */
    @Override
    public void registerGuiHandlers(mezz.jei.api.registration.IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(mods.eln.client.gui.BikeTrunkScreen.class,
            new mezz.jei.api.gui.handlers.IGuiContainerHandler<mods.eln.client.gui.BikeTrunkScreen>() {
                @Override
                public List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(mods.eln.client.gui.BikeTrunkScreen screen) {
                    return screen.extraAreas();
                }
            });
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (var e : MACHINES.entrySet()) {
            var list = e.getValue().get();
            if (list.getRecipes().isEmpty()) continue;
            for (ItemStack m : list.getMachines()) registration.addCraftingStation(TYPES.get(e.getKey()), m);
        }
        ItemStack collector = Eln.findItemStack("Tree Resin Collector", 1);
        if (!collector.isEmpty()) registration.addCraftingStation(ResinCategory.TYPE, collector);
    }
}
