package mods.eln.transparentnode.eggincubator;

import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import net.minecraft.world.item.ItemStack;

public class EggIncubatorInventory extends TransparentNodeElementInventory {

    public EggIncubatorInventory(int size, int stackLimit, TransparentNodeElement TransparentNodeElement) {
        super(size, stackLimit, TransparentNodeElement);
    }
// 26.x port: EggIncubatorInventory() removed (client rendering; redone in P3)

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[]{EggIncubatorContainer.EggSlotId};
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        // eggs only (the GUI's egg slot filter)
        return slot == EggIncubatorContainer.EggSlotId && stack.is(net.minecraft.world.item.Items.EGG);
    }

    @Override
    public boolean canExtractItem(int var1, ItemStack var2, int var3) {
        return false;
    }
}
