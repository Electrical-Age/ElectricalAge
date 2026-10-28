package mods.eln.anvil;

import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import net.minecraft.world.item.ItemStack;

/** Hoppers may top up the material (any face); the tool is the player's to put in and take out. */
public class ElectricalAnvilInventory extends TransparentNodeElementInventory {
    public ElectricalAnvilInventory(int size, int stackLimit, TransparentNodeElement element) {
        super(size, stackLimit, element);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[]{ElectricalAnvilElement.MATERIAL};
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return slot == ElectricalAnvilElement.MATERIAL && ElectricalAnvilElement.materialMayGo(stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return false;
    }
}
