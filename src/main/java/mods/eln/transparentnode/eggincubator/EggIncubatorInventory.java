package mods.eln.transparentnode.eggincubator;

import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

public class EggIncubatorInventory extends TransparentNodeElementInventory {

    public EggIncubatorInventory(int size, int stackLimit, TransparentNodeElement TransparentNodeElement) {
        super(size, stackLimit, TransparentNodeElement);
    }

    public EggIncubatorInventory(int size, int stackLimit, TransparentNodeElementRender TransparentnodeRender) {
        super(size, stackLimit, TransparentnodeRender);
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        return new int[]{EggIncubatorContainer.EggSlotId};
    }

    @Override
    public boolean canInsertItem(int var1, ItemStack var2, EnumFacing var3) {
        return true;
    }

    @Override
    public boolean canExtractItem(int var1, ItemStack var2, EnumFacing var3) {
        return false;
    }
}
