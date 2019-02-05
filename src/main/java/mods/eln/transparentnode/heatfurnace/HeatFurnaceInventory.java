package mods.eln.transparentnode.heatfurnace;

import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

public class HeatFurnaceInventory extends TransparentNodeElementInventory {
    public HeatFurnaceInventory(int size, int stackLimit, TransparentNodeElement TransparentNodeElement) {
        super(size, stackLimit, TransparentNodeElement);
    }

    public HeatFurnaceInventory(int size, int stackLimit, TransparentNodeElementRender TransparentnodeRender) {
        super(size, stackLimit, TransparentnodeRender);
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        return new int[]{HeatFurnaceContainer.combustibleId};
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
