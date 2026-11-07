package mods.eln.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mods.eln.compat.JeiPages.Draw;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;


/**
 * The wire-drawing machines (Baughn 2026-11-06, Wires stage 4): copper and rubber into cable kits and windings. The
 * spec is set on the machine's screen, so the page shows examples (each machine's presets, a 48 -> 230 V converter's
 * winding, an 11 kV one) with what they take; clicking a Winding or a kit finds this page.
 */
public class WireDrawingCategory extends AbstractRecipeCategory<Draw> {
    public static final IRecipeType<Draw> TYPE = IRecipeType.create("eln", "wire_drawing", Draw.class);

    public WireDrawingCategory(IGuiHelper gui, ItemStack machine) {
        super(TYPE, Component.translatable("jei.eln.wire_drawing"), gui.createDrawableItemStack(machine), 170, 54);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, Draw r, IFocusGroup focuses) {
        b.addInputSlot(1, 6).setStandardSlotBackground().add(r.copper());
        var rubber = b.addInputSlot(27, 6).setStandardSlotBackground();
        if (!r.rubber().isEmpty()) rubber.add(r.rubber());
        b.addOutputSlot(101, 6).setOutputSlotBackground().add(r.out());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder b, Draw r, IFocusGroup focuses) {
        b.addRecipeArrow().setPosition(64, 6);
        b.addText(Component.translatable(r.small() ? "jei.eln.wire_drawing.either" : "jei.eln.wire_drawing.big", String.format("%.0f", r.kJ())), 170, 10)
            .setPosition(0, 28);
        b.addText(Component.translatable("jei.eln.wire_drawing.how"), 170, 10).setPosition(0, 40);
    }
}
