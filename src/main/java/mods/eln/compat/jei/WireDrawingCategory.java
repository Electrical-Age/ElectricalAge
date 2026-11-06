package mods.eln.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mods.eln.Eln;
import mods.eln.node.voxel.CableKitItem;
import mods.eln.node.voxel.CableSpec;
import mods.eln.node.voxel.WindingItem;
import mods.eln.transparentnode.wiredrawing.WireDrawingDescriptor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * The wire-drawing machines (Baughn 2026-11-06, Wires stage 4): copper and rubber into cable kits and windings. The
 * spec is set on the machine's screen, so the page shows examples (each machine's presets, a 48 -> 230 V converter's
 * winding, an 11 kV one) with what they take; clicking a Winding or a kit finds this page.
 */
public class WireDrawingCategory extends AbstractRecipeCategory<WireDrawingCategory.Draw> {
    /** An example: what it takes (whole items), what comes out, and which machine makes it. */
    public record Draw(ItemStack copper, ItemStack rubber, ItemStack out, double kJ, boolean small) {
    }

    public static final IRecipeType<Draw> TYPE = IRecipeType.create("eln", "wire_drawing", Draw.class);

    public WireDrawingCategory(IGuiHelper gui, ItemStack machine) {
        super(TYPE, Component.translatable("jei.eln.wire_drawing"), gui.createDrawableItemStack(machine), 150, 54);
    }

    /** An example of blocks of wire of a spec: a kit (turns 0) or a winding of turns. */
    static Draw example(int tier, CableSpec spec, int blocks, int turns) {
        double len = turns > 0 ? turns * WindingItem.BLOCKS_PER_TURN : blocks;
        int cu = (int) Math.ceil(len * WireDrawingDescriptor.copperPerBlock(spec.mm2()) - 1e-9);
        int ru = (int) Math.ceil(len * WireDrawingDescriptor.rubberPerBlock(tier, spec.mm2()) - 1e-9);
        ItemStack out = turns > 0 ? WindingItem.make(tier, spec, turns) : CableKitItem.kit(tier, spec, blocks * 16);
        return new Draw(new ItemStack(Items.COPPER_INGOT, Math.max(1, cu)), new ItemStack(Eln.itemsByName.get("Rubber"), Math.max(1, ru)), out,
            len * WireDrawingDescriptor.energyPerBlock(spec.mm2()) / 1000, tier <= 2 && spec.mm2() <= 6);
    }

    static List<Draw> examples() {
        List<Draw> l = new ArrayList<>();
        for (int tier = 1; tier <= 4; tier++) l.add(example(tier, CableSpec.preset(tier), 16, 0));
        l.add(example(1, new CableSpec(6, CableSpec.BROWN), 16, 0));
        l.add(example(2, CableSpec.preset(2), 0, 58));    // a 48 -> 230 V converter's secondary on a cheap core
        l.add(example(4, CableSpec.preset(4), 0, 688));   // 400 V -> 11 kV on an optimal core
        return l;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, Draw r, IFocusGroup focuses) {
        b.addInputSlot(1, 6).setStandardSlotBackground().add(r.copper());
        b.addInputSlot(19, 6).setStandardSlotBackground().add(r.rubber());
        b.addOutputSlot(85, 6).setOutputSlotBackground().add(r.out());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder b, Draw r, IFocusGroup focuses) {
        b.addRecipeArrow().setPosition(50, 6);
        b.addText(Component.translatable(r.small() ? "jei.eln.wire_drawing.either" : "jei.eln.wire_drawing.big", String.format("%.0f", r.kJ())), 150, 10)
            .setPosition(0, 28);
        b.addText(Component.translatable("jei.eln.wire_drawing.how"), 150, 10).setPosition(0, 40);
    }
}
