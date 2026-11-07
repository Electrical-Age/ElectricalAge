package mods.eln.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import mods.eln.compat.JeiPages.Anneal;
import net.minecraft.world.item.ItemStack;

/**
 * Heat treatments in the Electrical Furnace: Silicon Steel Laminations become an Average Ferromagnetic Core
 * (Baughn 2026-11-06), Rubber Compound vulcanises to Rubber (2026-11-07), each in a window of temperature
 * (ElectricalFurnaceProcess.TREATMENTS). Not a vanilla smelting recipe, so JEI had nothing for the
 * core (AdventurAgent): this shows it.
 */
public class AnnealCategory extends AbstractRecipeCategory<Anneal> {
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
        // (Baughn: snappy; the furnace is the page's catalyst, and the laminations' tooltip tells of spoiling)
        b.addText(Component.translatable("jei.eln.anneal.how", (int) r.minC(), (int) r.maxC(), (int) r.kJ()), 150, 32).setPosition(0, 28);
    }
}
