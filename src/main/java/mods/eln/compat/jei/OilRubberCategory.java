package mods.eln.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mods.eln.compat.JeiPages.Ing;
import mods.eln.compat.JeiPages.OilStep;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The oil route to rubber (Baughn 2026-11-09: JEI had no entries for it): Desulfuriser, Steam Cracker, Polymeriser and
 * Carbon Black Burner as machine steps, their fluids (piped in and out) and items, so looking up Raw Rubber, Sulfur Dust,
 * Carbon Black, butadiene or Soul Dust finds them. The machine is shown between the inputs and the outputs.
 */
public class OilRubberCategory extends AbstractRecipeCategory<OilStep> {
    public static final IRecipeType<OilStep> TYPE = IRecipeType.create("eln", "oil_rubber", OilStep.class);

    public OilRubberCategory(IGuiHelper gui, ItemStack icon) {
        super(TYPE, Component.translatable("jei.eln.oil_rubber"), gui.createDrawableItemStack(icon), 160, 66);
    }

    static void fill(IRecipeSlotBuilder slot, Ing ing) {
        slot.setStandardSlotBackground();
        for (ItemStack s : ing.items()) slot.add(s);
        for (var f : ing.fluids()) slot.addFluidStack(f.fluid(), f.mb());
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, OilStep r, IFocusGroup focuses) {
        for (int i = 0; i < r.in().size(); i++) fill(b.addInputSlot(1 + 20 * i, 6), r.in().get(i));
        b.addSlot(mezz.jei.api.recipe.RecipeIngredientRole.CRAFTING_STATION, 62, 6).add(r.machine());
        for (int i = 0; i < r.out().size(); i++) {
            var slot = b.addOutputSlot(120 + 20 * i, 6);
            fill(slot, r.out().get(i));
            slot.setOutputSlotBackground();
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder b, OilStep r, IFocusGroup focuses) {
        b.addRecipeArrow().setPosition(86, 6);
        Component note = Component.translatable(r.key());
        if (r.kJ() > 0) note = Component.translatable("jei.eln.oil.energy", String.format("%.0f", r.kJ())).append(" ").append(note);
        b.addText(note, 160, 40).setPosition(0, 26);
    }
}
