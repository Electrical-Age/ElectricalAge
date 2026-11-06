package mods.eln.transparentnode.wiredrawing;

import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import net.minecraft.world.item.ItemStack;

/** Hoppers may feed copper and rubber (any face) and take the finished kit or winding. */
public class WireDrawingInventory extends TransparentNodeElementInventory {
    public WireDrawingInventory(TransparentNodeElement element) {
        super(3, 64, element);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[]{WireDrawingElement.COPPER, WireDrawingElement.RUBBER, WireDrawingElement.OUT};
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return slot == WireDrawingElement.COPPER && WireDrawingElement.isCopper(stack)
            || slot == WireDrawingElement.RUBBER && WireDrawingElement.isRubber(stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return slot == WireDrawingElement.OUT;
    }
}
