package mods.eln.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Tree resin comes from a Tree Resin Collector on the side of a log (Baughn: JEI should show that as a machine). Not a
 * recipe in the game: the collector fills slowly, if the log's trunk has leaves over it (TreeResinCollectorElement).
 */
public class ResinCategory extends AbstractRecipeCategory<ResinCategory.Resin> {
    /** Any log, the collector, the resin. */
    public record Resin(ItemStack collector, ItemStack resin) {
    }

    public static final IRecipeType<Resin> TYPE = IRecipeType.create("eln", "tree_resin_collector", Resin.class);

    public ResinCategory(IGuiHelper gui, ItemStack collector) {
        super(TYPE, Component.translatable("jei.eln.tree_resin_collector"), gui.createDrawableItemStack(collector), 140, 50);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, Resin r, IFocusGroup focuses) {
        b.addInputSlot(1, 6).setStandardSlotBackground().add(net.minecraft.world.item.crafting.Ingredient.of(
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getOrThrow(net.minecraft.tags.ItemTags.LOGS)));
        b.addInputSlot(23, 6).setStandardSlotBackground().add(r.collector());
        b.addOutputSlot(75, 6).setOutputSlotBackground().add(r.resin());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder b, Resin r, IFocusGroup focuses) {
        b.addRecipeArrow().setPosition(46, 6);
        b.addText(Component.translatable("jei.eln.tree_resin_collector.how"), 140, 22).setPosition(0, 28);
    }
}
