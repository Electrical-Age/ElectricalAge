package mods.eln.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mods.eln.misc.Recipe;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * An EA machine's recipes (macerator, compressor, plate machine, magnetiser, separator; Baughn: no machine recipes
 * were in JEI): one category per recipe list, its machines (every voltage) as the catalysts. Input (with its ore-tag
 * alternatives), outputs, and the energy a run takes.
 */
public class MachineCategory extends AbstractRecipeCategory<Recipe> {
    final String key;

    public MachineCategory(IGuiHelper gui, IRecipeType<Recipe> type, String key, ItemStack icon) {
        super(type, Component.translatable("jei.eln.machine." + key), gui.createDrawableItemStack(icon), 140, 40);
        this.key = key;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, Recipe r, IFocusGroup focuses) {
        var in = b.addInputSlot(6, 6).setStandardSlotBackground();
        in.add(r.input.copy());
        for (var t : r.inputTags)
            for (var h : net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(t)) {
                ItemStack s = new ItemStack(h.value(), r.input.getCount());
                if (!s.is(r.input.getItem())) in.add(s);
            }
        for (int i = 0; i < r.output.length; i++) b.addOutputSlot(62 + 20 * i, 6).setOutputSlotBackground().add(r.output[i].copy());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder b, Recipe r, IFocusGroup focuses) {
        b.addRecipeArrow().setPosition(30, 6);
        String e = mods.eln.misc.Utils.plotEnergy("", r.energy).trim();
        b.addText(Component.literal(e + (r.extraWeights != null ? "  " + Component.translatable("jei.eln.machine.extra").getString() : "")), 140, 12).setPosition(0, 28);
    }
}
