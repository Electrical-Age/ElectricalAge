package mods.eln.gui;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** A device slot (1.12 SlotWithSkin and its subclasses), its rule from a {@link SlotSpec}. */
public class ElnSlot extends Slot {
    public final SlotSpec spec;

    /** The level the menu is open in (for a furnace input's recipe test), or null. */
    final net.minecraft.world.level.Level level;

    public ElnSlot(Container container, SlotSpec spec) {
        this(container, spec, null);
    }

    public ElnSlot(Container container, SlotSpec spec, net.minecraft.world.level.Level level) {
        super(container, spec.index(), spec.x(), spec.y());
        this.spec = spec;
        this.level = level;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if ("Smeltable".equals(spec.kind()))
            return level == null || level.recipeAccess().propertySet(net.minecraft.world.item.crafting.RecipePropertySet.FURNACE_INPUT).test(stack);
        return spec.filter().test(stack);
    }

    /** 1.12 getSlotStackLimit (the slot's own limit; 26.x also caps by the item's max stack size). */
    @Override
    public int getMaxStackSize() {
        return spec.limit();
    }
}
