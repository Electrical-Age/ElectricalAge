package mods.eln.transparentnode.electricalfurnace;

import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

public class ElectricalFurnaceInventory extends TransparentNodeElementInventory {

    public ElectricalFurnaceInventory(int size, int stackLimit, TransparentNodeElement TransparentNodeElement) {
        super(size, stackLimit, TransparentNodeElement);
    }

    public ElectricalFurnaceInventory(int size, int stackLimit, TransparentNodeElementRender TransparentnodeRender) {
        super(size, stackLimit, TransparentnodeRender);
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        switch (Direction.fromFacing(side)) {
            case YP:
                return new int[]{ElectricalFurnaceElement.inSlotId};
            default:
                return new int[]{ElectricalFurnaceElement.outSlotId};
        }

    }

    @Override
    public boolean canInsertItem(int var1, ItemStack var2, EnumFacing side) {
        switch (Direction.fromFacing(side)) {
            case YP:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean canExtractItem(int var1, ItemStack var2, EnumFacing side) {
        switch (Direction.fromFacing(side)) {
            case YP:
                return false;
            default:
                return true;
        }
    }
}
