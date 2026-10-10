package mods.eln.gui;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** A device slot (1.12 SlotWithSkin and its subclasses), its rule from a {@link SlotSpec}. */
public class ElnSlot extends Slot {
    public final SlotSpec spec;

    public ElnSlot(Container container, SlotSpec spec) {
        super(container, spec.index(), spec.x(), spec.y());
        this.spec = spec;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return spec.filter().test(stack);
    }

    /** 1.12 getSlotStackLimit (the slot's own limit; 26.x also caps by the item's max stack size). */
    @Override
    public int getMaxStackSize() {
        return spec.limit();
    }
}
