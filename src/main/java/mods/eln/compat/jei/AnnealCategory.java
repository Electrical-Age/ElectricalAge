package mods.eln.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceProcess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Annealing in the Electrical Furnace (Baughn 2026-11-06): Silicon Steel Laminations become an Average Ferromagnetic
 * Core in a window of temperature (ElectricalFurnaceProcess). Not a vanilla smelting recipe, so JEI had nothing for the
 * core (AdventurAgent): this shows it.
 */
public class AnnealCategory extends AbstractRecipeCategory<AnnealCategory.Anneal> {
    public record Anneal(ItemStack in, ItemStack out) {
    }

    public static final IRecipeType<Anneal> TYPE = IRecipeType.create("eln", "anneal", Anneal.class);

    public AnnealCategory(IGuiHelper gui, ItemStack furnace) {
        super(TYPE, Component.translatable("jei.eln.anneal"), gui.createDrawableItemStack(furnace), 150, 60);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, Anneal r, IFocusGroup focuses) {
        b.addInputSlot(1, 6).setStandardSlotBackground().add(r.in());
        b.addOutputSlot(75, 6).setOutputSlotBackground().add(r.out());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder b, Anneal r, IFocusGroup focuses) {
        b.addRecipeArrow().setPosition(40, 6);
        b.addText(Component.translatable("jei.eln.anneal.how", (int) ElectricalFurnaceProcess.ANNEAL_MIN_C, (int) ElectricalFurnaceProcess.ANNEAL_MAX_C,
            (int) (ElectricalFurnaceProcess.ANNEAL_ENERGY / 1000), (int) ElectricalFurnaceProcess.ANNEAL_SPOIL_C), 150, 32).setPosition(0, 28);
    }
}
