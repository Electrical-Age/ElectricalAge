package mods.eln.gridnode.electricalpole;

import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import net.minecraft.world.item.ItemStack;

/** A utility pole's equipment: one item a slot, each slot only its own kind; no hopper access. */
public class PoleInventory extends TransparentNodeElementInventory {
    public PoleInventory(TransparentNodeElement element) {
        super(ElectricalPoleElement.SLOTS, 1, element);
    }

    /** May this stack go in this slot? */
    public static boolean fits(int slot, ItemStack s) {
        if (slot == ElectricalPoleElement.TRANSFORMER) return PoleTransformerItem.is(s);
        if (slot == ElectricalPoleElement.ANCHOR) return PoleGearItem.is(s, PoleGearItem.Kind.ANCHOR);
        return slot >= ElectricalPoleElement.LAMP0 && slot < ElectricalPoleElement.SLOTS && PoleGearItem.is(s, PoleGearItem.Kind.STREETLIGHT);
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        return fits(i, itemstack);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[0];
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return false;
    }
}
